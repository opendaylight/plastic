
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

@Ignore
class JsonValuesInjectorStressSpec extends Specification {

    JsonValuesInjector instance = new JsonValuesInjector()
    Set danglingInputs = []
    Set danglingOutputs = []

    def benchmark = { cls ->
        def start = System.currentTimeMillis()
        cls.call()
        def now = System.currentTimeMillis()
        now - start
    }

    def profiling() {
        given:
        def bound = ['ADDR[1]': "1.2.3.4",
                     'ADDR[2]': "5.6.7.8",
                     'LEN[1]' : 10,
                     'LEN[2]' : 20
        ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "components": [
                {
                  "subcomponent": {
                    "address": "${ADDR[*]}/${LEN[*]}",
                  }
                }
            ]
        }
        ''')
        when:
        long N = 5000000
        int elapsed = benchmark {
            for(long i = 0; i< N; i++) {
                instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)
            }
        }
        println(">>>>> PREVIOUS:")
        println(">>>>> Translations: 5000000   Elapsed:  61 sec (before @CompileStatic JsonValuesInjector)\n")
        println(">>>>> Translations: 5000000   Elapsed:  22 sec (after @CompileStatic) JsonValuesInjector\n")
        println(">>>>> CURRENT:")
        println(">>>>> Translations: ${N}   Elapsed(msec): ${elapsed}   Per-sec: ${1000.0*N/elapsed}\n\n")
        then:
        true
    }
}
