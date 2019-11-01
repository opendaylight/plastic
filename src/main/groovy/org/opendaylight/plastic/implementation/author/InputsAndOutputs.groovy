
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation.author

import org.opendaylight.plastic.implementation.Variables

trait InputsAndOutputs {

    Map<String,String> inputs
    Map<String,String> outputs
    Set<String> ignoredInputs = []
    Set<String> ignoredOutputs = []
    Set<String> optioned = []

    // Methods that start with an _ are internal and not meant to be called by derived classes.
    // They cannot be made private because "duck typing" logic in Plastic

    /* private */
    void _blessMissingInputs(Set<String> missings) {
        if (optioned.contains("*"))
            missings.clear()
        else {
            missings.removeAll(optioned)
            removeMatching(optioned, missings)
        }
    }

    /* private */
    void _blessDanglingInputs(Set<String> dangling) {
        if (ignoredInputs.contains("*"))
            dangling.clear()
        else {
            dangling.removeAll(ignoredInputs)
            removeMatching(ignoredInputs, dangling)
        }
    }

    /* private */
    void _blessDanglingOutputs(Set<String> dangling) {
        if (ignoredOutputs.contains("*"))
            dangling.clear()
        else {
            dangling.removeAll(ignoredOutputs)
            removeMatching(ignoredOutputs, dangling)
        }
    }

    /* private */
    void _preTweakValues(Map<String,String> mIn, Map<String,String> mOut)
    {
        this.inputs = mIn
        this.outputs = mOut
    }

    private void removeMatching(Set<String> wanted, Set<String> candidates) {
        // Optimization: leave this local - if it is outside, the closure references a missing property, slowing things down
        Variables variables = new Variables()

        Set<String> arrayedTargets = candidates.findAll {  variables.isIndexed(it) }
        Set<String> arrayedSources = wanted.findAll { variables.isGenericIndexed(it) }
        arrayedSources.each { s ->
            arrayedTargets.each { t ->
                if (variables.matches(t, s)) {
                    candidates.remove(t)
                }
            }
        }
    }
}
