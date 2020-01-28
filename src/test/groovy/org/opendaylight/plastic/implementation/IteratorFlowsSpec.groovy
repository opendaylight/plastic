
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

class IteratorFlowsSpec extends Specification {

    Object fromJson(String raw) {
        new JsonSlurper().parseText(raw)
    }

    def "no widening, one level up works"() {
        given:
        Map inputs = [
                /*
            'id[0]': 1,
            'id[1]': 2,
            'id[2]': 3,
            */

            '_[id[*]]': '[3]',

                /*
            'bbb[0][0]': 'a',
            'bbb[0][1]': 'b',
            'bbb[0][2]': 'c',
            'bbb[1][0]': 'd',
            'bbb[1][1]': 'e',
            'bbb[1][2]': 'f',
            'bbb[2][0]': 'g',
            'bbb[2][1]': 'h',
            'bbb[2][2]': 'i',
            */

            '_[bbb[^][*]]': '[3,3]'
        ]
        and:
        Object schemaOut = fromJson('''
        {
            "MY-AAA": [
                {
                    "MY-ID": "${id[*]}",
                    "MY-BBB": [ "${bbb[^][*]}" ]
                }
            ]
        }
        ''')
        and:
        IteratorFlows instance = new IteratorFlows(inputs)
        when:
        instance.processModel(schemaOut)
        then:
        instance.findings().collectEntries { [ it.key, it.value.asSpec() ] } == [
                0L: "_[id[*]]=[3]",
                1L: "_[bbb[^][*]]=[3,3]"
        ]
    }

    def "widening one level up works"() {
        given:
        Map inputs = [
                /*
            'id[0]': 1,
            */

            '_[id[*]]': '[1]',

                /*
            'bbb[0][0]': 'a',
            'bbb[0][1]': 'b',
            'bbb[0][2]': 'c',
            'bbb[1][0]': 'd',
            'bbb[1][1]': 'e',
            'bbb[1][2]': 'f',
            'bbb[2][0]': 'g',
            'bbb[2][1]': 'h',
            'bbb[2][2]': 'i',
            */

            '_[bbb[^][*]]': '[3,3]'
        ]
        and:
        Object schemaOut = fromJson('''
        {
            "MY-AAA": [
                {
                    "MY-ID": "${id[*]}",
                    "MY-BBB": [ "${bbb[^][*]}" ]
                }
            ]
        }
        ''')
        and:
        IteratorFlows instance = new IteratorFlows(inputs)
        when:
        instance.processModel(schemaOut)
        then:
        instance.findings().collectEntries { [ it.key, it.value.asSpec() ] } == [
                0L: "_[id[*]]=[3]",
                1L: "_[bbb[^][*]]=[3,3]"
        ]
    }

    def "widening one level up that has no explicit iterator works"() {
        given:
        Map inputs = [
                /*
            'bbb[0][0]': 'a',
            'bbb[0][1]': 'b',
            'bbb[0][2]': 'c',
            'bbb[1][0]': 'd',
            'bbb[1][1]': 'e',
            'bbb[1][2]': 'f',
            'bbb[2][0]': 'g',
            'bbb[2][1]': 'h',
            'bbb[2][2]': 'i',
            */

            '_[bbb[^][*]]': '[3,3]'
        ]
        and:
        Object schemaOut = fromJson('''
        {
            "MY-AAA": [
                {
                    "MY-BBB": [ "${bbb[^][*]}" ]
                }
            ]
        }
        ''')
        and:
        IteratorFlows instance = new IteratorFlows(inputs)
        when:
        instance.processModel(schemaOut)
        then:
        instance.findings().collectEntries { [ it.key, it.value.asSpec() ] } == [
                0L: "_[]=[3]",
                1L: "_[bbb[^][*]]=[3,3]"
        ]
    }

    def "widening two levels up, both with no explicit iterator works"() {
        given:
        Map inputs = [
                /*
                'bbb[0][0][0]': 'a',
                'bbb[0][0][1]': 'b',
                'bbb[0][0][2]': 'c',
                'bbb[0][1][0]': 'd',
                'bbb[0][1][1]': 'e',
                'bbb[0][1][2]': 'f',
                'bbb[0][2][0]': 'g',
                'bbb[0][2][1]': 'h',
                'bbb[0][2][2]': 'i',
                'bbb[1][0][0]': 'aa',
                'bbb[1][0][1]': 'bb',
                'bbb[1][0][2]': 'cc',
                'bbb[1][1][0]': 'dd',
                'bbb[1][1][1]': 'ee',
                'bbb[1][1][2]': 'ff',
                'bbb[1][2][0]': 'gg',
                'bbb[1][2][1]': 'hh',
                'bbb[1][2][2]': 'ii',
                */

            '_[bbb[^][^][*]]': '[2,3,3]'
        ]
        and:
        Object schemaOut = fromJson('''
        {
            "MY-AAA": [
                {
                    "MY-BBB": [
                        {
                            "MY-CCC": [ "${bbb[^][^][*]}" ]
                        }
                    ]
                }
            ]
        }
        ''')
        and:
        IteratorFlows instance = new IteratorFlows(inputs)
        when:
        instance.processModel(schemaOut)
        then:
        instance.findings().collectEntries { [ it.key, it.value.asSpec() ] } == [
                0L: "_[]=[2]",
                1L: "_[]=[3]",
                2L: "_[bbb[^][^][*]]=[2,3,3]"
        ]
    }

    def "widening two levels up, but with only one deep throws"() {
        given:
        Map inputs = [
                /*
                'bbb[0][0][0]': 'a',
                'bbb[0][0][1]': 'b',
                'bbb[0][0][2]': 'c',
                'bbb[0][1][0]': 'd',
                'bbb[0][1][1]': 'e',
                'bbb[0][1][2]': 'f',
                'bbb[0][2][0]': 'g',
                'bbb[0][2][1]': 'h',
                'bbb[0][2][2]': 'i',
                'bbb[1][0][0]': 'aa',
                'bbb[1][0][1]': 'bb',
                'bbb[1][0][2]': 'cc',
                'bbb[1][1][0]': 'dd',
                'bbb[1][1][1]': 'ee',
                'bbb[1][1][2]': 'ff',
                'bbb[1][2][0]': 'gg',
                'bbb[1][2][1]': 'hh',
                'bbb[1][2][2]': 'ii',
                */

            '_[bbb[^][^][*]]': '[2,3,3]'
        ]
        and:
        Object schemaOut = fromJson('''
        {
            "MY-AAA": [
                {
                    "MY-BBB": [ "${bbb[^][^][*]}" ]
                }
            ]
        }
        ''')
        and:
        IteratorFlows instance = new IteratorFlows(inputs)
        when:
        instance.processModel(schemaOut)
        then:
        thrown(IteratorFlows.IndexedButNoContainingArrayException)
    }

    def "multi-level parent relationships are established (anonymous parent, named child)"() {
        given:
        Map inputs = [
                '_[bbb[^][^][*]]': '[2,3,3]'
        ]
        and:
        Object schemaOut = fromJson('''
        {
            "MY-AAA": [
                {
                    "MY-BBB": [
                        {
                            "MY-CCC": [ "${bbb[^][^][*]}" ]
                        }
                    ]
                }
            ]
        }
        ''')
        and:
        IteratorFlows instance = new IteratorFlows(inputs)
        when:
        instance.processModel(schemaOut)
        and:
        Map<Long, Schemiterator> iterators = instance.findings()
        then:
        iterators.get(0L).parent == null
        iterators.get(1L).parent == iterators.get(0L)
        iterators.get(2L).parent == iterators.get(1L)
    }

    def "multi-level parent relationships are established (named parent, named child)"() {
        given:
        Map inputs = [
                '_[id[*]]': '[3]',
                '_[bbb[^][*]]': '[3,4]'
        ]
        and:
        Object schemaOut = fromJson('''
        {
            "MY-AAA": [
                {
                    "MY-ID": "${id[*]}",
                    "MY-BBB": [ "${bbb[^][*]}" ]
                }
            ]
        }
        ''')
        and:
        IteratorFlows instance = new IteratorFlows(inputs)
        when:
        instance.processModel(schemaOut)
        and:
        Map<Long, Schemiterator> iterators = instance.findings()
        then:
        iterators.get(0L).parent == null
        iterators.get(1L).parent == iterators.get(0L)
    }
}
