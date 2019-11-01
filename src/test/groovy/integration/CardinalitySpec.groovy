
/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package integration

import org.opendaylight.plastic.implementation.CartographerWorker
import org.opendaylight.plastic.implementation.VersionedSchema
import spock.lang.Specification

class CardinalitySpec extends Specification {

    CartographerWorker instance = new CartographerWorker()

    def "cardinality"() {
        def realInputSchema = new VersionedSchema("cardinality-in", "1.0", "json")
        def realOutputSchema = new VersionedSchema("cardinality-out", "1.0", "json")

        def input = '''
            {
                "device-name-here": "abc",
                "action" :"d$e$f"
            }
           '''

        def expectedOutput = '''
            {
                "dev-plus-action": "abc+d$e$f",
                "optional-in": ""
            }
            '''
        when:
        def output = instance.translate(realInputSchema, realOutputSchema, input)

        then:
        output.replaceAll("\n\\s*", "") == expectedOutput.replaceAll("\n\\s*", "")
    }
}
