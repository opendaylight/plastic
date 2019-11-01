
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.collect.Maps
import spock.lang.Specification


class MorpherFactorySpec extends Specification {

    File createFile(String fileName, String contents) {
        File file = new File(fileName)
        file.text = contents
        file.deleteOnExit()
        file
    }

    final String mClass = "MorpherForUnitTesting"
    final String mCode = "class ${mClass} { void tweakValues(Map i, Map o) {} }"

    MorpherFactoryLogger mockLogger = Mock()
    Map<String,String> seen = Maps.newHashMap()

    AppContext props = new AppContext()
    FilenamesCache fileCache = new FilenamesCache(new SearchPath().find("morphers"))

    MorpherLoader instance = new MorpherLoader(props,
            fileCache,
            new GroovyClassLoader(this.class.classLoader),
            seen, mockLogger)

    def setup() {
        fileCache.scan()
    }

    def "factory should be able to locate and instantiate ad hoc morpher"() {
        given:
        VersionedSchema schema = new VersionedSchema("morphers-factory-spec", "0.0", "XML")
        and:
        String fileName = instance.computeName(schema)
        createFile(fileName, mCode)
        when:
        def m1 = instance.locateImplicitly(schema)
        then:
        m1.wrapped.class.canonicalName == mClass
    }

    def "duplicate morpher classes are noticed"() {
        given:
        List<VersionedSchema> schemas = []
        for (i in (1..2)) {
            VersionedSchema schema = new VersionedSchema("morphers-factory-spec-${i}", "0.0", "XML")
            String fileName = instance.computeName(schema)
            createFile(fileName, mCode)
            schemas << schema
        }
        and:
        instance.locateImplicitly(schemas[0])
        when:
        // First call loads the class in against the first file just fine
        // Second call finds same class but now in a different file
        instance.locateImplicitly(schemas[1])
        then:
        1 * mockLogger.foundDuplicateClass(mClass,_)
    }
}
