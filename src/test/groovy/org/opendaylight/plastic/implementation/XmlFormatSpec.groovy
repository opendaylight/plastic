
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import org.xml.sax.SAXParseException
import spock.lang.Specification

class XmlFormatSpec extends Specification {

    XmlFormat instance = new XmlFormat()

    String cleaned(String xml) {
        xml.replaceAll("(\\n|\\s+)", "")
    }

    def "cannot create parsed xml if the input xml is mal-formed"() {
        when:
        instance.parse(input)
        then:
        thrown(SAXParseException)
        where:
        input         | _
        "<abc></def>" | _
    }

    def "always create parsed xml if the input xml is well-formed"() {
        when:
        instance.parse(input)
        then:
        noExceptionThrown()
        where:
        input                     | _
        "<abc>123</abc>"          | _
        "<abc foo=\"bar\"></abc>" | _
    }

    def "valid xml objects can be serialized to xml"() {
        given:
        String xmlIn = '<?xml version=\"1.0\" encoding=\"UTF-8\"?><abc><def>1</def></abc>'
        Object pxml = instance.parse(xmlIn)
        when:
        String xmlOut = instance.serialize(pxml)
        then:
        cleaned(xmlOut) == cleaned(xmlIn)
    }

    def "xml objects can be cloned"() {
        given:
        String xmlIn = '<?xml version=\"1.0\" encoding=\"UTF-8\"?><abc><def>1</def><ghi>2</ghi><abc>2</abc></abc>'
        Object pxml = instance.parse(xmlIn)
        String xmlOut = instance.serialize(pxml)
        when:
        Object cloned = instance.clone(pxml)
        String clonedOut = instance.serialize(cloned)
        then:
        cleaned(clonedOut) == cleaned(xmlOut)
    }
}
