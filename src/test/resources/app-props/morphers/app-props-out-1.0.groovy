/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

import org.opendaylight.plastic.implementation.BasicMorpher


class AppPropsMorpher extends BasicMorpher {
    void tweakValues(Map ins, Map outs) {
        String status = ""
        if (this.properties['morpher-status'])
            status = properties['morpher-status']
        else
            status = "morpher-not-ok"

        ins['morpher-says'] = outs['morpher-says'] = status
    }
}
