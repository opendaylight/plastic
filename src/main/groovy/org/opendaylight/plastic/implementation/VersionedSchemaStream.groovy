
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.base.Preconditions;


class VersionedSchemaStream {

    static InputStream asStream (String s) {
        new ByteArrayInputStream(s.getBytes())
    }

    static final ParserFactory factory = new ParserFactory()

    final VersionedSchema schema
    final InputStream parsable

    VersionedSchemaStream(VersionedSchema schema) {
        this(schema, asStream(""))
    }

    VersionedSchemaStream(VersionedSchema schema, InputStream parsable) {
        Preconditions.checkNotNull(parsable)
        Preconditions.checkNotNull(schema)

        this.schema = schema
        this.parsable = parsable
    }

    VersionedSchema getSchema(){
        schema
    }

    InputStream getParsable() {
        parsable
    }

    String toString() {
        schema.toString() + "  payload:..."
    }

    VersionedSchemaParsed parse() {
        factory.createParsed(this)
    }
}
