/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation.author

import spock.lang.Specification

class BetterJsonSpec extends Specification {
    def "a null root is caught as an error"() {
        given:
        BetterJson instance = new BetterJson(null)
        when:
        instance.asList()
        then:
        thrown(BetterJson.BetterJsonException)
    }

    def "a map as root cannot be converted to a list"() {
        given:
        BetterJson instance = new BetterJson([:])
        when:
        instance.asList()
        then:
        thrown(BetterJson.BetterJsonException)
    }

    def "a list as root can be retrieved without a path"() {
        given:
        List l = []
        BetterJson instance = new BetterJson(l)
        when:
        List found = instance.asList()
        then:
        found == l
    }

    def "a list as root cannot be drilled into with a path"() {
        given:
        List l = []
        BetterJson instance = new BetterJson(l)
        when:
        instance.asList('abc')
        then:
        thrown(BetterJson.BetterJsonException)
    }

    def "a non-existing path component is caught"() {
        given:
        BetterJson instance = new BetterJson([:])
        when:
        instance.asList('a')
        then:
        thrown(BetterJson.BetterJsonException)
    }

    def "a deeper non-existing path component is caught"() {
        given:
        BetterJson instance = new BetterJson(['a': ['c': 1]])
        when:
        instance.asList('a', 'b')
        then:
        thrown(BetterJson.BetterJsonException)
    }

    def "a path to a non-list is caught"() {
        given:
        BetterJson instance = new BetterJson(['a': ['b': 1]])
        when:
        instance.asList('a', 'b')
        then:
        thrown(BetterJson.BetterJsonException)
    }

    def "a path to a list is successful"() {
        given:
        List expected = [1, 2, 3]
        BetterJson instance = new BetterJson(['a': ['b': expected]])
        when:
        List found = instance.asList('a', 'b')
        then:
        found == expected
    }

    def "a path to a map is successful"() {
        given:
        Map expected = ['red': 1, 'blue': 2, 'green': 3]
        BetterJson instance = new BetterJson(['a': ['b': expected]])
        when:
        Map found = instance.asMap('a', 'b')
        then:
        found == expected
    }

    def "a path to a map across a list is successful"() {
        given:
        Map expected = ['red': 1, 'blue': 2, 'green': 3]
        BetterJson instance = new BetterJson(['a': [[:], expected]])
        when:
        Map found = instance.asMap('a', '1')
        then:
        found == expected
    }

    def "a path to a map across a list is can fail with out-of-bounds"() {
        given:
        Map expected = ['red': 1, 'blue': 2, 'green': 3]
        BetterJson instance = new BetterJson(['a': [[:], expected]])
        when:
        Map found = instance.asMap('a', '2')
        then:
        thrown(BetterJson.BetterJsonException)
    }

    def "a path to a list can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [ 1 ]]])
        expect:
        instance.isList('a', 'b')
    }

    def "a path to a non-list can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': 1]])
        expect:
        !instance.isList('a', 'b')
    }

    def "a path to a non-empty list can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [ 1 ]]])
        expect:
        instance.isNonEmptyList('a', 'b')
    }

    def "a path to an empty list can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': []]])
        expect:
        !instance.isNonEmptyList('a', 'b')
    }

    def "a path to a map can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [ 'c':  1 ]]])
        expect:
        instance.isMap('a', 'b')
    }

    def "a path to a non-map can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': 1]])
        expect:
        !instance.isMap('a', 'b')
    }

    def "a path to a non-empty map can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [ 'c': 1 ]]])
        expect:
        instance.isNonEmptyMap('a', 'b')
    }

    def "a path to an empty map can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [:]]])
        expect:
        !instance.isNonEmptyMap('a', 'b')
    }

    def "a path to an existing scalar can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [ 'c': 1 ]]])
        expect:
        instance.isScalar('a', 'b', 'c')
    }

    def "a path to a non-scalar is not mistaken for a scalar"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [ 'c': 1 ]]])
        expect:
        !instance.isScalar('a', 'b')
    }

    def "a path to a non-existing element is not mistaken for a scalar"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [ 'c': 1 ]]])
        expect:
        !instance.isScalar('a', 'b', 'd')
    }

    def "a path to an existing object can be detected"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [ 'c': 1 ]]])
        expect:
        instance.isObject('a', 'b')
    }

    def "a path to a non-existing element is not mistaken for an object"() {
        given:
        BetterJson instance = new BetterJson(['a': [ 'b': [ 'c': 1 ]]])
        expect:
        !instance.isObject('a', 'b', 'd')
    }
}
