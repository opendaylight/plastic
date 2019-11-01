
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

class XmlAggregatorSpec extends Specification {

    final String OPEN = XmlAggregator.XMLOPENER
    final String CLOSE = XmlAggregator.XMLCLOSER
    final String IOPEN = XmlAggregator.ITEMOPEN
    final String ICLOSE = XmlAggregator.ITEMCLOSE

    XmlAggregator instance = new XmlAggregator()

    def "simplest deaggregation works"() {
        given:
        String item1 = '''<abc>1</abc>'''
        String item2 = '''<def>2</def>'''
        String payload = """${OPEN}${IOPEN}${item1}${ICLOSE}${IOPEN}${item2}${ICLOSE}${CLOSE}"""
        when:
        List<String> found = instance.deAggregate(payload)
        then:
        found == [ item1, item2 ]
    }

    def "an empty deaggregation results in an empty list"() {
        given:
        String payload = """${OPEN}${CLOSE}"""
        when:
        List<String> found = instance.deAggregate(payload)
        then:
        found.isEmpty()
    }

    def "a missing leader is an error"() {
        given:
        String payload = """<abc>1</abc>"""
        when:
        instance.deAggregate(payload)
        then:
        thrown(Aggregator.MissingLeader)
    }

    def "a missing trailer is an error"() {
        given:
        String payload = """${OPEN}<abc>1</abc>"""
        when:
        instance.deAggregate(payload)
        then:
        thrown(Aggregator.MissingTrailer)
    }

    def "empty defaults can be serialized"() {
        given:
        Map<String,Object> defaults = [:]
        when:
        String serialized = instance.serializeDefaults(defaults)
        then:
        serialized == "<Map></Map>"
    }

    def "happy path defaults can be serialized"() {
        given:
        Map<String,Object> defaults = ["abc": 1, "def": "ghi"]
        when:
        String serialized = instance.serializeDefaults(defaults)
        then:
        serialized == "<Map><Key>abc</Key><Value>1</Value><Key>def</Key><Value>ghi</Value></Map>"
    }
}
