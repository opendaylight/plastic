
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

class ParsedJsonSpec extends Specification {

    VersionedSchemaStream asBoundSchema(String strm) {
        VersionedSchema verschema = new VersionedSchema("foo", "1.0", "json")
        new VersionedSchemaStream(verschema, new ByteArrayInputStream(strm.getBytes()))
    }

    ParsedJson asParsed(String strm) {
        new ParsedJson(asBoundSchema(strm))
    }

	def "input is xml"() {
		when:
		new ParsedJson(asBoundSchema('<main><a>something</a></main>'))
		then:
		thrown JsonFormat.JSONParseException
	}

	def "input is not xml or json"() {
		when:
		new ParsedJson(asBoundSchema('test{ "abc": [ { "value": "a" }, { "value": "b" }, { "value": "c" } ] }'))
		then:
		thrown VersionedSchemaParsed.MalformedException
	}

    def "empty defaults are parsed"() {
        given:
        ParsedJson instance = new ParsedJson(asBoundSchema('{}'))
        when:
        Map defaults = instance.asDefaults()
        then:
        defaults == [:]
    }

    def "simplest defaults are parsed"() {
        given:
        ParsedJson instance = new ParsedJson(asBoundSchema('{ "abc": 123 }'))
        when:
        Map defaults = instance.asDefaults()
        then:
        defaults == ['abc': 123]
    }

    def "default values include non-scalars"() {
        given:
        ParsedJson instance = new ParsedJson(asBoundSchema('{ "abc": 123, "nonscalar": { "def": 456 } }'))
        when:
        Map defaults = instance.asDefaults()
        then:
        defaults == ['abc': 123, 'nonscalar': ['def': 456]]
    }

    def "indexed variables can get default values"() {
        given:
        ParsedJson instance = new ParsedJson(asBoundSchema('{ "abc[*]": [ "a", "b", "c" ] }'))
        expect:
        instance.asDefaults() ==  ['abc[0]': "a", 'abc[1]': "b", 'abc[2]': "c", '_[abc[*]]': '[3]' ]
    }

    def "indexed variables can get non-scalar default values"() {
        given:
        ParsedJson instance = new ParsedJson(asBoundSchema('{ "abc[*]": [ { "value": "a" }, { "value": "b" }, { "value": "c" } ] }'))
        expect:
        instance.asDefaults() ==  ['abc[0]': ["value": "a"], 'abc[1]': ["value": "b"], 'abc[2]': ["value": "c"], '_[abc[*]]': '[3]' ]
    }

    def "non-map is flagged as a bad defaults collection"() {
        given:
        ParsedJson instance = new ParsedJson(asBoundSchema('[ 1 ]'))
        when:
        instance.asDefaults()
        then:
        thrown(VersionedSchemaParsed.BadDefaultsException)
    }

    def "an error results from matching an indexed variable to a scalar"() {
        given:
        ParsedJson instance = new ParsedJson(asBoundSchema('{ "abc[*]": "a" }'))
        when:
        instance.asDefaults()
        then:
        thrown(VersionedSchemaParsed.WantedListButFoundScalarException)
    }

    def "an error results from matching an indexed variable to a map"() {
        given:
        ParsedJson instance = new ParsedJson(asBoundSchema('{ "abc[*]": { "a": 123 } }'))
        when:
        instance.asDefaults()
        then:
        thrown(VersionedSchemaParsed.WantedListButFoundMapException)
    }

    def "overall clone works"() {
        given:
        ParsedJson instance = new ParsedJson(asBoundSchema('{ "abc": [ { "value": "a" }, { "value": "b" }, { "value": "c" } ] }'))
        when:
        ParsedJson cloned1 = instance.clone()
        ParsedJson cloned2 = instance.clone()
        and:
        ParsedJson mutated = instance.clone()
        mutated.parsed['abc'][2]['value'] = 'd'
        then:
        cloned1.parsed == cloned2.parsed
        cloned1.parsed != mutated.parsed
    }
}
