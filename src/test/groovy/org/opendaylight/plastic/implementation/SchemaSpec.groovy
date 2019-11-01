
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

class SchemaSpec extends Specification {

    VersionedSchema dontcare = new VersionedSchema("something", "1.0", "json")

    def "a schema can be independently cloned"() {
        given:
        String raw = '''
        {
            "a": 1, 
            "b": 2 
        }
        '''
        when:
        Schema instance1 = new Schema(dontcare, raw)
        Schema instance2 = instance1.clone()
        and:
        instance1.parsed['a'] += 1
        then:
        instance1.parsed['a'] == instance2.parsed['a'] + 1
    }
}
