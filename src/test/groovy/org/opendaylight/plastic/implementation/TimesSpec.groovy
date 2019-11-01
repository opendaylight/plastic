
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

import java.time.format.DateTimeParseException

class TimesSpec extends Specification {

    def "epochs can convert successfully"() {
        expect:
        Times.fromEpoch("0") == "1970-01-01T00:00:00Z"
        Times.fromEpoch("1516226479") == "2018-01-17T22:01:19Z"
    }

    def "epochs with UTC time zone convert without shifting"() {
        expect:
        Times.fromEpochWithOffset("0", "UTC") == "1970-01-01T00:00:00Z"
        Times.fromEpochWithOffset("1516226479", "UTC") == "2018-01-17T22:01:19Z"
    }

    def "epochs with other time zones are shifted properly"() {
        expect:
        Times.fromEpochWithOffset("0", "-06:00") == "1969-12-31T18:00:00-06:00"
        Times.fromEpochWithOffset("1516226479", "+01:00") == "2018-01-17T23:01:19+01:00"
        Times.fromEpochWithOffset("1513886113", "UTC") == "2017-12-21T19:55:13Z"
    }

    def "millisec resolution epochs convert successfully"() {
        expect:
        Times.fromEpochWithOffset("1513886113000", "UTC") == "2017-12-21T19:55:13Z"
        Times.fromEpochWithOffset("1513886113123", "UTC") == "2017-12-21T19:55:13.123Z"
    }

    def "ambiguous times can be standardized and still be marked as ambiguous"() {
        expect:
        Times.fromAmbiguous(input, Times.Ambiguities.MissingT, Times.Ambiguities.MissingZone) == output
        where:
        input                 | output
        "2017-06-20-19:55:17" | "2017-06-20T19:55:17-00:00"
    }

    def "bad ambiguous time will throw"() {
        when:
        Times.fromAmbiguous("abc")
        then:
        thrown(DateTimeParseException)
    }

    def "short abbreviations for a subset of time zones are acceptable"() {
        expect:
        Times.fromEpochWithOffset("1513886113000", "PST") == "2017-12-21T11:55:13-08:00"
        Times.fromEpochWithOffset("1513886113123", "EST") == "2017-12-21T14:55:13.123-05:00"
    }

    def "current epoch time with millisec precision can be standardized"() {
        expect:
        Times.now() =~ /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{0,3}.*/
    }

    def "specific time can be standardized"() {
        expect:
        Times.fromSpecific(input) == output
        where:
        input                            | output
        "Wed Oct 24 14:47:06 UTC 2018"   | "2018-10-24T14:47:06Z"
        "Wed Oct 24 14:49:09 GMT 2018"   | "2018-10-24T14:49:09Z"
        "Thu Oct 25 14:48:00 EST 2018"   | "2018-10-25T14:48:00-04:00"
    }

    def "bad specific time will throw"() {
        when:
        Times.fromSpecific("abc")
        then:
        thrown(DateTimeParseException)
    }
}
