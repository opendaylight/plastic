
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

class IrritableParsed extends VersionedSchemaParsed {

    static class UnknownParsingFormat extends PlasticException {
        UnknownParsingFormat(String type) {
            super("PLASTIC-UNKNOWN-PAR-FORMAT", "Could not find a parser for this type: "+type)
        }
    }

    IrritableParsed(VersionedSchemaStream schema) {
        super(schema)
    }

    VersionedSchema getSchema() {
        super.schema
    }

    def getParsed() {
        throw new UnknownParsingFormat(this.schema.type)
    }

    protected def parse(InputStream inStrm) {
        // do nothing since this is invoked as part of construction
    }

    @Override
    VersionedSchemaParsed clone() {
        throw new UnknownParsingFormat(this.schema.type)
    }

    @Override
    VersionedSchemaParsed cloneWith(Object alreadyParsed) {
        throw new UnknownParsingFormat(this.schema.type)
    }

    void inject(Map values, Set danglingInputs, Set danglingOutputs) {
        throw new UnknownParsingFormat(this.schema.type)
    }

    void inject(Map<String, Schema> varBinds) {
        throw new UnknownParsingFormat(this.schema.type)
    }

    String emit() {
        throw new UnknownParsingFormat(this.schema.type)
    }

    Bindings bindValues(VersionedSchemaParsed valuesSource) {
        throw new UnknownParsingFormat(this.schema.type)
    }

    Map fetchVariables() {
        throw new UnknownParsingFormat(this.schema.type)
    }

    Map asDefaults() {
        throw new UnknownParsingFormat(this.schema.type)
    }

    @Override
    String toShortString(int maxLen) {
        ""
    }

    @Override
    Aggregator createAggregator() {
        throw new UnknownParsingFormat(this.schema.type)
    }
}
