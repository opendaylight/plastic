
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
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.nio.file.Paths

class SearchPathLogger {

    static final Logger logger = LoggerFactory.getLogger(SearchPathLogger)

    void readProblem(Exception e) {
        logger.warn("Problem trying to read application propertiesa", e)
    }

    void missingResource(String res) {
        logger.warn("Missing application properties: {}", res)
    }

    void missingProperty(String res, String prop) {
        logger.warn("Application properties file {} is missing property {}", res, prop);
    }

    void blankProperty(String res, String prop) {
        logger.warn("Application properties file {} has a blank value for property {}", res, prop);
    }

    void badRoot(String dirName) {
        logger.warn("Ignoring alternative implementation root (does not exist or is not a directory): {}", dirName)
    }

    void usingRoot(String root) {
        logger.debug("Using implementation root area {}", root)
    }

    void tryingCandidate(File file) {
        logger.debug("Looking at candidate directory: {}, exists: {}, is-directory: {}",
                file.getAbsolutePath(), file.exists(), file.isDirectory());
    }

    void nullRootPath() {
        logger.warn("Plastic root directory was null - ignoring")
    }

    void emptyRootPath() {
        logger.warn("Plastic root directory was blank - ignoring")
    }
}

class SearchPath {

    static final String resPath = 'application.properties'
    static final List rootProperties = [ 'plasticRoot','cartographerRoot' ]

    static SearchPathLogger logger = new SearchPathLogger()

    String[] candidateDirs = ['.']

    SearchPath() {
        this(resPath)
    }

    SearchPath(String propFileNameOrDirs) {
        for (String ch : [ ";", ""+File.separatorChar, ""+File.pathSeparatorChar ]) {
            if (propFileNameOrDirs.indexOf(ch) != -1) {
                initializeCandidates(propFileNameOrDirs)
                return
            }
        }

        initializeFromProperties(propFileNameOrDirs, SearchPath.class.getClassLoader())
    }

    SearchPath(String propFileName, ClassLoader loader) {
        initializeFromProperties(propFileName, loader)
    }

    SearchPath(Path rootPath) {
        if (rootPath == null) {
            logger.nullRootPath()
            return
        }

        File rootFile = rootPath.toFile()
        String pathString = rootFile.absolutePath

        if (pathString.isEmpty()) {
            logger.emptyRootPath()
            return
        }

        initializeCandidates(pathString)
    }

    private void initializeFromProperties(String propFileName, ClassLoader loader) {

        InputStream strm = loader.getResourceAsStream(propFileName)
        try {
            if (strm == null) {
                logger.missingResource(propFileName)
                return
            }

            Properties props = new Properties()
            props.load(strm)

            String roots = null

            rootProperties.each { String propertyName ->
                if (roots == null) {
                    roots = props.getProperty(propertyName)
                }
            }

            String allNames = rootProperties.join(" or ")

            if (roots == null) {
                logger.missingProperty(propFileName, allNames)
                return
            }

            roots = roots.trim()

            if (roots.isEmpty()) {
                logger.blankProperty(propFileName, allNames)
                return
            }

            initializeCandidates(roots)
        }
        catch(Exception e) {
            logger.readProblem(e)
        }
    }

    private void initializeCandidates(String roots) {
        def candidates = []

        for (String root : roots.split(";")) {
            String trimmed = root.trim()
            File rootFile = new File(trimmed)
            if (rootFile.exists() && rootFile.isDirectory()) {
                logger.usingRoot(trimmed)
                candidates.add(trimmed)
            }
            else {
                logger.badRoot(trimmed)
            }
        }

        candidates.add('.')
        candidateDirs = candidates
    }

    String find(String base) {
        String result = findOrElse(base, null)
        return result == null ? "./${base}" : result;
    }

    String findOrElse(String base, String other) {

        try {
            for (String candidate : candidateDirs) {
                Path p = Paths.get(candidate, base)
                File file = p.toFile()
                logger.tryingCandidate(file)
                if (file.exists())
                    return file.getAbsolutePath()
            }
        }
        catch(Exception e) {
            // nothing to do
        }

        return other
    }
}
