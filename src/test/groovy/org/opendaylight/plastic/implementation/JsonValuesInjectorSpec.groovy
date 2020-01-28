
/*
 * Copyright (c) 2019-2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.json.JsonSlurper
import spock.lang.Specification


class JsonValuesInjectorSpec extends Specification {

    JsonValuesInjector instance = new JsonValuesInjector()
    Set danglingInputs = []
    Set danglingOutputs = []

    def "single substitution for an element value"() {
        given:
        HashMap bound = [ 'abc': '123' ]
        Object jsonModel = new JsonSlurper().parseText("{\"xyz\":\"\${abc}\"}")

        when:
        instance.inject(bound, jsonModel, danglingInputs, danglingOutputs)

        then:
        jsonModel['xyz'] == '123'
    }

    def "multiple substitution single element value"() {
        given:
        HashMap bound = [ 'abc': '123' ]
        Object jsonModel = new JsonSlurper().parseText('''
            {\"name\":\"\${abc}\",
             \"address\":{\"street\":\"${abc}\"}}
        ''')
        when:
        instance.inject(bound, jsonModel, danglingInputs, danglingOutputs)

        then:
        jsonModel.name == '123'
        jsonModel.address.street == '123'
    }

    def "multiple substitutions multiple element values"() {
        given:
        HashMap bound = [ 'name': 'June Bug', 'address-street': '25 Merry Lane' ]
        Object jsonModel = new JsonSlurper().parseText('''
            {\"name\":\"\${name}\",
             \"full-name\":\"\${name}\",
             \"address\":{\"street\":\"${address-street}\"}}
        ''')
        when:
        instance.inject(bound, jsonModel, danglingInputs, danglingOutputs)

        then:
        jsonModel.name == 'June Bug'
        jsonModel['full-name'] == 'June Bug'
        jsonModel.address.street == '25 Merry Lane'
    }

    def "multiple substitutions multiple types of element values"() {
        given:
        HashMap bound = [ 'name': 'June Bug', 'age': '25', 'alive': 'true' ]
        Object jsonModel = new JsonSlurper().parseText('''
            {\"name\":\"\${name}\",
             \"vitals\":{\"age\":\"${age}\", \"alive\":\"${alive}\"}}
        ''')
        when:
        instance.inject(bound, jsonModel, danglingInputs, danglingOutputs)

        then:
        jsonModel.name == 'June Bug'
        jsonModel.vitals.age == '25'
        jsonModel.vitals.alive == 'true'
    }

    def "multiple substitutions are supported as a single value"() {
        given:
        HashMap bound = [ 'ggg': '123' ]
        Object model = new JsonSlurper().parseText('''
            {\"abc\":\"\${ggg}+\${ggg} = 2*\${ggg}\"}
        ''')

        when:
        model = instance.inject(bound, model, danglingInputs, danglingOutputs)

        then:
        model.abc == "123+123 = 2*123"
    }

    def "multiple values can be substituted as a single value"() {
        given:
        HashMap bound = [ 'ggg': '123', 'hhh': '456' ]
        Object model = new JsonSlurper().parseText('''
            {\"abc\":\"\${ggg}+\${hhh} != 2*\${ggg}\"}
        ''')

        when:
        model = instance.inject(bound, model, danglingInputs, danglingOutputs)

        then:
        model.abc == "123+456 != 2*123"
    }

    def "an input variable that is not mapped to an output is detected as an error"() {
        given:
        HashMap bound = [ 'ggg': '123', 'hhh': 456 ]
        Object model = new JsonSlurper().parseText('''
            {\"abc\":\"\${ggg}\"}
        ''')

        when:
        instance.inject(bound, model, danglingInputs, danglingOutputs)

        then:
        !danglingInputs.isEmpty()
    }

    def "an output variable that is not mapped to an input is detected as an error"() {
        given:
        HashMap bound = [ 'ggg': '123' ]
        Object model = new JsonSlurper().parseText('''
            {\"abc\":\"\${hhh}\"}
        ''')

        when:
        instance.inject(bound, model, danglingInputs, danglingOutputs)

        then:
        !danglingOutputs.isEmpty()
    }

    def "input variable types are retained when variable is complete value"() {
        given:
        HashMap bound = [ 'str': '123', 'num': 123, 'duble': 123.456, 'bool': true ]
        Object model = new JsonSlurper().parseText('''
            {
              \"str\":\"\${str}\",
              \"num\":\"\${num}\",
              \"duble\":\"\${duble}\",
              \"bool\":\"\${bool}\"
            }
        ''')

        when:
        instance.inject(bound, model, danglingInputs, danglingOutputs)

        then:
        model.str instanceof String
        model.num instanceof Number
        model.duble instanceof Number
        model.bool instanceof Boolean
    }

    def "multiple substitutions multiple types of element values types retained"() {
        given:
        HashMap bound = [ 'name': 'June Bug', 'age': 25, 'alive': true ]
        Object jsonModel = new JsonSlurper().parseText('''
            {\"name\":\"\${name}\",
             \"vitals\":{\"age\":\"${age}\", \"alive\":\"${alive}\"}}
        ''')
        when:
        instance.inject(bound, jsonModel, danglingInputs, danglingOutputs)

        then:
        jsonModel.name == 'June Bug'
        jsonModel.vitals.age == 25
        jsonModel.vitals.alive == true
    }

    def "type of value lost when used as part of an output schema value"() {
        given:
        HashMap bound = [ 'name': 'June Bug', 'age': 25, 'alive': true ]
        Object model = new JsonSlurper().parseText('''
            {\"person\":\"\${name}\",
             \"description\":\"\${name} is \${age} years old (alive: \${alive})\"}
        ''')
        when:
        instance.inject(bound, model, danglingInputs, danglingOutputs)

        then:
        model.person == 'June Bug'
        model.description == 'June Bug is 25 years old (alive: true)'
    }

    def "a simple array of scalar values is expanded properly"() {
        given:
        def bound = ['ADDR[0]': "1.2.3.4",
                     'ADDR[1]': "5.6.7.8",
                     'ADDR[2]': "9.10.11.12",
                     '_[ADDR[*]]': '[3]'
        ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "addresses": [ "${ADDR[*]}" ]
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.addresses == [ "1.2.3.4", "5.6.7.8", "9.10.11.12" ]
    }

    def "full value replacement can use bound values of various types"() {
        given:
        def bound = [ 'ABC': value ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "test": "${ABC}"
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.test == expected
        where:
        value                | expected
        null                 | ""
        "1"                  | "1"
        123                  | 123
        true                 | true
        12345678901234567890 | 12345678901234567890
        [ 'a' ]              | [ 'a' ]
        [ 'a': 1 ]           | [ 'a': 1 ]
    }

    def "partial value replacement using bound values of scalar types results in string values"() {
        given:
        def bound = [ 'ABC': value ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "test": "abc ${ABC}"
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.test == expected
        where:
        value                | expected
        null                 | "abc "
        "1"                  | "abc 1"
        123                  | "abc 123"
        true                 | "abc true"
        12345678901234567890 | "abc 12345678901234567890"
    }

    def "edge: partial value replacement for non-scalar bound values stringifies them"() {
        given:
        def bound = [ 'ABC': value ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "test": "abc ${ABC}"
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.test == expected
        where:
        value                | expected
        [ 'a' ]              | 'abc [a]'
        [ 'a': 1 ]           | "abc {a=1}"
    }

    def "a multi-variable array of scalar values is expanded properly"() {
        given:
        def bound = ['ADDR[0]': "1.2.3.4",
                     'ADDR[1]': "5.6.7.8",
                     'ADDR[2]': "9.10.11.12",
                     '_[ADDR[*]]': '[3]'
        ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "addresses": [ "AAA-${ADDR[*]}", "BBB-${ADDR[*]}" ]
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.addresses == [ "AAA-1.2.3.4",
                                        "BBB-1.2.3.4",
                                        "AAA-5.6.7.8",
                                        "BBB-5.6.7.8",
                                        "AAA-9.10.11.12",
                                        "BBB-9.10.11.12" ]
    }

    def "an array of scalar values is can result in an empty expansion without error"() {
        given:
        def bound = [:]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "addresses": [ "${ADDR[*]}" ]
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.addresses == []
    }

    def "an array of object values is expanded properly"() {
        given:
        def bound = ['ADDR[0]': "1.2.3.4",
                     'ADDR[1]': "5.6.7.8",
                     'ADDR[2]': "9.10.11.12",
                     '_[ADDR[*]]': '[3]',
                     "EXTRA" : "foobar"
        ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "components": [
                {
                  "address": "${ADDR[*]}",
                  "extra": "${EXTRA}"
                }
            ]
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.components == [
                [ "address" : "1.2.3.4", "extra" : "foobar" ],
                [ "address" : "5.6.7.8", "extra" : "foobar" ],
                [ "address" : "9.10.11.12", "extra" : "foobar"]
        ]
    }

    def "a mulitiple-variable array of object values is expanded properly"() {
        given:
        def bound = ['ADDR[0]': "1.2.3.4",
                     'ADDR[1]': "5.6.7.8",
                     'ADDR[2]': "9.10.11.12",
                     '_[ADDR[*]]': '[3]',
                     "EXTRA" : "foobar"
        ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "components": [
                { 
                  "address": "AA-${ADDR[*]}",
                  "extra": "AA-${EXTRA}"
                },
                { 
                  "address": "BB-${ADDR[*]}",
                  "extra": "BB-${EXTRA}"
                }
            ]
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.components == [
                [ "address" : "AA-1.2.3.4", "extra" : "AA-foobar" ],
                [ "address" : "BB-1.2.3.4", "extra" : "BB-foobar" ],
                [ "address" : "AA-5.6.7.8", "extra" : "AA-foobar" ],
                [ "address" : "BB-5.6.7.8", "extra" : "BB-foobar" ],
                [ "address" : "AA-9.10.11.12", "extra" : "AA-foobar"],
                [ "address" : "BB-9.10.11.12", "extra" : "BB-foobar"]
        ]
    }

    def "an array of object values is expanded properly with multiple variables in a single output"() {
        given:
        def bound = ['ADDR[0]': "1.2.3.4",
                     'ADDR[1]': "5.6.7.8",
                     '_[ADDR[*]]': '[2]',
                     'LEN[0]' : 10,
                     'LEN[1]' : 20,
                     '_[LEN[*]]': '[2]'
        ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "components": [
                {
                  "address": "${ADDR[*]}/${LEN[*]}",
                }
            ]
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.components == [
                [ "address" : "1.2.3.4/10" ],
                [ "address" : "5.6.7.8/20" ]
        ]
    }

    def "an array of objects with sub-object values is expanded properly with multiple variables in a single output"() {
        given:
        def bound = ['ADDR[0]': "1.2.3.4",
                     'ADDR[1]': "5.6.7.8",
                     '_[ADDR[*]]': '[2]',
                     'LEN[0]' : 10,
                     'LEN[1]' : 20,
                     '_[LEN[*]]': '[2]',
                     'UNUSED' : 'dummy' // bug defense: should not interfere with result
        ]
        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "components": [
                {
                  "subcomponent": {
                    "address": "${ADDR[*]}/${LEN[*]}",
                  }
                }
            ]
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.components == [
                [ "subcomponent": ["address" : "1.2.3.4/10" ]],
                [ "subcomponent": ["address" : "5.6.7.8/20" ]]
        ]
    }

    def "a non-scalar value is injected properly"() {
        given:
        def bound = ['ABC': "123",
                     'DEF' : [ "dd", "ee", "ff" ] ]

        Object payloadAndOutput = new JsonSlurper().parseText('''
        {
            "components": [
                {
                  "here": "${ABC}",
                  "there": "${DEF}"
                }
            ]
        }
        ''')
        when:
        instance.inject(bound, payloadAndOutput, danglingInputs, danglingOutputs)

        then:
        payloadAndOutput.components == [
                [
                        "here" : "123",
                        "there" : [ "dd", "ee", "ff" ]
                ]
        ]
    }

    def "emitting large integers does not use scientific notation"() {
        given:
        HashMap bound = ['abc': 15000000]
        Object jsonModel = new JsonSlurper().parseText("{\"xyz\":\"\${abc}\"}")

        when:
        instance.inject(bound, jsonModel, danglingInputs, danglingOutputs)

        then:
        jsonModel['xyz'] == 15000000
    }
}
