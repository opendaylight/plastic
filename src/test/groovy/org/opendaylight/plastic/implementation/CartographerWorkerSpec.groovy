
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import spock.lang.Specification

class CartographerWorkerSpec extends Specification {

    VersionedSchema testInputSchema = Mock()
    VersionedSchema testOutputSchema = Mock()
    String testPayload = "{}"

    def "translate requires an input schema object parameter"() {
        given:
        CartographerWorker instance = new CartographerWorker()
        when:
        instance.translate(null, testOutputSchema, testPayload)
        then:
        thrown(NullPointerException)
    }

    def "translate requires an output schema object parameter"() {
        given:
        CartographerWorker instance = new CartographerWorker()
        when:
        instance.translate(testInputSchema, null, testPayload)
        then:
        thrown(NullPointerException)
    }

    def "translate requires a payload parameter"() {
        given:
        CartographerWorker instance = new CartographerWorker()
        when:
        instance.translate(testInputSchema, testOutputSchema, null)
        then:
        thrown(NullPointerException)
    }

    def "early calls to translate must wait until initialization completes"() {
        given:
        VersionedSchema inschema = new VersionedSchema("in-schema-unit-test", "1.0", "json")
        VersionedSchema outschema = new VersionedSchema("out-schema-unit-test", "1.0", "json")
        and:
        Poller poller = new Poller(10, { Pollee p, int pass ->  sleep(100) /* Sluggish LibraryLoader */ })
        and:
        PlanResolution mockResolution = Mock()
        MapTask mockTask = Mock()
        Schema mockSchema = Mock()
        mockTask.map(*_) >> mockSchema
        mockResolution.lookupMappings(_) >> mockTask
        and:
        CartographerWorker instance = new CartographerWorker(ShortCircuit.useStandardCaches(), poller, mockResolution, null)
        when:
        instance.translate(inschema, outschema, testPayload) // should block until initialization
        int passCompleted = poller.passes
        then:
        passCompleted == 1 // first pass is completed because of synchronization
    }

    def "an in-flight translate call should cause a skipped polling interval"() {
        given:
        VersionedSchema inschema = new VersionedSchema("in-schema-unit-test", "1.0", "json")
        VersionedSchema outschema = new VersionedSchema("out-schema-unit-test", "1.0", "json")
        and:
        Poller poller = new Poller(1, { Pollee p, int pass ->  })
        and:
        PlanResolution mockResolution = Mock()
        MapTask mockTask = Mock()
        Schema mockPvs = Mock()
        mockTask.map(*_) >> mockPvs
        mockResolution.lookupMappings(_) >> { println("faux translation"); sleep(2500); mockTask }
        and:
        CartographerWorker instance = new CartographerWorker(ShortCircuit.useStandardCaches(), poller, mockResolution, null)
        when:
        instance.translate(inschema, outschema, testPayload)
        then:
        poller.countPastSuppressions() == 2 // 2 passes * 1 sec < 2.5 sec or skip@t=1sec, skip@t=2sec, hit@t=3sec
    }

    // Cannot use normal Spock stubbing logic because it is synchronized and that defeats
    // the concurrency validation for this next test case. So have to use ad hoc test doubles.

    static class TestMapTask extends MapTask {

        static TranslationPlan<Schema,Morpher> fakeplan
        static {
            fakeplan = new TranslationPlan<Schema, Morpher>()
            fakeplan.schemas.add(null)
        }

        TestMapTask() {
            super(fakeplan)
        }

        @Override
        Schema map(Schema parsedPayload, Schema parsedDefaults) {
            Thread.currentThread().sleep(500)
            return simplestJson()
        }

        Schema simplestJson() {
            VersionedSchema schema = new VersionedSchema("foobar", "1.0", "json")
            String small = "{}"
            new Schema(schema, small)
        }
    }

    def "translate calls should overlap (not be serialized)"() {
        given:
        VersionedSchema inschema = new VersionedSchema("in-schema-unit-test", "1.0", "json")
        VersionedSchema outschema = new VersionedSchema("out-schema-unit-test", "1.0", "json")
        Poller poller = new Poller(10, { Pollee p, int pass -> })
        PlanResolution mockResolution = Mock()

        def mockTasks = [ new TestMapTask(), new TestMapTask(), new TestMapTask(), new TestMapTask() ]
        mockResolution.lookupMappings(*_) >>> mockTasks
        and:
        CartographerWorker instance = new CartographerWorker(ShortCircuit.useStandardCaches(), poller, mockResolution, null)
        when:
        def threads = []
        mockTasks.size().times {
            threads << new Thread({
                instance.translate(inschema, outschema, testPayload)
            })
        }
        threads.each { it.start() }
        threads.each { it.join()  }
        then:
        poller.maxConcurrency() == threads.size()
    }
}
