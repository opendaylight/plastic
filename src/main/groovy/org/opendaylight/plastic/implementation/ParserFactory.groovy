
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

class ParserFactory {

    static class UnsupportedSchemaType extends PlasticException {

        String[] expected
        String found

        UnsupportedSchemaType(String[] expected, String found) {
            super("PLASTIC-UNSUPPORTED-SCHEMA", "Unsupported mapping schema type found: "+found)
            this.expected = expected
            this.found = found
        }
    }

    def supportedTypes = [:]

    ParserFactory() {
        // Could not use a variable for a key in the literal inline initialization above
        supportedTypes[ParsedJson.KEY] = ParsedJson
        supportedTypes[ParsedXml.KEY] = ParsedXml
        supportedTypes[ParsedChunkyJson.KEY] = ParsedChunkyJson
    }

    VersionedSchemaParsed createParsed(VersionedSchemaStream boundSchema) {

        String type = boundSchema.schema.type.toLowerCase()

        if (supportedTypes.containsKey(type)) {
            def clazz = supportedTypes[type]
            return clazz.newInstance([boundSchema] as Object[])
        }
        else
            return new IrritableParsed(boundSchema)
    }

    private boolean supports(String candidateType) {
        return supportedTypes.containsKey(candidateType.toLowerCase())
    }

    private String[] allSupportedTypes() {
        supportedTypes.keySet() as String[]
    }

    void checkValid(String type) {
        if (!supports(type))
            throw new UnsupportedSchemaType(allSupportedTypes(), type)
    }
}
