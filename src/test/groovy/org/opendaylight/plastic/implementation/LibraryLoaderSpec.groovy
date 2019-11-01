
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import spock.lang.Shared
import spock.lang.Specification

class LibraryLoaderSpec extends Specification {

    LibraryLoaderLogger mockLogger = Mock()
    GroovyClassLoader groovyClassLoader = new GroovyClassLoader(this.class.getClassLoader())
    Map<String,Class> libs = new HashMap<>()
    Set<String> libDirs = new HashSet<>()

    @Shared
    File emptyDir
    @Shared
    File libDir
    @Shared
    File libSubDir

    @Shared
    String morpher1groovy="""
    import UnitTestMorpher3
    import libsubdir.UnitTestMorpher4
    class UnitTestMorpher1 {
        void tweakValues(Map ins, Map outs) {
        }
    }
    """

    @Shared
    String morpher2groovy="""
    class UnitTestMorpher2 {
        void tweakValues(Map ins, Map outs) {
        }
    }
    """

    @Shared
    String morpher3groovy="""
    class UnitTestMorpher3 {
        void tweakValues(Map ins, Map outs) {
        }
    }
    """

    @Shared
    String morpher4groovy="""
    package libsubdir
    class UnitTestMorpher4 {
        void tweakValues(Map ins, Map outs) {
        }
    }
    """

    def setupSpec() {
        File child = File.createTempFile("cart-unit-test",".tmp")
        child.deleteOnExit()

        File tmpDir = child.parentFile

        emptyDir = new File(tmpDir, 'empty')
        emptyDir.mkdir()
        emptyDir.deleteOnExit()

        libDir = new File(tmpDir, 'notempty')
        libDir.mkdir()
        libDir.deleteOnExit()

        File m1 = new File(libDir, "UnitTestMorpher1.groovy")
        m1.write(morpher1groovy)
        m1.deleteOnExit()

        libSubDir = new File(libDir, "libsubdir")
        libSubDir.mkdir()
        libSubDir.deleteOnExit()

        File m2 = new File(libSubDir, "UnitTestMorpher2.groovy")
        m2.write(morpher2groovy)
        m2.deleteOnExit()

        File m3 = new File(libDir, "UnitTestMorpher3.groovy")
        m3.write(morpher3groovy)
        m3.deleteOnExit()

        File m4 = new File(libSubDir, "UnitTestMorpher4.groovy")
        m4.write(morpher4groovy)
        m4.deleteOnExit()
    }

    def "an empty library directory should result in no library classes found" () {
        given:
        LibraryLoader instance = new LibraryLoader(emptyDir.absolutePath, groovyClassLoader, libs, libDirs, mockLogger)
        when:
        instance.resync()
        then:
        libs.isEmpty()
    }

    def "each library directory should end up on the classpath" () {
        given:
        LibraryLoader instance = new LibraryLoader(libDir.absolutePath, groovyClassLoader, libs, libDirs, mockLogger)
        when:
        Set<String> beforePaths = instance.getClassPath()
        instance.addToClassPath()
        Set<String> afterPaths = instance.getClassPath()
        then:
        Set<String> diff = afterPaths - beforePaths
        diff == [ libDir.toURI().toString(), libSubDir.toURI().toString() ] as Set
    }

    def "a non-empty library directory should result in classes found" () {
        given:
        LibraryLoader instance = new LibraryLoader(libDir.absolutePath, groovyClassLoader, libs, libDirs, mockLogger)
        when:
        instance.resync()
        then:
        libs.values().collect { Class c -> c.simpleName } as Set == [ 'UnitTestMorpher1', 'UnitTestMorpher2',
                                                                      'UnitTestMorpher3', 'UnitTestMorpher4' ] as Set
    }

    def "a duplicate class name gets logged as an error"() {
        given:
        File m3dup = new File(libDir, "UnitTestMorpher3Duplicated.groovy")
        m3dup.write(morpher3groovy)
        m3dup.deleteOnExit()
        and:
        LibraryLoader instance = new LibraryLoader(libDir.absolutePath, groovyClassLoader, libs, libDirs, mockLogger)
        when:
        instance.resync()
        then:
        1 * mockLogger.foundDuplicateClass(_,_)
    }
}
