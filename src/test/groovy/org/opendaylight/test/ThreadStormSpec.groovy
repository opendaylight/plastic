
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.test

import spock.lang.Specification

class ThreadStormSpec extends Specification {

    def "can do fully parallel executions"() {
        given:
        final NumThreads = 10
        ThreadStorm storm = new ThreadStorm(NumThreads)
        when:
        ThreadStorm.Report report = storm.run { sleep(100) }
        then:
        report.maxInFlight == NumThreads
    }

    def "can do fully serial executions"() {
        given:
        final NumThreads = 10
        ThreadStorm storm = new ThreadStorm(NumThreads).serially()
        when:
        ThreadStorm.Report report = storm.run { sleep(100) }
        then:
        report.maxInFlight == 1
    }

    def "can do partially parallel executions"() {
        given:
        final NumThreads = 10
        final Concurrency = 2
        ThreadStorm storm = new ThreadStorm(NumThreads, Concurrency)
        when:
        ThreadStorm.Report report = storm.run { sleep(100) }
        then:
        report.maxInFlight == Concurrency
    }
}
