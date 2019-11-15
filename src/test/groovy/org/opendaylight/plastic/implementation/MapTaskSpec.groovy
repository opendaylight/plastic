
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Specification

import org.slf4j.Logger
import spock.lang.Unroll;


class MapTaskSpec extends Specification {

    def asStrm(String s) {
        new ByteArrayInputStream(s.getBytes())
    }

    Schema asSchema(String name, String vers, String kind, String content) {
        VersionedSchema schema = new VersionedSchema(name, vers, kind)
        new Schema(schema, content)
    }

    Object asJson(String raw) {
        new JsonSlurper().parseText(raw)
    }

    Schema parsedEmptyDefaults = asSchema("foo", "1.0", "json", "")

    Morpher mockMorpher = Mock()

    // -------------------------------------------------------------------------------
    // Morphers sould be hooked into the conversion pipeline

    def "a map task should give the morpher an early shot at processing input values"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <a>${abc}</a>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "123"
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        when:
        instance.map(payload, parsedEmptyDefaults)
        then:
        1 * mockMorpher.tweakInputs(['abc': '123'], _)
    }

    def "a map task should give the morpher a shot at hash values"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <a>${abc}</a>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "123"
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        when:
        instance.map(payload, parsedEmptyDefaults)
        then:
        1 * mockMorpher.tweakValues(['abc': '123'], ['abc': '123'])
    }

    def "a map task should give the morpher a shot at parsed/structured values"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <a>${abc}</a>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "123"
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        when:
        instance.map(payload, parsedEmptyDefaults)
        then:
        1 * mockMorpher.tweakParsed(['bandwidth': "123"], _)
    }

    // -------------------------------------------------------------------------------
    // Dangling and missing values should result in errors

    def "a missing input variable should result in an error"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
                '''
                {
                    "some-input": "${abc}"
                }
                '''
        )
        Schema schemaOut = asSchema("foo", "1.0", "json",
                '''
                {
                    "some-output": "${abc}"
                }
                '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
                '''
                {
                    "something-else": "some-other-value"
                }
                '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        when:
        instance.map(payload, parsedEmptyDefaults)
        then:
        thrown(MapTask.MissingInputsException)
    }

    def "a dangling input should result in a warning"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <a>these-are-not-the-droids-you-are-looking-for</a>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "123"
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        and:
        MapTask.log = Mock(Logger)
        when:
        instance.map(payload, parsedEmptyDefaults)
        then:
        (1.._) * MapTask.log.warn(_)
    }

    def "a dangling output should result in an error"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <a>${abc}${def}</a>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "123"
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        when:
        instance.map(payload, parsedEmptyDefaults)
        then:
        thrown(MapTask.DanglingOutputVariables)
    }

    def "dots are allowed in attribute names"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
                '''
        {
            "band.width": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "json",
                '''
        {
            "goes.here": "${abc}"
        }
        '''
        )
        String payload = '''
        {
            "band.width": "123"
        }
        '''
        String expected = '''
        {
            "goes.here": "123"
        }
        '''
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        and:
        MapTask.log = Mock(Logger)
        when:
        Schema result = instance.map(asSchema("foo", "1.0", "json", payload), parsedEmptyDefaults)
        String found = result.emit()
        then:
        asJson(found) == asJson(expected)
    }

    @Unroll()
    def "an input payload is allowed to have falsiness values without warning about dangling inputs"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "json",
        '''
        {
            "here": "${abc}"
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        and:
        MapTask.log = Mock(Logger)
        when:
        instance.map(asSchema("foo", "1.0", "json", payload), parsedEmptyDefaults)
        then:
        0 * MapTask.log.warn(_)
        where:
        payload                  | _
        '{ "bandwidth": "123" }' | _
        '{ "bandwidth":     0 }' | _
        '{ "bandwidth":    "" }' | _
        '{ "bandwidth":    [] }' | _
        '{ "bandwidth":    {} }' | _
    }

    // -------------------------------------------------------------------------------
    // Defaults can be bound to inputs and outputs and are a fallback concept

    def "default values should be used when inputs are missing"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <a>${abc}</a>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "def": "456"
        }
        '''
        )
        Schema defaults = asSchema("foo", "1.0", "json",
        '''
        {
            "abc": "123"
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        when:
        instance.map(payload, defaults)
        then:
        1 * mockMorpher.tweakValues(['abc': '123'], ['abc': '123'])
        // 0 * MapTask.log.warn(*_)
    }

    def "depth should not affect default values in the input schema"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "${abc}",
            "jitter": "${def=25}",
            "parent": {
                "lag": "${ghi=100}"
            }
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <ab>
        <a>${abc}</a>
        <b>${def}</b>
        <c>${ghi}</c>
        </ab>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "123"
        }
        '''
        )
        Schema defaults = asSchema("foo", "1.0", "json", "{}")
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        and:
        Map expected = ['abc': '123', 'def': '25', 'ghi': '100']
        when:
        instance.map(payload, defaults)
        then:
        1 * mockMorpher.tweakValues(expected, expected)
    }

    def "call-supplied default values should not override payload-supplied inputs"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <a>${abc}</a>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "bandwidth": "123"
        }
        '''
        )
        Schema defaults = asSchema("foo", "1.0", "json",
        '''
        {
            "abc": "102030"
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        when:
        instance.map(payload, defaults)
        then:
        1 * mockMorpher.tweakValues(['abc': '123'], ['abc': '123'])
    }

    def "call-supplied default values override schema-embeded default values"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
        "bandwidth": "${abc=123}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <a>${abc}</a>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        { "def": "456" }
        '''
        )
        Schema defaults = asSchema("foo", "1.0", "json",
        '''
        { "abc": "789" }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        when:
        Schema result = instance.map(payload, defaults)
        then:
        result.emit().contains("789")
    }

    def "default values are bound to outputs that have no corresponding inputs"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
                '''
        {
            "bandwidth": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <root>
            <a>${abc}</a>
            <b>${def}</b>
        </root>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
                '''
        {
            "bandwidth": "123"
        }
        '''
        )
        Schema defaults = asSchema("foo", "1.0", "json",
        '''
        {
            "def": "456"
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        when:
        instance.map(payload, defaults)
        then:
        1 * mockMorpher.tweakValues(['abc': '123', 'def': '456'], ['abc': '123', 'def': '456'])
    }

    @Ignore // not supported in implementation - just testing
    def "the same variable can appear in multiple places if default values are used"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "value1": "${abc=1}",
            "value2": "${abc=2}",
        }
        '''
        )
        Schema schemaOut = asSchema("bar", "1.0", "xml",
        '''
        <root>
            <a>${abc}</a>
        </root>
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "value1": "123"
        }
        '''
        )
        Schema defaults = asSchema("foo", "1.0", "json", "{}")
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        when:
        instance.map(payload, defaults)
        then:
        1 * mockMorpher.tweakValues(['abc': '123'], ['abc': '123'])
    }

    // -------------------------------------------------------------------------------
    // Morphers can suppress the normal error handling

    class ListlessMorpher extends BasicMorpher
    {
        void tweakInputs(Map inputs, Object payload) {
        }
        void tweakValues(Map inputs, Map outputs) {
        }
        void tweakParsed(Object paylood, Object outgoing) {
        }
    }

    def "morphers can allow a missing input variable"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "some-input": "${abc}",
            "some-input-2": "${def}"
        }
        '''
        )
        Schema schemaOut = asSchema("foo", "1.0", "json",
        '''
        {
            "some-output": "${def}"
        }
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "some-input-2": "DEF"
        }
        '''
        )
        and:
        BasicMorpher basicMorpher = new ListlessMorpher()
        basicMorpher.optionalInputs("abc")
        Morpher morpher = new Morpher(Mock(VersionedSchema), basicMorpher, "fake-file-name")
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [morpher])
        MapTask instance = new MapTask(plan)
        when:
        instance.map(payload, parsedEmptyDefaults)
        then:
        notThrown()
    }

    def "morphers can disable the dangling inputs warning"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "some-input": "${abc}",
            "some-input-2": "${def}"
        }
        '''
        )
        Schema schemaOut = asSchema("foo", "1.0", "json",
        '''
        {
            "some-output": "${def}"
        }
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "some-input": "123",
            "some-input-2": "456"
        }
        '''
        )
        and:
        BasicMorpher inner = new ListlessMorpher()
        inner.ignoreUnusedInputs("abc")
        Morpher morpher = new Morpher(Mock(VersionedSchema), inner, "fake-file-name")
        MapTask.log = Mock(Logger)
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [morpher])
        MapTask instance = new MapTask(plan)
        when:
        instance.map(payload, parsedEmptyDefaults)
        then:
        0 * MapTask.log.warn(_)
    }

    def "morphers can allow a dangling output"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
            "some-input": "${abc}"
        }
        '''
        )
        Schema schemaOut = asSchema("foo", "1.0", "json",
        '''
        {
            "some-output": "${abc}",
            "some-output-2": "${def}"
        }
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "some-input": "ABC"
        }
        '''
        )
        and:
        BasicMorpher inner = new ListlessMorpher()
        inner.ignoreUnusedOutputs("def")
        and:
        Morpher morpher = new Morpher(Mock(VersionedSchema), inner, "fake-file-name")
        MapTask.log = Mock(Logger)
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [morpher])
        MapTask instance = new MapTask(plan)
        when:
        instance.map(payload, parsedEmptyDefaults)
        then:
        notThrown(Exception)
    }

    def "an arrayed payload of complex objects can be bound to an arrayed output"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
          "events": [
            "${events[*]}"
          ]
        }
        '''
        )
        Schema schemaOut = asSchema("foo", "1.0", "json",
        '''
        {
          "output": {
            "code": "0",
            "message": "SUCCESS",
            "alarms": [
              "${events[*]}"
            ]
          }
        }
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "events": [
                {},
                {},
                {}
            ]
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        when:
        Schema results = instance.map(payload, parsedEmptyDefaults)
        then:
        results.parsed == ['output':['alarms':[[:], [:], [:]], 'code': '0', 'message': 'SUCCESS']]
        0 * MapTask.log.warn(_)
    }

    def "an arrayed payload of heterogeneous objects can have default values"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
        '''
        {
          "events": [
            {
               "name": "${name[*]=zzz}"
            }
          ]
        }
        '''
        )
        Schema schemaOut = asSchema("foo", "1.0", "json",
        '''
        {
          "output": {
            "code": "0",
            "message": "SUCCESS",
            "alarms": [
              {
                 "name": "${name[*]}"
              }
            ]
          }
        }
        '''
        )
        Schema payload = asSchema("foo", "1.0", "json",
        '''
        {
            "events": [
                {
                   "name": "abc"
                },
                {
                   "dummy": "000"
                }
            ]
        }
        '''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        when:
        Schema results = instance.map(payload, parsedEmptyDefaults)
        then:
        results.parsed == ['output':['alarms':[['name': "abc"], ['name': 'zzz']], 'code': '0', 'message': 'SUCCESS']]
    }

    def "chunky json can be input and output"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "cjson",
'''-
        {
          "events": [
            "${events[*]}"
          ]
        }
-'''
        )
        Schema schemaOut = asSchema("foo", "1.0", "cjson",
'''-
        {
          "output": {
            "code": "0",
            "message": "SUCCESS",
            "alarms": [
              "${events[*]}"
            ]
          }
        }
-'''
        )
        Schema payload = asSchema("foo", "1.0", "cjson",
'''-
        {
            "events": [
                {},
                {},
                {}
            ]
        }
-'''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        when:
        Schema results = instance.map(payload, parsedEmptyDefaults)
        then:
        results.parsed == [['output':['alarms':[[:], [:], [:]], 'code': '0', 'message': 'SUCCESS']]]
        0 * MapTask.log.warn(_)
    }

    def "arrayed chunky json can be input and output"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "cjson",
'''-
            {
                "name": "${label[*]}"
            }
-'''
        )
        Schema schemaOut = asSchema("foo", "1.0", "cjson",
'''-
        {
          "color": "${label[*]}"
        }
-'''
        )
        Schema payload = asSchema("foo", "1.0", "cjson",
'''-
        {
            "name": "red"
        }
-
        {
            "name": "green"
        }
-
        {
            "name": "blue"
        }
-'''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        when:
        Schema results = instance.map(payload, parsedEmptyDefaults)
        then:
        results.parsed == [['color': 'red'], ['color': 'green'], ['color': 'blue']]
        0 * MapTask.log.warn(_)
    }

    def "bnc-ap child batching test case using chunky json works"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json",
'''[
        "${element[*]}"
]'''
        )
        Schema schemaOut = asSchema("foo", "1.0", "cjson",
'''-
        "${element[*]}"
-'''
        )
        Schema payload = asSchema("foo", "1.0", "json",
'''[
        {
            "name": "red"
        }
,
        {
            "name": "green"
        }
,
        {
            "name": "blue"
        }
]'''
        )
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        MapTask.log = Mock(Logger)
        when:
        Schema results = instance.map(payload, parsedEmptyDefaults)
        then:
        results.parsed == [['name': 'red'], ['name': 'green'], ['name': 'blue']]
        0 * MapTask.log.warn(_)
    }
}
