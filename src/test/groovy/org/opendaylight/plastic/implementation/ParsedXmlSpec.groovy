
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


class ParsedXmlSpec extends Specification {

    VersionedSchemaStream schemaBoundToStream(String strm) {
        VersionedSchema verschema = new VersionedSchema("foo", "1.0", "XML")
        new VersionedSchemaStream(verschema, new ByteArrayInputStream(strm.getBytes()))
    }

    def "default values are recognized"() {
        given:
        String rawDefaults = """
        <${XmlFormat.MAP}>
           <${XmlFormat.ENTRY}><${XmlFormat.KEY}>abc</${XmlFormat.KEY}><${XmlFormat.VALUE}>123</${XmlFormat.VALUE}></${XmlFormat.ENTRY}>
           <${XmlFormat.ENTRY}><${XmlFormat.KEY}>def</${XmlFormat.KEY}><${XmlFormat.VALUE}>456</${XmlFormat.VALUE}></${XmlFormat.ENTRY}>
        </${XmlFormat.MAP}>
        """
        VersionedSchema schema = new VersionedSchema("foo", "1.0", "XML")
        VersionedSchemaStream bvs = new VersionedSchemaStream(schema, new ByteArrayInputStream(rawDefaults.getBytes()))
        ParsedXml pxml = new ParsedXml(bvs)
        when:
        Map defaults = pxml.asDefaults()
        then:
        defaults == [ 'abc': '123', 'def': '456' ]
    }

    ParsedXml asParsed(String xmlIn) {
        VersionedSchema schema = new VersionedSchema("foo", "1.0", "XML")
        VersionedSchemaStream bvs = new VersionedSchemaStream(schema, new ByteArrayInputStream(xmlIn.getBytes()))
        new ParsedXml(bvs)
    }
}
