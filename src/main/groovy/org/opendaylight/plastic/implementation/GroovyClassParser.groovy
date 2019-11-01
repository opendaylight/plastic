
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

import java.util.concurrent.ConcurrentHashMap

class FileStats {

    private static String asKey(File f) {
        f.absolutePath.toLowerCase()
    }

    final String name
    final String key
    final long modTime
    final long len
    final boolean exists

    FileStats(File file) {
        this.name = file.absolutePath
        this.key = asKey(file)
        this.exists = file.exists()

        if (exists) {
            this.modTime = file.lastModified()
            this.len = file.length()
        }
        else {
            this.modTime = -1
            this.len = -1
        }
    }

    FileStats create() {
        return new FileStats(new File(name))
    }

    @Override
    int hashCode() {
        name.hashCode()
    }

    @Override
    boolean equals(Object other) {
        if (other instanceof FileStats) {
            FileStats fother = (FileStats) other
            return equalTo(fother)
        }
        false
    }

    boolean equalTo(FileStats other) {
        other != null && modTime == other.modTime && len == other.len && key.equals(other.key)
    }
}

/**
 * The GroovyClassLoader has its own cache, but it can still do relatively expensive things on each
 * call, like constructing GroovySourceCode objects, so we are laying more caching on top of that.
 * We use the modification time plus size to help figure out if an entry is stale (modification time
 * alone seemed unreliable).
 */
class GroovyClassParser implements Pollee {

    static final Logger logger = LoggerFactory.getLogger(GroovyClassParser.class)
    static final String SIMPLE = GroovyClassParser.class.getSimpleName()

    final GroovyClassLoader gcl
    final Map<String,FileStats> seenStats
    final Map<String,Class> seenClasses

    GroovyClassParser(GroovyClassLoader gcl) {
        this.gcl = gcl
        this.seenClasses = new ConcurrentHashMap<>()
        this.seenStats = new ConcurrentHashMap<>()
    }

    Class parseClass(String fileName) {
        parseClass(new File(fileName))
    }

    synchronized Class parseClass(File groovyFile) {
        String key = FileStats.asKey(groovyFile)
        FileStats previous = seenStats.get(key)
        if (previous != null) {
            return seenClasses.get(key)
        }
        else {
            Class result = gcl.parseClass(groovyFile) // expensive
            FileStats current = new FileStats(groovyFile)

            seenStats.put(key, current)
            seenClasses.put(key, result)
            return result
        }
    }

    @Override
    void phase(int i) {
        resync()
    }

    synchronized void resync() {
        List<String> deathRow = []

        for (String key: seenStats.keySet()) {
            FileStats oldStat = seenStats.get(key)
            FileStats newStat = oldStat.create()
            if (!newStat.equalTo(oldStat))
                deathRow.add(key)
        }

        for (String removeMe : deathRow) {
            logger.info("{} clearing cache due to file system change for {}", SIMPLE, removeMe)
            seenStats.remove(removeMe)
            seenClasses.remove(removeMe)
        }
    }
}
