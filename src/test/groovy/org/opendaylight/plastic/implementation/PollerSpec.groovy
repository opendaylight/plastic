
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import spock.lang.Specification

import java.util.concurrent.Semaphore

class PollerSpec extends Specification {

    class Sleeper {
        Semaphore blocker = new Semaphore(0)

        def tick() {
            blocker.release(1)
        }
        def snooze(long msec) {
            blocker.acquire()
        }
    }

    Pollee pollee1 = Mock(Pollee)
    Pollee pollee2 = Mock()
    Pollee pollee3 = Mock()

    def pollees = [ pollee1, pollee2, pollee3 ]

    def "a poller should initially call each pollee upon startup"() {
        given:
        Poller instance = new Poller()
        pollees.each { instance.register(it) }
        when:
        instance.start()
        instance.waitTillInitialized()
        then:
        pollees.each { Pollee p ->
            1 * p.phase(0)
        }
    }

    def "a poller should initially call each pollee once per phase aka sleep interval"() {
        given:
        Sleeper sleeper = new Sleeper()
        Poller instance = new Poller()
        instance.sleeper = { long t -> sleeper.snooze(t) }
        and:
        pollees.each { Pollee p -> instance.register(p) }
        when:
        instance.start()
        sleeper.tick()
        sleeper.tick()
        sleep(100)
        then:
        pollees.each { Pollee p ->
            3 * p.phase(_)
        }
    }

    def "a poller should execute one phase if the polling interval is zero"() {
        given:
        Sleeper sleeper = new Sleeper()
        Poller instance = new Poller(0)
        and:
        pollees.each { Pollee p -> instance.register(p) }
        when:
        instance.start()
        sleeper.tick()
        sleeper.tick()
        sleep(100)
        then:
        pollees.each { Pollee p ->
            1 * p.phase(0)
        }
    }
}
