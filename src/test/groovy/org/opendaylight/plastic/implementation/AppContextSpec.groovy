/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification


class AppContextSpec extends Specification {

    @Shared
    File rootDir

    def setupSpec() {
        File tmpDir = File.createTempDir()
        tmpDir.deleteOnExit()

        rootDir = new File(tmpDir, 'app-conf-unit-test')
        rootDir.mkdir()
        rootDir.deleteOnExit()
    }

    SearchPath path
    AppContext instance

    def setup() {
        path = new SearchPath(rootDir.absolutePath)
        instance = new AppContext(path)
    }

    File writeProperties(String name, Map properties) {
        File m1 = new File(rootDir, name)
        m1.text = ""
        m1.deleteOnExit()

        m1.withWriter { out ->
            properties.each { k,v ->
                out.println("${k} = ${v}")
            }
        }

        m1
    }

    def "can find properties using a default name"() {
        given:
        Map data = ['a': "0", 'b': "1", 'c': "2"]
        File pf = writeProperties("cartographer.properties", data)
        when:
        instance.reread()
        then:
        instance.isEqualTo(data)
        cleanup:
        pf.delete()
    }

    def "can find properties using the other default name"() {
        given:
        Map data = ['a': "00", 'b': "11", 'c': "22"]
        File pf = writeProperties("plastic.properties", data)
        when:
        instance.reread()
        then:
        instance.isEqualTo(data)
        cleanup:
        pf.delete()
    }

    def asJson(String s) {
        new JsonSlurper().parseText(s)
    }

    // Unit tests above, integration test below

    def "application properties can be referenced within morphers or classififers"() {
        given:
        VersionedSchema inschema = new VersionedSchema("\${app-props-classifier}", "1.0", "json")
        VersionedSchema outschema = new VersionedSchema("app-props-out", "1.0", "json")
        and:
        File pf = new File("src/test/resources/app-props/app-props-payload.json")
        String payload = pf.text
        and:
        CartographerWorker instance = new CartographerWorker(new SearchPath("app-props.properties"))
        when:
        String result = instance.translate(inschema, outschema, payload)
        then:
        asJson(result) == ['classifier-says': 'classifier-ok', 'morpher-says': 'morpher-ok', 'schema-says': 'schema-ok']
    }
}
