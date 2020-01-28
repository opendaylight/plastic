package org.opendaylight.plastic.implementation

import spock.lang.Specification

class SchemiteratorSpec extends Specification {

    def "bad format iterator specs are caught"() {
        when:
        new Schemiterator(candidate)
        then:
        thrown(PlasticException)
        where:
        candidate        | _
        "abc"            | _
        "_[abc]"         | _
        "_[abc]="        | _
        "_[abc]=["       | _
        "_[]=[]"         | _
        "_[abc]=[-1]"    | _
    }

    def "iterator specs can be parsed"() {
        when:
        Schemiterator instance = new Schemiterator(candidate)
        then:
        instance.toString().startsWith(s)
        where:
        candidate          | s
        "_[abc]=[]"        | "_[abc]=[]"
        "_[abc]=[0]"       | "_[abc]=[0]"
        "_[abc]=[3,5,123]" | "_[abc]=[3,5,123]"
    }

    def "iterator has an initial value"() {
        when:
        Schemiterator instance = new Schemiterator(candidate)
        then:
        instance.value() == s
        where:
        candidate           | s
        "_[abc]=[]"         | ""
        "_[abc]=[0]"        | "[0]"
        "_[abc]=[1,2,3,4]"  | "[0][0][0][0]"
    }

    def "iterator can be incremented"() {
        given:
        Schemiterator instance = new Schemiterator(candidate)
        when:
        instance.increment()
        then:
        instance.value() == s
        !instance.isDone()
        where:
        candidate           | s
        "_[abc]=[3]"        | "[1]"
        "_[abc]=[3,2,2]"    | "[0][0][1]"
    }

    def "iterator can be incremented till its done"() {
        given:
        Schemiterator instance = new Schemiterator(candidate)
        when:
        50.times { instance.increment() }
        then:
        instance.value() == s
        instance.isDone()
        where:
        candidate           | s
        "_[abc]=[0]"        | "[0]"
        "_[abc]=[3]"        | "[2]"
        "_[abc]=[3,4]"      | "[2][3]"
    }

    def "prepended iterators contribute to the value but are not incremented"() {
        given:
        Schemiterator a = new Schemiterator("_[a]=[3]")
        1.times { a.increment() }
        and:
        Schemiterator b = new Schemiterator("_[b]=[3]")
        2.times { b.increment() }
        and:
        Schemiterator instance = new Schemiterator("_[c]=[5]")
        and:
        instance.prepend(b)
        instance.prepend(a)
        when:
        3.times { instance.increment() }
        then:
        instance.value() == "[1][2][3]"
    }

    def "merging two iterators yields the largest dimension and widest ranges"() {
        given:
        Schemiterator lefty = new Schemiterator(left)
        Schemiterator righty = new Schemiterator(right)
        when:
        Schemiterator instance = new Schemiterator(lefty,righty)
        then:
        instance.toString() == expected
        where:
        left                | right         | expected
        ""                  | "_[r]=[0]"    | "_[r]=[0]"
        "_[l]=[0]"          | "_[r]=[0]"    | "_[r,l]=[0] [0]"
        "_[l]=[7]"          | "_[r]=[8]"    | "_[r,l]=[8] [0]"
        "_[l]=[7,3]"        | "_[r]=[8]"    | "_[r,l]=[8,3] [0][0]"
        "_[l]=[7,3]"        | "_[r]=[1,8]"  | "_[r,l]=[7,8] [0][0]"
    }
}
