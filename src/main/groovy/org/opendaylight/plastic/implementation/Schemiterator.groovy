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

    static List<Schemiterator> instantiateAll(Map<String,Object> bindings) {
        List<Schemiterator> results = new ArrayList<>()

        bindings.each { k,v ->
            if (k.startsWith('_[') && k.endsWith(']')) {
                results.add (new Schemiterator(k, (String) v))
            }
        }

        return results
    }

    private Set<String> names = new HashSet<>()
    private long[] ranges = new int[0]
    private long[] current = new int[0]
    private Schemiterator parent = null

    // TODO: figure out if we really need the multiple names feature

    // Create an anonymous elastic iterator that only increments the least significant digit.
    // Useful when you do not know the upper limit of the range apriori. Using max
    // values for the range causes merging to use the current values instead of
    // ranges.
    //
    Schemiterator(int dimensions) {
        initUncapped(dimensions)
    }

    private void initUncapped(int dimensions) {
        this.ranges = new long[dimensions]
        Arrays.fill(ranges, Long.MAX_VALUE) // indefinite range
        this.current = new long[dimensions]
        Arrays.fill(current, 0L)
    }

    // Create from an arrayed variable name like ABC[^][*]
    //
    Schemiterator(String candidate) {
        if (candidate != null && !candidate.isEmpty()) {
            int bracket = candidate.indexOf('[')
            if (bracket > -1) {
                String baseName = candidate.substring(0, bracket)
                validateName(baseName)
                names.add(baseName)
                int dims = count(candidate, '[' as char)
                initUncapped(dims)
            }
        }
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
        prepend(first)

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
    }

    String parseName(String candidate) {
        if (candidate == null || candidate.isEmpty())
            throw new PlasticException("ITER-NO-NAME",  "The iterator specification has a missing name: ${candidate}")

        if (!candidate.startsWith('_[') || !candidate.endsWith(']'))
            throw new PlasticException("ITER-BAD-NAME-FORM", "The iterator specification has a bad name format: ${candidate}")

        candidate.substring(0+'_['.length(), candidate.length()-1)
    }

    void validateName(String candidate) {
        if (candidate == null || candidate.isEmpty())
            throw new PlasticException("ITER-BAD-NAME", "The iterator specification has a missing name: ${candidate}")
    }

    int[] parseRanges(String candidate) {
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

    void addName(String name) {
        names.add(name)
    }

    void addNames(Schemiterator other) {
        names.addAll(other.names)
    }

    void prepend(Schemiterator myParent) {
        this.parent = myParent
    }

    void increment() {
        if (!isDone()) {
            for (int i = current.size() - 1; i >= 0; i--) {
                current[i]++
                if (current[i] < ranges[i])
                    return
                if (i > 0)
                    current[i] = 0
                else
                    current[i] = ranges[i]-1
            }
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
        fullName() + value()
    }

    boolean isDone() {
        for (int i = 0; i< current.size(); i++) {
            if (current[i] < (ranges[i]-1))
                return false
        }
        true
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
        if (val.length() != 0)
            val.insert(0, " ")

        asSpec() + val.toString()
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
}
