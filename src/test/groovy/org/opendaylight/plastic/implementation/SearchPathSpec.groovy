
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.io.Files
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

class SearchPathSpec extends Specification {

    File createTmpDir() {
        File d = Files.createTempDir()
        d.deleteOnExit()
        d
    }

    File createTmpFile(File dir, String name) {
        File f = new File(dir, name)
        f.createNewFile()
        f.deleteOnExit()
        f
    }

    def "should be able to read location for support dirs from application properties" () {
        given:
        SearchPath searcher = new SearchPath("testapplication1.properties")
        when:
        String here = searcher.find(dir)
        then:
        here != null && !here.isEmpty()
        where:
        dir        || _
        "morphers" || _
        "schemas"  || _
    }

    def "should be able to read locations when multiple paths are present" () {
        given:
        SearchPath searcher = new SearchPath("testapplication1and2.properties")
        when:
        String here = searcher.find(dir)
        then:
        here != null && !here.isEmpty()
        where:
        dir       || _
        "other1"  || _
        "other2"  || _
    }

    def "should be able to read location directly from supplied path" () {
        given:
        File d = createTmpDir()
        File f = createTmpFile(d, "unit-test.properties")
        and:
        Path dpath = Paths.get(d.absolutePath)
        SearchPath searcher = new SearchPath(dpath)
        when:
        String here = searcher.find("unit-test.properties")
        then:
        here.equals(f.absolutePath)
    }

    def "should be able to read location directly from supplied dir name" () {
        given:
        File d = createTmpDir()
        File f = createTmpFile(d, "unit-test-2.properties")
        and:
        SearchPath searcher = new SearchPath(d.absolutePath)
        when:
        String here = searcher.find("unit-test-2.properties")
        then:
        here.equals(f.absolutePath)
    }
}
