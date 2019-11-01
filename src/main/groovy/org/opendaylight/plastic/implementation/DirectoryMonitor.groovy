
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import groovy.transform.PackageScope

// TODO: look into using NIO's WatchService to replace this class

class DirectoryMonitor {

    static class FileStats {

        final String name
        final long modTime
        final long len

        FileStats(File file) {
            this.name = file.absolutePath
            this.modTime = file.lastModified()
            this.len = file.length()
        }

        boolean differsFrom(FileStats other) {
            return (other.modTime != modTime || other.len != len)
        }
    }

    static class FileStatsDifference {

        private String parent
        private Set<String> added
        private Set<String> modified
        private Set<String> deleted

        FileStatsDifference(String parent, Map<String,FileStats> before, Map<String,FileStats> after) {
            this.parent = parent

            Set<String> common = before.keySet().intersect(after.keySet())
            this.added = after.keySet() - common
            this.deleted = before.keySet() - common
            this.modified = common.grep { String name -> before[name].differsFrom(after[name]) }
        }

        boolean hasChanges() {
            !added.isEmpty() || !modified.isEmpty() || !deleted.isEmpty()
        }

        Set<String> changed() {
            added + modified + deleted
        }

        Set<String> added ()   { this.added }
        Set<String> modified() { this.modified }
        Set<String> deleted()  { this.deleted }

        String toString() {
            int nadd = added.size()
            int nmod = modified.size()
            int ndel = deleted.size()
            "${parent}: ${nadd} added, ${nmod} modified, ${ndel} deleted"
        }
    }

    private final File target
    private final List<Closure> listeners
    private final Map<String,FileStats> current
    private final String[] extensions

    DirectoryMonitor(String directory, String... extensions) {
        this.target = new File(directory)
        this.listeners = Lists.newArrayList()
        this.current = Maps.newHashMap()
        this.extensions = extensions
    }

    synchronized void registerListener(Closure listener) {
        listeners.add(listener)
    }

    // This should be called once, initially when the system starts

    @PackageScope
    void takeSnapShot() {
        Map<String,FileStats> newly = scan(target)
        updateCurrent(newly)
    }

    // This should be called periodically by a polling mechanism to recheck the monitored directory

    @PackageScope
    void takeSnapShotAndNotify() {
        Map<String,FileStats> newly = scan(target)

        FileStatsDifference diff = new FileStatsDifference(target.absolutePath,  current, newly)
        if (diff.hasChanges()) {
            listeners.each { Closure listener -> listener(diff) }
            updateCurrent(newly)
        }
    }

    synchronized private void updateCurrent(Map<String,FileStats> newly) {
        current.clear()
        current.putAll(newly)
    }

    @PackageScope
    Map<String,FileStats> scan(File candidate) {
        Map<String,FileStats> results = Maps.newHashMap()
        recursivelyScan(results, candidate)
        results
    }

    private void recursivelyScan(Map<String,FileStats> known, File candidate) {
        if (candidate.isDirectory()) {
            candidate.eachFile { File f -> recursivelyScan(known, f) }
            candidate.eachDir { File dir -> recursivelyScan(known, dir) }
        }
        else {
            String path = candidate.absolutePath
            String[] hits = extensions.grep { String ext -> path.endsWith(ext) }

            if (extensions.length == 0 || hits.length != 0)
                known[candidate.absolutePath] = new FileStats(candidate)
        }
    }
}
