
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


import org.opendaylight.plastic.implementation.author.BetterJson
import org.opendaylight.plastic.implementation.author.Plans
import spock.lang.Specification


class BatchingUseCaseSpec extends Specification {

    def benchmark = { cls ->
        def start = System.currentTimeMillis()
        cls.call()
        def now = System.currentTimeMillis()
        now - start
    }

    final int N = 10000

    class RandomVarBinds {

        Tuple[] varBindPool = [ new Tuple('abc', 1), new Tuple('def', 2), new Tuple('ghi', 3) ]
        Random random = new Random()
        int len = varBindPool.size()

        Tuple next() {
            int index = Math.abs(random.nextInt() % len)
            varBindPool[index]
        }

        List collected(int n) {
            List results = []
            n.times { results << next() }
            results
        }
    }

    final List varBinds = new RandomVarBinds().collected(N)

    Schema asSchema(String strm) {
        VersionedSchema verschema = new VersionedSchema("foo", "1.0", "json")
        new Schema(verschema, strm)
    }

    String expand(String jsonSchema, List names, List<Tuple> varBinds) {
        varBinds.collect { Tuple t ->
            Set danglingIn = []
            Set danglingOut = []
            Map bound = [ (names[0]): t[0], (names[1]): t[1] ]
            Schema pj = asSchema(jsonSchema)
            pj.inject(bound, danglingIn, danglingOut)
            pj.emit()
        }.join("\n,\n")
    }

    // Classifier that iterates over the batch and sets up the child classifiers and
    // replaces the flex points in the payload with variables
    //
    class MyBatchAwareClassifier extends PlanningClassifier {

        @Override
        TranslationPlanLite classify(Schema payload, TranslationPlanLite plan) {
            Object tree = payload.getParsed()
            BetterJson better = new BetterJson(tree)
            List container = better.asList('container-in')

            TranslationPlanLite parent = Plans.newParent(
                    Plans.asSchema("batch-in-schema", "1.0", "json"),
                    plan.lastSchema())

            for (int i = 0; i< container.size(); i++) {
                TranslationPlanLite child = Plans.newPlan(
                        Plans.asSchema("\${concrete-classifier}", "1.0", "json"),
                        Plans.asSchema("concrete-out-schema", "1.0", "json"))

                Plans.realizeChildPlan(child, "my-batch-aware", payload, container, i)
                parent.addChild(child)
            }

            parent
        }
    }

    // Classifier that is run against each item of the batch to set up each plan
    //
    class ConcreteClassifier extends SimpleClassifier {
        @Override
        String classify(Object parsedPayload) {
            return "concrete-in-schema"
        }
    }

    String batchInSchema = '''
    {
        "container-in": [
            "${items[*]}"
        ]
    }
    '''

    String batchOutSchema = '''
    {
        "container-out": [
            "${items[*]}"
        ]
    }
    '''

    String concreteInSchema = '''
    {
        "name": "${my-name}",
        "value": "${my-value}"
    }
    '''

    String concreteOutSchema = '''
    {
        "name-value": "${my-name}|${my-value}"
    }
    '''

    String testPayload = """
    {
        "container-in": [
            ${expand(concreteInSchema, ["my-name","my-value"], varBinds)}
        ]
    }
    """

    String expected = """
    {
        "container-out": [
            ${expand(concreteOutSchema, ["my-name","my-value"], varBinds)}
        ]
    }
    """

    def "translate can use a classifier that sets up and runs batching"() {
        given:
        Poller poller = new Poller()

        SchemaLoader modelStreamLocator = new ShortCircuit.TestSchemaLoader()
                .add("concrete-in-schema",  concreteInSchema)
                .add("concrete-out-schema", concreteOutSchema)
                .add("batch-in-schema",     batchInSchema)
                .add("batch-out-schema",    batchOutSchema)


        MorpherLoader morpherFactory = new ShortCircuit.TestMorpherFactory()

        PlanResolution mapResolver = new ShortCircuit.TestPlanResolution(modelStreamLocator, morpherFactory)

        ShortCircuit.TestClassifierResolver planResolver = new ShortCircuit.TestClassifierResolver()
                .addDeluxe("batch-aware-classifier", new MyBatchAwareClassifier())
                .addSimple("concrete-classifier",    new ConcreteClassifier())

        and:
        VersionedSchema inSchema = new VersionedSchema("\${batch-aware-classifier}", "1.0", "json")
        VersionedSchema outSchema = new VersionedSchema("batch-out-schema", "1.0", "json")
        and:
        CartographerWorker instance = new CartographerWorker(ShortCircuit.useStandardCaches(), poller, mapResolver, planResolver)
        when:
        String result
        int elapsed = benchmark {
            result = instance.translate(inSchema, outSchema, testPayload)
        }
        println("Batch translations ... N: ${N}  Elapsed (msec): ${elapsed}")
        then:
        new BetterJson(result).asObject() == new BetterJson(expected).asObject()
    }
}
