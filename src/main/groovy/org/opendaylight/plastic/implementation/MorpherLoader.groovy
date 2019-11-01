
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.base.Preconditions
import com.google.common.collect.Maps
import groovy.transform.PackageScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MorpherFactoryLogger {

    static final Logger logger = LoggerFactory.getLogger(MorpherLoader)

    void foundDuplicateClass(String className, String fileName) {
        logger.error(String.format("Class name %s already found in file %s", className, fileName))
    }
}

class MorpherLoader implements Pollee {

    final MorpherFactoryLogger logger
    final GroovyClassLoader gcl
    final FilenamesCache fileCache
    final Map<String,String> seen // <Class-Name,File-Name> (don't reference Class directly)
    final GroovyClassParser gparser  // private instance because morphers are unique population
    final AppContext appProps

    MorpherLoader(AppContext props, FilenamesCache cache, GroovyClassLoader gcl) {
        this(props, cache, gcl, Maps.newConcurrentMap(), new MorpherFactoryLogger())
    }

    @PackageScope
    MorpherLoader(AppContext props, FilenamesCache cache, GroovyClassLoader gcl, Map<String,String> seen, MorpherFactoryLogger logger) {
        this.appProps = props
        this.logger = logger
        this.gcl = gcl
        this.fileCache = cache
        this.seen = seen
        this.gparser = new GroovyClassParser(gcl)
    }

    Morpher locateImplicitly (VersionedSchema schema) {
        Preconditions.checkNotNull(schema)
        String morpherFileName = computeName(schema)
        instantiate(schema, morpherFileName)
    }

    Morpher locateExplicitly (VersionedSchema schema, String morpherName) {
        Preconditions.checkNotNull(morpherName)
        String morpherFileName = computeName(schema, morpherName)
        instantiate(schema, morpherFileName)
    }

    @PackageScope
    Morpher instantiate(VersionedSchema schema, String morpherFileName) {
        Object morpher = null

        def morpherFile = new File(morpherFileName)
        if (morpherFile.exists()) {
            Class clazz = gparser.parseClass(morpherFile)

            String newKey = clazz.getName()
            String newValue = morpherFile.absolutePath

            boolean hasKey = seen.containsKey(newKey)
            boolean hasValue = hasKey && seen[newKey].equals(newValue)

            if (hasKey && hasValue)
                morpher = clazz.newInstance()
            else if (!hasKey && !hasValue) {
                morpher = clazz.newInstance()
                seen.put(newKey, newValue)
            }
            else
                logger.foundDuplicateClass(newKey, seen.get(newKey));
        }

        if (morpher == null)
            morpher = new NopMorpher(schema, new Object(), morpherFileName)
        else
            morpher = new Morpher(schema, morpher, morpherFileName)

        morpher.setAppContext(appProps)
        morpher
    }

    protected String computeName(VersionedSchema target, String name) {
        String base = "${name}-${target.version}.groovy"
        String result = fileCache.getFile(base)
        result == null ? base : result
    }

    protected String computeName(VersionedSchema target) {
        String base = "${target.name}-${target.version}.groovy"
        String result = fileCache.getFile(base)
        result == null ? base : result
    }

    @Override
    void phase(int i) {
        gparser.phase(i)
    }
}
