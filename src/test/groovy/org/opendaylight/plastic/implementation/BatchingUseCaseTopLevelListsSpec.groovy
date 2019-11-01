
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

class BatchingUseCaseTopLevelListsSpec extends Specification {

    // Classifier that iterates over the batch and sets up the child classifiers and
    // replaces the flex points in the payload with variables
    //
    class MyBatchAwareClassifier extends PlanningClassifier {

        @Override
        TranslationPlanLite classify(Schema payload, TranslationPlanLite plan) {
            Object tree = payload.getParsed()
            BetterJson better = new BetterJson(tree)
            List container = better.asList()

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

    String testPayload = '''
    [ 
        {
            "name": "abc",
            "value": 1
        },
        {
            "name": "def",
            "value": 2
        },
        {
            "name": "ghi",
            "value": 3
        }
    ]
    '''

    String batchInSchema = '''
    [
        "${items[*]}"
    ]
    '''

    String batchOutSchema = '''
    {
        "stuff": [
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

    String expected = '''
{ "stuff": 
    [
        {
            "name-value": "abc|1"
        },
        {
            "name-value": "def|2",
        },
        {
            "name-value": "ghi|3",
        }
    ]
    }
    '''

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
        String result = instance.translate(inSchema, outSchema, testPayload)
        then:
        new BetterJson(result).asObject() == new BetterJson(expected).asObject()
    }
}
