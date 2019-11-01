
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

class ClassCacheClearer implements Pollee {

    static final Logger logger = LoggerFactory.getLogger(ClassCacheClearer)

    def clearNow = { GroovyClassLoader gcl, DirectoryMonitor.FileStatsDifference diffs ->
        logger.debug("Clearing entire Groovy class cache due to changed code for ${diffs.toString()}")
        gcl.clearCache()
    }

    private final GroovyClassLoader targetClassLoader
    private final DirectoryMonitor monitor

    ClassCacheClearer(GroovyClassLoader gcl, String directory) {
        this(gcl, new DirectoryMonitor(directory, ".groovy"), {})
    }

    ClassCacheClearer(GroovyClassLoader gcl, String directory, Runnable callback) {
        this(gcl, new DirectoryMonitor(directory, ".groovy"), callback)
    }

    ClassCacheClearer(GroovyClassLoader gcl, DirectoryMonitor monitor, Runnable callback) {
        this.targetClassLoader = gcl
        this.monitor = monitor

        monitor.registerListener { DirectoryMonitor.FileStatsDifference diff ->
            clearNow(targetClassLoader, diff)
            callback.run()
        }
    }

    @Override
    void phase(int i) {
        if (i == 0)
            monitor.takeSnapShot()
        else
            monitor.takeSnapShotAndNotify()
    }
}
