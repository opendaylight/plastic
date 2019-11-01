
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.base.Preconditions.checkArgument


class ParsedChunkyJson extends VersionedSchemaParsed {

    static class UnrecognizedPreparsedException extends PlasticException {

        VersionedSchemaStream schema
        Object alreadyParsed

        UnrecognizedPreparsedException(VersionedSchemaStream schema, Object alreadyParsed) {
            super("PLASTIC-DEFLT-UNREC-PREP", "Unrecognized value should be preparsed JSON but is not: "
                    + schema.schema + " with value: " + alreadyParsed)
            this.schema = schema
            this.alreadyParsed = alreadyParsed
        }
    }

    static class InjectionFailedForVariable extends PlasticException {

        String target
        Object parsed

        InjectionFailedForVariable(String target, Object parsed) {
            super("PLASTIC-INJ-FAIL", "Could not find injection target variable named \'${target}\' in payload: "+parsed)
            this.target = target
            this.parsed = parsed
        }
    }

    static final String KEY = ChunkyJsonFormat.FORMATKEY
    static final ChunkyJsonFormat format = new ChunkyJsonFormat()

    VersionedSchemaStream boundSchema
    JsonFinderBinder finderBinder
    JsonValuesInjector valuesInjector

    final def parsed

    ParsedChunkyJson(VersionedSchemaStream schema) {
        this(schema, format.parse(schema.parsable))
    }

    protected ParsedChunkyJson(VersionedSchemaStream schema, Object alreadyParsed) {
        super(schema)
        checkNotNull(alreadyParsed)
        checkArgument(schema.schema.type == ChunkyJsonFormat.FORMATKEY)

        this.boundSchema = schema
        this.finderBinder = new JsonFinderBinder()
        this.valuesInjector = new JsonValuesInjector()

        if (!isCollection(alreadyParsed))
            throw new UnrecognizedPreparsedException(schema, alreadyParsed)
        this.parsed = alreadyParsed
    }

    @Override
    VersionedSchemaParsed clone() {

        // Unfortunately the top level object (either a map or an array) does
        // shallow cloning, so we do a custom deep clone. We do not use a
        // serialization based implementation because some things like LazyMap
        // throw a NotSerializable exception.

        return new ParsedChunkyJson(boundSchema, format.clone(parsed))
    }

    @Override
    VersionedSchemaParsed cloneWith(Object alreadyParsed) {
        return new ParsedChunkyJson(boundSchema, alreadyParsed)
    }

    Bindings bindValues(VersionedSchemaParsed valuesSource) {
        finderBinder.process(parsed, valuesSource.parsed)
    }

    Map fetchVariables() {
        Map foundPaths = [:]
        Map foundVars = [:]
        finderBinder.buildVariablesToPaths(parsed, foundPaths, foundVars)
        foundVars
    }

    void inject(Map values, Set danglingInputs, Set danglingOutputs) {
        valuesInjector.inject(values, parsed, danglingInputs, danglingOutputs)
    }

    void inject(Map<String, Schema> varBinds) {
        Set danglingInputs = []
        Set danglingOutputs = []
        Map map = varBinds.collectEntries { k,v -> [k, v.parsed] }
        inject(map, danglingInputs, danglingOutputs)
        varBinds.each { k, v ->
            if (danglingInputs.contains(k))
                throw new InjectionFailedForVariable(k, parsed)
        }
    }

    String emit() {
        format.serialize(parsed)
    }

    Map asDefaults() {
        if (!(parsed instanceof List))
            throw new BadDefaultsException(this)

        Map results = [:]

        List parsedList = (List) parsed
        if (!parsedList.isEmpty()) {
            Object first = parsedList[0]

            if (!(first instanceof Map))
                throw new BadDefaultsException(this)

            results = asDefaults((Map)first, boundSchema.schema)
        }

        results
    }

    @Override
    String toShortString(int maxLen) {
        String emitted = emit()
        int len = (emitted.length() > maxLen) ? maxLen : emitted.length()
        emitted.substring(0, len)
    }

    @Override
    Aggregator createAggregator() {
        format.createAggregator()
    }
}
