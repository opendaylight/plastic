
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

class TranslationPlanSpec extends Specification {

    def "a new plan has no explicit morphers"() {
        given:
        TranslationPlan<String,String> instance = new TranslationPlan<>("foo-schema", "bar-schema")
        expect:
        !instance.hasMorphers()
    }

    def "the first and last schemas can be accessed"() {
        given:
        TranslationPlan<String,String> instance = new TranslationPlan<>("foo-schema", "bar-schema")
        expect:
        instance.firstSchema() == "foo-schema"
        instance.lastSchema() == "bar-schema"
    }

    def "schemas can be iterated over"() {
        given:
        TranslationPlan<String,String> instance = new TranslationPlan<>("foo-schema", "bar-schema")
        instance.addSchema("boo")
        instance.addSchema("baz")
        when:
        int hits = 0
        instance.eachSchema { hits++ }
        then:
        hits == 4
    }

    def "morphers can be conditionally added"() {
        given:
        TranslationPlan<String,String> instance = new TranslationPlan<>("foo-schema", "bar-schema")
        when:
        instance.maybeAddMorpher(null)
        instance.maybeAddMorpher(null)
        then:
        !instance.hasMorphers()
    }

    def "the collection of morphers can be iterated over"() {
        given:
        TranslationPlan<String,String> instance = new TranslationPlan<>("foo-schema", "bar-schema")
        instance.maybeAddMorpher("A")
        instance.maybeAddMorpher("B")
        instance.maybeAddMorpher("C")
        when:
        int hits = 0
        instance.eachMorpher { hits++ }
        then:
        hits == 3
    }
}
