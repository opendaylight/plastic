package org.opendaylight.plastic.implementation

import spock.lang.Specification

class SchemiteratorSpec extends Specification {

    // NOTE: the iterator name is agnostic on using abc versus abc[*]. But all all the client logic
    // is going to use the generically indexed names, so the unit tests below are just showing that.

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
        Schemiterator source = new Schemiterator("abc[^][*]")
        Map bindings = [:]
        source.writeSpec(bindings)
        when:
        Map<String,Schemiterator> reconstituted = Schemiterator.instantiateIterators(bindings)
        then:
        reconstituted.size() == 1
        reconstituted.each { name, instance ->
            instance == source
        }
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

    def "iterator can be incremented till its done"() {
        given:
        Schemiterator instance = new Schemiterator(key,value)
        when:
        50.times { instance.increment() }
        then:
        instance.value() == s
        instance.isDone()
        where:
        key        | value   | s
        "_[abc]"   | "[0]"   | "[0]"
        "_[abc]"   | "[3]"   | "[2]"
        "_[abc]"   | "[3,4]" | "[2][3]"
    }

    def "iterator can initially be in the done state"() {
        expect:
        new Schemiterator("_[abc]","[0]").isDone()
    }

    def "iterator gives all adorned names"() {
        given:
        Schemiterator a = new Schemiterator("_[a[^][*]]", "[3,3]")
        Schemiterator b = new Schemiterator("_[b[*][*]]", "[3,3]")
        when:
        Schemiterator instance = new Schemiterator(a, b)
        instance.increment()
        then:
        instance.replaceables() == ['a[^][*]': 'a[0][1]',
                                    'b[*][*]': 'b[0][1]']
    }

    def "prepended iterators contribute to the value but are not incremented"() {
        given:
        Schemiterator a = new Schemiterator("_[a]","[3]")
        1.times { a.increment() }
        and:
        Schemiterator b = new Schemiterator("_[b]","[3]")
        2.times { b.increment() }
        and:
        Schemiterator instance = new Schemiterator("_[c]","[5]")
        3.times { instance.increment() }
        when:
        b.prepend(a)
        instance.prepend(b)
        then:
        instance.value() == "[1][2][3]"
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

    def asStack(String s) {
        Stack<Long> results = new Stack<>()
        String[] vals = s.split(',')
        vals.each { results.push(Long.parseLong(it)) }
        results
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
}
