
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

class ParsedXml extends VersionedSchemaParsed {

    static final String KEY = XmlFormat.FORMATKEY
    static XmlFormat format = new XmlFormat()

    VersionedSchemaStream boundSchema
    XmlFinderBinder finderBinder
    XmlValuesInjector valuesInjector

    final Node parsed

    ParsedXml(VersionedSchemaStream schema) {
        this(schema, format.parse(schema.parsable))
    }

    protected ParsedXml(VersionedSchemaStream schema, Node alreadyParsed) {
        super(schema)

        this.boundSchema = schema
        this.finderBinder = new XmlFinderBinder()
        this.valuesInjector = new XmlValuesInjector()
        this.parsed = alreadyParsed
    }

    @Override
    VersionedSchemaParsed clone() {
        Node cloned = format.clone(parsed)
        return new ParsedXml(boundSchema, cloned)
    }

    @Override
    VersionedSchemaParsed cloneWith(Object alreadyParsed) {
        format.mustBeNode(alreadyParsed)
        return new ParsedXml(boundSchema, (Node)alreadyParsed)
    }

    Bindings bindValues(VersionedSchemaParsed valuesSource) {
        format.mustBeNode(valuesSource.parsed)
        finderBinder.process(parsed, (Node) valuesSource.parsed)
    }

    Map fetchVariables() {
        Map foundPaths = [:]
        Map foundVars = [:]
        finderBinder.buildVariablesToPaths(parsed, foundPaths, foundVars)
        foundVars
    }

    void inject(Map values, Set danglingInputs, Set danglingOutputs) {
        valuesInjector.inject(values, parsed)
    }

    void inject(Map<String, Schema> varBinds) {
        Set danglingInputs = []
        Set danglingOutputs = []
        inject(varBinds, danglingInputs, danglingOutputs)
    }

    String emit() {
        format.serialize(parsed)
    }

    Map asDefaults() {
        Map results = [:]
        parsed."${XmlFormat.ENTRY}".findAll { Node dflt ->
            NodeList key = dflt."${XmlFormat.KEY}"
            NodeList value = dflt."${XmlFormat.VALUE}"
            if (key != null && value != null)
                results[dflt."${XmlFormat.KEY}".text()] = dflt."${XmlFormat.VALUE}".text()
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
