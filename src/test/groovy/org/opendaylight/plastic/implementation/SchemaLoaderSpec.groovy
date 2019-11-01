
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


class SchemaLoaderSpec extends Specification {

    FilenamesCache cache = new FilenamesCache(new SearchPath().find("schemas"))
    SchemaLoader instance = new SchemaLoader(cache)

    def setup() {
        cache.scan()
    }

    def equals(inStream, expectedFileName) {
        // verifies the inStream contains the expected file content by comparing against the expected file line by line
        String schemaFileName = "./schemas/${expectedFileName}"

        def schemaFile = new File(schemaFileName)
        if (schemaFile.exists()) {
            def iterator = new ByteArrayInputStream(schemaFile.getBytes()).newReader().iterator()

            inStream.eachLine() { s -> assert iterator.next() == s }

            return true
        } else {
            println("unable to open the schema file : ${schemaFileName}")
            assert false : "Test design issues - please investigate"
        }

        false
    }

    def "throws exception if no VersionedSpec is given"() {
        when:
        instance.locate(null)
        then:
        thrown(NullPointerException)
    }

    def "throws exception if a VersionedSpec is given which does not resolve to a known file"() {
        when:
        instance.locate(new VersionedSchema("test", "test", "JSON"))
        then:
        thrown(SchemaLoader.LocationNotFoundException)
    }

    def "returns an InputStream for a good JSON type schema"() {
        when:
        def inStream = instance.locate(new VersionedSchema("uni-create", "1.0", "JSON"))

        then:
        inStream.available() > 0
        equals(inStream, 'inputs/uni-create-1.0.json')
    }

    def "returns an InputStream for a good XML type schema"() {
        when:
        def inStream = instance.locate(new VersionedSchema("uni-create-epm", "2.1", "XML"))

        then:
        inStream.available() > 0
        equals(inStream, 'outputs/uni-create-epm-2.1.xml')
    }

}
