
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
import groovy.json.JsonSlurper
import spock.lang.Specification

class EpnmIntegrationSpec extends Specification {

    CartographerWorker instance = new CartographerWorker()
    def slurper = new JsonSlurper()

    def "translate create-uni RPC JSON payload to XML"() {
        def realInputSchema = new VersionedSchema("create-uni-dm", "1.0", "JSON")
        def realOutputSchema = new VersionedSchema("create-uni-dm-epnm", "2.1", "XML")
        def input = '''
            {
              "name": "UNI-A",
              "comments": "UNI Order 369943620E_58010",
              "uni-activate": true,
              "customer-ref": "MD=CISCO_EPNM!CUSTOMER=Infrastructure",
              "ni-name": "369006400_58102",
              "ni-comments": "1GBPS CARRIER_ETHERNET_VPN",
              "tp-ref": "MD=CISCO_EPNM!ND=acme-46!FTP=name=GigabitEthernet0/4/6;lr=lr-gigabit-ethernet",
              "ni-activate": true,
              "mtu": 1522,
              "speed": "1GBPS",
              "mode": "FULL DUPLEX",
              "enable-link-management": false,
              "enable-link-oam": false,
              "service-multiplexing": false,
              "bundling": false,
              "all-to-one-bundling": false
            }
        '''

        def expectedOutput = '''
            <?xml version="1.0" encoding="UTF-8"?>
            <provision-service-request xmlns="urn:cisco:params:xml:ns:yang:service:activation:v1">
              <service-order-data>
                <customer-ref>MD=CISCO_EPNM!CUSTOMER=Infrastructure</customer-ref>
                <service-type>carrier-ethernet-vpn</service-type>
                <service-subtype>UNI</service-subtype>
                <service-activate>true</service-activate>
                <termination-point-list>
                  <termination-point-config>
                    <tp-ref>MD=CISCO_EPNM!ND=acme-46!FTP=name=GigabitEthernet0/4/6;lr=lr-gigabit-ethernet</tp-ref>
                    <network-interface-name>369006400_58102</network-interface-name>
                  </termination-point-config>
                </termination-point-list>
                <network-interface-list>
                  <network-interface>
                    <name>369006400_58102</name>
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

    def "translate EPNM UNI create XML response to create-uni RPC response"() {
        def realInputSchema = new VersionedSchema("create-uni-dm-epnm-rsp", "2.1", "XML")
        def realOutputSchema = new VersionedSchema("create-uni-dm-rsp", "1.0", "JSON")
        def input = '''
            <provision-service-response xmlns="urn:cisco:params:xml:ns:yang:service:activation:v1">
                <request-id>fdb20698-d310-4013-aff6-34198d36498f</request-id>
                <service-type>carrier-ethernet-vpn</service-type>
                <service-subtype>UNI</service-subtype>
                <preview>false</preview>
                <request-time>Thu May 04 08:38:10 UTC 2017</request-time>
                <completion-status>SUBMITTED</completion-status>
            </provision-service-response>
        '''

        def expectedOutput = '''
            {
                "request-id": "fdb20698-d310-4013-aff6-34198d36498f",
                "request-time": "Thu May 04 08:38:10 UTC 2017",
                "completion-status": "SUBMITTED"
            }
        '''

        when:
        def output = instance.translate(realInputSchema, realOutputSchema, input)

        then:
        def outputMap = slurper.parseText(output)
        def expectedMap = slurper.parseText(expectedOutput)
        ['request-id', 'request-time', 'completion-status'].each { key ->
            expectedMap[key] == outputMap[key]
        }
    }

}
