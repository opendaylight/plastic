/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

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
import org.opendaylight.plastic.implementation.author.Plans
import spock.lang.Specification

class PlansSpec extends Specification {

    def asJson(String s) {
        JsonSlurper slurper = new JsonSlurper()
        slurper.parse(s.getBytes())
    }

    def "can iterate using a direct path from a top-level scalar list"() {
        given:
        String path = "[*]"
        String data = "[ 1, 2, 3 ]"
        and:
        List root = asJson(data)
        Plans.WalkToList instance = new Plans.WalkToList(path)
        List found = []
        when:
        instance.walk(root) { l, i ->
            found[i] = l[i]
        }
        then:
        found == [ 1, 2, 3 ]
    }

    def "can iterate using a direct path from a top-level list"() {
        given:
        String path = "[*].a.b.c"
        String data = '''
        [
            {
                "a": {
                    "b": {
                        "c": [1, 2, 3]
                    }
                }
            }
        ]
        '''
        and:
        List root = asJson(data)
        Plans.WalkToList instance = new Plans.WalkToList(path)
        List found = []
        when:
        instance.walk(root) { l, i ->
            found[i] = l[i]
        }
        then:
        found == [ 1, 2, 3 ]
    }

    def "can iterate using a direct path to a list"() {
        given:
        String path = "a.b.c"
        String data = '''
        {
            "a": {
                "b": {
                    "c": [1, 2, 3]
                }
            }
        }
        '''
        and:
        Map root = asJson(data)
        Plans.WalkToList instance = new Plans.WalkToList(path)
        List found = []
        when:
        instance.walk(root) { l, i ->
            found[i] = l[i]
        }
        then:
        found == [ 1, 2, 3 ]
    }

    def "can iterate using an indirect path to a nested list with a single item"() {
        given:
        String path = "a.b.c[*].d"
        String data = '''
        {
            "a": {
                "b": {
                    "c": [
                        {
                            "d": [ 4, 5, 6 ]
                        }
                    ]
                }
            }
        }
        '''
        and:
        Map root = asJson(data)
        Plans.WalkToList instance = new Plans.WalkToList(path)
        and:
        List found = []
        when:
        instance.walk(root) { l, i ->
            found[i] = l[i]
        }
        then:
        found == [ 4, 5, 6 ]
    }

    def "can iterate using an indirect path to a nested list"() {
        given:
        String path = "a.b.c[*].d"
        String data = '''
        {
            "a": {
                "b": {
                    "c": [
                        {
                            "d": [ 4, 5, 6 ]
                        },
                        {
                            "d": [ 7, 8, 9 ]
                        },
                        {
                            "d": [ 10, 11, 12 ]
                        }
                    ]
                }
            }
        }
        '''
        and:
        Map root = asJson(data)
        Plans.WalkToList instance = new Plans.WalkToList(path)
        and:
        List found = []
        when:
        instance.walk(root) { l, i ->
            found.add(l[i])
        }
        then:
        found == [ 4, 5, 6, 7, 8, 9, 10, 11, 12 ]
    }

    def "can iterate using an indirect path to a doubly nested list"() {
        given:
        String path = "a.b.c[*].d[*].e"
        String data = '''
        {
            "a": {
                "b": {
                    "c": [
                        {
                            "d": [
                                {
                                    "e": [ 1, 2 ]
                                },
                                {
                                    "e": [ 3, 4 ]
                                }
                            ]
                        },
                        {
                            "d": [
                                {
                                    "e": [ 5, 6, 7 ]
                                }
                            ]
                        },
                        {
                            "d": [
                                {
                                    "e": [ ]
                                },
                                {
                                    "e": [ 8, 9, 10 ]
                                }
                            ]
                        }
                    ]
                }
            }
        }
        '''
        and:
        Map root = asJson(data)
        Plans.WalkToList instance = new Plans.WalkToList(path)
        and:
        List found = []
        when:
        instance.walk(root) { l, i ->
            found.add(l[i])
        }
        then:
        found == [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 ]
    }
}
