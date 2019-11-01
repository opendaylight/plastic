
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Specification

@Ignore // don't slow down unit tests
class JsonFinderBinderStressSpec extends Specification {

    JsonSlurper slurper = new JsonSlurper()
    JsonFinderBinder instance = new JsonFinderBinder()

    def array1Model() {
        '''
        {
            "addresses": [ "${ADD[*]}" ]
        }
        '''
    }

    def array1Payload() {
        '''
        {
            "addresses": [ "1.2.3.4", "5.6.7.8", "9.10.11.12" ]
        }
        '''
    }

    def benchmark = { cls ->
        def start = System.currentTimeMillis()
        cls.call()
        def now = System.currentTimeMillis()
        now - start
    }

    def profiling() {
        given:
        def modelJson = array1Model()
        def payloadJson = array1Payload()

        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)

        when:
        long N = 5000000
        int elapsed = benchmark {
            for(long i = 0; i< N; i++) {
                instance.process(model, payload)
            }
        }
        println(">>>>> PREVIOUS:")
        println(">>>>> Translations: 5000000   Elapsed: 100 sec (before @CompileStatic)\n")
        println(">>>>> Translations: 5000000   Elapsed:  45 sec (after @CompileStatic)\n")
        println(">>>>> Translations: 5000000   Elapsed:  36 sec (after getElementValue() optimization)\n")
        println(">>>>> Translations: 5000000   Elapsed:  34 sec (after replacing currying)\n")
        println(">>>>> CURRENT:")
        println(">>>>> Translations: ${N}   Elapsed(msec): ${elapsed}   Per-sec: ${1000.0*N/elapsed}\n\n")
        then:
        true
    }
}
