
/*
 * Copyright (c) 2019-2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * This is easily the most complex code in Plastic.
 *
 * - Read the tutorial, particularly on arrayed variables (aka generic variables)
 * - Look closely at the unit tests to see how the logic works from the outside
 *
 * Here are some important concepts to help understanding of the logic
 *
 * - Use of recursion to walk the output schema
 * - Recursing into a data structure
 * - Returning out of a data structure
 * - Delaying structure additions/deletions to avoid concurrent modifications
 * - Iterators that have multiple explicit dimensions
 * - Iterators that have implicit (borrowed) dimensions
 * - Recursing into the first use of an interator
 * - Recursing out of the dimensional depth of an iterator
 */

@CompileStatic
class JsonValuesInjector {

    static Logger logger = LoggerFactory.getLogger(JsonValuesInjector)

    Object inject(Map values, Object model, Set danglingInputs, Set danglingOutputs) {

        IteratorFlows flows = new IteratorFlows(values)
        flows.processModel(model)
        int numVariables = flows.numberOfVariables()

        IteratorExpansion expansion = new IteratorExpansion(flows)
        expansion.processModel(model)

        Set<String> expectedInputVars = (Set<String>) values.keySet()
        Set<String> foundInputVars = [] as Set

        // some classifiers are doing multi-level child translations so that the model is truncated
        // by markers until the variable is actually injected. so walking cannot get to those
        // parts of the model yet. so we loop until we aren't making progress or we hit the maximum
        // depth.

        final int MAXDEPTH = 5 // do we have the guts to just make this infinite?

        for (int passes = 0; passes < MAXDEPTH; passes++) {
            int preFound = foundInputVars.size()

            IterativeReplacement replacer = new IterativeReplacement(values, danglingOutputs, foundInputVars)
            replacer.processModel(model)

            int postFound = foundInputVars.size()

            // This test for termination is not precise. If the morpher logic is ignoring some inputs
            // then the found values will never equal the given input values. This means we could do
            // one extra pass.

            boolean stalledOut = postFound == preFound
            boolean foundEverything = postFound >= numVariables

            if (stalledOut || foundEverything) {
                if (passes > 1)
                    logger.debug("Used injection passes of: {}", passes)
                break;
            }
        }

        // Hotspot: used to use danglingInputs.addAll(expectedInputVars - foundInputVars)

        expectedInputVars.each { expected ->
            if (!Variables.isInternal(expected) && !foundInputVars.contains(expected))
                danglingInputs.add(expected)
        }

        model
    }
}
