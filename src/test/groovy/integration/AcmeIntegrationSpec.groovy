
/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package integration

import groovy.json.JsonSlurper
import org.opendaylight.plastic.implementation.CartographerWorker
import org.opendaylight.plastic.implementation.VersionedSchema
import spock.lang.Specification

class AcmeIntegrationSpec extends Specification {

    def asJson(String s) {
        new JsonSlurper().parseText(s)
    }

    def benchmark = { cls ->
        def start = System.currentTimeMillis()
        cls.call()
        def now = System.currentTimeMillis()
        now - start
    }

    CartographerWorker instance = new CartographerWorker()

    def "translate ABC Create from JSON to XML"() {
        def realInputSchema = new VersionedSchema("acme-input", "1.0", "json")
        def realOutputSchema = new VersionedSchema("acme-output", "1.0", "json")

        def input = '''
            {
              "adminStatus": "UP",
              "description": "some-description",
              "deviceName": "some-device-name",
              "interfaceName": "some-interface-name",
               "ipAddressesV4": [
                        {
                          "ip-address": "192.168.1.1",
                          "prefix-length": "10"
                        },
                        {
                          "ip-address": "192.168.1.2",
                          "prefix-length": "20"
                        }
              ],
              "ipAddressesV6": [
                        {
                          "ip-address": "0::::",
                          "prefix-length": "11"
                        }
              ],
              "mtu": "1500",
              "mplsEnable": false,
              "bundleId": "na",
              "vlanId": "100",
              "subIntfId": "11",
              "operStatus": "UP",
              "deviceVendor": "juniper"
            }
           '''

        def expectedOutput = '''
            {
              "unit": [
                {
                  "description": "VLAN - Created by the Acme",
                  "dev-name": "some-device-name",
                  "family": {
                    "inet": {
                      "address": [
                        {
                          "name": "192.168.1.1/10"
                        },
                        {
                          "name": "192.168.1.2/20"
                        }
                      ]
                    },
                    "inet6": {
                      "address": [
                        {
                          "name": "0::::/11"
                        }
                      ]
                    }
                  },
                  "name": "some-interface-name",
                  "vlan-id": "100"
                }
              ]
            }
        '''
        when:

        def span = benchmark {
            (1..100).each {
                instance.translate(realInputSchema, realOutputSchema, input)
            }
        }

        def output = instance.translate(realInputSchema, realOutputSchema, input)

        then:
        asJson(output) == asJson(expectedOutput)
        println "Span for 100 is msec: ${span}"
    }
}
