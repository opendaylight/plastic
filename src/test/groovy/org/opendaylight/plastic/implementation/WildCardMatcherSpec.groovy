package org.opendaylight.plastic.implementation

import spock.lang.Specification

class WildCardMatcherSpec extends Specification {

    def "use of wildcarding is recognizable"() {
        expect:
        WildCardMatcher.usesWildcarding(candidate) == expected
        where:
        candidate   | expected
        ''          | false
        'abc'       | false
        '|def  '    | false
        'foobar | ' | false
        '||'        | true
        '|${abc}|'  | true
    }

    def "weird: a single variable is a valid wildcard statement"() {
        given:
        def template = '|${abc}|'
        def candidate = "one two three"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'one two three'
    }

    def "multiple variables can be in a wildcard statement"() {
        given:
        def template = '|${abc}/${def}|'
        def candidate = "one/two three"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'one'
        result.get('def') == 'two three'
    }

    def "asterisks can be used for unwanted string content"() {
        given:
        def template = '|*${abc}/${def}*|'
        def candidate = "one two/three four"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'two'
        result.get('def') == 'three'
    }

    def "asterisks are greedy but are anchored by literals"() {
        given:
        def template = '|*${abc} ${def}*|'
        def candidate = "one two/three four"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'three'
        result.get('def') == 'four'
    }

    def "can explicitly represent each word with an asterisks or variable"() {
        given:
        def template = '|* ${abc}/${def} *|'
        def candidate = "one two/three four"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'two'
        result.get('def') == 'three'
    }

    def "can grab the first word"() {
        given:
        def template = '|${abc} *|'
        def candidate = "one two/three four"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'one'
    }

    def "weird: asterisks without any literal anchor are valid"() {
        given:
        def template = '|*${abc}*|'
        def candidate = "one two three"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'three'
    }

    def "weird: asterisks and multiple variables without any literal anchor are valid"() {
        given:
        def template = '|*${abc}*${def}*|'
        def candidate = "one two three"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == ' '
        result.get('def') == 'three'
    }

    def "weird: no asterisks with no literals is valid"() {
        given:
        def template = '|${abc}${def}|'
        def candidate = "one two three"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'one'
        result.get('def') == ' two three'
    }

    def "edge: all forms of bad templates are recognized"() {
        when:
        new WildCardMatcher(bad)
        then:
        thrown(WildCardMatcher.NoWildcardingFound)
        where:
        bad    | _
        ''     | _
        null   | _
        "a"    | _
    }

    def "edge: all forms of bad candidates do not match and do not throw"() {
        given:
        def template = '|*${abc}*|'
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        instance.fetch(candidate)
        then:
        thrown(WildCardMatcher.NoMatchFound)
        where:
        candidate | _
        ''        | _
        null      | _
    }

    def "weird: template with no variables matches but without bindings"() {
        given:
        def template = '|*|'
        def candidate = "one two three"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.isEmpty()
    }

    def "edge: a repeated variable is legal but odd"() {
        given:
        def template = '|${abc} ${abc}|'
        def candidate = "one two three"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'one'
    }

    def "can grab the last word"() {
        given:
        def template = '|* ${abc}|'
        def candidate = "one two/three four"
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'four'
    }

    def "regex special characters are protected in wildcard statements"() {
        given:
        def template = '|() {2,} [^abc] ??? ${abc}|'
        def candidate = '() {2,} [^abc] ??? last'
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Bindings result = instance.fetch(candidate)
        then:
        result.get('abc') == 'last'
    }

    def "client logic can easily work with wildcarding"() {
        given:
        def template = '|*${abc}*${def}*|'
        and:
        WildCardMatcher instance = new WildCardMatcher(template)
        when:
        Variables vars = instance.getVariables()
        def found = []
        vars.toEach { String name, Object val -> found.add (name) }
        then:
        vars.hasMultiple()
        found == [ 'abc', 'def' ]
    }
}
