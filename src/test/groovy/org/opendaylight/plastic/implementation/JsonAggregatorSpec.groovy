
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import spock.lang.Ignore
import spock.lang.Specification

class JsonAggregatorSpec extends Specification {

    JsonAggregator instance = new JsonAggregator()

    def "simplest deaggregation works"() {
        given:
        String item1 = '''{ "abc": 1 }'''
        String item2 = '''{ "def": 2 }'''
        String payload = """[ ${item1}, ${item2} ]"""
        when:
        List<String> found = instance.deAggregate(payload)
        then:
        found == [ item1, item2 ]
    }

    def "quoted characters do not mess up the deaggregation works"() {
        given:
        String item1 = """{ \"abc\": \"${quoteds}\" }"""
        String item2 = '''{ "def": 2 }'''
        String payload = """[ ${item1}, ${item2} ]"""
        when:
        List<String> found = instance.deAggregate(payload)
        then:
        found == [ item1, item2 ]
        where:
        quoteds          | _
        "[[[["           | _
        "]]"             | _
        "{"              | _
        "}}"             | _
        "fo\\\"o\\\"bar" | _
    }

    def "incidental whitespace has no affect on deaggregation"() {
        given:
        String item1 = '''{
            "abc": 1
        }'''
        String item2 = '''{
        "def": 2
        }'''
        String payload = """[
             ${item1},
             ${item2}
        ]"""
        when:
        List<String> found = instance.deAggregate(payload)
        then:
        found == [ item1, item2 ]
    }

    def benchmark = { cls ->
        def start = System.currentTimeMillis()
        cls.call()
        def now = System.currentTimeMillis()
        now - start
    }

    @Ignore // 1,000,000 takes 15.3 sec on a test machine
    def "deaggregation must be performant"() {
        given:
        Map<String,Object> sample = ["abc": 1, "def": "ghi", "jkl": 3, "mno": 4, "pqr": "aaa", "stu": "vvv", "wxy": 5, "zzz": ""]
        String serialized = instance.serializeDefaults(sample)
        and:
        final int N = 1000000
        and:
        N.times { instance.add(serialized) }
        String blob = instance.emit()
        when:
        int elapsed = benchmark {
            List<String> unblobbed = instance.deAggregate(blob)
        }
        then:
        print "Elapsed: ${elapsed} (msec)"
    }

    def "empty defaults can be serialized"() {
        given:
        Map<String,Object> defaults = [:]
        when:
        String serialized = instance.serializeDefaults(defaults)
        then:
        serialized == "{}"
    }

    def "happy path defaults can be serialized"() {
        given:
        Map<String,Object> defaults = ["abc": 1, "def": "ghi"]
        when:
        String serialized = instance.serializeDefaults(defaults)
        then:
        serialized == '''{"abc":1,"def":"ghi"}'''
    }
}
