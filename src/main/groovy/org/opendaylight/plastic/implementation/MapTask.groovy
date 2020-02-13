
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class MapTask {

    static class MissingInputsException extends PlasticException {

        VersionedSchema input
        VersionedSchema output
        Set missings

        MissingInputsException(VersionedSchema inS, VersionedSchema outS, Set missings) {
            super("PLASTIC-MISSING-IN", "For (in-> ${inS}) (out-> ${outS}), the following input variables were not found on the incoming payload: " + missings)

            this.input = inS
            this.output = outS
            this.missings = missings
        }
    }

    static class DangingInputsException extends PlasticException {

        VersionedSchema input
        VersionedSchema output
        Set danglings

        DangingInputsException(VersionedSchema inS, VersionedSchema outS, Set danglings) {
            super("PLASTIC-DANGL-IN", "For (in-> ${inS}) (out-> ${outS}), the following input variables were not matched to output variables: " + danglings)

            this.input = inS
            this.output = outS
            this.danglings = danglings
        }
    }

    static String noteOnIndexing(Set unmatchedVars) {
        String result = ""
        for (String unmatchedVar : unmatchedVars) {
            if (Variables.isIndexed(unmatchedVar)) {
                result = "Try using default values for indexed variables to avoid this issue."
                break
            }
        }
        result
    }

    static class DanglingOutputsException extends PlasticException {

        VersionedSchema input
        VersionedSchema output
        Set danglings

        DanglingOutputsException(VersionedSchema inS, VersionedSchema outS, Set danglings) {
            super("PLASTIC-DANGL-OUT", "For (in-> ${inS}) (out-> ${outS}), the following output variables were not matched " +
                    "to input variables: ${danglings} " +
                    MapTask.noteOnIndexing(danglings)) // Groovy bug requires qualified reference

            this.input = inS
            this.output = outS
            this.danglings = danglings
        }
    }

    static class DanglingOutputVariables extends PlasticException {

        VersionedSchema input
        VersionedSchema output
        Set unmatched

        DanglingOutputVariables(VersionedSchema inS, VersionedSchema outS, Set unmatchedVars) {
            super("PLASTIC-DANGLING-OUT-VARS","For (in:${inS}) (out:${outS}), the following output variables " +
                    "had no matching inputs: ${unmatchedVars} " +
                    MapTask.noteOnIndexing(unmatchedVars)) // Groovy bug requires qualified reference

            this.input = inS
            this.output = outS
            this.unmatched = unmatchedVars
        }
    }

    static Logger log = LoggerFactory.getLogger(MapTask.class)

    TranslationPlan<Schema,Morpher> plan
    Schema input
    Schema output

    MapTask(TranslationPlan<Schema,Morpher> plan) {
        this.plan = plan
        this.input = plan.firstSchema()
        this.output = plan.lastSchema()
    }

    Schema map(Schema parsedPayload, Schema parsedDefaults) {

        Bindings boundInputs = input.bindValues(parsedPayload)

        Map outputVars = output.fetchVariables()
        replaceGenericIndexesWithSpecificsFromTo(boundInputs.bindings(), outputVars)
        maybeMergeFromTo(outputVars, boundInputs.bindings())

        Map defaultValues = parsedDefaults.asDefaults()
        copyFromTo(defaultValues, boundInputs)

        for (Morpher m : plan.morphers) {
            m.tweakInputs(boundInputs.bindings(), parsedPayload.parsed)
        }

        Set missingInputs = collectMissings(boundInputs.bindings())
        for (Morpher m : plan.morphers) {
            m.blessMissingInputs(missingInputs)
        }

        if (!missingInputs.isEmpty())
            throw new MissingInputsException(input.schema, output.schema, missingInputs)
        else
            removeMissing(boundInputs.bindings())

        Map boundInputsOutputs = collectBindings(boundInputs.bindings(), outputVars)

        for (Morpher m : plan.morphers) {
            m.preTweakValues(boundInputs.bindings(), boundInputsOutputs)
            m.tweakValues(boundInputs.bindings(), boundInputsOutputs)
        }

        Set danglingInputs = []
        Set danglingOutputs = []
        output.inject(boundInputsOutputs, danglingInputs, danglingOutputs)

        for (Morpher m : plan.morphers) {
            m.blessDanglingInputs(danglingInputs)
        }

        if (!danglingInputs.isEmpty())
            throw new DangingInputsException(input.schema, output.schema, danglingInputs)

        for (Morpher m : plan.morphers) {
            m.blessDanglingOutputs(danglingOutputs)
        }

        if (!danglingOutputs.isEmpty())
            throw new DanglingOutputsException(input.schema, output.schema, danglingOutputs)

        for (Morpher m : plan.morphers) {
            m.tweakParsed(parsedPayload.parsed, output.parsed)
        }

        // The above errors are (conditionally) fatal, but dangling inputs also
        // is considered a warning, hence another check below.

        // Hotspot: used to use danglingInputs = boundInputs.keySet() - outputVars.keySet()
        difference(danglingInputs, boundInputs.bindings(), outputVars)

        for (Morpher m : plan.morphers) {
            m.blessDanglingInputs(danglingInputs)
        }
        warnDanglingInputs(danglingInputs)

        Set union = boundInputsOutputs.keySet().collect()
        for (Morpher m : plan.morphers) {
            m.blessDanglingOutputs(union)
        }

        assertNoDanglingOutputs(boundInputsOutputs.subMap(union))

        output
    }

    private void difference(Set results, Map setA, Map setB) {
        results.clear()
        setA.each { k,v ->
            if(!setB.containsKey(k))
                results.add(k)
        }
    }

    void replaceGenericIndexesWithSpecificsFromTo(Map src, Map dst) {
        Map results = [:]
        Variables vars = new Variables()
        dst.each { k,v ->
            List hits = vars.matches(src, k)
            if (!hits.isEmpty()) {
                hits.each { h -> results[h] = v }
            }
            else
                results[k] = v
        }
        dst.clear()
        dst.putAll(results)
    }

    void maybeMergeFromTo(Map fromMap, Map toMap) {
        fromMap.each { k,v ->
            if (toMap.containsKey(k) && toMap[k] == null)
                toMap[k] = v
        }
    }

    // TODO: this can be moved into Bindings
    void copyFromTo(Map fromMap, Bindings bindings) {
        fromMap.each { k,v ->
            if (v != null) {
                bindings.putIfMissingOrNull(k, v)
                bindings.putIfDefault(k, v)
            }
        }
    }

    void removeMissing(Map map) {
        def deathrow = map.findAll { k,v -> v == null }.collect { k,v -> k }
        deathrow.each { k -> map.remove(k) }
    }

    private Set collectMissings(Map boundValues) {
        Set results = boundValues.findAll { k, v -> v == null }.collect { k, v -> k }
        results
    }

    private Map collectBindings(Map boundInputs, Map outputVars) {
        Variables vars = new Variables()
        Map results = outputVars.collectEntries { ov, val ->
            if (vars.isGenericIndexed(ov)) {
                boundInputs.each { k, v ->
                    if (vars.matches(k, ov))
                        [ k, firstNonNull(boundInputs[k], outputVars[k]) ]
                }
            }
            else
                [ ov, firstNonNull(boundInputs[ov], outputVars[ov]) ]
        }

        boundInputs.each { k,v ->
            if (k.startsWith('_'))
                results.put(k, v)
        }

        results
    }

    private Object firstNonNull(Object... objs) {
        for (obj in objs)
            if (obj != null)
                return obj
        null
    }

    private void warnDanglingInputs(Set unhandledInputs) {
        def dangling = unhandledInputs.findAll { s -> !s.startsWith('_')}
        if (!dangling.isEmpty()) {
            log.warn("For (in:${input.schema}) (out:${output.schema}), the following input variables had no matching outputs: "+ dangling)
        }
    }

    private void assertNoDanglingOutputs(Map boundOutputs) {
        def unhandledOutputs = boundOutputs.findAll { k, v -> v == null }
        if (!unhandledOutputs.isEmpty())
            throw new DanglingOutputVariables(input.schema, output.schema, unhandledOutputs.keySet())
    }
}
