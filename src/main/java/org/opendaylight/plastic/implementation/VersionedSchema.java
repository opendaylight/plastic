
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

import com.google.common.base.Preconditions;

public class VersionedSchema {

    private final String name;
    private final String version;
    private final String type;
    private final String tostring;

    public VersionedSchema(VersionedSchema schema)
    {
        this(schema.getName(), schema.getVersion(), schema.getType());
    }

    public VersionedSchema(String name, String version, String type)
    {
        Preconditions.checkArgument(version != null && !version.trim().isEmpty());
        Preconditions.checkArgument(type != null && !type.trim().isEmpty());
        Preconditions.checkArgument(name != null && !name.trim().isEmpty());

        this.name = name.trim();
        this.version = version.trim();
        this.type = type.trim().toLowerCase();
        this.tostring = "["+name+"/"+version+"/"+type+"]";
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getType() {
        return this.type;
    }

    public VersionedSchema rename(String newName) {
        return new VersionedSchema(newName, version, type);
    }

    public VersionedSchema clone() {
        return new VersionedSchema(name, version, type);
    }

    @Override
    public String toString() {
        return tostring;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VersionedSchema))
            return false;

        VersionedSchema other = (VersionedSchema) obj;
        return name.equals(other.name) && version.equals(other.version) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return tostring.hashCode();
    }
}
