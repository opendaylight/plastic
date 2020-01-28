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

    private Set<String> names = new HashSet<>()
    private long[] ranges = new int[0]
    private long[] current = new int[0]
    private List<Schemiterator> prependeds = new ArrayList<>()

    Schemiterator() {
        this("")
    }

    // Parse format: _[name]=[i,j,...]
    //
    Schemiterator(String internalSpec) {
        if (internalSpec != null && !internalSpec.isEmpty()) {
            String name = parseName(internalSpec)
            validateName(name)
            names.add(name)
            ranges = parseRanges(internalSpec)
            validateRanges(ranges)
            current = createZeros(ranges.length)
        }
    }

    //
    Schemiterator(Schemiterator first, Schemiterator second) {
        names.addAll(first.names)
        names.addAll(second.names)
        prepend(first)

        // merge the dimensions (left-aligned) and ranges (wider wins)

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
            longer[i] = Math.max(shorter[i], longer[i])
        }
    }

    String parseName(String candidate) {
        if (candidate == null || candidate.isEmpty())
            throw new PlasticException("ITER-NO-NAME",  "The iterator specification has a missing name: ${candidate}")

        int right = candidate.indexOf(']')
        if (!candidate.startsWith('_[') || right < 0)
            throw new PlasticException("ITER-BAD-NAME-FORM", "The iterator specification has a bad name format: ${candidate}")

        candidate.substring(0+'_['.length(), right)
    }

    void validateName(String candidate) {
        if (candidate == null || candidate.isEmpty())
            throw new PlasticException("ITER-BAD-NAME", "The iterator specification has a missing name: ${candidate}")
    }

    int[] parseRanges(String candidate) {
        int left = candidate.indexOf("]=[")
        if (left < 0 || !candidate.endsWith(']'))
            throw new PlasticException("ITER-BAD-FORM", "The iterator specification has a bad name format: ${candidate}")

        left += "]=[".length()

        int right = candidate.lastIndexOf(']')
        String allDims = candidate.substring(left, right)
        String[] sDims = allDims.isEmpty() ? new String[0] : allDims.split(',')
        asIntegers(candidate, sDims)
    }

    private long[] asIntegers(String candidate, String[] candidateInts) {
        try {
            long[] results = new long[candidateInts.length]
            for (int i = 0; i < candidateInts.length; i++) {
                results[i] = Long.parseLong(candidateInts[i])
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

    void prepend(Schemiterator other) {
        prependeds.add(0, other)
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
        for (Schemiterator prepended : prependeds) {
            sb.append(prepended.value())
        }
        for (long v : current) {
            sb.append('[')
            sb.append(v)
            sb.append(']')
        }
        sb.toString()
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

    @Override
    String toString() {
        StringBuilder sb = new StringBuilder()
        for (long v : ranges) {
            if (sb.length() != 0)
                sb.append(',')
            sb.append(v)
        }

        StringBuilder val = new StringBuilder(value())
        if (val.length() != 0)
            val.insert(0, " ")

        "_[" + names.join(",") + "]=[" + sb.toString() + "]" + val.toString()
    }
}
