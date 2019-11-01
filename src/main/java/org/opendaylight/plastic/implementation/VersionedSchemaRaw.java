
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;


public class VersionedSchemaRaw {

    private final VersionedSchema schema;
    private final String raw;

    public VersionedSchemaRaw(VersionedSchema schema, String raw) {
        this.schema = schema;
        this.raw = raw;
    }

    public VersionedSchemaRaw(String schemaName, String schemaVersion, String schemaType, String raw) {
        this.schema = new VersionedSchema(schemaName, schemaVersion, schemaType);
        this.raw = raw;
    }

    public boolean matches(VersionedSchemaRaw other) {
        return schema.getType().equals(other.schema.getType());
    }

    public VersionedSchemaRaw cloneWith(String raw) {
        return new VersionedSchemaRaw(schema, raw);
    }

    public VersionedSchema getSchema() {
        return schema;
    }

    public String getRaw() {
        return raw;
    }
}
