
/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation

import spock.lang.Specification

class SchemiteratorSpec extends Specification {

    Stack<Long>  asStack(String s) {
        Stack<Long> results = new Stack<>()
        String[] vals = s.split(',')
        vals.each { results.push(Long.parseLong(it)) }
        results
    }

    def "bad format iterator specs are caught"() {
        when:
        new Schemiterator(candidate, "[1,2,3]")
        then:
        thrown(PlasticException)
        where:
        candidate           | _
        "_[abc"             | _
        "_[abc[*]]="        | _
        "_[abc[*]]=["       | _
    }

    def "iterator can be created from iterator spec key/value"() {
        when:
        Schemiterator instance = new Schemiterator(key,value)
        then:
        instance.toString().startsWith(s)
        where:
        key                | value        | s
        "_[abc]"           | "[]"         | "_[abc]=[]"
        "_[abc[*]]"        | "[0]"        | "_[abc[*]]=[0]"
        "_[abc[^][^][*]]"  | "[3,5,123]"  | "_[abc[^][^][*]]=[3,5,123]"
    }

    def "iterator can be created from arrayed variable name"() {
        when:
        Schemiterator instance = new Schemiterator(candidate)
        then:
        instance.value() == expected
        where:
        candidate          | expected
        "abc"              | ""
        "abc[*]"           | "[0]"
        "abc[^]"           | "[0]"
        "abc[^][*]"        | "[0][0]"
    }

    def "iterator can be created from arrayed variable name with ranges"() {
        expect:
        new Schemiterator("abc[*]", 3L).asSpec()        == "_[abc[*]]=[3]"
        new Schemiterator("abc[^][*]", 3L, 4L).asSpec() == "_[abc[^][*]]=[3,4]"
    }

    def "iterators can be created from binding map"() {
        given:
        def map = [ "abc": 0, "_[def]": "[2]", "ghi": [], "_[jkl]": "[3,4,77]" ]
        expect:
        Schemiterator.instantiateIterators(map).size() == 2
    }

    def "no iterators may be found in a binding map"() {
        given:
        def map = [ "abc": "0", "_def": "ddd", "red": 3 ]
        expect:
        Schemiterator.instantiateIterators(map).isEmpty()
    }

    def "iterator can be written and recovered"() {
        given:
        String name = "abc[^][*]"
        Schemiterator source = new Schemiterator(name)
        Map bindings = [:]
        source.writeSpecTo(bindings)
        when:
        Map<String,Schemiterator> reconstituted = Schemiterator.instantiateIterators(bindings)
        then:
        reconstituted[name] == source
    }

    def "iterator can be read"() {
        given:
        Map bindings = [
                '_[test1[*][*][*]]': '[1,2,3]',
                '_[test0[*]]': '[0]'
        ]
        when:
        Schemiterator instance = new Schemiterator(name)
        instance.readSpecFrom(bindings)
        then:
        instance.asSpec() == expected
        where:
        name                | expected
        "test0[*]"          | "_[test0[*]]=[0]"
        "test1[*][*][*]"    | "_[test1[*][*][*]]=[1,2,3]"
    }

    def "iterator can detected"() {
        given:
        Map bindings = [
                '_[test1[*][*][*]]': '[1,2,3]',
                '_[test0[*]]': '[0]'
        ]
        expect:
        Schemiterator.hasSpec(name, bindings) == expected
        where:
        name                | expected
        "test0[*]"          | true
        "test1[*][*][*]"    | true
        "test0"             | false
        "abc"               | false
        ""                  | false
    }

    def "iterator has an initial value"() {
        when:
        Schemiterator instance = new Schemiterator(key,value)
        then:
        instance.value() == s
        where:
        key                    | value       | s
        "_[abc]"               | "[]"        | ""
        "_[abc[*]]"            | "[0]"       | "[0]"
        "_[abc[^][^][^][*]]"   | "[1,2,3,4]" | "[0][0][0][0]"
    }

    def "iterator has effective dimensions which supports flattening of multidimensional iteration"() {
        when:
        Schemiterator instance = new Schemiterator(name)
        then:
        instance.effectiveDimensions() == expected
        where:
        name                   | expected
        "abc"                  | 0
        "abc[*]"               | 1
        "abc[^][^][^][*]"      | 4
        "abc[^][*][*][*]"      | 2
        "abc[*][*][*][*]"      | 1
    }

    def "iterator can be incremented"() {
        given:
        Schemiterator instance = new Schemiterator(key,value)
        when:
        instance.increment()
        then:
        instance.value() == s
        !instance.isDone()
        where:
        key                | value      | s
        "_[abc[*]]"        | "[3]"      | "[1]"
        "_[abc[*][*][*]]"  | "[3,2,2]"  | "[0][0][1]"
    }

    def "an over-incremented iterator caps out and is done"() {
        given:
        Schemiterator instance = new Schemiterator(key,value)
        when:
        50.times { instance.increment() }
        then:
        instance.value() == s
        instance.isDone()
        where:
        key              | value   | s
        "_[abc[*]]"      | "[0]"   | "[0]"
        "_[abc[*]]"      | "[3]"   | "[2]"
        "_[abc[*][*]]"   | "[3,4]" | "[2][3]"
    }

    def "an iterator just reaching max value is not done until another increment happens"() {
        given:
        Schemiterator instance = new Schemiterator("_[a[*]]","[3]")
        and:
        (instance.ranges[0]-1).times { instance.increment() }
        when:
        instance.increment()
        then:
        instance.isDone()
        !old(instance.isDone())
    }

    def "because of borrowing, done for an iterator only applies to its asterisks not its complete range"() {
        given:
        Schemiterator a = new Schemiterator("_[a[*]]","[3]")
        Schemiterator b = new Schemiterator("_[b[^][*]]","[2,3]")
        and:
        b.parent = a
        and:
        (b.ranges[1]-1).times { b.increment() }
        when:
        b.increment()
        then:
        !old(b.isDone())
        b.isDone()
    }

    def "iterator can initially be in the done state"() {
        expect:
        new Schemiterator("_[abc]","[0]").isDone()
        new Schemiterator(0).isDone()
    }

    def "iterator gives all adorned names"() {
        given:
        Schemiterator a = new Schemiterator("_[a[^][*]]", "[3,3]")
        Schemiterator b = new Schemiterator("_[b[*][*]]", "[3,3]")
        when:
        Schemiterator instance = new Schemiterator(a, b)
        instance.increment()
        then:
        instance.replaceables() == [
                'a[*][*]': 'a[0][1]',
                'a[^][*]': 'a[0][1]',
                'b[*][*]': 'b[0][1]',
                'b[^][*]': 'b[0][1]'
        ]
    }

    def "explicitly borrowing iterators contribute to the value but incrementing is for asterisks"() {
        given:
        Schemiterator a = new Schemiterator("_[a[*]]","[3]")
        Schemiterator b = new Schemiterator("_[b[^][*]]","[1,3]")
        Schemiterator instance = new Schemiterator("_[c[^][^][*]]","[1,1,5]")
        and:
        b.parent = a
        instance.parent = b
        and:
        1.times { a.increment() }
        2.times { b.increment() }
        when:
        3.times { instance.increment() }
        then:
        instance.value() == "[1][2][3]"
    }

    def "child iterators of different depth ask for the right depth of value"() {
        given:
        Schemiterator a  = new Schemiterator("_[a[*]]","[3]")
        Schemiterator b  = new Schemiterator("_[b[^][*]]","[1,3]")
        Schemiterator c1 = new Schemiterator("_[c1[^][^][*]]","[1,1,5]")
        Schemiterator c2 = new Schemiterator("_[c2[^][*]]","[1,5]")
        and:
        b.parent = a
        c1.parent = b
        c2.parent = b
        and:
        1.times { a.increment() }
        2.times { b.increment() }
        3.times { c1.increment() }
        4.times { c2.increment() }
        expect:
        c1.value() == "[1][2][3]" // c1 gets value c1-b-a
        c2.value() == "[2][4]"    // c2 gets value c2-b
    }

    def "implicitly borrowing iterators contribute to the value but incrementing is for asterisks"() {
        given:
        Schemiterator a = new Schemiterator("_[a[*]]","[3]")
        Schemiterator b = new Schemiterator("_[b[*]]","[3]")
        Schemiterator c = new Schemiterator("_[c[^][^][*]]","[1,1,5]")
        and:
        b.parent = a
        c.parent = b
        when:
        1.times { a.increment() }
        2.times { b.increment() }
        7.times { c.increment() }
        then:
        c.value() == "[1][2][4]"
    }

    def "merging two iterators yields the largest dimension and widest ranges"() {
        given:
        Schemiterator lefty = new Schemiterator(leftKey,leftValue)
        Schemiterator righty = new Schemiterator(rightKey,rightValue)
        when:
        Schemiterator instance = new Schemiterator(lefty,righty)
        then:
        instance.toString() == expected
        where:
        leftKey      | leftValue    | rightKey    | rightValue      || expected
        "_[l]"       | "[0]"        | "_[r]"      | "[0]"           || "_[r,l]=[0] -> [0]"
        "_[l]"       | "[7]"        | "_[r]"      | "[8]"           || "_[r,l]=[8] -> [0]"
        "_[l]"       | "[7,3]"      | "_[r]"      | "[8]"           || "_[r,l]=[8,3] -> [0][0]"
        "_[l]"       | "[7,3]"      | "_[r]"      | "[1,8]"         || "_[r,l]=[7,8] -> [0][0]"
    }

    def "merging of an indefinite ranged iterator always gives way to the other one"() {
        given:
        Schemiterator lefty = new Schemiterator("_[lefty]","[7,3,1]")
        Schemiterator righty = new Schemiterator(3)
        when:
        Schemiterator instance = new Schemiterator(lefty,righty)
        then:
        instance.toString() == "_[lefty]=[7,3,1] -> [0][0][0]"
    }

    def "merging of an iterator always recalculates virtual dimensions"() {
        given:
        String[] lparts = left.split("=", -2)
        Schemiterator lefty = new Schemiterator(lparts[0], lparts[1])
        and:
        String[] rparts = right.split("=", -2)
        Schemiterator righty = new Schemiterator(rparts[0],rparts[1])
        when:
        Schemiterator instance = new Schemiterator(lefty,righty)
        then:
        instance.effectiveDimensions() == depth
        where:
        left                       | right                      | depth
        "_[abc[*][*]]=[2,2]"       | "_[abc[*][*]]=[2,2]"       | 1
        "_[abc[*][*]]=[2,2]"       | "_[abc[^][*]]=[2,2]"       | 2
        "_[abc[*][*][*]]=[2,2,3]"  | "_[abc[^][^][*]]=[2,2,2]"  | 3
    }

    def "iterator can have the current values set"() {
        when:
        Schemiterator instance = new Schemiterator(candidate)
        instance.setCurrentFromIndices(asStack(indices))
        then:
        instance.value() == expected
        where:
        candidate          | indices  | expected
        "abc[*]"           | "1"      | "[1]"
        "abc[^]"           | "2,1"    | "[1]"
        "abc[^][*]"        | "4,3"    | "[4][3]"
        "abc[^][*]"        | "3,2,1"  | "[2][1]"
    }

    def "ranges can flow up to parents and widen them"() {
        given:
        Schemiterator empty = new Schemiterator(0)
        Schemiterator a = new Schemiterator("a[*]", 3)
        Schemiterator b = new Schemiterator("b[^][*]", 4, 2)
        Schemiterator c = new Schemiterator("c[^][*]", 1, 2)
        Schemiterator d = new Schemiterator("d[*]", 1)
        Schemiterator e = new Schemiterator("e[*]", 2)
        Schemiterator f = new Schemiterator("f[^][^][*]", 6,7,8)
        and:
        Map<String,Schemiterator> working = [
                'empty': empty,
                'a': a,
                'b': b,
                'c': c,
                'd': d,
                'e': e,
                'f': f
        ]
        and:
        Stack<Schemiterator> stack = []
        contents.split(",").each { stack.push(working[(it)]) }
        when:
        Schemiterator tos = stack.peek()
        tos.flowOut(stack)
        then:
        stack.get(0).asSpec() == expectedBos
        where:
        contents    |  expectedBos
        "a,b"       |  "_[a[*]]=[4]"
        "a,c"       |  "_[a[*]]=[3]"
        "empty,b"   |  "_[]=[4]"
        "d,e,f"     |  "_[d[*]]=[6]"
    }
}
