
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


class XmlFinderBinderSpec extends Specification {
    XmlFinderBinder instance = new XmlFinderBinder()

    def asXML(String s) {
        String data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+s
        InputStream istrm = new ByteArrayInputStream(data.getBytes())
        new XmlParser(false,false).parse(istrm)
    }

    def "inverting a hash map works"() {
        given:
        def inmap = ['abc': '1', 'def': '2', 'ghi': '1' ]
        def expected = [ '1': [ 'abc', 'ghi' ], '2': [ 'def' ]]
        when:
        def found = instance.invertMap(inmap)
        then:
        found.equals(expected)
    }

    def "no variables are found for 'empty' model"() {
        given:
        def model = asXML("<abc></abc>")
        def payload = asXML("<abc></abc>")
        when:
        def found = instance.process(model, payload).bindings()
        then:
        found.isEmpty()
    }

    def "a variable is found in a model and its value is bound"() {
        given:
        def model = asXML("<abc>\${xyz}</abc>")
        def payload = asXML("<abc>123</abc>")
        when:
        def found = instance.process(model, payload).bindings()
        then:
        found['xyz'] == '123'
    }

    def "values can be duplicated"() {
        given:
        def model = asXML("<abc>\${xyz1}\${xyz2}</abc>")
        def payload = asXML("<abc>123</abc>")
        when:
        def found = instance.process(model, payload).bindings()
        then:
        found['xyz1'] == '123'
        found['xyz2'] == '123'
    }

    def "deeply nested variables can be bound"() {
        given:
        def model = asXML(
                "<p1>" +
                "   <abc>\${xyz1}</abc>" +
                "   <p2>" +
                "      <def>\${xyz2}</def>" +
                "      <p3>" +
                "         <ghi>\${xyz3}</ghi>" +
                "      </p3>" +
                "   </p2>" +
                "</p1>")
        def payload = asXML(
                "<p1>" +
                   "<abc>123</abc>" +
                   "<p2>" +
                      "<def>456</def>" +
                      "<p3>" +
                         "<ghi>789</ghi>" +
                      "</p3>" +
                   "</p2>" +
                "</p1>")
        when:
        def found = instance.process(model, payload).bindings()
        then:
        found['xyz1'] == '123'
        found['xyz2'] == '456'
        found['xyz3'] == '789'
    }

    def "a variable from the model but not found in the payload has a null value"() {
        given:
        def model = asXML('''
        <p1>
            <something>${abc}</something>
        </p1>
        ''')
        def payload = asXML('<p1></p1>')

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found.containsKey("abc")
        found["abc"] == null
    }

    def "a variable from the model but not found in the payload has a default value"() {
        given:
        def model = asXML('''
        <p1>
            <something>${abc=1234}</something>
        </p1>
        ''')
        def payload = asXML('<p1></p1>')

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found["abc"] == "1234"
    }

    def "a default value does not override an explicit value"() {
        given:
        def model = asXML('''
        <p1>
            <something>${abc=222}</something>
        </p1>
        ''')
        def payload = asXML('''
        <p1>
            <something>333</something>
        </p1>
        ''')

        when:
        Map found = instance.process(model, payload).bindings()
        
        then:
        found["abc"] == "333"
        }

    def "multiple variables can have individual defaults"() {
        given:
        def model = asXML('''
        <p1>
            <something>${abc=111}${def=222}</something>
        </p1>
        ''')
        def payload = asXML('<p1></p1>')

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found["abc"] == "111"
        found["def"] == "222"
    }

    def "multiple variables with mixed presence in the payload have correct defaults"() {
        given:
        def model = asXML('''
        <p1>
            <something>${abc=111}${def}</something>
        </p1>
        ''')
        def payload = asXML('<p1></p1>')

        when:
        Map found = instance.process(model, payload).bindings()

        then:
        found["abc"] == "111"
        found.containsKey("def") && found["def"] == null
    }
}
