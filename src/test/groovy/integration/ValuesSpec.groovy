
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

class ValuesSpec extends Specification {

    CartographerWorker instance = new CartographerWorker()

    def asJson(String s) {
        JsonSlurper slurper = new JsonSlurper()
        slurper.parse(s.getBytes())
    }

    def "various values testing"() {
        given:
        def realInputSchema = new VersionedSchema("values-in", "1.0", "json")
        def realOutputSchema = new VersionedSchema("values-out", "1.0", "json")

        def input = '''
            {
                "some-string": "abc",
                "some-int": 123,
                "zero-int": 0,
                "zero-string": "0",
                "explicit-big-int": 1200000000,
                "explicit-big-string": "14000000"
            }
           '''

        def expectedOutput = '''
            {
                "better-exp-big-int": 1200000000,
                "better-exp-big-string": "14000000",
                "better-big-int": 1500000000,
                "better-int": 123,
                "better-string": "abc",
                "better-zero": 0,
                "zero-int": 0,
                "zero-string": "0"
            }
            '''
        def defaults = '''
            {
                "ghi": 1500000000,
                "jkl": 0
            }
            '''
        when:
        def output = instance.translateWithDefaults(realInputSchema, realOutputSchema, input, defaults)
        then:
        asJson(output) == asJson(expectedOutput)
    }
}
