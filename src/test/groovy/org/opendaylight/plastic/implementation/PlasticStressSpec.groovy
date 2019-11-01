
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import spock.lang.Ignore
import spock.lang.Specification

@Ignore // don't slow down unit tests
class PlasticStressSpec extends Specification {

    def benchmark = { cls ->
        def start = System.currentTimeMillis()
        cls.call()
        def now = System.currentTimeMillis()
        now - start
    }

    SearchPath search = new SearchPath("stress.properties")
    CartographerWorker instance = new CartographerWorker(search, 0)

    def "run a stress test"() {
        given:
        VersionedSchema inputVs = new VersionedSchema("stress-in", "1.0", "json")
        VersionedSchema outputVs = new VersionedSchema("stress-out", "1.0", "json")
        and:
        String element = '''
        {
            "aa": 0,
            "bb": 1,
            "cc": 2,
            "dd": 3,
            "ee": 4,
            "ff": 5,
            "gg": 6,
            "hh": 7,
            "ii": 8,
            "jj": 9
        }
        '''
        String[] elements = [element] * N
        String payload = "[" + elements.join(",") + "]"
        when:
        int elapsed = benchmark {
            instance.translate(inputVs, outputVs, payload)
        }
        println(">>>>> Translations: ${N}   Elapsed(msec): ${elapsed}   Per-sec: ${1000.0*N/elapsed}\n\n")
        then:
        elapsed < 15000 // 9000 worst case should take less than 15 sec
        where:
        N       | _
        6000    | _
        7000    | _
        8000    | _
        9000    | _
    }
}
