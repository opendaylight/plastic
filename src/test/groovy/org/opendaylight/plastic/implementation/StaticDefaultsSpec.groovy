
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
import spock.lang.Specification


class StaticDefaultsSpec extends Specification {

    String testPayload = '''
    {
        "value1": "rose"
    }
    '''

    String inSchema = '''
    {
        "value1": "${color1=pink}",
        "value2": "${color2=lime}",
        "value3": "${color3=cyan}",
    }
    '''

    String outSchema = '''
    {
        "red": "${color1}",
        "green": "${color2}",
        "blue": "${color3}"
    }
    '''

    String expected = '''
    {
        "red": "rose",
        "green": "lime",
        "blue": "cyan"
    }
    '''

    class ConcreteClassifier extends SimpleClassifier {
        @Override
        String classify(Object parsedPayload) {
            return "in-schema"
        }
    }

    def "translate can use a classifier that sets up and runs batching"() {
        given:
        Poller poller = new Poller()

        SchemaLoader modelStreamLocator = new ShortCircuit.TestSchemaLoader()
                .add("in-schema",     inSchema)
                .add("out-schema",    outSchema)

        MorpherLoader morpherFactory = new ShortCircuit.TestMorpherFactory()
        PlanResolution mapResolver = new ShortCircuit.TestPlanResolution(modelStreamLocator, morpherFactory)

        ShortCircuit.TestClassifierResolver planResolver = new ShortCircuit.TestClassifierResolver()
                .addSimple("concrete-classifier",    new ConcreteClassifier())

        and:
        VersionedSchema inSchema = new VersionedSchema("\${concrete-classifier}", "1.0", "json")
        VersionedSchema outSchema = new VersionedSchema("out-schema", "1.0", "json")
        and:
        CartographerWorker instance = new CartographerWorker(ShortCircuit.useStandardCaches(), poller, mapResolver, planResolver)
        when:
        String result = instance.translate(inSchema, outSchema, testPayload)
        then:
        new BetterJson(result).asObject() == new BetterJson(expected).asObject()
    }
}
