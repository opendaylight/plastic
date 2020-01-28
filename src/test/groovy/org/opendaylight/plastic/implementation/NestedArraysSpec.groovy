/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.slf4j.Logger
import spock.lang.Specification

class NestedArraysSpec extends Specification {

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

    def "non-indexed static contents in arrays work"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        {
            "aaa": [ "${aaa}" ],
        }
        ''')
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        [
            {
                "AAA": [ "${aaa}" ]
            }
        ]
        ''')
        String payload = '''
        {
            "aaa": [ "a" ],
        }
        '''
        String expected = '''
        [
            {
                "AAA": [ "a" ]
            }
        ]
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

    def "mixing single indexed, same sized siblings, top-level output array works"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        {
            "aaa": [ "${aaa[*]}" ],
            "bbb": [ "${bbb[*]}" ]
        }
        ''')
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        [
            {
                "aaa-bbb": [ "${aaa[*]}/${bbb[*]}" ]
            }
        ]
        ''')
        String payload = '''
        {
            "aaa": [ "a", "b", "c" ],
            "bbb": [ 10, 20, 30 ]
        }
        '''
        String expected = '''
        [
            {
                "aaa-bbb": [ "a/10", "b/20", "c/30" ]
            }
        ]
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

    def "sibling arrays of differing sizes mixed into same output result in an error"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        {
            "aaa": [ "${aaa[*]}" ],
            "bbb": [ "${bbb[*]}" ]
        }
        ''')
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        [
            {
                "aaa-bbb": [ "${aaa[*]}/${bbb[*]}" ]
            }
        ]
        ''')
        String payload = '''
        {
            "aaa": [ "a", "b", "c" ],
            "bbb": [ 10, 20, 30, 40, 50 ]
        }
        '''
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        and:
        MapTask.log = Mock(Logger)
        when:
        instance.map(asSchema("foo", "1.0", "json", payload), parsedEmptyDefaults)
        then:
        thrown(MapTask.DanglingOutputsException)
    }

    def "output default values (!) allow sibling arrays of differing sizes mixed into same output to work"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        {
            "aaa": [ "${aaa[*]}" ],
            "bbb": [ "${bbb[*]}" ]
        }
        ''')
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        [
            {
                "aaa-bbb": [ "${aaa[*]=xyz}/${bbb[*]}" ]
            }
        ]
        ''')
        String payload = '''
        {
            "aaa": [ "a", "b", "c" ],
            "bbb": [ 10, 20, 30, 40, 50 ]
        }
        '''
        String expected = '''
        [
            {
                "aaa-bbb": [ "a/10", "b/20", "c/30", "xyz/40", "xyz/50" ]
            }
        ]
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

    def "(simplest case) double indexing, rectangular shape, no flattening works"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        {
            "aaa": [
                {
                    "id": "${id[*]}",
                    "bbb": [ "${bbb[^][*]}" ]
                }
            ]
        }
        ''')
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        {
            "MY-AAA": [
                {
                    "MY-ID": "${id[*]}",
                    "MY-BBB": [ "${bbb[^][*]}" ]
                }
            ]
        }
        ''')
        String payload = '''
        {
            "aaa": [
                {
                    "id": "1",
                    "bbb": [ "a", "b", "c", "cc" ]
                },
                {
                    "id": "2",
                    "bbb": [ "d", "e", "f", "ff" ]
                },
                {
                    "id": "3",
                    "bbb": [ "g", "h", "i", "ii" ]
                }
            ]
        }
        '''
        String expected = '''
        {
            "MY-AAA": [
                {
                    "MY-ID": "1",
                    "MY-BBB": [ "a", "b", "c", "cc" ]
                },
                {
                    "MY-ID": "2",
                    "MY-BBB": [ "d", "e", "f", "ff" ]
                },
                {
                    "MY-ID": "3",
                    "MY-BBB": [ "g", "h", "i", "ii" ]
                }
            ]
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

    def "double indexing, non-rectangular shape, no flattening works"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        {
            "aaa": [
                {
                    "id": "${id[*]}",
                    "bbb": [ "${bbb[^][*]}" ]
                }
            ]
        }
        ''')
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        {
            "MY-AAA": [
                {
                    "MY-ID": "${id[*]}",
                    "MY-BBB": [ "${bbb[^][*]}" ]
                }
            ]
        }
        ''')
        String payload = '''
        {
            "aaa": [
                {
                    "id": "1",
                    "bbb": [ "a", "b", "c" ]
                },
                {
                    "id": "2",
                    "bbb": [ "d", "e" ]
                },
                {
                    "id": "3",
                    "bbb": [ "f" ]
                }
            ]
        }
        '''
        String expected = '''
        {
            "MY-AAA": [
                {
                    "MY-ID": "1",
                    "MY-BBB": [ "a", "b", "c" ]
                },
                {
                    "MY-ID": "2",
                    "MY-BBB": [ "d", "e" ]
                },
                {
                    "MY-ID": "3",
                    "MY-BBB": [ "f" ]
                }
            ]
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

    def "double indexing, non-rectangular shape, but with complete flattening works"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        {
            "aaa": [
                {
                    "id": "${id[*]}",
                    "bbb": [ "${bbb[*][*]}" ]
                }
            ]
        }
        ''')
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        {
            "MY-IDS": [ "${id[*]}" ],
            "MY-BBBS": [ "${bbb[*][*]}" ]
        }
        ''')
        String payload = '''
        {
            "aaa": [
                {
                    "id": "1",
                    "bbb": [ "a", "b", "c" ]
                },
                {
                    "id": "2",
                    "bbb": [ "d", "e" ]
                },
                {
                    "id": "3",
                    "bbb": [ "f" ]
                }
            ]
        }
        '''
        String expected = '''
        {
            "MY-IDS": [ "1", "2", "3" ],
            "MY-BBBS": [ "a", "b", "c", "d", "e", "f" ]
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

    def "flattening of a child (of scalars) can occur within an array that isn't flattened"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        {
            "aaa": [
                {
                    "id": "${id[*]}",
                    "bbb": [ "${bbb[*][*]}" ]
                }
            ]
        }
        ''')
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        [
                {
                    "foo": [
                        "${id[*]}"
                     ],
                    "bar": [
                        "${bbb[^][*]}"
                    ]
                }
        ]
        ''')
        String payload = '''
        {
            "aaa": [
                {
                    "id": 1,
                    "bbb": [ "a", "b", "c" ]
                },
                {
                    "id": 2,
                    "bbb": [ "d", "e" ]
                },
                {
                    "id": 3,
                    "bbb": [ "f" ]
                }
            ]
        }
        '''
        String expected = '''
        [
                {
                    "foo": [ 1, 2, 3 ],
                    "bar": [ "a", "b", "c" ]
                },
                {
                    "foo": [ 1, 2, 3 ],
                    "bar": [ "d", "e" ]
                },
                {
                    "foo": [ 1, 2, 3 ],
                    "bar": [ "f" ]
                }
        ]
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

    def "flattening of a child (of objects) can occur within an array that isn't flattened"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        {
            "aaa": [
                {
                    "id": "${id[*]}",
                    "bbb": [ { "val": "${bbb[*][*]}" } ]
                }
            ]
        }
        ''')
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        [
                {
                    "foo": [
                        "${id[*]}"
                     ],
                    "bar": [
                        { "val": "${bbb[^][*]}" }
                    ]
                }
        ]
        ''')
        String payload = '''
        {
            "aaa": [
                {
                    "id": 1,
                    "bbb": [ { "val": "a" }, { "val": "b" }, { "val": "c" } ]
                },
                {
                    "id": 2,
                    "bbb": [ { "val": "d" }, { "val": "e" } ]
                },
                {
                    "id": 3,
                    "bbb": [ { "val": "f"  } ]
                }
            ]
        }
        '''
        String expected = '''
        [
                {
                    "foo": [ 1, 2, 3 ],
                    "bar": [ { "val": "a" }, { "val": "b" }, { "val": "c" } ]
                },
                {
                    "foo": [ 1, 2, 3 ],
                    "bar": [ { "val": "d" }, { "val": "e" } ]
                },
                {
                    "foo": [ 1, 2, 3 ],
                    "bar": [ { "val": "f" } ]
                }
        ]
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

    def "three deep nested can be replicated"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        [
            {
                "outer-id": "${outer-id[*]}",
                "outer-child": [
                    {
                        "middle-id": "${middle-id[*][*]}",
                        "middle-child": [
                            { 
                                "inner-id": "${inner-id[*][*][*]}"
                            }
                        ]
                    }
                ]
            }
        ]    
        ''')
        String payload = '''
        [
            {
                "outer-id": 1,
                "outer-child": [
                    {
                        "middle-id": 1,
                        "middle-child": [
                            { 
                                "inner-id": 1
                            }
                        ]
                    }
                ]
            },
            {
                "outer-id": 2,
                "outer-child": [
                    {
                        "middle-id": 1,
                        "middle-child": [
                            { 
                                "inner-id": 1
                            }
                        ]
                    },
                    {
                        "middle-id": 2,
                        "middle-child": [
                            { 
                                "inner-id": 1
                            },
                            { 
                                "inner-id": 2
                            }
                        ]
                    }
                ]
            }
        ]    
        '''
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        [
            {
                "MY-OUTER-ID": "${outer-id[*]}",
                "MY-OUTER-CHILD": [
                    {
                        "MY-MIDDLE-ID": "${middle-id[^][*]}",
                        "MY-MIDDLE-CHILD": [
                            { 
                                "MY-INNER-ID": "${inner-id[^][^][*]}"
                            }
                        ]
                    }
                ]
            }
        ]    
        ''')
        String expected = '''
        [
            {
                "MY-OUTER-ID": 1,
                "MY-OUTER-CHILD": [
                    {
                        "MY-MIDDLE-ID": 1,
                        "MY-MIDDLE-CHILD": [
                            { 
                                "MY-INNER-ID": 1
                            }
                        ]
                    }
                ]
            },
            {
                "MY-OUTER-ID": 2,
                "MY-OUTER-CHILD": [
                    {
                        "MY-MIDDLE-ID": 1,
                        "MY-MIDDLE-CHILD": [
                            { 
                                "MY-INNER-ID": 1
                            }
                        ]
                    },
                    {
                        "MY-MIDDLE-ID": 2,
                        "MY-MIDDLE-CHILD": [
                            { 
                                "MY-INNER-ID": 1
                            },
                            { 
                                "MY-INNER-ID": 2
                            }
                        ]
                    }
                ]
            }
        ]    
        '''
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        and:
        MapTask.log = Mock(Logger)
        when:
        Schema result = instance.map(asSchema("foo", "1.0", "json", payload), parsedEmptyDefaults)
        String found = JsonOutput.prettyPrint(result.emit())
        expected = JsonOutput.prettyPrint(expected)
        then:
        asJson(found) == asJson(expected)
    }

    def "four deep nested can be replicated"() {
        given:
        Schema schemaIn = asSchema("foo", "1.0", "json", '''
        [
            {
                "outer-id": "${outer-id[*]}",
                "outer-child": [
                    {
                        "middle-id": "${middle-id[*][*]}",
                        "middle-child": [
                            { 
                                "inner-id": "${inner-id[*][*][*]}",
                                "inner-child": [
                                     "${inner-child[*][*][*][*]}"
                                 ]
                            }
                        ]
                    }
                ]
            }
        ]    
        ''')
        String payload = '''
        [
            {
                "outer-id": 1,
                "outer-child": [
                    {
                        "middle-id": 1,
                        "middle-child": [
                            { 
                                "inner-id": 1,
                                "inner-child": [
                                     "a"
                                 ]
                            }
                        ]
                    }
                ]
            },
            {
                "outer-id": 2,
                "outer-child": [
                    {
                        "middle-id": 1,
                        "middle-child": [
                            { 
                                "inner-id": 1,
                                "inner-child": [
                                     "aa"
                                 ]
                            }
                        ]
                    },
                    {
                        "middle-id": 2,
                        "middle-child": [
                            { 
                                "inner-id": 1,
                                "inner-child": [
                                     "aa", "bb"
                                 ]
                            },
                            { 
                                "inner-id": 2,
                                "inner-child": [
                                     "cc", "dd"
                                 ]
                            }
                        ]
                    }
                ]
            }
        ]    
        '''
        Schema schemaOut = asSchema("bar", "1.0", "json", '''
        [
            {
                "MY-OUTER-ID": "${outer-id[*]}",
                "MY-OUTER-CHILD": [
                    {
                        "MY-MIDDLE-ID": "${middle-id[^][*]}",
                        "MY-MIDDLE-CHILD": [
                            { 
                                "MY-INNER-ID": "${inner-id[^][^][*]}",
                                "MY-INNER-CHILD": [
                                     "${inner-child[^][^][^][*]}"
                                 ]
                            }
                        ]
                    }
                ]
            }
        ]    
        ''')
        String expected = '''
        [
            {
                "MY-OUTER-ID": 1,
                "MY-OUTER-CHILD": [
                    {
                        "MY-MIDDLE-ID": 1,
                        "MY-MIDDLE-CHILD": [
                            { 
                                "MY-INNER-ID": 1,
                                "MY-INNER-CHILD": [
                                     "a"
                                 ]
                            }
                        ]
                    }
                ]
            },
            {
                "MY-OUTER-ID": 2,
                "MY-OUTER-CHILD": [
                    {
                        "MY-MIDDLE-ID": 1,
                        "MY-MIDDLE-CHILD": [
                            { 
                                "MY-INNER-ID": 1,
                                "MY-INNER-CHILD": [
                                     "aa"
                                 ]
                            }
                        ]
                    },
                    {
                        "MY-MIDDLE-ID": 2,
                        "MY-MIDDLE-CHILD": [
                            { 
                                "MY-INNER-ID": 1,
                                "MY-INNER-CHILD": [
                                     "aa", "bb"
                                 ]
                            },
                            { 
                                "MY-INNER-ID": 2,
                                "MY-INNER-CHILD": [
                                     "cc", "dd"
                                 ]
                            }
                        ]
                    }
                ]
            }
        ]    
        '''
        and:
        TranslationPlan plan = new TranslationPlan(schemaIn, schemaOut, [mockMorpher])
        MapTask instance = new MapTask(plan)
        and:
        MapTask.log = Mock(Logger)
        when:
        Schema result = instance.map(asSchema("foo", "1.0", "json", payload), parsedEmptyDefaults)
        String found = JsonOutput.prettyPrint(result.emit())
        then:
        asJson(found) == asJson(expected)
    }
}

