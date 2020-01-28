
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

import java.lang.reflect.Array


abstract class VersionedSchemaParsed {

    static class WantedListButFoundMapException extends PlasticException {

        VersionedSchema schema
        String key

        WantedListButFoundMapException(VersionedSchemaStream schema, String key) {
            this(schema.schema, key)
        }

        WantedListButFoundMapException(VersionedSchema schema, String key) {
            super("PLASTIC-DEFLT-FND-MAP", "Default variable wanted a list but found a map instead: "
                    + schema + " with key: " + key)
            this.schema = schema
            this.key = key
        }
    }

    static class WantedListButFoundScalarException extends PlasticException {

        VersionedSchema schema
        String key
        String value

        WantedListButFoundScalarException(VersionedSchemaStream schema, String key, String value) {
            this(schema.schema, key, value)
        }

        WantedListButFoundScalarException(VersionedSchema schema, String key, String value) {
            super("PLASTIC-DEFLT-FND-SCALAR", "Default variable wanted a list but found a scalar instead: "
                    + schema + " with key: " + key + " and value: " + value)
            this.schema = schema
            this.key = key
            this.value = value
        }
    }

    static class MalformedException extends PlasticException {

        VersionedSchema schema
        Exception inner

        MalformedException(VersionedSchema schema, Exception inner) {
            super("PLASTIC-MALFORMED-DATA", "Could not parse data based on schema: "+schema, inner)
            this.schema = schema
            this.inner = inner
        }

        MalformedException(String schemaType, Exception inner) {
            super("PLASTIC-MALFORMED-DATA", "Could not parse data based on schema: "+schemaType, inner)
            this.schema = new VersionedSchema("Placeholder", "Placeholder", schemaType)
            this.inner = inner
        }
    }

    static class BadDefaultsException extends PlasticException {
        BadDefaultsException(VersionedSchemaParsed schema) {
            super("PLASTIC-BAD-DEFAULTS", "Could not convert the following into default values: "+schema.emit())
        }
    }

    final VersionedSchema schema

    VersionedSchemaParsed(VersionedSchemaStream schema) {
        Preconditions.checkNotNull(schema)

        this.schema = schema.schema
    }

    VersionedSchema getSchema() {
        schema
    }

    /*
     * Return the parsed version of the input stream
     */
    abstract def getParsed()

    /*
     * Deep-clone this instance
     */
    abstract VersionedSchemaParsed clone()

    /*
     * Clone this instance but its already-parsed payload should be used instead
     * of parsing from a stream.
     */
    abstract VersionedSchemaParsed cloneWith(Object alreadyParsed)

    /*
     * Walk the parsed tree, find variables, and substitute the values found from
     * the values map. It is an error to discover a variable and not have it in the
     * values map.
     */
    abstract void inject(Map values, Set danglingInputs, Set danglingOutputs)

    /*
     * Use the given path (key) to drill into this parsed value and replace the
     * chunk there with the replacement (value). The path must be valid or an
     * exception is thrown. The replacement must not be NULL and is not cloned
     * (the value is inserted directly).
     */
    abstract void inject(Map<String, Schema> varBinds)

    /*
     * Convert the parsed tree into a string representation
     */
    abstract String emit()

    /*
     * Walk the parsed tree to locate the variables, and look in the corresponding
     * place on the values source tree to find the value. Record discovered variables
     * and discovered values in the returned map.
     */
    abstract Bindings bindValues(VersionedSchemaParsed valuesSource)

    /*
     * Walk the parsed tree and record any found variables (name as key and possibly
     * null default value). Return that map.
     */
    abstract Map fetchVariables()

    /*
     * Return the parsed input stream as a map of default values (or throw)
     */
    abstract Map asDefaults()

    /*
     * Return a human-readable version of this object (truncated content)
     */
    abstract String toShortString(int maxLen)

    /*
     * Return an aggregator that can assemble a collection that is sytactically valid
     */
    abstract Aggregator createAggregator()


    protected Map asDefaults(Map values, VersionedSchema boundSchema) {

        Map results = [:]

        Variables vars = new Variables()
        values.each { k,v ->
            if (Variables.isGenericIndexed(k)) {
                if (v instanceof List) {
                    List indexedVars = vars.generateManyIndexed(k, v.size())
                    indexedVars.eachWithIndex { String indexedVar, int i ->
                        results.putAt(indexedVar, isCollection(v[i]) ? v[i] : ""+v[i])
                    }
                    Schemiterator.insertIteratorSpec(results, k, indexedVars)
                }
                else if (v instanceof Map)
                    throw new WantedListButFoundMapException(boundSchema, k);
                else
                    throw new WantedListButFoundScalarException(boundSchema, k, v);
            }
            else {
                results[k] = v
            }
        }
        results
    }

    protected boolean isCollection(obj) {
        return (obj instanceof Map || obj instanceof List || obj instanceof Array)
    }

}

