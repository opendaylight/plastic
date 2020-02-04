/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation

import groovy.transform.CompileStatic;

/**
 * Schemiterator (excluding empty iterator) has
 * - one or more names
 * - one or more dimensions
 * - each dimension has a range 0 ... L-1
 * - merging where larger ranges and bigger dimensions win
 * - incrementable with a current value
 * - incrementing only applies to non-parent dimensions
 * - a concept of "is done", which needs to be checked FIRST to allow for zero length arrays
 * - "is done" only applies to non-parent dimension
 *
 */
@CompileStatic
class Schemiterator {

    static Map<String,Schemiterator> instantiateIterators(Map<String,Object> bindings) {
        Map<String,Schemiterator> results = new HashMap<>()

        bindings.each { k,v ->
            if (k.startsWith('_[') && k.endsWith(']')) {
                Schemiterator iterator = new Schemiterator(k, (String) v)
                results.put(iterator.fullName(), iterator)
            }
        }

        return results
    }

    static def insertIteratorSpec(Map bindings, String arrayName, List<String> array) {
        Schemiterator iterator = new Schemiterator(arrayName, (long)array.size())
        iterator.writeSpec(bindings)
    }

    private static String adorn(String varName) {
        StringBuilder sb = new StringBuilder()
        sb.append('_[')
        sb.append(varName)
        sb.append(']')
        sb.toString()
    }

    private boolean done = false
    private Set<String> names = new HashSet<>()
    private long[] ranges = new int[0]
    private long[] current = new int[0]
    private Schemiterator parent = null

    // Create an anonymous uncapped iterator that only increments the least significant digit.
    // Useful when you do not know the upper limit of the range apriori. Using max
    // values for the range causes merging to use the current values instead of
    // ranges.
    //
    Schemiterator(int dimensions) {
        initUncapped(dimensions)
        done = calculateInitialDone()
    }

    // Create an uncapped iterator from an arrayed variable name like ABC[^][*]
    // The dimensionality comes from the brackets
    //
    Schemiterator(String candidate) {
        if (candidate != null && !candidate.isEmpty()) {
            int bracket = candidate.indexOf('[')
            if (bracket > -1) {
                String baseName = candidate.substring(0, bracket)
                validateName(baseName)
                names.add(candidate)
                int dims = count(candidate, '[' as char)
                initUncapped(dims)
            }
        }
        done = calculateInitialDone()
    }

    // Create a capped iterator from an arrayed variable name like ABC[^][*] with the given ranges
    //
    Schemiterator(String candidate, long... ranges) {
        if (candidate != null && !candidate.isEmpty()) {
            int bracket = candidate.indexOf('[')
            if (bracket > -1) {
                String baseName = candidate.substring(0, bracket)
                validateName(baseName)
                names.add(candidate)
                this.ranges = (long[])ranges.clone()
                current = createZeros(ranges.length)

                int dims = count(candidate, '[' as char)
                if (dims != ranges.length)
                    throw new PlasticException("ITER-MIS-SIZED", "The dimensionality of the iterator $candidate is not ${ranges.length}")
            }
        }
        done = calculateInitialDone()
    }

    // Create from either an iterator spec key/value like _[name] and [i,j,...]
    //
    Schemiterator(String iterKey, String iterValue) {
        String name = parseName(iterKey)
        validateName(name)
        names.add(name)
        ranges = parseRanges(iterValue)
        validateRanges(ranges)
        current = createZeros(ranges.length)
        done = calculateInitialDone()
    }

    private void initUncapped(int dimensions) {
        this.ranges = new long[dimensions]
        Arrays.fill(ranges, Long.MAX_VALUE) // indefinite range
        this.current = new long[dimensions]
        Arrays.fill(current, 0L)
    }

    private int count(String s, char c) {
        int count = 0
        for (int i = s.length()-1; i >= 0; i--) {
            if (s.charAt(i) == c)
                count++
        }
        count
    }

    // Merging of two iterators into this one
    //
    Schemiterator(Schemiterator first, Schemiterator second) {
        names.addAll(first.names)
        names.addAll(second.names)

        // no defined behavior for merging iterators with different parentage, so just take the first parents
        prepend(first.parent)

        // merge the dimensions (left-aligned) and ranges (wider wins)
        // Max value on a range means the range is not really known (and shouldn't win out over a known range)

        long[] longer
        long[] shorter

        if (first.ranges.length > second.ranges.length) {
            ranges = (long[])first.ranges.clone()
            longer = ranges
            shorter = second.ranges
        }
        else {
            ranges = (long[])second.ranges.clone()
            longer = ranges
            shorter = first.ranges
        }

        for (int i = 0; i< shorter.length; i++) {
            if (shorter[i] == Long.MAX_VALUE)
                continue
            else if (longer[i] == Long.MAX_VALUE)
                longer[i] = shorter[i]
            else
                longer[i] = Math.max(shorter[i], longer[i])
        }

        current = createZeros(ranges.length)
        done = calculateInitialDone()
    }

    private boolean calculateInitialDone() {
        for (long range : ranges) {
            if (range != 0)
                return false
        }
        true
    }

    private String parseName(String candidate) {
        if (candidate == null || candidate.isEmpty())
            throw new PlasticException("ITER-NO-NAME",  "The iterator specification has a missing name: ${candidate}")

        if (!candidate.startsWith('_[') || !candidate.endsWith(']'))
            throw new PlasticException("ITER-BAD-NAME-FORM", "The iterator specification has a bad name format: ${candidate}")

        candidate.substring(0+'_['.length(), candidate.length()-1)
    }

    private void validateName(String candidate) {
        if (candidate == null || candidate.isEmpty())
            throw new PlasticException("ITER-BAD-NAME", "The iterator specification has a missing name: ${candidate}")
    }

    private long[] parseRanges(String candidate) {
        int left = candidate.indexOf('[')
        if (left < 0 || !candidate.endsWith(']'))
            throw new PlasticException("ITER-BAD-FORM", "The iterator specification has a bad name format: ${candidate}")

        left++ // skip [
        int right = candidate.lastIndexOf(']')
        String allDims = candidate.substring(left, right)
        String[] sDims = allDims.isEmpty() ? new String[0] : allDims.split(',')
        asLongs(candidate, sDims)
    }

    private long[] asLongs(String candidate, String[] candidateLongs) {
        try {
            long[] results = new long[candidateLongs.length]
            for (int i = 0; i < candidateLongs.length; i++) {
                results[i] = Long.parseLong(candidateLongs[i])
            }
            results
        }
        catch(Exception e) {
            throw new PlasticException("ITER-BAD-DIMS", "The iterator specification has one or more bad dimensions: ${candidate}")
        }
    }

    private void validateRanges(long[] candidates) {
        for (int i = 0; i< candidates.length; i++) {
            if (candidates[i] < 0)
                throw new PlasticException("ITER-BAD-NAME", "Encountered a negative range in an interator: " +
                        Arrays.toString(candidates))
        }
    }

    private long[] createZeros(int len) {
        long[] result = new long[len];
        Arrays.fill(result, 0)
        result
    }

    void prepend(Schemiterator myParent) {
        this.parent = myParent
    }

    void increment() {
        if (!done) {
            for (int i = current.size() - 1; i >= 0; i--) {
                current[i] = (current[i]+1) % ranges[i]
                if (current[i] != 0)
                    return
            }

            for (int i = 0; i< current.size(); i++) {
                current[i] = ranges[i]-1
            }

            done = true
        }
    }

    String value() {
        StringBuilder sb = new StringBuilder()
        if (parent != null)
            sb.append(parent.value())

        for (long v : current) {
            sb.append('[')
            sb.append(v)
            sb.append(']')
        }
        sb.toString()
    }

    String adornedValue() {
        Variables.basename(names[0]) + value()
    }

    Map<String,String> replaceables() {
        Map<String,String> results = new HashMap<>()
        String curValue = value()
        StringBuilder sb = new StringBuilder()
        for (String name : names) {
            sb.setLength(0)
            sb.append(Variables.basename(name))
            sb.append(curValue)
            results.put(name, sb.toString())
        }
        results
    }

    boolean isDone() {
        done
    }

    Schemiterator mergeWith(Schemiterator other) {
        new Schemiterator(this, other)
    }

    String fullName() {
        names.join(',')
    }

    String asSpec() {
        StringBuilder sb = new StringBuilder()
        for (long v : ranges) {
            if (sb.length() != 0)
                sb.append(',')
            sb.append(v)
        }

        "_[" + fullName() + "]=[" + sb.toString() + "]"
    }

    void writeSpec(Map<String, Object> bindings) {
        String[] parts = asSpec().split('=', -2)
        bindings.put(parts[0], parts[1])
    }

    @Override
    String toString() {
        StringBuilder val = new StringBuilder(value())
        asSpec() + ' -> ' + val.toString()
    }

    void setCurrentFromIndices(Stack<Long> indices) {
        int wanted = current.length
        int given = indices.size()
        if (given < wanted) {
            String me = asSpec()
            throw new PlasticException("PLASTIC-ITER-SHORT-DIM", "The following iterator was given too few dimensions ($given) : $me")
        }

        for (int i = 0; i< wanted; i++) {
            long v = indices.elementAt((given-wanted)+i)
            current[i] = v
        }
    }

    void rerangeUsingCurrent() {
        for (int i = 0; i< current.length; i++) {
            ranges[i] = current[i] + 1
        }
    }
}
