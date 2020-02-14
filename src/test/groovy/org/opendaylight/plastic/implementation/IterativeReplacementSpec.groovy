/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation

import groovy.json.JsonSlurper
import spock.lang.Specification


class IterativeReplacementSpec extends Specification {

    def bindings = ['ADDR[0]': "1.2.3.4",
                    'ADDR[1]': "5.6.7.8",
                    'ADDR[2]': "9.10.11.12",
                    '_[ADDR[*]]': '[3]',
                    'LEN[0]': 2,
                    'LEN[1]': 4,
                    'LEN[2]': 6,
                    '_[LEN[*]]': '[3]'
    ]
    Set<String> danglingOutputs = []
    Set<String> foundInputs = []

    IterativeReplacement instance = new IterativeReplacement(bindings, danglingOutputs, foundInputs)

    def "recursive replacing works for lists"() {
        given:
        Object model = new JsonSlurper().parseText('''
                ["${ADDR[0]}/${LEN[0]}"]
        ''')
        and:
        Object expected = new JsonSlurper().parseText('''
                ["1.2.3.4/2"]
        ''')
        when:
        instance.processModel(model)
        then:
        model == expected
    }

    def "recursive replacing works for lists in lists"() {
        given:
        Object model = new JsonSlurper().parseText('''
        {
            "components": [
                ["${ADDR[0]}/${LEN[0]}"]
            ]
        }
        ''')
        and:
        Object expected = new JsonSlurper().parseText('''
        {
            "components": [
                ["1.2.3.4/2"]
            ]
        }
        ''')
        when:
        instance.processModel(model)
        then:
        model == expected
    }

    def "recursive replacing works for maps"() {
        given:
        Object model = new JsonSlurper().parseText('''
        {
            "address": "${ADDR[2]}/${LEN[2]}"
        }
        ''')
        and:
        Object expected = new JsonSlurper().parseText('''
        {
            "address": "9.10.11.12/6"
        }
        ''')
        when:
        instance.processModel(model)
        then:
        model == expected
    }

    def "recursive replacing works for maps in lists"() {
        given:
        Object model = new JsonSlurper().parseText('''
        {
            "components": [
                {
                  "subcomponent": {
                    "address": "${ADDR[2]}/${LEN[2]}",
                  }
                }
            ]
        }
        ''')
        and:
        Object expected = new JsonSlurper().parseText('''
        {
            "components": [
                {
                  "subcomponent": {
                    "address": "9.10.11.12/6",
                  }
                }
            ]
        }
        ''')
        when:
        instance.processModel(model)
        then:
        model == expected
    }
}
