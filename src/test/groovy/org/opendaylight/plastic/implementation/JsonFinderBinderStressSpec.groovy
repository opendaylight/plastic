
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

// To run this stress test from the command line, use
//     mvn -Dtest=JsonFinderBinderStressSpec test
// Note we more reliable micro-benchmarking!
//     https://www.baeldung.com/java-microbenchmark-harness

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

        // warm up - hotspot optimization, cache filling, etc

        for(long i = 0; i< N/5; i++) {
            instance.process(model, payload)
        }

        int elapsed = benchmark {
            for(long i = 0; i< N; i++) {
                instance.process(model, payload)
            }
        }

        println("\n\n>>>>>> ${this.class.simpleName}.process(...): Iterations: ${N}   Elapsed: ${elapsed} msec  Per-sec: ${1000.0*N/elapsed}  Acceptable ~ 42 sec<<<<<<\n\n")
        then:
        true
    }
}
