
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
import com.google.common.collect.Sets
import groovy.transform.PackageScope
import org.codehaus.groovy.control.CompilationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LibraryLoaderLogger {

    static Logger logger = LoggerFactory.getLogger(LibraryLoader)

    void resyncing() {
        logger.debug("Resyncing classpath and classes in plastic directory")
    }

    void noNewLibDirs() {
        logger.debug("Found no new plastic directories to add to classpath")
    }

    void addingLibDir(Set<String> newlyAdded) {
        logger.debug(String.format("Adding newly found plastic directories to classpath: %s", newlyAdded.toString()))
    }

    void possiblyLoading(String className) {
        logger.debug(String.format("Loading (possibly) new library class from file %s", className))
    }

    void failedLoadingLibraryClass(String className, Exception e) {
        logger.error(String.format("Failure trying to load library class from file %s", className), e)
    }

    void foundDuplicateClass(Class clazz, String fileName) {
        logger.error(String.format("Found class %s duplicated in file %s. Please fix and restart controller",
                clazz.getSimpleName(), fileName))
    }
}

class LibraryLoader implements Pollee {

    final LibraryLoaderLogger logger
    final GroovyClassLoader gcl
    final String rootLibDir
    final Set<String> libDirs
    final Map<String,Class> libraries // keep classes from being GC'ed

    LibraryLoader(String root, GroovyClassLoader gcl) {
        this(root, gcl, Maps.newConcurrentMap(), Sets.newConcurrentHashSet(), new LibraryLoaderLogger())
    }

    LibraryLoader(String root, GroovyClassLoader gcl, Map<String,Class> libs, Set<String> libDirs,
                  LibraryLoaderLogger logger) {
        this.logger = logger
        this.rootLibDir = root
        this.libraries = libs
        this.libDirs = libDirs
        this.gcl = gcl
    }

    @Override
    void phase(int i) {
        resync()
    }

    void clear() {
        libDirs.clear()
        libraries.clear()
    }

    private void resync() {

        logger.resyncing()

        // adding to classpath is required so that imports among the files
        // will resolve. without this a valid import might fail because its
        // class hasn't been loaded yet.

        addToClassPath()

        // we explicitly recursively load all of the groovy files found in the
        // plastic directory. newly found files will be put into the groovy class
        // cache. changed files will not be noticed without clearing that
        // cache (not part of this class's responsibility)

        loadFiles()
    }

    @PackageScope
    void addToClassPath() {
        Set<String> newState = findDirs()
        Set<String> oldState = libDirs

        Set<String> newlyAdded = newState - oldState
        if (newlyAdded.isEmpty())
            logger.noNewLibDirs()
        else {
            logger.addingLibDir(newlyAdded)

            OurClassPath cp = new OurClassPath(gcl)
            cp.add(newlyAdded).commit()

            libDirs.clear()
            libDirs.addAll(newState)
        }
    }

    private Set<String> findDirs() {
        Set<String> results = Sets.newHashSet()

        File root = new File(rootLibDir)
        results.add(root.absolutePath)

        root.eachDirRecurse() { File dir -> results.add(dir.absolutePath) }
        return results
    }

    @PackageScope
    List<String> getClassPath() {
        getClassPaths(gcl)
    }

    private List<String> getClassPaths(ClassLoader loader) {
        List<String> results = Lists.newArrayList()
        if (loader != null) {
            List<String> urls = loader.URLs.collect { URL url -> url.toString() }
            results.addAll(urls)

            if (loader.getParent() != null)
                results.addAll(getClassPaths(loader.getParent()))
        }
        results
    }

    @PackageScope
    void loadFiles() {
        File root = new File(rootLibDir)
        root.eachFile {
            file -> loadOnlyGroovyFiles(file) }
        root.eachDirRecurse() { dir ->
            dir.eachFile { file ->
                loadOnlyGroovyFiles(file) }
        }
    }

    private void loadOnlyGroovyFiles(File file) {
        if (file.getName().toLowerCase().endsWith(".groovy"))
            loadGroovyFile(file)
    }

    // Note: even though the class looks the same (and is loaded from a different file),
    // class1.equals(class2) is returning false, meaning that they look different to
    // the runtime. Hence the equivalency tests below are using class.getName() to find
    // collisions

    private void loadGroovyFile(File groovyFile) {
        String canonical = groovyFile.canonicalPath
        logger.possiblyLoading(canonical)
        try {
            Class libClass = gcl.parseClass(groovyFile)
            boolean hasValue = libraries.find { k,v -> libClass.getName().equals(v.getName()) }
            boolean hasKey = libraries.containsKey(canonical)

            if (hasKey && hasValue) {
                ignore()
            }
            else if (!hasKey && !hasValue) {
                libraries.put(canonical, libClass)
            }
            else if (hasValue) {
                reportDuplicates(libClass)
            }
        }
        catch (IOException | CompilationFailedException e) {
            logger.failedLoadingLibraryClass(canonical, e)
        }
    }

    private void ignore() {
        // this must be a repeated polling of existing stuff, so don't load it
        // we rely on clear() being called by the cache clearer to reset our state
    }

    private void reportDuplicates(Class libClass) {
        libraries.each { String k, Class v ->
            if (v.getName().equals(libClass.getName())) {
                logger.foundDuplicateClass(libClass, k)
            }
        }
    }
}
