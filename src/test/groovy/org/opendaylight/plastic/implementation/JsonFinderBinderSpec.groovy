
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.slf4j.Logger
import spock.lang.Ignore
import spock.lang.Specification

class JsonFinderBinderSpec extends Specification {

    Logger mockLogger = Mock()

    JsonSlurper slurper = new JsonSlurper()
    JsonFinderBinder instance = new JsonFinderBinder()

    static final UNI_ID = "UNI_1234"

    def testModel() {
        def builder = new JsonBuilder()
        def uniModelEntries = [ [ "nui-id": "\${nui-id}" ] ]

        builder {
            uni(uniModelEntries.each { entry ->
                entry.collect { key, value ->
                    final def var = [ "$key": "$value" ]
                    var
                } }) }
        builder.toString()
    }

    def testPayload() {
        def builder = new JsonBuilder()
        def uniModelEntries = [ [ "nui-id": "$UNI_ID" ] ]

        builder {
            uni(uniModelEntries.each { entry ->
                entry.collect { key, value ->
                    final def var = [ "$key": "$value" ]
                    var
                } }) }
        builder.toString()
    }

    def array1Model() {
        '''
        {
            "addresses": [ "${ADD[*]}" ]
        }
        '''
    }

    def array1Payload() {
        '''
        {
            "addresses": [ "1.2.3.4", "5.6.7.8", "9.10.11.12" ]
        }
        '''
    }

    def array1EmptyArrayPayload() {
        '''
        {
            "addresses": []
        }
        '''
    }

    def array2Payload() {
        '''
        {
            "components": [
            { "address": "1.2.3.4" },
            { "address": "5.6.7.8" },
            { "address": "9.10.11.12" }
            ]
        }
        '''
    }

    def array2Model() {
        '''
        {
            "components": [
            { "address": "${ADD[*]}" }
            ]
        }
        '''
    }

    def array3Payload() {
        '''
        {
            "components-1": [
                "1.2.3.4",
                "5.6.7.8",
                "9.10.11.12"
            ],
            "components-2": [
                "1.2.3.4",
                "5.6.7.8",
                "9.10.11.12",
                "13.14.15.16"
            ]
        }
        '''
    }

    def array3Model() {
        '''
        {
            "components-1": [
                "${VAL1[*]}"
            ],
            "components-2": [
                "${VAL2[*]}"
            ]
        }
        '''
    }

    def multivarsPayload() {
        '''
        {
            "prop": "123"
        }
        '''
    }

    def multivarsModel() {
        '''
        {
            "prop": "${aa}${bb}"
        }
        '''
    }

    def nestedListsPayload() {
        '''
        [ [ { "complex": "array" }, { "complex": "nested" } ], [ { "complex": "lists" }, { "complex": "maps" } ] ]
        '''
    }

    def nestedListsModel() {
        '''
        [ [ "${object-list[*]}" ] ]
        '''
    }

    def expectedNestedValues = ["array", "nested", "lists", "maps"]

    def "internal path creation works"() {
        expect:
        instance.concatPath("") == ""
        instance.concatPath("a") == "a"
        instance.concatPath("","a") == "a"
        instance.concatPath("", "ab", "", "cd") == "ab.cd"
    }

    def "a variable is found in a model and its value is bound"() {
        given:
        def modelJson = testModel()
        def payloadJson = testPayload()

        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found["nui-id"] == UNI_ID
    }

    def "variables are bound to paths"() {
        given:
        def modelJson = testModel()
        def model = slurper.parseText(modelJson)

        when:
        Map pathVars = [:]
        Map varVals = [:]
        instance.buildPathsToVariables(model, pathVars, varVals)

        then:
        pathVars.keySet() == [ 'uni.[].nui-id' ] as Set
    }

    def "variables are bound to paths that can contain dots"() {
        given:
        String modelJson = '''
        {
            "uni.verse": [
                {
                    "uni.verse-id": "${nui-1}"
                }
            ]
        }
        '''
        def model = slurper.parseText(modelJson)
        when:
        Map pathVars = [:]
        Map varVals = [:]
        instance.buildPathsToVariables(model, pathVars, varVals)

        then:
        pathVars.keySet() == [ 'uni\\.verse.[].uni\\.verse-id' ] as Set
    }

    def "variables bound to paths fetch their values from a payload"() {
        given:
        def pathVars = ['uni.[].nui-id': new SimpleVariableBinder('nui-id')]

        def payloadJson = testPayload()
        def payload = slurper.parseText(payloadJson)

        when:
        def boundVars = instance.fetchVarToValues(pathVars, payload)

        then:
        boundVars['nui-id'] == UNI_ID
    }

    def "retrieves the value of a mapped JSON element based on a path"() {
        given:
        def path = 'uni.[].nui-id'
        def payloadJson = testPayload()
        def payload = slurper.parseText(payloadJson)

        JsonFinderBinder.Recorder recorder = new JsonFinderBinder.Recorder()
        recorder.useBinder(new SimpleVariableBinder("test-variable"))
        when:
        instance.getPathValue(path, payload, recorder)

        then:
        recorder.bindings()["test-variable"] == UNI_ID
    }

    def "retrieves the value of a mapped JSON element based on a path that has dots"() {
        given:
        def path = 'uni\\.verse.[].uni\\.verse-id'
        and:
        String payloadJson = '''
        {
            "uni.verse": [
                {
                    "uni.verse-id": "123"
                }
            ]
        }
        '''
        def payload = slurper.parseText(payloadJson)
        and:
        JsonFinderBinder.Recorder recorder = new JsonFinderBinder.Recorder()
        recorder.useBinder(new SimpleVariableBinder("test-variable"))

        when:
        instance.getPathValue(path, payload, recorder)

        then:
        recorder.bindings()["test-variable"] == "123"
    }

    def "detect * being bound to various sized arrays"() {
        given:
        String jpayload = '''
        {
            "array1": [
                {
                    "member": "123"
                }
            ],
            "array2": [
                {
                    "member": "456",
                },
                {
                    "member": "789"
                }
            ]
        }
        '''
        and:
        def path1 = 'array1.[].member'
        def path2 = 'array2.[].member'
        and:
        def payload = slurper.parseText(jpayload)
        and:
        JsonFinderBinder.Recorder recorder = new JsonFinderBinder.Recorder()

        when:
        ['one': path1, 'two': path2].each { String name, String path ->
            recorder.useBinder (new SimpleVariableBinder("$name[*]"))
            instance.getPathValue(path, payload, recorder)
        }

        then:
        !recorder.hasUniformValues()
    }

    def "an scalar variable in an array is found in a model and its value is bound"() {
        given:
        def model = slurper.parseText(array1Model())
        def payload = slurper.parseText(array1Payload())

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found["ADD[0]"] == "1.2.3.4"
        found["ADD[1]"] == "5.6.7.8"
        found["ADD[2]"] == "9.10.11.12"
    }

    def "an scalar variable in an empty array is not an error"() {
        given:
        def model = slurper.parseText(array1Model())
        def payload = slurper.parseText(array1EmptyArrayPayload())

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found == [:]
    }

    def "an hash variable in an array is found in a model and its value is bound"() {
        given:
        def model = slurper.parseText(array2Model())
        def payload = slurper.parseText(array2Payload())

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found["ADD[0]"] == "1.2.3.4"
        found["ADD[1]"] == "5.6.7.8"
        found["ADD[2]"] == "9.10.11.12"
    }

    def "array bindings of different sizes can be made visible"() {
        given:
        def model = slurper.parseText(array3Model())
        def payload = slurper.parseText(array3Payload())

        instance.logger = mockLogger
        when:
        instance.process(model, payload).bindings()

        then:
        1 * mockLogger.debug({ it =~ "ARRAY-SIZES" })
    }

    // Conceptual rub that needs resovling eventually. This class is used on both the input
    // and output side. Multiple variables in the same "value slot" on the output make
    // sense. But on the input side, it is ambiguous; it is almost a poor man's regex if
    // we implemented it. For now we are just letting the value itself be duplicated for
    // each variable.

    def "multiple values can be found without masking each other"() {
        given:
        def model = slurper.parseText(multivarsModel())
        def payload = slurper.parseText(multivarsPayload())

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found["aa"] == "123"
        found["bb"] == "123"
    }

    def "multiple values on the same input are warned about"() {
        given:
        def model = slurper.parseText(multivarsModel())
        def payload = slurper.parseText(multivarsPayload())
        and:
        instance.logger = mockLogger
        when:
        instance.process(model, payload).bindings()

        then:
        1 * instance.logger.warn({ it =~ "MULT-IN-VARS" }, _)
    }

    def "large integers are not emitted in scientific notation"() {
        given:
        def model = slurper.parseText('{ "ABC": "${abc}" }')
        def payload = slurper.parseText('{ "ABC": 15000000 }')

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found["abc"] == 15000000
    }

    def "a variable from the model but not found in the payload has a null value"() {
        given:
        def modelJson = '''
        {
            "something": "${abc}"
        }
        '''
        def payloadJson = '{}'

        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found.containsKey("abc")
        found["abc"] == null
    }

    def "a shallow variable from the model but not found in the payload has a default value"() {
        given:
        def modelJson = '''
        {
            "something": "${abc=1234}"
        }
        '''
        def payloadJson = '{}'

        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found["abc"] == "1234"
    }

    def "a deep variable from the model but not found in the payload has a default value"() {
        given:
        def modelJson = '''
        {
            "parent": {
                "something": "${abc=1234}"
            }
        }
        '''
        def payloadJson = '{}'

        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found["abc"] == "1234"
    }

    def "a default value does not override an explicit value"() {
        given:
        def modelJson = '''
        {
            "something": "${abc=222}"
        }
        '''
        def payloadJson = '''
        {
            "something": "333"
        }
        '''

        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found["abc"] == "333"
    }

    def "multiple variables can have individual defaults"() {
        given:
        def modelJson = '''
        {
            "something": "${abc=111}${def=222}"
        }
        '''
        def payloadJson = '{}'

        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found["abc"] == "111"
        found["def"] == "222"
    }

    def "multiple variables with mixed presence in the payload have correct defaults"() {
        given:
        def modelJson = '''
        {
            "something": "${abc=111}${def}"
        }
        '''
        def payloadJson = '{}'

        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found["abc"] == "111"
        found.containsKey("def") && found["def"] == null
    }

    def "a non-scalar value can be bound to a variable - needed for some defaults and for batch"() {
        given:
        def modelJson = '''
        {
            "something": "${abc}"
        }
        '''
        def payloadJson = '''
        {
            "something": {
               "good": 1
            }
        }
        '''

        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found["abc"] == ['good': 1]
    }

    def "a scalar value has its type preserved"() {
        given:
        def modelJson = '''
        {
            "something": "${abc}"
        }
        '''
        def payloadJson = """
        {
            \"something\": $value
        }
        """
        def model = slurper.parseText(modelJson)
        def payload = slurper.parseText(payloadJson)
        when:
        Map found = instance.process(model, payload).bindings()
        then:
        found["abc"].class == type
        where:
        value     | type
        1         | Integer.class
        1.2       | BigDecimal.class
        "\"a\""   | String.class
        []        | ArrayList.class
    }

    def "a top level array of scalars can have hash members bound"() {
        given:
        String spayload = '''
        [
            "1.2.3.4",
            "5.6.7.8",
            "9.10.11.12"
        ]
        '''
        String smodel = '''
        [
            "${ADD[*]}"
        ]
        '''

        def model = slurper.parseText(smodel)
        def payload = slurper.parseText(spayload)

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found == ["ADD[0]": "1.2.3.4", "ADD[1]": "5.6.7.8", "ADD[2]": "9.10.11.12"]
    }

    def "an empty array should not create fake values (at [0])"() {
        given:
        String jpayload = '[]'
        String jmodel = '[ "${ADD[*]}" ]'

        def model = slurper.parseText(jmodel)
        def payload = slurper.parseText(jpayload)

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found.isEmpty()
    }

    def "a top level array of hashes can have hash members bound"() {
        given:
        String spayload = '''
        [
            { "address": "1.2.3.4" },
            { "address": "5.6.7.8" },
            { "address": "9.10.11.12" }
        ]
        '''
        String smodel = '''
        [
            { "address": "${ADD[*]}" }
        ]
        '''

        def model = slurper.parseText(smodel)
        def payload = slurper.parseText(spayload)

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found == ["ADD[0]": "1.2.3.4", "ADD[1]": "5.6.7.8", "ADD[2]": "9.10.11.12"]
    }

    def "an array of arrays can have all the elements of the inner arrays in a single bound array"() {
        given:
        def payload = slurper.parseText(nestedListsPayload())
        def model = slurper.parseText(nestedListsModel())

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found.size() == 4
        (0..3).each { index ->
            String key = "object-list[${index}]"
            def entry = found[key]
            entry instanceof Map
            entry.key == "complex"
            entry.value == expectedNestedValues[index]
        }
    }

    def "wildcarding for matches is supported"() {
        given:
        String jpayload = '''
        [
            { "address": "1 2" },
            { "address": "3 4" },
            { "address": "5 6" }
        ]
        '''
        String jmodel = '''
        [
            { "address": "|${LEFT[*]} ${RIGHT[*]}|" }
        ]
        '''

        def model = slurper.parseText(jmodel)
        def payload = slurper.parseText(jpayload)

        when:
        def found = instance.process(model, payload).bindings()

        then:
        found == ["LEFT[0]":  "1",
                  "RIGHT[0]": "2",
                  "LEFT[1]":  "3",
                  "RIGHT[1]": "4",
                  "LEFT[2]":  "5",
                  "RIGHT[2]": "6"]
    }
}
