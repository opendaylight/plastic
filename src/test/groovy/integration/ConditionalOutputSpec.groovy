
/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package integration

import org.opendaylight.plastic.implementation.CartographerWorker
import org.opendaylight.plastic.implementation.VersionedSchema
import spock.lang.Specification

class ConditionalOutputSpec extends Specification {

    CartographerWorker instance = new CartographerWorker()

    static def input1 = '''
    {
        "interface-name": "intf-1/123",
        "device-name": "dev123",
        "interface-type" : "iana-if-type:other",
        "action" :"CREATE",
        "description":"This is an interface",
        "mtu": "9150",
        "vlan-id": "22",
        "ip-addresses-v4" : [
            {
                 "ip-address" : "192.168.1.1",
                 "prefix-length" : "24"
            },
            {
                 "ip-address" : "192.168.2.1",
                 "prefix-length" : "24"
            }
        ]
    }
    '''

    static def input2 = '''
    {
        "interface-name": "intf-1/123",
        "device-name": "dev123",
        "interface-type" : "iana-if-type:other",
        "action" :"CREATE",
        "description":"This is an interface",
        "vlan-id": "22",
        "ip-addresses-v4" : [
            {
                 "ip-address" : "192.168.1.1",
                 "prefix-length" : "24"
            },
            {
                 "ip-address" : "192.168.2.1",
                 "prefix-length" : "24"
            }
        ]
    }
    '''

    static def input3 = '''
    {
        "interface-name": "intf-1/123",
        "device-name": "dev123",
        "interface-type" : "iana-if-type:other",
        "action" :"CREATE",
        "description":"This is an interface",
        "mtu": "9150",
        "vlan-id": "22",
        "ip-addresses-v4" : [
        ]
    }
    '''

    static def input4 = '''
    {
        "interface-name": "intf-1/123",
        "device-name": "dev123",
        "interface-type" : "iana-if-type:other",
        "action" :"CREATE",
        "description":"This is an interface",
        "mtu": "9150",
        "vlan-id": "22",
    }
    '''

    static def expectedOutput1 = '''
    {
        "requests": [
            {
                "URL": "network-topology:network-topology/topology/topology-netconf/node/dev123/yang-ext:mount/ietf-interfaces:interfaces/interface/intf-1%2F123",
                "payload": {
                    "interface": [
                        {
                            "description": "This is an interface",
                            "mtu": "9150",
                            "name": "intf-1/123",
                            "type": "vlan-interface-std:l3ipvlan",
                            "vlan-interface-std:vlan": {
                                    "ip-management-vlan-std:ip": {
                                            "address": [
                                                    {
                                                            "ip": "192.168.1.1",
                                                            "prefix-length": "24"
                                                    },
                                                    {
                                                            "ip": "192.168.2.1",
                                                            "prefix-length": "24"
                                                    }
                                            ]
                                    }
                            }
                        }
                    ]
                },
                "type": "PUT"
            }
        ]
    }
    '''

    static def expectedOutput2 = '''
    {
        "requests": [
            {
                "URL": "network-topology:network-topology/topology/topology-netconf/node/dev123/yang-ext:mount/ietf-interfaces:interfaces/interface/intf-1%2F123",
                "payload": {
                    "interface": [
                        {
                            "description": "This is an interface",
                            "name": "intf-1/123",
                            "type": "vlan-interface-std:l3ipvlan",
                            "vlan-interface-std:vlan": {
                                    "ip-management-vlan-std:ip": {
                                            "address": [
                                                    {
                                                            "ip": "192.168.1.1",
                                                            "prefix-length": "24"
                                                    },
                                                    {
                                                            "ip": "192.168.2.1",
                                                            "prefix-length": "24"
                                                    }
                                            ]
                                    }
                            }
                        }
                    ]
                },
                "type": "PUT"
            }
        ]
    }
    '''

    static def expectedOutput3 = '''
    {
        "requests": [
            {
                "URL": "network-topology:network-topology/topology/topology-netconf/node/dev123/yang-ext:mount/ietf-interfaces:interfaces/interface/intf-1%2F123",
                "payload": {
                    "interface": [
                        {
                            "description": "This is an interface",
                            "mtu": "9150",
                            "name": "intf-1/123",
                            "type": "vlan-interface-std:l3ipvlan"
                        }
                    ]
                },
                "type": "PUT"
            }
        ]
    }
    '''

    def "support conditional output construction"() {
        given:
        def realInputSchema = new VersionedSchema("conditionaloutput-in", "1.0", "json")
        def realOutputSchema = new VersionedSchema("conditionaloutput-out", "1.0", "json")

        when:
        def output = instance.translate(realInputSchema, realOutputSchema, payload)

        then:
        output.replaceAll("(\n|\\s*)", "") == expected.replaceAll("(\n|\\s*)", "")

        where:
        payload | expected
        input1 | expectedOutput1
        input2 | expectedOutput2
        input3 | expectedOutput3
        input4 | expectedOutput3
    }
}
