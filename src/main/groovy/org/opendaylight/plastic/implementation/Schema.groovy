
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

class Schema {

    final static ParserFactory parserFactory = new ParserFactory()

    final VersionedSchemaParsed parsedSchema

    Schema(VersionedSchema version, String contents) {
        InputStream payloadStream = new ByteArrayInputStream(contents.getBytes())
        VersionedSchemaStream payload = new VersionedSchemaStream(version, payloadStream)
        this.parsedSchema = payload.parse()
    }

    Schema(VersionedSchema inSchema, SchemaLoader loader) {
        def model = loader.locate(inSchema)
        def boundIn = new VersionedSchemaStream(inSchema, model)
        this.parsedSchema = parserFactory.createParsed(boundIn)
    }

    private Schema(VersionedSchemaParsed parsedSchema) {
        this.parsedSchema = parsedSchema
    }

    Bindings bindValues(Schema payload) {
        parsedSchema.bindValues(payload.parsedSchema)
    }

    Map fetchVariables() {
        parsedSchema.fetchVariables()
    }

    Map asDefaults() {
        parsedSchema.asDefaults()
    }

    void inject(Map values, Set danglingInputs, Set danglingOutputs) {
        parsedSchema.inject(values, danglingInputs, danglingOutputs)
    }

    void inject(Map<String,Schema> varBinds) {
        parsedSchema.inject(varBinds)
    }

    VersionedSchema getSchema() {
        parsedSchema.schema
    }

    Object getParsed() {
        parsedSchema.parsed
    }

    Schema cloneWith(Object payload) {
        new Schema(parsedSchema.cloneWith(payload))
    }

    Schema clone() {
        new Schema(parsedSchema.clone())
    }

    String toShortString(int len) {
        parsedSchema.toShortString(len)
    }

    String emit() {
        parsedSchema.emit()
    }
}
