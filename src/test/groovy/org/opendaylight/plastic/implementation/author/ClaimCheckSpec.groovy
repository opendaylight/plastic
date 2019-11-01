
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


import groovy.json.JsonSlurper
import org.opendaylight.plastic.implementation.Schema
import org.opendaylight.plastic.implementation.VersionedSchema
import spock.lang.Specification

class ClaimCheckSpec extends Specification {

    Schema asSchema(String strm) {
        VersionedSchema verschema = new VersionedSchema("foo", "1.0", "json")
        new Schema(verschema, strm)
    }

    def "a claimcheck properly replaces a json scalar"() {
        given:
        String payload = '''
        {
            "abc": {
                "items": [
                    "A",
                    "B"
                ]
            }
        }
        '''
        Schema jsonPayload = asSchema(payload)
        and:
        String expected = '''
        {
            "abc": {
                "items": [
                    "\${CLAIMCHECK[0]}",
                    "B"
                ]
            }
        }
        '''
        Object jsonExpected = new JsonSlurper().parseText(expected)
        when:
        ClaimCheck instance = new ClaimCheck("CLAIMCHECK", jsonPayload, jsonPayload.parsed.abc.items, 0)
        instance.swap()
        then:
        jsonPayload.parsed == jsonExpected
    }

    def "a claimcheck properly replaces a json object in a list"() {
        given:
        String payload = '''
        {
            "abc": {
                "items": [
                    { 
                        "A": 1
                    },
                    {
                        "B": 2
                    }
                ]
            }
        }
        '''
        Schema jsonPayload = asSchema(payload)
        and:
        String expected = '''
        {
            "abc": {
                "items": [
                    { 
                        "A": 1
                    },
                    "\${CLAIMCHECK[1]}"
                ]
            }
        }
        '''
        Object jsonExpected = new JsonSlurper().parseText(expected)
        when:
        ClaimCheck instance = new ClaimCheck("CLAIMCHECK", jsonPayload, jsonPayload.parsed.abc.items, 1)
        instance.swap()
        then:
        jsonPayload.parsed == jsonExpected
    }

    def "a claimcheck properly replaces a json object in a map"() {
        given:
        String payload = '''
        {
            "abc": {
                "items": {
                    "key1": { 
                        "A": 1
                    },
                    "key2": {
                        "B": 2
                    }
                }
            }
        }
        '''
        Schema jsonPayload = asSchema(payload)
        and:
        String expected = '''
        {
            "abc": {
                "items": {
                    "key1": { 
                        "A": 1
                    },
                    "\${CLAIMCHECK[key2]}" : {
                    }
                }
            }
        }
        '''
        Object jsonExpected = new JsonSlurper().parseText(expected)
        when:
        ClaimCheck instance = new ClaimCheck("CLAIMCHECK", jsonPayload, jsonPayload.parsed.abc.items, "key2")
        instance.swap()
        then:
        jsonPayload.parsed == jsonExpected
    }
}
