
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


import org.opendaylight.plastic.implementation.PlasticException
import spock.lang.Specification

class MoVariablesSpec extends Specification {

    def removeIterators(Map bindings) {
        bindings.findAll { k,v ->
            !k.startsWith('_')
        }
    }

    def "morpher variables can be empty"() {
        given:
        MoVariables instance = new MoVariables([:])
        expect:
        instance.isEmpty()
        instance.size() == 0
    }

    def "morpher variables can have a non-zero size"() {
        given:
        MoVariables instance = new MoVariables(['a': 11, 'b': 33])
        expect:
        !instance.isEmpty()
        instance.size() == 2
    }

    def "morpher variables can detect bound variables"() {
        given:
        MoVariables instance = new MoVariables(['a': 11, 'b': 33])
        expect:
        instance.isBound('a')
        !instance.isBound('c')
    }

    def "morpher array can be empty"() {
        given:
        MoVariables instance = new MoVariables(['a': 11, 'b': 33])
        when:
        MoArray slice = instance.asArray("xyzzy[*]")
        then:
        slice.isEmpty()
    }

    def "morpher array can be non-empty"() {
        given:
        MoVariables instance = new MoVariables(['a[0]': 11, 'a[3]': 33, 'c': 'abc'])
        when:
        MoArray slice = instance.asArray("a[*]")
        then:
        slice.size() == 4
    }

    def "morpher array size must count holes too"() {
        given:
        int size = 6
        MoVariables instance = new MoVariables(['a[0]': 11, 'a[1]': 33, 'a[5]': 'abc', '_[a[*]]': "[$size]"])
        when:
        MoArray slice = instance.asArray("a[*]")
        then:
        slice.size() == size
    }

    def "morpher array must include holes in iterating"() {
        given:
        List expected = [ null, 11, 33, null, null, 'abc' ]
        MoVariables instance = new MoVariables(['a[1]': expected[1],
                                                'a[2]': expected[2],
                                                'a[5]': expected[5],
                                                '_[a[*]]': "[${expected.size()}]"])
        when:
        MoArray slice = instance.asArray("a[*]")
        then:
        for (int i = 0; i< slice.size(); i++) {
            slice[i] == expected[i]
        }
    }

    def "morpher array doublet test case of [ hole, non-hole ]"() {
        given:
        MoVariables instance = new MoVariables(['a[1]': 'abc', '_[a[*]]': "[2]"])
        when:
        MoArray slice = instance.asArray("a[*]")
        then:
        slice[0] == null
        slice[1] == 'abc'
    }

    def "missing iterator is added"() {
        given:
        Map bindings = ['a[0]': 11, 'a[1]': 33, 'a[5]': 'abc']
        MoVariables instance = new MoVariables(bindings)
        when:
        instance.asArray("a[*]")
        then:
        bindings.size() == old(bindings.size())+1
    }

    def "morpher array must use a generic indexed variable or an error results"() {
        given:
        MoVariables instance = new MoVariables(['a': 11])
        when:
        instance.asArray("xyzzy")
        then:
        thrown(PlasticException)
    }

    def "morpher variables can be multiply sliced"() {
        given:
        MoVariables instance = new MoVariables(['a[0]': 11, 'a[1]': 33, 'b[5]': 'abc', 'c[45]': 66])
        when:
        List slices = instance.asArrays("a[*]","b[*]","c[*]")
        then:
        slices.collect { MoArray mv -> mv.size() } == [2, 6, 46]
    }

    def "morpher array can access values in order even with holes"() {
        given:
        MoArray instance = new MoArray("a[*]", ['a[7]': 'a7', 'a[0]': 'a0', 'a[1]': 'a1', 'b[5]': 'b5', 'a[2]': 'a2', 'a[9]': 'a9'])
        List found = []
        when:
        (0..9).each { i ->
            found << instance[i]
        }
        then:
        found == ['a0', 'a1', 'a2', null, null, null, null, 'a7', null, 'a9']
    }

    def "morpher array can be set"() {
        given:
        MoArray instance = new MoArray("a[*]", [:])
        instance.set(['0', '1', '2', '3', '4'])
        when:
        List values = instance.orderedValues()
        then:
        values == ['0', '1', '2', '3', '4']
    }

    def "morpher array can be have individual members set"() {
        given:
        MoArray instance = new MoArray("a[*]", ['a[0]': ''])
        when:
        instance[0] = value
        then:
        value == instance[0]
        where:
        value      || _
        null       || _
        [ 1 ]      || _
        [ 'a': 1 ] || _
        "0"        || _
        "abc"      || _
        0          || _
    }

    def "setting a morpher array affects the original map"() {
        given:
        Map original = ['a[30]': '30', 'a[31]': '31']
        MoArray instance = new MoArray("a[*]", original)
        when:
        instance.set(['0', '1', '2', '3', '4'])
        then:
        removeIterators(original) == ['a[0]': '0', 'a[1]': '1', 'a[2]': '2', 'a[3]': '3', 'a[4]': '4']
    }

    def "morpher array can be set to empty"() {
        given:
        MoArray instance = new MoArray("a[*]", [:])
        instance.set([])
        when:
        List values = instance.orderedValues()
        then:
        values == []
    }

    def "clearing a morpher array affects the original empty"() {
        given:
        Map original = ['a[0]': '0']
        MoArray instance = new MoArray("a[*]", original)
        when:
        instance.set([])
        then:
        removeIterators(original) == [:]
    }

    def "setting a morpher array item can be set (and affects original)"() {
        given:
        Map original = ['a[0]': '0', 'a[1]': '1']
        MoArray instance = new MoArray("a[*]", original)
        when:
        instance[0] = "000"
        then:
        original['a[0]'] == '000'
    }

    def "setting a morpher array item with out-of-bounds index is bad"() {
        given:
        Map original = ['a[0]': '0', 'a[1]': '1']
        MoArray instance = new MoArray("a[*]", original)
        when:
        instance[2] = "2"
        then:
        thrown(PlasticException)
    }

    def "a morpher array item can be added"() {
        given:
        Map original = ['a[0]': '0', 'a[1]': '1']
        MoArray instance = new MoArray("a[*]", original)
        when:
        instance.add("2")
        then:
        original['a[2]'] == "2"
    }

    def "morpher array item can be gotten"() {
        given:
        Map original = ['a[0]': '0', 'a[1]': '1', 'a[2]': '2']
        MoArray instance = new MoArray("a[*]", original)
        expect:
        instance[0] == '0'
        instance[2] == '2'
    }

    def "variables can be merged"() {
        given:
        Map map1 = ['a[0]':  '0', 'a[1]':  '1', 'a[2]':  '2']
        Map map2 = ['b[0]': '00', 'b[1]': '01', 'b[2]': '02']
        MoVariables from = new MoVariables(map1)
        MoVariables to = new MoVariables (map2)
        when:
        to.mergeFrom(from)
        then:
        map2.size() == old(map1.size()) + old(map2.size())
    }

    def "variables can act like a readable built-in map"() {
        given:
        MoVariables instance = new MoVariables(['a[0]':  '0', 'a[1]':  '1', 'a[2]':  '2'])
        expect:
        !instance.isEmpty()
        instance.size() == 3
        instance['a[2]'] == '2'
    }

    def "variables can be iterated over like a built-in map"() {
        given:
        Map original = ['a[0]':  '0', 'a[1]':  '1', 'a[2]':  '2']
        MoVariables instance = new MoVariables(original)
        when:
        Map found = [:]
        instance.each { k,v -> found[k] = v }
        then:
        found == original
    }

    def "variables can act like a writable built-in map"() {
        given:
        MoVariables instance = new MoVariables(['a[0]':  '0', 'a[1]':  '1', 'a[2]':  '2'])
        when:
        instance['xyz'] = '123'
        then:
        instance['xyz'] == '123'
        when:
        instance.remove('xyz')
        then:
        instance['xyz'] == null
    }

    def "array can act like a readable built-in list"() {
        given:
        MoVariables vars = new MoVariables(['a[0]':  '0', 'a[1]':  '1', 'a[2]':  '2'])
        MoArray instance = vars.asArray("a[*]")
        expect:
        (0..2).each { i -> instance[i] == "${i}" }
    }

    def "array be created like another array"() {
        given:
        MoVariables vars = new MoVariables(['a[0]':  '0', 'a[1]':  '1', 'a[2]':  '2', 'b': '3', 'c': '4'])
        MoArray src = vars.asArray("a[*]")
        when:
        MoArray array = vars.newArray("aa[*]", src.size())
        then:
        array.size() == 3
    }

    def "array can be created from an existing list"() {
        given:
        MoVariables vars = new MoVariables(['a[0]':  '0', 'a[1]':  '1', 'a[2]':  '2', 'b': '3', 'c': '4'])
        List values = [ 10, 20, 30, 40, 50 ]
        when:
        MoArray array = vars.newArray("d[*]", values)
        then:
        array.size() == values.size()
    }

    def "array can act like a writable built-in list"() {
        given:
        Map underlying = ['a[0]':  '0', 'a[1]':  '1', 'a[2]':  '2']
        MoVariables vars = new MoVariables(underlying)
        and:
        MoArray instance = vars.asArray("a[*]")
        expect:
        (0..2).each { i -> instance[i] = "${i+10}" }
        and:
        removeIterators(underlying) == ['a[0]':  '10', 'a[1]':  '11', 'a[2]':  '12']
    }
}
