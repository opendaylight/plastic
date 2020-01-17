
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
import spock.lang.Unroll


class VariablesSpec extends Specification {

    def "should recognize occurrences of plastic variables"() {
        given:
        Variables instance = new Variables(input)
        expect:
        instance.raw() == expected
        where:
        input | expected
        '${a}'           | "a"
        '${a}${b}'       | "a, b"
        '  ${abcd}  '    | "abcd"
        'abc${abcd}abcd' | "abcd"
        '${   a    }'    | "a"
    }

    def "should not recognize partial occurrences of plastic variables"() {
        given:
        Variables instance = new Variables(input)
        expect:
        instance.isPresent() == expected
        where:
        input             | expected
        '${a}'            | true
        '${a'             | false
        '  {abcd}  '      | false
        '$ {abcd}'        | false
    }

    def "can custom process each found plastic variable"() {
        given:
        Variables instance = new Variables("\${a}\${b}\${c}")
        def glued = ""
        when:
        instance.toEach { k,v -> glued += k }
        then:
        glued == "abc"
    }

    def "adorns a variable name"() {
        expect:
        Variables.adorn("string") == '${string}'
        Variables.adorn("hyphen-ated") == '${hyphen-ated}'
    }

    def "indexed variables can be recognized"() {
        expect:
        Variables.isIndexed(input) == expected
        where:
        input | expected
        "abc"        | false
        "abc["       | false
        "abc]"       | false
        "abc*"       | false
        "abc[*"      | false
        "abc*]"      | false
        "abc[]"      | false
        "abc[*]"     | true
        "abc[0]"     | true
        "abc[1234]"  | true
    }

    def "generic indexed variables can be recognized"() {
        expect:
        Variables.isGenericIndexed(input) == expected
        where:
        input       | expected
        ""          | false
        "abc"       | false
        '${abc}'    | false
        "abc[*]"    | true
        '${abc[*]}' | true
    }

    def "indexes can be generified"() {
        expect:
        Variables.generifyIndex(input) == expected
        where:
        input           | expected
        '${abc}'        | '${abc}'
        'def[1]'        | 'def[*]'
        '${ghi[1]}'     | '${ghi[*]}'
        '${ghi[1]=345}' | '${ghi[*]=345}'
        '${ghi[*]=345}' | '${ghi[*]=345}'
    }

    def "individual indices can be extracted" () {
        given:
        Variables instance = new Variables(['${abc[1]}', '${def[10]}', '${ghi[100]}', 'jkl' ] as String[])
        when:
        def found = instance.extractIndices()
        then:
        found == [ '[1]', '[10]', '[100]']
    }

    def "specific indexed variables can be generated from a generic indexed variable"() {
        expect:
        Variables.generateManyIndexed("abc[*]", 3) == ["abc[0]", "abc[1]", "abc[2]" ]
    }

    def "no indexed variables are generaged from a non-indexed variable"() {
        expect:
        Variables.generateManyIndexed("abc", 3) == []
    }

    def "no indexed variables are generaged from a nonsense count"() {
        expect:
        Variables.generateManyIndexed("abc[*]", -3) == []
    }

    def "matching works"() {
        expect:
        Variables.matches(s1, s2) == expected
        where:
        s1       | s2       | expected
        ""       | ""       | true
        "abc"    | "abc"    | true
        "abc[1]" | "abc[1]" | true
        "abc[1]" | "abc[*]" | true
        "abc"    | "def"    | false
        "abc[1]" | "abc[2]" | false
    }

    def "multiple matching works"() {
        given:
        Variables instance = new Variables()
        and:
        int dontcare = 0
        Map keyValues = ['abc': dontcare, 'abc[0]': dontcare, 'abc[1]': dontcare, 'def[0]': dontcare]
        String target = 'abc[*]'
        when:
        List results = instance.matches(keyValues, target)
        then:
        results == ['abc[0]', 'abc[1]']
    }

    @Unroll
    def "variable #input should have presence of default value #present"() {
        given:
        Variables instance = new Variables(input)
        expect:
        instance.hasValue(name) == present
        where:
        input        | name    | present
        "\${a}"      | "a"     | false
        "\${a}"      | "b"     | false
        "\${a=}"     | "a"     | true
        "\${a=1}"    | "a"     | true
        "\${a[*]=1}" | "a[0]"  | true
    }

    @Unroll
    def "variable #input should have default values"() {
        given:
        Variables instance = new Variables(input)
        expect:
        instance.getValue(name) == value
        where:
        input           | name | value
        "\${a}"         | "a"  | null
        "\${a}"         | "b"  | null
        "\${a=}"        | "a"  | ""
        "\${a=1}"       | "a"  | "1"
        "\${a=   1   }" | "a"  | "   1   "
    }

    @Unroll
    def "raw variable names can be found in #input"() {
        given:
        Variables instance = new Variables(input)
        expect:
        instance.getRawNames() == expected
        where:
        input           | expected
        ''              | []
        '\${a}'         | ['\${a}']
        '\${a}\${b}'    | ['\${a}', '\${b}']
        '\${a[*]}\${b}' | ['\${a[*]}', '\${b}']
    }

    @Unroll
    def "ad hoc parsing of multiple variables works"() {
        given:
        Variables instance = new Variables()
        expect:
        instance.splitApart(input) == expected
        where:
        input           | expected
        ''              | [ ]
        'xyz'           | [ 'xyz' ]
        '${a}'          | [ '${a}' ]
        '${a}${b}'      | [ '${a}', '${b}' ]

        'xyz${a}'       | [ 'xyz', '${a}' ]
        '${a}xyz'       | [ '${a}', 'xyz' ]
        '${a}xyz${b}'   | [ '${a}', 'xyz', '${b}' ]

        '${a'           | [ '${a' ]
        'a}'            | [ 'a}' ]
    }
}
