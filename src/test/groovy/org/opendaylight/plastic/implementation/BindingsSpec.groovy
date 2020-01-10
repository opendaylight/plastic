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

class BindingsSpec extends Specification {

    def "bindings can be retrieved"() {
        given:
        Map<String,Object> map = [ 'a': 1, 'b': [ 0, 1 ] ]
        Bindings instance = new Bindings(map)
        expect:
        instance.bindings() == [ 'a': 1, 'b': [ 0, 1 ] ]
    }

    def "missing variables can be injected"() {
        given:
        Map<String,Object> map = [ 'a': 1, 'b': [ 0, 1 ] ]
        Bindings instance = new Bindings(map)
        when:
        instance.addMissingVariables([ 'a', 'c', 'd' ] as Set)
        then:
        instance.bindings() == [ 'a': 1, 'b': [ 0, 1 ], 'c': null, 'd': null ]
    }

    def "variables with their values can be injected"() {
        given:
        Map<String,Object> map = [ 'a': 1, 'b': [ 0, 1 ] ]
        Bindings instance = new Bindings(map)
        when:
        instance.set('c', '012')
        then:
        instance.bindings() == [ 'a': 1, 'b': [ 0, 1 ], 'c': '012' ]
    }

    def "missing variables can be assigned from defaults"() {
        given:
        Map<String,Object> map = [ 'a': 1, 'b': [ 0, 1 ], 'c': null, 'd': null ]
        Bindings instance = new Bindings(map)
        when:
        instance.applyDefaults([ 'a': 10, 'c': [ 0 ], 'd': 'ddd' ])
        then:
        instance.bindings() == [ 'a': 1, 'b': [ 0, 1 ], 'c': [ 0 ], 'd': 'ddd' ]
    }

    def "variables with default values can be overridden"() {
        given:
        Map<String,Object> map = [ 'a': 1, 'b': [ 0, 1 ], 'c': null, 'd': null ]
        Bindings instance = new Bindings(map)
        instance.applyDefaults([ 'a': 10, 'c': [ 0 ], 'd': 'ddd' ])
        when:
        instance.overrideDefaultValuesWith([ 'c': 'ccccc', 'd': 'ddddd' ])
        then:
        instance.bindings() == [ 'a': 1, 'b': [ 0, 1 ], 'c': 'ccccc', 'd': 'ddddd' ]
    }
}
