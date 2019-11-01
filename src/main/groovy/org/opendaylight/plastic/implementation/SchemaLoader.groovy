
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

class SchemaLoader {

    static class LocationNotFoundException extends PlasticException {

        LocationNotFoundException(String msg) {
            super("PLASTIC-LOCATION-NOT-FOUND", "The following schema file name was not found: " + msg)
        }
    }

    FilenamesCache cache

    SchemaLoader(FilenamesCache cache) {
        this.cache = cache
    }

    InputStream locate(VersionedSchema target) {
        Preconditions.checkNotNull(target)

        InputStream payload = null

        if (target && target.name && target.version && target.type) {
            String schemaFileName = computeName(target)
            def schemaFile = new File(schemaFileName)

            if (schemaFile.exists()) {
                payload = new ByteArrayInputStream(schemaFile.getBytes())
            } else {
                throw new LocationNotFoundException(schemaFileName)
            }

            Preconditions.checkNotNull(payload, schemaFileName)
        }

        Preconditions.checkNotNull(payload)
        return payload
    }

    protected String computeName(VersionedSchema target) {
        String base = "${target.name}-${target.version}.${target.type}"
        String result = cache.getFile(base)
        result == null ? base : result
    }
}
