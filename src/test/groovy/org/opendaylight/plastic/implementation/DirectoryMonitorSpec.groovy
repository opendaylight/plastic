
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
import com.google.common.io.Files
import spock.lang.Specification

class DirectoryMonitorSpec extends Specification {

    File createTmpDir() {
        File d = Files.createTempDir()
        d.deleteOnExit()
        d
    }

    File createTmpDir(File parent, String dirName) {
        File f = new File(parent, dirName)
        f.mkdir()
        f.deleteOnExit()
        f
    }

    File createTmpFile(File dir, String name) {
        File f = new File(dir, name)
        f.text = ""
        f.deleteOnExit()
        f
    }

    Map<String,File> createAbc() {
        File root = createTmpDir()
        File a = createTmpDir(root, "A")
        File aa = createTmpFile(a, "AA")

        File b = createTmpDir(root, "B")
        File bb = createTmpFile(b, "BB")

        File c = createTmpDir(root, "C")
        File cc = createTmpFile(c, "CC")

        Map<String,File> results = Maps.newHashMap()
        results.put("ABC", root)
        results.put("A", a)
        results.put("B", b)
        results.put("C", c)
        results.put("AA", aa)
        results.put("BB", bb)
        results.put("CC", cc)

        results
    }

    Map<String,File> files = createAbc()
    DirectoryMonitor instance = new DirectoryMonitor(files["ABC"].absolutePath)

    Closure mockListener = Mock()
    DirectoryMonitor.FileStatsDifference diff
    Closure listener = { d -> diff = d }

    def "no notification for an unchanged directory"() {
        given:
        instance.registerListener(mockListener)
        and:
        instance.takeSnapShot()
        when:
        instance.takeSnapShotAndNotify()
        then:
        0 * mockListener._
    }

    def "notified diffs should show a modified file"() {
        given:
        instance.registerListener(listener)
        and:
        instance.takeSnapShot()
        files["AA"].text = "foobar"
        when:
        instance.takeSnapShotAndNotify()
        then:
        diff.added().isEmpty()
        !diff.modified().isEmpty()
        diff.deleted().isEmpty()
    }

    def "notified diffs should show an added file"() {
        given:
        instance.registerListener(listener)
        and:
        instance.takeSnapShot()
        createTmpFile(files["A"], "AA2")
        when:
        instance.takeSnapShotAndNotify()
        then:
        diff.added().size() == 1
        diff.modified().isEmpty()
        diff.deleted().isEmpty()
    }

    def "notified diffs should show a deleted file"() {
        given:
        instance.registerListener(listener)
        and:
        instance.takeSnapShot()
        files["CC"].delete()
        when:
        instance.takeSnapShotAndNotify()
        then:
        diff.added().isEmpty()
        diff.modified().isEmpty()
        diff.deleted().size() == 1
    }
}
