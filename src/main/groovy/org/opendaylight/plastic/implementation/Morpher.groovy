
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import org.opendaylight.plastic.implementation.author.MoVariables
import groovy.transform.Canonical


class Morpher {

    static class MalformedMorpher extends PlasticException {
        MalformedMorpher(String fileName) {
            super("PLASTIC-MALFORMED-MORPHER", "The following morpher is missing required methods: " + fileName)
        }
    }

    // TODO: the "context" feature was not being used by authors, so it is no longer exposed - remove it here

    @Canonical
    static class Context {
        VersionedSchema relatedSchema
        String fileName
    }

    def wrapped

    List<MetaMethod> availableMethods

    boolean tweakInputsPresent
    boolean tweakValuesPresent
    boolean tweakParsedPresent
    boolean blessDanglingInputsPresent
    boolean preTweakValuesPresent
    boolean blessMissingInputsPresent
    boolean blessDanglingOutputsPresent
    boolean setContextPresent
    boolean tweakMoInputsPresent
    boolean tweakMoValuesPresent

    def desiredMethods = [
            ["tweakValues", Map, Map],
            ["tweakParsed", Object, Object],
            ["_blessDanglingInputs", Set],
            ["_preTweakValues", Map, Map],
            ["_blessMissingInputs", Set],
            ["_blessDanglingOutputs", Set],
            ["_setContext", Context],
            ["tweakInputs", Map, Object],
            ["tweakInputs", MoVariables, Object],
            ["tweakValues", MoVariables, MoVariables]
    ]

    Morpher(Object wrapped) {
        this.wrapped = wrapped
    }

    Morpher(VersionedSchema relatedSchema, Object wrapped, String fileName) {
        this.wrapped = wrapped
        this.availableMethods = wrapped.metaClass.getMethods()

        this.tweakValuesPresent = can(desiredMethods[0])
        this.tweakParsedPresent = can(desiredMethods[1])
        this.blessDanglingInputsPresent = can(desiredMethods[2])
        this.preTweakValuesPresent = can(desiredMethods[3])
        this.blessMissingInputsPresent = can(desiredMethods[4])
        this.blessDanglingOutputsPresent = can(desiredMethods[5])
        this.setContextPresent = can(desiredMethods[6])
        this.tweakInputsPresent = can(desiredMethods[7])
        this.tweakMoInputsPresent = can(desiredMethods[8])
        this.tweakMoValuesPresent = can(desiredMethods[9])

        def requireds = [tweakInputsPresent, tweakValuesPresent, tweakParsedPresent, tweakMoInputsPresent, tweakMoValuesPresent]
        int count = requireds.collect { r -> r ? 1 : 0 }.sum()
        if (!count)
            throw new MalformedMorpher(fileName)

        setContext(new Context(relatedSchema, fileName))
    }

    void setAppContext(AppContext properties) {
        if (can(["setContext", Map])) {
            wrapped.setContext(properties.asMap())
        }
    }

    protected boolean can(List methodItems) {
        wrapped.metaClass.respondsTo(wrapped, *methodItems)
    }

    void setContext(Context context) {
        if (setContextPresent)
            wrapped._setContext(context)
    }

    void blessMissingInputs(Set missings) {
        if (blessMissingInputsPresent)
            wrapped._blessMissingInputs(missings)
    }

    void blessDanglingInputs(Set dangling) {
        if (blessDanglingInputsPresent)
            wrapped._blessDanglingInputs(dangling)
    }

    def blessDanglingOutputs(Set dangling) {
        if (blessDanglingOutputsPresent)
            wrapped._blessDanglingOutputs(dangling)
    }

    void tweakInputs(Map inMap, inTree) {
        if (tweakMoInputsPresent) {
            MoVariables moIns = new MoVariables(inMap)
            wrapped.tweakInputs(moIns, inTree)
        }
        else if (tweakInputsPresent) {
            wrapped.tweakInputs(inMap, inTree)
        }
    }

    void preTweakValues(Map inMap, Map outMap) {
        if (preTweakValuesPresent)
            wrapped._preTweakValues(inMap, outMap)
    }

    void tweakValues(Map inMap, Map outMap) {
        if (tweakMoValuesPresent) {
            MoVariables moIns = new MoVariables(inMap)
            MoVariables moOuts = new MoVariables(outMap)
            wrapped.tweakValues(moIns, moOuts)
        }
        else if (tweakValuesPresent) {
            wrapped.tweakValues(inMap, outMap)
        }
    }

    void tweakParsed(inTree, outTree) {
        if (tweakParsedPresent)
            wrapped.tweakParsed(inTree, outTree)
    }
}
