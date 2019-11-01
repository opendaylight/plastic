
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

class ChunkyJsonFormatSpec extends Specification {

    ChunkyJsonFormat instance = new ChunkyJsonFormat()

    def "a chunky json sequence is parsed"() {
        when:
        Object parsed = instance.parse(
'''-
        {
            "abc": 123
        }
-                    
        {
            "def": 456
        }
-                    
        {
            "ghi": 789
        }
-''')
        then:
        parsed == [ ["abc": 123], ["def": 456], ["ghi": 789] ]
    }

    def "a chunky json sequence can be created by serialization"() {
        given:
        List l = [ ["abc": 123], ["def": 456], ["ghi": 789] ]
        when:
        String result = instance.serialize(l)
        then:
        result == '''-\n{\n    "abc": 123\n}\n-\n{\n    "def": 456\n}\n-\n{\n    "ghi": 789\n}\n-'''
    }

    def "low-level clone works"() {
        given:
        Object parsed = instance.parse('-\n{ "abc": [ { "value": "a" }, { "value": 2 }, { "value": 3.333 } ] }\n-')
        when:
        Object cloned1 = instance.clone(parsed)
        Object cloned2 = instance.clone(parsed)
        and:
        Object mutated = instance.clone(parsed)
        mutated[0]['abc'][2]['value'] = 'd'
        then:
        cloned1 == cloned2
        cloned1 != mutated
    }
}
