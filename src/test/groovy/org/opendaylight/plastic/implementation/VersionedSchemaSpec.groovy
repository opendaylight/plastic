
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

class VersionedSchemaSpec extends Specification {
    VersionedSchema instance = new VersionedSchema("mockName", "mockVersion", "mockType")

    def "versioned schema has a String property called 'type'"() {
        expect:
        instance.getType() == "mockType".toLowerCase()
    }

    def "versioned schema has a String property called 'version'"() {
        expect:
        instance.getVersion() == "mockVersion"
    }

    def "versioned schema has a String property called 'name'"() {
        expect:
        instance.getName() == "mockName"
    }

    def "can convert to human readable format"() {
        expect:
        instance.toString()
    }
}
