
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

@Ignore // ODL build env is tiny and this runs at 2.5 X slower than my dev laptop, so disable by default
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
        VersionedSchema inputVs = new VersionedSchema("stress-in-2", "1.0", "json")
        VersionedSchema outputVs = new VersionedSchema("stress-out-2", "1.0", "json")
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
        println(">>>>> PREVIOUS:")
        /*
        Historical improvements...
        println(">>>>> Translations: 1000000   Elapsed:  52 sec (before @CompileStatic JsonFinderBinder)\n")
        println(">>>>> Translations: 1000000   Elapsed:  33 sec (after @CompileStatic JsonFinderBinder)\n")
        println(">>>>> Translations: 1000000   Elapsed:  30 sec (after @CompileStatic JsonValuesInjector)\n")
        println(">>>>> Translations: 1000000   Elapsed:  30 sec (after @CompileStatic Variables)\n")
        println(">>>>> Translations: 1000000   Elapsed:  23 sec (after currying optimization in JsonFinderBinder)\n")
        println(">>>>> Translations: 1000000   Elapsed:" +
                "  18 sec (after nullPrimitiveOrCollection optimization in JsonFinderBinder)\n")
        println(">>>>> Translations: 100000   Elapsed:" +
                "  29 sec (after fix brackets in the stress-in-2-1.0.json input schema)\n")
        println(">>>>> Translations: 100000   Elapsed:" +
                "  26 sec (after isGenericIndexed() optimization in Variables)\n")
        */

        int currentBestSec = 18 // seconds

        println(">>>>> Translations: 100000   Elapsed: ${currentBestSec} sec\n")
        println(">>>>> CURRENT:")
        println(">>>>> Translations: ${N}   Elapsed(msec): ${elapsed}   Per-sec: ${1000.0*N/elapsed}\n\n")
        then:
        elapsed < currentBestSec * 1000
        where:
        N        | _
        100000   | _
    }
}
