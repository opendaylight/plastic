
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.transform.PackageScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap


class FilenamesCache {

    static Logger logger = LoggerFactory.getLogger(FilenamesCache)

    private Map<String, String> files = new ConcurrentHashMap<>()
    private Map<String,String> dirs = new ConcurrentHashMap<>()
    private File theRoot

    FilenamesCache(File root) {
        validate(root)
        this.theRoot = root
    }

    FilenamesCache(String root) {
        this(new File(root))
    }

    @PackageScope
    FilenamesCache() {
        this.theRoot = null
    }

    String root() {
        this.theRoot == null ? "" : this.theRoot.absolutePath
    }

    private boolean isValid(File f) {
        f != null && f.exists() && f.isDirectory()
    }

    private void validate(File f) {
        if (f == null) {
            logger.warn("PLASTIC-FN-CACHE: filenames cache cannot have a null root")
        }
        else {
            if (!f.exists())
                logger.warn("PLASTIC-FN-CACHE: filenames cache cannot have a non-existent root: ${f.absolutePath}")
            else if (!f.directory)
                logger.warn("PLASTIC-FN-CACHE: filenames cache root must be a directory, not a file: ${f.absolutePath}")
        }
    }

    boolean isEmpty() {
        files.isEmpty() && dirs.isEmpty()
    }

    int fileCount() {
        files.size()
    }

    int dirCount() {
        dirs.size()
    }

    void scan() {
        synchronized (files) {
            files.clear()
            dirs.clear()
            if (isValid(this.theRoot))
                recursivelyScan(this.theRoot, true)
        }
    }

    private void recursivelyScan(File candidate, boolean isRoot) {
        if (candidate.isDirectory()) {
            candidate.eachFile { File f -> recursivelyScan(f, false) }
            candidate.eachDir { File dir -> recursivelyScan(dir, false) }
            if (!isRoot)
                dirs[candidate.name] = candidate.absolutePath
        }
        else {
            files[candidate.name] = candidate.absolutePath
        }
    }

    String getFile(String basename) {
        files[basename]
    }

    String getDirectory(String basename) {
        dirs[basename]
    }
}
