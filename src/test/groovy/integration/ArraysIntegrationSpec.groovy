
/*
* Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package integration

import groovy.json.JsonSlurper
import org.opendaylight.plastic.implementation.CartographerWorker
import org.opendaylight.plastic.implementation.VersionedSchema
import spock.lang.Ignore
import spock.lang.Specification

class ArraysIntegrationSpec extends Specification {

    def asJson(String s) {
        new JsonSlurper().parseText(s)
    }

    def benchmark = { cls ->
        def start = System.currentTimeMillis()
        cls.call()
        def now = System.currentTimeMillis()
        now - start
    }

    CartographerWorker instance = new CartographerWorker()

    def "array translation works"() {
        def realInputSchema = new VersionedSchema("arrays-input", "1.0", "json")
        def realOutputSchema = new VersionedSchema("arrays-output", "1.0", "json")

        def input = '''
            {
              "name": "SNC1",
              "rate": 200,
              "include-nodes": [
                 {
                 "device-name-a": "abc",
                 "interface-name-a": "abc",
                 "device-name-b": "def",
                 "interface-name-b": "def"
                 }
              ],
              "exclude-nodes": [
                 {
                 "device-name-a": "ghi",
                 "interface-name-a": "ghi",
                 "device-name-b": "jkl",
                 "interface-name-b": "jkl"
                 }
              ],
              "end-points": [
                {
                  "device-name": "NODE1",
                  "interface-name": "1/31/1",
                  "ccmd-device-name": "SITE1",
                  "ccmd-adj-name": "1-6-5"
                },
                {
                  "device-name": "NODE2",
                  "interface-name": "1/31/1",
                  "ccmd-device-name": "SITE2",
                  "ccmd-adj-name": "1-6-5"
                }
              ],
              "vendor": "ciena",
              "model": "6500t"
            }
           '''

        def expectedOutput = '''
            {
              "name": "SNC1",
              "rate": 200,
              "include-nodes": [
                 {
                 "device-name-a": "abc",
                 "interface-name-a": "abc",
                 "device-name-b": "def",
                 "interface-name-b": "def"
                 }
              ],
              "exclude-nodes": [
                 {
                 "device-name-a": "ghi",
                 "interface-name-a": "ghi",
                 "device-name-b": "jkl",
                 "interface-name-b": "jkl"
                 }
              ],
              "pending-endpoints": [
                {
                  "device-name": "NODE1",
                  "interface-name": "1/31/1",
                  "ccmd-device-name": "SITE1",
                  "ccmd-adj-name": "1-6-5"
                },
                {
                  "device-name": "NODE2",
                  "interface-name": "1/31/1",
                  "ccmd-device-name": "SITE2",
                  "ccmd-adj-name": "1-6-5"
                }
              ],
             "static-scalar-original-types": {
                 "an-integer": 3,
                 "a-float": 4.56,
                 "a-boolean": false
             }
            }
        '''
        when:

        def span = benchmark {
            (1..1).each {
                instance.translate(realInputSchema, realOutputSchema, input)
            }
        }

        def output = instance.translate(realInputSchema, realOutputSchema, input)

        then:
        def jout = asJson(output)
        def jexp = asJson(expectedOutput)
        jout == jexp
        println "Span for N is msec: ${span}"
    }
}
