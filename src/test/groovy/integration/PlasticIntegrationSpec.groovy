
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

class PlasticIntegrationSpec extends Specification {

    CartographerWorker instance = new CartographerWorker()

    def "translate UNI Create from JSON to XML"() {
        def realInputSchema = new VersionedSchema("uni-create", "1.0", "JSON")
        def realOutputSchema = new VersionedSchema("uni-create-epm", "2.1", "XML")
        def input = '''
            {
              "uni": [{
                "univ-id": "400_58102",
                "name": "UNI-A",
                "description": "UNI Order 369943620E_58010",
                "service-type": "CARRIER-ETHERNET",
                "service-subtype": "UNI",
                "admin-status": "UP",
                "tenant-name": "Infrastructure",
                "vem-name": "lab-parrot",
                "skip-deploy": false,
                "endpoint": {
                  "device": "nc-46",
                  "port": "GigabitEthernet0/4/6",
                  "port-bandwidth": "gigabit-ethernet"
                },
                "ce-vlan-id-for-untagged-and-priority": 1,
                "ce-vlan-ids": [{
                  "vlan-id": 1
                }],
                "uni-ce-data-profile": {
                  "name": "uni-ce-data-profile-1",
                  "description": "1GBPS CARRIER_ETHERNET_VPN",
                  "all-to-one-bundling-enabled": false,
                  "bundling-enabled": false,
                  "link-oam-enabled": false,
                  "speed": "1GBPS",
                  "mode": "FULL-DUPLEX",
                  "mtu": 1522,
                  "service-multiplexing-enabled": false,
                  "activate": true
                }
              }]
            }
        '''

        def expectedOutput = '''
            <?xml version="1.0" encoding="UTF-8"?>
            <provision-service-request xmlns="service:activation:v1">
              <service-order-data>
                <customer-ref>Infrastructure</customer-ref>
                <service-type>carrier-ethernet-vpn</service-type>
                <service-subtype>UNI</service-subtype>
                <service-activate>true</service-activate>
                <termination-point-list>
                  <termination-point-config>
                    <tp-ref>gigabit-ethernet</tp-ref>
                    <network-interface-name>400_58102</network-interface-name>
                  </termination-point-config>
                </termination-point-list>
                <network-interface-list>
                  <network-interface>
                    <name>400_58102</name>
                    <ce-data>
                      <activate>true</activate>
                      <description>1GBPS CARRIER_ETHERNET_VPN</description>
                      <mtu>1522</mtu>
                      <speed>1GBPS</speed>
                      <mode>FULL DUPLEX</mode>
                      <service-multiplexing>false</service-multiplexing>
                      <bundling>false</bundling>
                      <enable-link-oam>false</enable-link-oam>
                      <enable-link-Management>false</enable-link-Management>
                      <all-to-one-bundling>false</all-to-one-bundling>
                    </ce-data>
                  </network-interface>
                </network-interface-list>
              </service-order-data>
            </provision-service-request>
        '''
        when:
        def output = instance.translate(realInputSchema, realOutputSchema, input)

        then:
        output.replaceAll("\n\\s*", "") == expectedOutput.replaceAll("\n\\s*", "")
    }
}
