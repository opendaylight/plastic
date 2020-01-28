/*
 * Copyright (c) 2019-2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation

import groovy.transform.CompileStatic;

/**
 * A simple way to iterate through a count where the "digits" can be of any modulus.
 * Its values will always be a concatentation of "digits" where each digit has a value
 * of 0..L[i]-1. Incrementing it beyond the max will leave it at the max value but it
 * will have the isDone flag set. Use the isLegitimate flag to test whether counting
 * is even possible (negative counts make no sense).
 */
@CompileStatic
class MultiModuloCounter {

    private final long[] ranges
    private final long[] current

    MultiModuloCounter(long[] wanteds) {
        this.ranges = wanteds
        this.current = new long[ranges.length]
        reset()
    }

    private void reset() {
        for (int i = 0; i< current.length; i++) {
            current[i] = 0
        }
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

    long[] value() {
        (long[]) current.clone()
    }

    boolean isDone() {
        for (int i = 0; i< current.size(); i++) {
            if (current[i] < (ranges[i]-1))
                return false
        }
        true
    }

    boolean isCountable() {
        for (long r : ranges) {
            if (r < 0)
                return false
        }
        true
    }

    boolean isZero() {
        for (long r : ranges) {
            if (r != 0)
                return false
        }
        true
    }
}
