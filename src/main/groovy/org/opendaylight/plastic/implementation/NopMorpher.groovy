
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


class NopMorpher extends Morpher {

    NopMorpher(VersionedSchema input,
               Object wrapped, String fileName) {
        super(wrapped)
    }

    void tweakValues(Map inMap, Map outMap) {
    }

    void tweakParsed(inTree, outTree) {
    }
}
