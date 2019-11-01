
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package org.opendaylight.plastic.implementation.author

import spock.lang.Specification
import spock.lang.Unroll

class Testable implements InputsAndOutputs {}

class InputsAndOutputsSpec extends Specification {

    InputsAndOutputs instance = new Testable()

    @Unroll
    def "all inputs can be ignored"() {
        given:
        instance."$innerListName" = ignoreThese
        Set results = incoming as Set
        when:
        instance."$methodName"(results)
        then:
        results == survivors
        where:
        innerListName   | methodName            | ignoreThese  | incoming      | survivors
        'optioned'      | '_blessMissingInputs' | [ '*' ]      | ['a', 'b']    | [] as Set
        'optioned'      | '_blessMissingInputs' | [ 'a' ]      | ['a', 'b']    | ['b'] as Set
        'optioned'      | '_blessMissingInputs' | [ 'a', 'b' ] | ['a', 'b']    | [] as Set
        'optioned'      | '_blessMissingInputs' | [ 'c' ]      | ['a', 'b']    | ['a', 'b'] as Set
        'optioned'      | '_blessMissingInputs' | [ 'a[*]' ]   | ['a[0]', 'b'] | ['b'] as Set
        'optioned'      | '_blessMissingInputs' | [ 'a[*]' ]   | ['a[0]']      | [] as Set

        'ignoredInputs' | '_blessDanglingInputs' | [ '*' ]      | ['a', 'b'] | [] as Set
        'ignoredInputs' | '_blessDanglingInputs' | [ 'a' ]      | ['a', 'b'] | ['b'] as Set
        'ignoredInputs' | '_blessDanglingInputs' | [ 'a', 'b' ] | ['a', 'b'] | [] as Set
        'ignoredInputs' | '_blessDanglingInputs' | [ 'c' ]      | ['a', 'b'] | ['a', 'b'] as Set

        'ignoredOutputs' | '_blessDanglingOutputs' | [ '*' ]      | ['a', 'b'] | [] as Set
        'ignoredOutputs' | '_blessDanglingOutputs' | [ 'a' ]      | ['a', 'b'] | ['b'] as Set
        'ignoredOutputs' | '_blessDanglingOutputs' | [ 'a', 'b' ] | ['a', 'b'] | [] as Set
        'ignoredOutputs' | '_blessDanglingOutputs' | [ 'c' ]      | ['a', 'b'] | ['a', 'b'] as Set
    }
}
