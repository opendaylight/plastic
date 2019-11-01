
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

class ParsedChunkyJsonSpec extends Specification {

    VersionedSchemaStream asBoundSchema(String strm) {
        VersionedSchema verschema = new VersionedSchema("foo", "1.0", "cjson")
        new VersionedSchemaStream(verschema, new ByteArrayInputStream(strm.getBytes()))
    }

    ParsedJson asParsed(String strm) {
        new ParsedJson(asBoundSchema(strm))
    }

    def "empty defaults are parsed"() {
        given:
        ParsedChunkyJson instance = new ParsedChunkyJson(asBoundSchema('-\n-'))
        when:
        Map defaults = instance.asDefaults()
        then:
        defaults == [:]
    }

    def "simplest defaults are parsed"() {
        given:
        ParsedChunkyJson instance = new ParsedChunkyJson(asBoundSchema('-\n{ "abc": 123 }\n-'))
        when:
        Map defaults = instance.asDefaults()
        then:
        defaults == ['abc': 123]
    }

    def "default values include non-scalars"() {
        given:
        ParsedChunkyJson instance = new ParsedChunkyJson(asBoundSchema('-\n{ "abc": 123, "nonscalar": { "def": 456 } }\n-'))
        when:
        Map defaults = instance.asDefaults()
        then:
        defaults == ['abc': 123, 'nonscalar': ['def': 456]]
    }

    def "unexpected format is flagged as a bad defaults collection"() {
        when:
        ParsedChunkyJson instance = new ParsedChunkyJson(asBoundSchema('[ 1 ]'))
        instance.asDefaults()
        then:
        thrown(Aggregator.MissingLeader)
    }
}
