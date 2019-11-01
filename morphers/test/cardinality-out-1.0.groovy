
/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package test

import org.opendaylight.plastic.implementation.BasicMorpher


class CardinalityOutputMorpher extends BasicMorpher
{
    CardinalityOutputMorpher() {
        optionalInputs('optional-in')
        ignoreUnusedInputs('optional-in')
        ignoreUnusedOutputs('optional-in')
    }

    void tweakValues(Map ins, Map outs) {
        if(!isBound('optional-in')) {
            println("optional-in was missing")
        }
    }
}
