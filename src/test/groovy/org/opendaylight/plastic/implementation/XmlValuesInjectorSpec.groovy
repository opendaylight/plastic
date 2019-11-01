
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.xml.XmlUtil
import spock.lang.Specification


class XmlValuesInjectorSpec extends Specification {

    XmlValuesInjector instance = new XmlValuesInjector()

    Node asXml(String input) {
        new XmlParser().parseText(input)
    }

    def asString(Node model) {
        String prelude = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        String raw = XmlUtil.serialize(model)
        raw = raw.replaceAll("\\s*\n\\s*","")
        if (raw.startsWith(prelude))
            raw = raw.substring(prelude.length())
        raw
    }

    def "single substitution for an element value"() {
        given:
        HashMap bound = [ 'ggg': '123' ]
        Node model = asXml("<abc>\${ggg}</abc>")

        when:
        def out = instance.inject(bound, model)

        then:
        asString(out) == "<abc>123</abc>"
    }

    def "multiple substitutions are supported as a single value"() {
        given:
        HashMap bound = [ 'ggg': '123' ]
        Node model = asXml("<abc>\${ggg} \${ggg}</abc>")

        when:
        def out = instance.inject(bound, model)

        then:
        asString(out) == "<abc>123 123</abc>"
    }

    def "multiple values can be substituted as a single value"() {
        given:
        HashMap bound = [ 'ggg': '123', 'hhh': '456' ]
        Node model = asXml("<abc>\${ggg}\${hhh}</abc>")

        when:
        def out = instance.inject(bound, model)

        then:
        asString(out) == "<abc>123456</abc>"
    }

    def "value substitution occurs for deeply nested models"() {
        given:
        HashMap bound = [ 'abc': '123', 'def': '456', 'ghi': '789' ]
        def model = asXml(
                "<p1>" +
                "   <abc>\${abc}</abc>" +
                "   <p2>" +
                "      <def>\${def}</def>" +
                "      <p3>" +
                "         <ghi>\${ghi}</ghi>" +
                "      </p3>" +
                "   </p2>" +
                "</p1>")

        when:
        def out = instance.inject(bound, model)

        then:
        asString(out) == "<p1><abc>123</abc><p2><def>456</def><p3><ghi>789</ghi></p3></p2></p1>"
    }

    def "an input variable that is not mapped to an output is detected as an error"() {
        given:
        HashMap bound = [ 'ggg': '123', 'hhh': 456 ]
        Node model = asXml("<abc>\${ggg}</abc>")

        when:
        instance.inject(bound, model)

        then:
        thrown(XmlValuesInjector.DangingInputsException)
    }

    def "an output variable that is not mapped to an input is detected as an error"() {
        given:
        HashMap bound = [ 'ggg': '123' ]
        Node model = asXml("<abc>\${hhh}</abc>")

        when:
        instance.inject(bound, model)

        then:
        thrown(XmlValuesInjector.DangingOutputsException)
    }
}
