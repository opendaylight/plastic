
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


import org.opendaylight.plastic.implementation.author.MoVariables
import spock.lang.Specification


class MorpherSpec extends Specification {

    GroovyClassLoader loader = new GroovyClassLoader()

    VersionedSchema inSchema = new VersionedSchema("in-name", "1.0", "JSON")
    VersionedSchema outSchema = new VersionedSchema("out-name", "1.1", "XML")

    String fileName = "some-file-somewhere-on-disk"

    Object morpherDummerThanARock = new Object()
    Object morpherTweakValues
    Object morpherTweakMoValues
    Object morpherTweakParsed

    def setup() {
        Class clazz

        String tweakValuesCode =
'''
class ValuesTweaker {
    void tweakValues(Map inMap, Map outMap) {
        outMap['abc'] = inMap['abc'].toUpperCase()
    }
}
'''

        clazz = loader.parseClass(tweakValuesCode)
        morpherTweakValues = clazz.newInstance()

        String tweakMoValuesCode =
                '''
import org.opendaylight.plastic.implementation.ExtendedBasicMorpher
import org.opendaylight.plastic.implementation.author.MoVariables

class ValuesTweaker extends ExtendedBasicMorpher {
    void tweakValues(MoVariables inMap, MoVariables outMap) {
        outMap.put('abc', inMap.get('abc').toUpperCase())
    }
}
'''

        clazz = loader.parseClass(tweakMoValuesCode)
        morpherTweakMoValues = clazz.newInstance()

        String tweakParsedCode =
'''
class ParsedTweaker {
    void tweakParsed(intree, outtree) {
         outtree.def[0].value = 456
   }
}
'''
        clazz = loader.parseClass(tweakParsedCode)
        morpherTweakParsed = clazz.newInstance()
    }

    def "a morpher that is missing all methods is rejected"() {
        when:
        new Morpher(inSchema, morpherDummerThanARock, fileName)

        then:
        thrown(Morpher.MalformedMorpher)
    }

    def "a morpher that can only tweak values is acceptable"() {
        when:
        new Morpher(inSchema, morpherTweakValues, fileName)

        then:
        noExceptionThrown()
    }

    def "a morpher that can only tweak mo values is acceptable"() {
        when:
        new Morpher(inSchema, morpherTweakMoValues, fileName)

        then:
        noExceptionThrown()
    }

    def "a morpher can tweak values"() {
        given:
        Morpher instance = new Morpher(inSchema, morpherTweakValues, fileName)

        and:
        Map inMap = ['abc': 'now is the time']
        Map outMap = [:]

        when:
        instance.tweakValues(inMap, outMap)

        then:
        outMap == ['abc': 'NOW IS THE TIME']
    }

    def "a morpher can tweak mo values"() {
        given:
        Morpher instance = new Morpher(inSchema, morpherTweakMoValues, fileName)

        and:
        Map inMap = ['abc': 'now is the time']
        Map outMap = [:]

        when:
        instance.tweakValues(inMap, outMap)

        then:
        outMap == ['abc': 'NOW IS THE TIME']
    }

    def "a morpher that can only tweak parsed is acceptable"() {
        when:
        new Morpher(inSchema, morpherTweakParsed, fileName)

        then:
        noExceptionThrown()
    }

    def "a morpher can tweak parsed"() {
        given:
        Morpher instance = new Morpher(inSchema, morpherTweakParsed, fileName)

        and:
        Node xmlIn = new XmlParser().parseText("<root><abc>123</abc></root>")
        Node xmlOut = new XmlParser().parseText("<root><def>123</def></root>")

        when:
        instance.tweakParsed(xmlIn, xmlOut)

        then:
        xmlOut.text() == "456"
    }

    class TestMorpher extends BasicMorpher
    {
        void tweakInputs(Map inputs, Object payload) {
        }
        void tweakValues(Map inputs, Map outputs) {
        }
        void tweakParsed(Object paylood, Object outgoing) {
        }
    }

    def "the BasicMorpher provides full support as a morpher"() {
        given:
        TestMorpher basic = new TestMorpher()
        Morpher instance = new Morpher(Mock(VersionedSchema), basic, "fake-file-name")
        expect:
        instance.tweakInputsPresent
        instance.tweakValuesPresent

        instance.tweakParsedPresent
        instance.blessDanglingInputsPresent
        instance.preTweakValuesPresent
        instance.blessMissingInputsPresent
        instance.blessDanglingOutputsPresent
    }

    class TestMoMorpher extends ExtendedBasicMorpher
    {
        int tweakValuesCalled = 0

        void tweakInputs(MoVariables inputs, Object payload) {
        }
        void tweakValues(MoVariables inputs, MoVariables outputs) {
            tweakValuesCalled++
        }
        void tweakParsed(Object paylood, Object outgoing) {
        }
    }

    def "the ExtendedBasicMorpher provide full support as a morpher"() {
        given:
        TestMoMorpher basic = new TestMoMorpher()
        Morpher instance = new Morpher(Mock(VersionedSchema), basic, "fake-file-name")
        expect:
        instance.tweakMoInputsPresent
        instance.tweakMoValuesPresent

        instance.tweakParsedPresent
        instance.blessDanglingInputsPresent
        instance.preTweakValuesPresent
        instance.blessMissingInputsPresent
        instance.blessDanglingOutputsPresent
    }

    def "the map versus movariables methods are distinct"() {
        given:
        TestMoMorpher basic = new TestMoMorpher()
        Morpher instance = new Morpher(Mock(VersionedSchema), basic, "fake-file-name")
        when:
        instance.tweakValues([:],[:])
        then:
        basic.tweakValuesCalled == 1
    }
}
