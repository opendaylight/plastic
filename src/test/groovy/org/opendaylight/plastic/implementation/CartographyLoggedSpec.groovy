
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

class CartographyLoggedSpec extends Specification{

    BetterLogger mockLogger = Mock()
    Cartography mockInner = Mock()

    VersionedSchema mockInput = Mock()
    VersionedSchema mockOutput = Mock()

    String anyPayload = "do-not-care"

    CartographyLogged instance = new CartographyLogged(mockInner, mockLogger)

    def "outer wrapper translate should propogate to the inner translate"() {
        when:
        instance.translate(mockInput, mockOutput, anyPayload)
        then:
        1 * mockInner.translate(mockInput, mockOutput, anyPayload)
    }
}
