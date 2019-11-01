
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import spock.lang.Specification

class CartographyServiceAdapterSpec extends Specification{

    Cartography mockCartography = Mock()

    String anyPayload = "do-not-care"

    CartographyServiceAdapter instance = new CartographyServiceAdapter(mockCartography)

    def "adapter should construct inputs and forward the call"() {
        when:
        instance.translate("in-name", "1.0", "in-type", "out-name", "1.0", "out-type", anyPayload)
        then:
        1 * mockCartography.translateWithDefaults(_ as VersionedSchema, _ as VersionedSchema, anyPayload, _)
    }

    def coverage() {
        expect:
        instance.close()
    }
}
