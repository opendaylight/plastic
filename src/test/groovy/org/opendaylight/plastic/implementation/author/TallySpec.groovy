
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

class TallySpec extends Specification {

    def "a tally can count completions"() {
        given:
        Tally instance = new Tally("unit-test")
        when:
        5.times { instance.completed() }
        then:
        instance.toString().contains("5")
    }

    def "a tally can count various failures"() {
        given:
        Tally instance = new Tally("unit-test")
        when:
        instance.account("red", "green", "red")
        then:
        instance.asMap() == ['red': 2, 'green': 1, 'completions': 0 ]
    }

    def "a tally reports categories"() {
        given:
        Tally instance = new Tally("unit-test")
        when:
        instance.account("red", "green", "red")
        then:
        ["red", "green"].each { instance.toString().contains(it) }
    }

    def "tallies can be arranged in a heirarcy and summed up"() {
        given:
        Tally red = new Tally("red-child")
        1.times { red.account("red"); red.completed() }
        and:
        Tally green = new Tally("green-child")
        2.times { green.account("green"); green.completed() }
        and:
        Tally blue = new Tally("blue-child")
        3.times { blue.account("blue"); blue.completed() }
        and:
        Tally parent = new Tally("parent")
        parent.adopt(red, green, blue)
        when:
        Tally sum = red.sumUpwards()
        then:
        sum.asMap() == ['red': 1, 'green': 2, 'blue': 3, 'completions': 6]
    }

    def "the top of a heirarchy can be found"() {
        given:
        Tally red = new Tally("red")
        Tally green = new Tally("green")
        Tally blue = new Tally("blue")
        and:
        Tally colors = new Tally("colors")
        colors.adopt(red, green, blue)
        and:
        Tally top = new Tally("top")
        top.adopt(colors)
        expect:
        red.parentmost().name == top.name
    }

    def "tallies can be heirarchically arranged and found by name"() {
        given:
        Tally red = new Tally("red")
        Tally green = new Tally("green")
        Tally blue = new Tally("blue")
        and:
        Tally colors = new Tally("colors")
        colors.adopt(red, green, blue)
        and:
        Tally top = new Tally("top")
        top.adopt(colors)
        expect:
        red.parentmost().findTally("blue").name == blue.name
    }

    def "tallies can be periodically written to a log"() {
        given:
        Tally mockTally = Mock()
        Tally.PeriodicLogger instance = new Tally.PeriodicLogger(mockTally, 50) // 0, 50, 100, 150, ...
        when:
        100.times { i -> instance.completed(i) }
        then:
        2 * mockTally.writeToLog()
    }

    def "tallies can be logarithmicly written to a log"() {
        given:
        Tally mockTally = Mock()
        Tally.LogarithmicLogger instance = new Tally.LogarithmicLogger(mockTally, 70) // 1, 2, 4, 8, 16, 32, 64, 70, 140, 210, ...
        when:
        100.times { i -> instance.completed(i) }
        then:
        8 * mockTally.writeToLog()
    }
}
