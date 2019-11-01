
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import java.util.concurrent.ConcurrentHashMap

interface SchemaSource {

    Schema createSchema(VersionedSchema inSchema, String raw)
    Schema createSchema(VersionedSchema inSchema)
    Schema createSimilar(VersionedSchema versionedSchema, String raw)
}

class CachingSchemaSource implements SchemaSource, Pollee {

    private final SchemaLoader schemaLoader
    private final Map<VersionedSchema,Schema> cache = new ConcurrentHashMap<>()

    CachingSchemaSource(SchemaLoader schemaLoader) {
        this.schemaLoader = schemaLoader
    }

    // This one cannot be cached because the version is not a key to this content
    //
    @Override
    Schema createSchema(VersionedSchema inSchema, String raw) {
        new Schema(inSchema, raw)
    }

    @Override
    Schema createSchema(VersionedSchema inSchema) {
        synchronized (cache) {
            if (!cache.containsKey(inSchema)) {
                Schema entry = new Schema(inSchema, schemaLoader)
                cache.put(inSchema, entry)
            }

            cache.get(inSchema).clone()
        }
    }

    // This one cannot be cached because the version is not a key to this content
    //
    @Override
    Schema createSimilar(VersionedSchema reference, String content) {
        return new Schema(reference, content);
    }

    @Override
    void phase(int i) {
        cache.clear()
    }
}
