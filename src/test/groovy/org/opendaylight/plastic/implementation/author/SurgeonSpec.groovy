
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
*/

package org.opendaylight.plastic.implementation.author

import spock.lang.Specification

class SurgeonSpec extends Specification {

    // listify() happy path shallow tests

    def "a path target that is an existing list is left without changes"() {
        given:
        Map root = ['a':['b':['c': [ 1, 2, 3 ]]]]
        Surgeon instance = new Surgeon(root)
        when:
        List found = instance.listify('a', 'b', 'c')
        then:
        found == [ 1, 2, 3 ]
        root == ['a':['b':['c': found]]]
    }

    def "a path target that is a scalar is wrapped in a list"() {
        given:
        Map root = ['a':['b':['c': 1 ]]]
        Surgeon instance = new Surgeon(root)
        when:
        List found = instance.listify('a', 'b', 'c')
        then:
        found == [ 1 ]
        root == ['a':['b':['c': found]]]
    }

    def "a path target that is missing is a created as an empty list"() {
        given:
        Map root = ['a':['b':[:]]]
        Surgeon instance = new Surgeon(root)
        when:
        List found = instance.listify('a', 'b', 'c')
        then:
        found == []
        root == ['a':['b':['c': found]]]
    }

    // listify() happy path deeper tests (behind another parent list, where any surgery is repeated per member)

    def "a deep path target that is an existing list is left without changes"() {
        given:
        Map root = ['a':['b':['c': [ ['d': [1]], ['d': [2]] ]]]]
        Surgeon instance = new Surgeon(root)
        when:
        List found = instance.listify('a', 'b', 'c', 'd')
        then:
        found == [ 2 ]
        root == ['a':['b':['c': [ ['d': [1]], ['d': found] ]]]]
    }

    def "a deep path target that is a scalar is wrapped in a list(s)"() {
        given:
        Map root = ['a':['b':['c': [ ['d': 1], ['d': 2] ]]]]
        Surgeon instance = new Surgeon(root)
        when:
        List lastFound = instance.listify('a', 'b', 'c', 'd')
        then:
        lastFound == [ 2 ]
        root == ['a':['b':['c': [ ['d': [1]], ['d': lastFound] ]]]]
    }

    def "a deep path target that is missing is a created as an empty list"() {
        given:
        Map root = ['a':['b':['c': [ [:], [:] ]]]]
        Surgeon instance = new Surgeon(root)
        when:
        List lastFound = instance.listify('a', 'b', 'c', 'd')
        then:
        lastFound == []
        root == ['a':['b':['c': [ ['d': []], ['d': lastFound] ]]]]
    }

    def "a deep path target that encounters an empty list is a nop"() {
        given:
        Map root = ['a':['b':['c': []]]]
        Surgeon instance = new Surgeon(root)
        when:
        List found = instance.listify('a', 'b', 'c', 'd', 'e')
        then:
        found == []
        root == ['a':['b':['c': []]]]
    }

    // listify() happy path for top-level lists

    def "a path target through a top level list that is an existing list is left without changes"() {
        given:
        List root = [ ['a':['b':[ 1 ]]],  ['a':['b':[ 2 ]]] ]
        Surgeon instance = new Surgeon(root)
        when:
        List lastFound = instance.listify('a', 'b')
        then:
        lastFound == [ 2 ]
        root == [ ['a':['b':[ 1 ]]],  ['a':['b': lastFound]] ]
    }

    def "(alt-syntax) a path target through a top level list that is an existing list is left without changes"() {
        given:
        List root = [ ['a':['b':[ 1 ]]],  ['a':['b':[ 2 ]]] ]
        Surgeon instance = new Surgeon(root)
        when:
        List lastFound = instance.listify('[]', 'a', 'b') // alternative syntax
        then:
        lastFound == [ 2 ]
        root == [ ['a':['b':[ 1 ]]],  ['a':['b': lastFound]] ]
    }

    def "a path target through a top level list that is a scalar is wrapped in a list(s)"() {
        given:
        List root = [ ['a':['b':['c': 1]]], ['a':['b':['c': 2]]] ]
        Surgeon instance = new Surgeon(root)
        when:
        List lastFound = instance.listify('a', 'b', 'c')
        then:
        lastFound == [ 2 ]
        root == [ ['a':['b':['c': [1]]]], ['a':['b':['c': lastFound]]] ]
    }

    def "a path target through a top level list that is missing is a created as an empty list"() {
        given:
        List root = [ ['a':['b':[:]]],  ['a':['b':[:]]] ]
        Surgeon instance = new Surgeon(root)
        when:
        List lastFound = instance.listify('a', 'b', 'c')
        then:
        lastFound == []
        root == [ ['a':['b':['c': []]]], ['a':['b':['c': lastFound]]] ]
    }

    def "a path target through a top level list from a top level list that encounters an empty list is a nop"() {
        given:
        List root = [ ['a':['b':[]]],  ['a':['b':[]]] ]
        Surgeon instance = new Surgeon(root)
        when:
        List lastFound = instance.listify('a', 'b', 'c', 'd', 'e')
        then:
        lastFound == []
        root == [ ['a':['b':[]]],  ['a':['b':lastFound]] ]
    }

    // listify() negative tests

    def "a null root is not allowed"() {
        given:
        Surgeon instance = new Surgeon(null)
        when:
        instance.listify('a', 'b', 'c')
        then:
        thrown(Surgeon.SurgeonException)
    }

    def "a scalar root is not allowed"() {
        given:
        Surgeon instance = new Surgeon(1)
        when:
        instance.listify('a', 'b', 'c')
        then:
        thrown(Surgeon.SurgeonException)
    }

    def "all path components but the last must be a container"() {
        given:
        Map root = ['a':['b': 1]]
        Surgeon instance = new Surgeon(root)
        when:
        instance.listify('a', 'b', 'c')
        then:
        thrown(Surgeon.SurgeonException)
    }

    // mapify() happy path shallow tests

    def "a path target that is an existing map is left without changes"() {
        given:
        Map root = ['a':['b':['c': ['foo':'bar']]]]
        Surgeon instance = new Surgeon(root)
        when:
        Map found = instance.mapify('a', 'b', 'c')
        then:
        found == ['foo':'bar']
        root == ['a':['b':['c': found]]]
    }

    def "a path target that is a scalar is wrapped in a map"() {
        given:
        Map root = ['a':['b':['c': 1 ]]]
        Surgeon instance = new Surgeon(root)
        when:
        Map found = instance.mapify('a', 'b', 'c')
        then:
        found == [ '???': 1 ]
        root == ['a':['b':['c': found]]]
    }

    def "a path target that is missing is a created as an empty map"() {
        given:
        Map root = ['a':['b':[:]]]
        Surgeon instance = new Surgeon(root)
        when:
        Map found = instance.mapify('a', 'b', 'c')
        then:
        found == [:]
        root == ['a':['b':['c': found]]]
    }

    def "placing a value down a non-existing path is an error"() {
        given:
        Map root = ['a':['b': 'foobar']]
        Surgeon instance = new Surgeon(root)
        when:
        instance.placeValue(1, 'a', 'b', 'c')
        then:
        thrown(Surgeon.SurgeonException)
    }

    def "a value can be placed down an existing path"() {
        given:
        Map root = ['a':['b':[:]]]
        Surgeon instance = new Surgeon(root)
        when:
        instance.placeValue(value, 'a', 'b', 'c')
        then:
        root == ['a':['b':['c': value]]]
        where:
        value     | _
        1         | _
        []        | _
        [1,2,3]   | _
        ['a': 1]  | _
    }

    def "a value can be placed down an existing path that includes an array"() {
        given:
        Map root = ['a':['b':[  ['c': 1], ['c': 2], ['c': 3]  ]]]
        Surgeon instance = new Surgeon(root)
        when:
        instance.placeValue([123], 'a', 'b', 'c')
        then:
        root == ['a':['b':[  ['c': [123]], ['c': [123]], ['c': [123]]  ]]]
    }
}
