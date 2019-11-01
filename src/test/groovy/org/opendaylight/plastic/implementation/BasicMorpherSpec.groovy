
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


class BasicMorpherSpec extends Specification {

    BasicMorpher instance = new BasicMorpher()

    def "basic morpher does not ignore variables by default"() {
        given:
        Set inputs = ["a", "b", "c"] as Set
        when:
        instance."${method}"(inputs)
        then:
        inputs == ["a", "b", "c"] as Set
        where:
        method                  | _
        "_blessDanglingInputs"  | _
        "_blessDanglingOutputs" | _
        "_blessMissingInputs"   | _
    }

    def "basic morpher does ignores only specific variables"() {
        given:
        Set inputs = ["a", "b", "c"] as Set
        instance."${publicMethod}"("b")
        when:
        instance."${privateMethod}"(inputs)
        then:
        inputs == ["a", "c"] as Set
        where:
        privateMethod           | publicMethod
        "_blessDanglingInputs"  | "ignoreUnusedInputs"
        "_blessDanglingOutputs" | "ignoreUnusedOutputs"
        "_blessMissingInputs"   | "optionalInputs"
    }

    def "basic morpher can ignore all variables"() {
        given:
        Set inputs = ["a", "b", "c"] as Set
        instance."${publicMethod}"()
        when:
        instance."${privateMethod}"(inputs)
        then:
        inputs.isEmpty()
        where:
        privateMethod           | publicMethod
        "_blessDanglingInputs"  | "ignoreUnusedInputs"
        "_blessDanglingOutputs" | "ignoreUnusedOutputs"
        "_blessMissingInputs"   | "optionalInputs"
    }

    def "basic morpher can ignore arrayed variables"() {
        given:
        Set inputs = ["a", "b", "c", "d[0]", "d[1]"] as Set
        instance."${publicMethod}"("d[*]")
        when:
        instance."${privateMethod}"(inputs)
        then:
        inputs == ["a", "b", "c"] as Set
        where:
        privateMethod           | publicMethod
        "_blessDanglingInputs"  | "ignoreUnusedInputs"
        "_blessDanglingOutputs" | "ignoreUnusedOutputs"
        "_blessMissingInputs"   | "optionalInputs"
    }

    def "isBound can tell if a variable has a value or not"() {
        given:
        instance.inputs = ['abc':1, 'def':"ghi"]
        expect:
        instance.isBound('abc')
        instance.isBound('def')
        !instance.isBound('xyz')
    }

    def "isEmpty can tell if a collection has members or not"() {
        expect:
        instance.isEmpty(null)
        instance.isEmpty([])
        !instance.isEmpty(['a'])
        instance.isEmpty([:])
        !instance.isEmpty(['a':1])
    }

    def "isEmpty should not be called on a non-collection"() {
        when:
        instance.isEmpty("abc")
        then:
        thrown(RuntimeException)
    }

    def "basic morpher provides a convenient url encoding call" () {
        expect:
        instance.urlEncode("") == ""
        instance.urlEncode("abc") == "abc"
        instance.urlEncode("a b c") == "a+b+c"
    }
}
