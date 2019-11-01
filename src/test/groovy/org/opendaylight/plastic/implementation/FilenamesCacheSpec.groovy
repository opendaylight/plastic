
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import org.slf4j.Logger
import spock.lang.Shared
import spock.lang.Specification


class FilenamesCacheSpec extends Specification {

    @Shared
    File someFile
    @Shared
    File emptyDir
    @Shared
    File rootDir
    @Shared
    File subDir

    def setupSpec() {
        someFile = File.createTempFile("cart-unit-test-2", ".tmp")
        someFile.deleteOnExit()

        File tmpDir = File.createTempDir()
        tmpDir.deleteOnExit()

        emptyDir = new File(tmpDir, 'my-empty')
        emptyDir.mkdir()
        emptyDir.deleteOnExit()

        rootDir = new File(tmpDir, 'my-root')
        rootDir.mkdir()
        rootDir.deleteOnExit()

        File m1 = new File(rootDir, "m1.txt")
        m1.write("")
        m1.deleteOnExit()

        subDir = new File(rootDir, "subdir")
        subDir.mkdir()
        subDir.deleteOnExit()

        File m2 = new File(subDir, "m2.txt")
        m2.write("")
        m2.deleteOnExit()

        File m3 = new File(rootDir, "m3.txt")
        m3.write("")
        m3.deleteOnExit()

        File m4 = new File(subDir, "m4.txt")
        m4.write("")
        m4.deleteOnExit()
    }

    def "a cache needs to start with a good root directory"() {
        when:
        new FilenamesCache(root)
        then:
        notThrown(Exception)
        where:
        root            | _
        "."             | _
        someFile.parent | _
    }

    def "a cache root that is bad is caught"() {
        given:
        Logger logger = Mock(Logger)
        FilenamesCache.logger = logger
        when:
        new FilenamesCache(root)
        then:
        1 * logger.warn(*_)
        where:
        root                  | _
        "xyz"                 | _
        someFile.absolutePath | _
    }

    def "a new cache is empty"() {
        given:
        FilenamesCache instance = new FilenamesCache(".")
        expect:
        instance.isEmpty()
    }

    def "a cache on an empty root is empty"() {
        given:
        FilenamesCache instance = new FilenamesCache(emptyDir)
        when:
        instance.scan()
        then:
        instance.isEmpty()
    }

    def "a cache on a non-empty root is not empty"() {
        given:
        FilenamesCache instance = new FilenamesCache(rootDir)
        when:
        instance.scan()
        then:
        !instance.isEmpty()
    }

    def "a cache scan finds the expected things"() {
        given:
        FilenamesCache instance = new FilenamesCache(rootDir)
        when:
        instance.scan()
        then:
        instance.fileCount() == 4
        instance.dirCount() == 1
    }

    def "a cache can be rescanned giving updated results"() {
        given:
        FilenamesCache instance = new FilenamesCache(rootDir)
        instance.scan()
        int prevCount = instance.fileCount()
        when:
        File m0 = new File(rootDir, "m0.txt")
        m0.text = " "
        and:
        instance.scan()
        m0.delete()
        then:
        instance.fileCount() == prevCount+1
    }

    def "a cache allows lookups by file base name"() {
        given:
        FilenamesCache instance = new FilenamesCache(rootDir)
        when:
        instance.scan()
        then:
        instance.getFile("m4.txt")
        instance.getFile("foobar") == null
    }

    def "a cache allows lookups by dir base name"() {
        given:
        FilenamesCache instance = new FilenamesCache(rootDir)
        when:
        instance.scan()
        then:
        instance.getDirectory("subdir")
    }
}
