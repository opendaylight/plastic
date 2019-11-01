
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


class ParserFactorySpec extends Specification {

    ParserFactory instance = new ParserFactory()

    VersionedSchema inSchema = new VersionedSchema("foo", "1.0", "JSON")
    VersionedSchema unknownSchema = new VersionedSchema("boo", "1.0", "XYZ")

    String rawJson = "{}"

    VersionedSchemaStream boundIn = new VersionedSchemaStream(inSchema, new ByteArrayInputStream(rawJson.getBytes()))
    VersionedSchemaStream unsupported = new VersionedSchemaStream(unknownSchema, new ByteArrayInputStream("".getBytes()))

    def "factory recognizes the types it supports or not"() {
        expect:
        instance.supports("JsOn")
        instance.supports("xMl")
        !instance.supports("XYZ")
    }

    def "the set of supported types is known"() {
        given:
        def found = instance.allSupportedTypes() as Set
        expect:
        found == ["json", "xml", "cjson"] as Set
    }

    def "a parsed schema for supported input and output can be created"() {
        given:
        VersionedSchemaParsed parsed = instance.createParsed(boundIn)
        when:
        parsed.emit()
        then:
        noExceptionThrown()
    }

    def "an uncooperative task for unsupported output is created"() {
        given:
        VersionedSchemaParsed parsed = instance.createParsed(unsupported)
        when:
        parsed.emit()
        then:
        thrown(IrritableParsed.UnknownParsingFormat)
    }
}
