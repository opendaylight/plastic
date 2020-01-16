
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


class ParrotNotificationSpec extends Specification {

    CartographerWorker instance = new CartographerWorker()
    JsonSlurper slurper = new JsonSlurper()

    def "translate parrot notification into standard json notification"() {
        given:
        def realInputSchema = new VersionedSchema("nui-create-notify-parrot", "1.0", "JSON")
        def realOutputSchema = new VersionedSchema("std-notification", "1.0", "JSON")
        def input = '''
            {
              "change-update": {
                "notification-id": 6521099600772301159,
                "request-id": "91d87d81-3b4f-4dc9-a12d-88c128d19d2c",
                "topic": "service-activation",
                "time-of-update": "2017-05-11 10:37:25.591",
                "operation": "push:create",
                "service-activation-response": {
                  "ni-ref": "MD=CISCO_EPNM!NI=369002209_50002",
                  "service-type": "carrier-ethernet-vpn",
                  "service-subtype": "UNI",
                  "preview": false,
                  "request-time": "Thu May 11 10:37:22 UTC 2017",
                  "completion-time": "Thu May 11 10:37:24 UTC 2017",
                  "deploy-results": {
                    "result": {
                      "node-ref": "MD=CISCO_EPNM!ND=acme-1",
                      "state": "SUCCESSFUL",
                      "config": ""
                    }
                  },
                  "completion-status": "SUCCESS"
                }
              }
            }
        '''

        when:
        def output = instance.translate(realInputSchema, realOutputSchema, input)
        and:
        def found = slurper.parseText(output)
        def expected = slurper.parseText(input)
        then:
        // The found and expected are Maps and do not compare when they should.
        // Suspect that there are some GStrings on one and Strings on the others.
        // Void the issue by converting the whole maps to a strings.
        found.toString() == expected.toString()
    }
}
