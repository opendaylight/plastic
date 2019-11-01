
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package integration

import groovy.json.JsonSlurper
import org.opendaylight.plastic.implementation.CartographerWorker
import org.opendaylight.plastic.implementation.SearchPath
import org.opendaylight.plastic.implementation.VersionedSchema
import spock.lang.Specification


class ArrayPlanningSpec extends Specification {

    def asJson(String s) {
        JsonSlurper slurper = new JsonSlurper()
        slurper.parse(s.getBytes())
    }

    CartographerWorker instance = new CartographerWorker(new SearchPath("plans.properties"))

    def inputSchema = new VersionedSchema("\${ArrayPlanningClassifier}", "1.0", "json")
    def dummySchema = new VersionedSchema("ignored-and-overwritten-by-classifier", "1.0", "json")

    def "can successfully use planning classifier to run child translations"() {

        def input = new File("src/test/resources/plans/payloads/array-plan-payload.json").text

        def expectedOutput = '''
            {
                "details": [
                   {
                     "d": [
                       {
                         "inner": 4
                       },
                       {
                         "inner": 5
                       },
                       {
                         "inner": 6
                       }
                     ]
                   },
                   {
                     "d": [
                       {
                         "inner": 7
                       },
                       {
                         "inner": 8
                       }
                     ]
                   },
                   {
                     "d": [
                       {
                         "inner": 9
                       }
                     ]
                   },
                   {
                     "d": [
                     ]
                   }
                ],
                "important": "important-value"
            }
            '''
        when:
        def output = instance.translate(inputSchema, dummySchema, input)

        then:
        asJson(output) == asJson(expectedOutput)
    }
}
