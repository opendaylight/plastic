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
 * - incrementing only applies to non-parent-supplied dimensions
 * - a concept of "is done", which needs to be checked FIRST to allow for zero length arrays
 * - "is done" only applies to non-parent-supplied dimension
 *
 */
@CompileStatic
class Schemiterator {

    static Map<String,Schemiterator> instantiateIterators(Map<String,Object> bindings) {
        Map<String,Schemiterator> results = new HashMap<>()

        bindings.each { k,v ->
            if (k.startsWith('_[') && k.endsWith(']')) {
                Schemiterator iterator = new Schemiterator(k, (String) v)
                iterator.addToMap(results)
            }
        }

        return results
    }

    static void insertIteratorSpec(Map bindings, String arrayName, List<String> array) {
        Schemiterator iterator = new Schemiterator(arrayName, (long)array.size())
        iterator.writeSpecTo(bindings)
    }

    static boolean hasSpec(String arrayName, Map bindings) {
        String key = "_[$arrayName]"
        bindings.containsKey(key)
    }

    private boolean done = false
    private Set<String> names = new HashSet<>()
    private long[] ranges = new int[0]
    private long[] current = new int[0]
    private Schemiterator parent = null

    private int asteriskDimensions = 0
    private int caretDimensions = 0

    // Create an anonymous uncapped iterator that only increments the least significant digit.
    // Useful when you do not know the upper limit of the range apriori. Using max
    // values for the range causes merging to use the current values instead of
    // ranges.
    //
    Schemiterator(int dimensions) {
        initUncapped(dimensions)
        done = calculateInitialDone()
        calculateVirtualDimensions()
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
        calculateVirtualDimensions()
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
        calculateVirtualDimensions()
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
        calculateVirtualDimensions()
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

    // Merging of two sibling iterators into this one
    //
    Schemiterator(Schemiterator first, Schemiterator second) {

        names.addAll(first.names)
        names.addAll(second.names)

        // note: no defined behavior for merging iterators with different parentage
        parent = firstNonNull(first.parent, second.parent)

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

        calculateVirtualDimensions()
    }

    boolean equals(Object other) {
        if (!(other instanceof Schemiterator))
            return false
        Schemiterator that = (Schemiterator) other
        if (names.size() != that.names.size())
            return false
        if (caretDimensions != that.caretDimensions)
            return false
        if (asteriskDimensions != that.asteriskDimensions)
            return false
        if (!Arrays.equals(ranges, that.ranges))
            return false
        if (!Arrays.equals(current, that.current))
            return false
        if (!names.equals(that.names))
            return false
        true
    }

    private void copyFrom(Schemiterator other) {
        names.clear()
        names.addAll(other.names)

        parent = other.parent
        ranges = Arrays.copyOf(other.ranges, other.ranges.length)
        current = Arrays.copyOf(other.current, other.current.length)

        done = other.done

        caretDimensions = other.caretDimensions
        asteriskDimensions = other.asteriskDimensions
    }

    private Schemiterator firstNonNull(Schemiterator... objs) {
        for (obj in objs)
            if (obj != null)
                return obj
        null
    }

    private boolean calculateInitialDone() {
        for (long range : ranges) {
            if (range != 0)
                return false
        }
        true
    }

    private void calculateVirtualDimensions() {

        int widest = 0
        int maxCarets = 0
        int maxAsterisks = 0

        for (String name : names) {
            int asterisks = count(name, '*' as char)
            int carets = count(name, '^' as char)
            widest = Math.max(widest, carets + asterisks)
            maxCarets = Math.max(maxCarets, carets)
            maxAsterisks = Math.max(maxAsterisks, asterisks)
        }

        asteriskDimensions = widest-maxCarets
        caretDimensions = maxCarets
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

    int effectiveDimensions() {
        caretDimensions+(asteriskDimensions > 0 ? 1 : 0)
    }

    void flowOut(List<Schemiterator> stack) {
        int slen = stack.size()
        if (slen >= caretDimensions+1) {
            if (stack[-1] != this)
                throw new PlasticException("PLASTIC-TOS-BAD", "Internal error: the top of the iterator stack (${stack[-1]}) should be (${this})")

            long[] borroweds = Arrays.copyOf(ranges, caretDimensions)

            final int tosIndex = slen-1
            final int lastIndex = tosIndex-1

            for (int i = 0; i < caretDimensions && borroweds.length > 0; i++) {
                Schemiterator parentIterator = stack.get(lastIndex-i)
                borroweds = parentIterator.consumeFlow(borroweds)

                // Need to estalish the parent relationship so distributed incrementing works

                Schemiterator childIterator = stack.get(lastIndex-i+1)
                childIterator.parent = parentIterator
            }
        }
        else {
            throw new PlasticException("PLASTIC-NOT-BORROWABLE",
                    "Not enough enclosing lists for iterator: ${this} (only ${slen} available)")
        }
    }

    private long[] consumeFlow(long[] requiredIterations) {
        if (requiredIterations.length == 0) {
            return new long[0]
        }
        else if (ranges.length == 0) {
            // An empty iterator will absorb only a single index and let the others keep flowing
            // TODO: this is really initialization logic - refactor into method
            ranges = new long[1]
            ranges[0] = requiredIterations[-1]
            current = createZeros(ranges.length)
            done = calculateInitialDone()
            asteriskDimensions = ranges.length
            caretDimensions = 0
            long[] result = Arrays.copyOf(requiredIterations, requiredIterations.length-1)
            return result
        }
        else if (ranges[0] == Long.MAX_VALUE) {
            return new long[0]
        }
        else {
            boolean changed = false
            int len = Math.min(asteriskDimensions, requiredIterations.length)

            for (int i = 0; i< len; i++) {
                int thisIndex = ranges.length-1-i
                long thisRange = ranges[thisIndex]
                int thatIndex = requiredIterations.length-1-i
                long thatRange = requiredIterations[thatIndex]
                if (thatRange > thisRange) {
                    ranges[thisIndex] = thatRange
                    changed = true
                }
            }
            if (changed)
                done = calculateInitialDone()
            Arrays.copyOf(requiredIterations, requiredIterations.length-len)
        }
    }

    void increment() {
        if (!done) {

            // is having carets but no parent a real use case?
            //
            int leftmostIndex =  (parent == null) ? 0 : current.length-asteriskDimensions
            int rightmostIndex = current.size()-1

            for (int i = rightmostIndex; i >= leftmostIndex; i--) {
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

    void reset() {
        for (int i = 0; i< current.length; i++) {
            current[i] = 0
        }
        done = calculateInitialDone()
    }

    String value() {
        computeValue(current.length)
    }

    private String computeValue(int digitsNeeded) {
        StringBuilder sb = new StringBuilder()

        if (digitsNeeded > 0) {

            // is having carets but no parent a real use case?
            //
            int leftmostIndex = (parent == null) ? 0 : current.length - asteriskDimensions

            if (parent != null)
                sb.append(parent.computeValue(digitsNeeded-asteriskDimensions))

            for (int i = leftmostIndex; i < current.length; i++) {
                sb.append('[')
                sb.append(current[i])
                sb.append(']')
            }
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
            results.put(Variables.alternativeName(name), sb.toString()) // TODO: optimize by creating alt at addName() time
        }
        results
    }

    boolean isDone() {
        done
    }

    Schemiterator mergeWith(Schemiterator other) {
        new Schemiterator(this, other)
    }

    void mergeFrom(Schemiterator other) {
        Schemiterator intermediatry = new Schemiterator(this, other)
        copyFrom(intermediatry)
    }

    void mergeFromUsingName(Schemiterator other, String replacementName) {
        Set<String> saved = other.names
        other.names.clear()
        other.names.add(replacementName)
        mergeFrom(other)
        other.names = saved
    }

    String allNames() {
        names.join(',')
    }

    String asSpec() {
        StringBuilder sb = new StringBuilder()
        for (long v : ranges) {
            if (sb.length() != 0)
                sb.append(',')
            sb.append(v)
        }

        "_[" + allNames() + "]=[" + sb.toString() + "]"
    }


    // TODO: gather all this ad hoc formatting logic into a Spec class

    void writeSpecTo(Map<String, Object> bindings) {
        String[] parts = asSpec().split('=', -2)
        bindings.put(parts[0], parts[1])
    }

    void readSpecFrom(Map bindings) {

        for (String name : names) {
            String key = "_[$name]"
            if (bindings.containsKey(key)) {
                String value = bindings.get(key)
                if (!value.startsWith('[') || !value.endsWith(']'))
                    throw new PlasticException("PLASTIC-ITER-READ-FMT",
                            "Bad value for iterator specification ${allNames()} in bindings: $value")

                value = value.substring(1, value.length()-1)
                String[] parts = value.split(',')
                long[] theseRanges = new long[parts.length]

                for (int i = 0; i< parts.length; i++) {
                    String part = parts[i]
                    long dim = Long.parseLong(part)
                    theseRanges[i] = dim
                }

                Schemiterator shadow = new Schemiterator(name, theseRanges)
                copyFrom(shadow)

                return
            }
        }

        throw new PlasticException("PLASTIC-ITER-READ",
                "Could not find iterator specification in bindings for ${allNames()}")
    }

    void addToMap(Map<String,Schemiterator> destination) {
        for (String name : names) {
            destination.put(name, this)
            destination.put(Variables.alternativeName(name), this)
        }
    }

    @Override
    String toString() {
        StringBuilder sb = new StringBuilder()
        sb.append(asSpec())
        sb.append(' -> ')
        sb.append(value())
        sb.toString()
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

    long size() {
        long s = 1
        for (long r : ranges) {
            s *= r
        }
        ranges.length ? s : 0
    }
}
