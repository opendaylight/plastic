
/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package integration

import spock.lang.Specification

class PlasticRunnerIT extends Specification {

    final String srcDir = "src/test/resources/cartrunnerroot"
    final String dstDir = "target"
    final String runDir = "${dstDir}/plasticrunner"
    IFileNameFinder fileNameFinder = new FileNameByRegexFinder()
    final String pkgName = fileNameFinder.getFileNames(dstDir,"plastic-runner").findAll {it.endsWith(".tar.gz")}[0]
    final String expected = "src/test/resources/cartrunnerroot/cartroot-out-1.0-expected.json"

    final def cmds = [
            [ dstDir, "tar -xvf ${pkgName}" ],
            [ srcDir, "cp -R . ../../../../${runDir}" ],
            [ runDir, "./plastic_runner cartrunnerroot.properties" ]
    ]

    def executeCmd(List<String> cmds) {
        def (dir, cmd) = cmds
        println ("In dir (${dir}) executing ${cmd} ...")

        File dirFile = new File(dir)
        def builder = new ProcessBuilder(cmd.split())
        builder.directory(dirFile)

        def process = builder.start()
        StringBuilder output = new StringBuilder()
        StringBuilder error = new StringBuilder()
        process.waitForProcessOutput(output, error)

        if (process.exitValue()) {
            System.err.println "[ERROR] ${error}\n${output}"
            throw new Exception(error.toString())
        }

        output
    }

    String crush(String raw) {
        raw.replaceAll('\n','').replaceAll("\\s+", " ")
    }

    def ensureDirectory(String... paths) {
        paths.each { p ->
            File f = new File(p)
            if (!f.exists())
                throw new Exception("File ${p} does not exist!")
            if (!f.isDirectory())
                throw new Exception("File ${p} is not a directory!")
        }
    }

    def "can use runner to translate successfully"() {
        given:
        ensureDirectory(srcDir, dstDir)
        String expectedContents = new File(expected).text
        String found = ""
        when:
        cmds.each { cmd ->
            found = executeCmd(cmd)
        }
        then:
        crush(found).endsWith(crush(expectedContents))
    }
}
