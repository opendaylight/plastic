
/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package test

import org.opendaylight.plastic.implementation.BasicMorpher


class ConditionalOutputMorpher extends BasicMorpher
{
    ConditionalOutputMorpher() {
        optionalInputs('MTU')
        ignoreUnusedOutputs('MTU', 'ADDRESS4[*]', 'ADDRESS4LEN[*]')
    }

    void tweakParsed(Object inTree, Object outTree) {
        if (!isBound('MTU')) {
            outTree.requests[0].payload.interface[0].remove('mtu')
        }
        if (isEmpty(inTree.'ip-addresses-v4')) {
            outTree.requests[0].payload.interface[0].remove('vlan-interface-std:vlan')
        }
    }
}
