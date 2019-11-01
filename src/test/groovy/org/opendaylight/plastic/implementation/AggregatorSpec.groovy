
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

class AggregatorSpec extends Specification {

    def "simple inline aggregation works"() {
        given:
        Aggregator instance = new Aggregator("PREFIX-", ",", "-SUFFIX") {
            protected List<String> realDeAggregate(String multiPayload) { null }
            String serializeDefaults(Map m) { null }
        }
        when:
        instance.add("a")
        instance.add("b")
        instance.add("c")
        then:
        instance.emit() == "PREFIX-a,b,c-SUFFIX"
    }

    def "simple wrapped aggregation works"() {
        given:
        Aggregator instance = new Aggregator("<ALL>", "<ITEM>", "</ITEM>", "</ALL>") {
            protected List<String> realDeAggregate(String multiPayload) { null }
            String serializeDefaults(Map m) { null }
        }
        when:
        instance.add("a")
        instance.add("b")
        instance.add("c")
        then:
        instance.emit() == "<ALL><ITEM>a</ITEM><ITEM>b</ITEM><ITEM>c</ITEM></ALL>"
    }

    def "aggregation ignores insignificant items"() {
        given:
        Aggregator instance = new Aggregator("PREFIX-", ",", "-SUFFIX") {
            protected List<String> realDeAggregate(String multiPayload) { null }
            String serializeDefaults(Map m) { null }
        }
        when:
        instance.add(item)
        then:
        instance.emit() == "PREFIX--SUFFIX"
        where:
        item | _
        null | _
        ""   | _
    }
}
