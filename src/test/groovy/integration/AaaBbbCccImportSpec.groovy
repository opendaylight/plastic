
/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package integration

import groovy.json.JsonSlurper
import org.opendaylight.plastic.implementation.CartographerWorker
import org.opendaylight.plastic.implementation.VersionedSchema
import spock.lang.Specification

class AaaBbbCccImportSpec extends Specification {

    def asJson(String s) {
        new JsonSlurper().parseText(s)
    }

    CartographerWorker instance = new CartographerWorker()

    def "imports in the lib directory succeed"() {
        def realInputSchema = new VersionedSchema("aaa-bbb-ccc-input", "1.0", "json")
        def realOutputSchema = new VersionedSchema("aaa-bbb-ccc-output", "1.0", "json")

        def input = '''
            {
              "dontcare": "dontcare"
            }
           '''
        when:
        instance.translate(realInputSchema, realOutputSchema, input)
        then:
        notThrown(Exception)
    }
}
