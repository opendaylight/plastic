
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.sun.org.apache.xpath.internal.operations.Bool
import spock.lang.Specification

class JsonFormatSpec extends Specification {

    JsonFormat instance = new JsonFormat()

    def "happy path formatting works"() {
        given:
        List l = [ ["abc": 123], ["def": 456], ["ghi": 789] ]
        when:
        String result = instance.serialize(l)
        then:
        result == '''[
    {
        "abc": 123
    },
    {
        "def": 456
    },
    {
        "ghi": 789
    }
]'''
    }

    def "low-level clone works"() {
        given:
        Object parsed = instance.parse('{ "abc": [ { "value": "a" }, { "value": 2 }, { "value": 3.333 } ] }')
        when:
        Object cloned1 = instance.clone(parsed)
        Object cloned2 = instance.clone(parsed)
        and:
        Object mutated = instance.clone(parsed)
        mutated['abc'][2]['value'] = 'd'
        then:
        cloned1 == cloned2
        cloned1 != mutated
    }

    def "original types are preserved when parsing"() {
        when:
        Map parsed = instance.parse('{ "array": [ { "integer": 3 }, { "double": 2.34 }, { "boolean": true } ] }')
        then:
        parsed['array'] instanceof List
        List list = parsed['array']
        list[0]['integer'] instanceof Integer
        list[1]['double'] instanceof BigDecimal
        list[2]['boolean'] instanceof Boolean
    }
}
