package org.opendaylight.plastic.implementation

import spock.lang.Specification

class MultiModuloCounterSpec extends Specification {

    def "iterator can be incremented"() {
        given:
        MultiModuloCounter instance = new MultiModuloCounter(ranges as long[])
        when:
        instance.increment()
        then:
        instance.value() == expected as long[]
        !instance.isDone()
        where:
        ranges  | expected
        [3]     | [1]
        [3,2,2] | [0, 0, 1]
    }

    def "iterator can be incremented till its done"() {
        given:
        MultiModuloCounter instance = new MultiModuloCounter(ranges as long[])
        when:
        50.times { instance.increment() }
        then:
        instance.value() == expected as long[]
        instance.isDone()
        where:
        ranges    | expected
        [0]       | [0]
        [3]       | [2]
        [3,4]     | [2, 3]
    }

}
