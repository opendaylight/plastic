
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
import groovy.json.JsonSlurper
import spock.lang.Specification

class DefaultsSpec extends Specification {

    CartographerWorker instance = new CartographerWorker()

    def asJson(String raw) {
        def slurper = new JsonSlurper()
        slurper.parse(raw.getBytes())
    }

    def "default values should work for scalars, blobs, and indexed values"() {
        def realInputSchema = new VersionedSchema("defaults-in", "1.0", "json")
        def realOutputSchema = new VersionedSchema("defaults-out", "1.0", "json")

        def defaults = '''
            {
                "scalar-value": 12345,
                "blob-value": { "inside": 789 },
                "array-value[*]": [ "av-1", "av-2" ],
            }
            '''

        def input = '''
            {
                "ignore-me": "abc"
            }
           '''

        def expectedOutput = '''
            {
                "results": {
                     "my-array-values": [ "av-1", "av-2" ],
                     "my-blob-value": { "inside": 789 },
                     "my-scalar-value": 12345
                }
            }
            '''
        when:
        def output = instance.translateWithDefaults(realInputSchema, realOutputSchema, input, defaults)
        and:
        def foundjson = asJson(output)
        def expectjson = asJson(expectedOutput)

        then:
        foundjson == expectjson
    }
}
