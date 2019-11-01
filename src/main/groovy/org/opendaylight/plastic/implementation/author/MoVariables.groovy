
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation.author


import org.opendaylight.plastic.implementation.Variables
import org.opendaylight.plastic.implementation.PlasticException

class MoVariables {

    private static final Variables variables = new Variables()

    static {
        MoVariables.metaClass.each = { Closure cs ->
            delegate.bindings.each { k,v -> cs(k,v) }
        }
    }

    private Map bindings

    MoVariables(Map varBindings) {
        this.bindings = varBindings
    }

    MoArray asArray(String slice) {
        new MoArray(slice, bindings)
    }

    List<MoArray> asArrays(String... sliceNames) {
        List<MoArray> results = []
        sliceNames.each { n -> results.add(asArray(n)) }
        results
    }

    MoArray newArray(String newSlice, int len, Object defValue) {
        List newKeys = variables.generateIndexed(newSlice, len)
        MoVariables self = this
        newKeys.each { k -> self[k] = defValue }
        new MoArray(newSlice, bindings)
    }

    MoArray newArray(String newSlice, int len) {
        newArray(newSlice, len, "")
    }

    boolean contains(String key) {
        bindings.containsKey(key)
    }

    boolean isBound(String varName) {
        if (contains(varName)) {
            Object bound = bindings.get(varName)
            return bound != null
        }
        false
    }

    void mergeFrom(MoVariables source) {
        source.bindings.each { k,v ->
            if (!bindings.containsKey(k) || bindings[k] == null) {
                bindings[k] = v
            }
        }
    }

    // Forwarded methods from Map. Tried using @Delegate on bindings but it is the same
    // as inheriting from Map, which causes overloading issues with type signatures using
    // Map and MoVariables

    boolean isEmpty() {
        bindings.isEmpty()
    }

    int size() {
        bindings.size()
    }

    void put(String key, Object value) {
        bindings.put(key, value)
    }

    Object get(String key) {
        bindings.get(key)
    }

    Object remove(String key) {
        bindings.remove(key)
    }

    void putAt(String key, Object value) {
        bindings.put(key, value)
    }

    Object getAt(String key) {
        bindings[key]
    }

    boolean containsKey(String key) {
        bindings.containsKey(key)
    }
}

class MoArray {

    private static final String ExceptId = "PLASTIC-MORPH-ARR"
    private static final Variables variables = new Variables()

    private static final Closure customSorter = { String a, String b ->
        if (a.length() == b.length()) {
            return (a <=> b)
        }

        def (abase,aindex) = a.replace('[',' ').replace(']',' ').split('\\s')
        def (bbase,bindex) = b.replace('[',' ').replace(']',' ').split('\\s')

        if (abase != bbase) {
            return (abase <=> bbase)
        }

        (Integer.parseInt(aindex) <=> Integer.parseInt(bindex))
    }

    private Map bindings
    private Set keys
    private String sliceName

    private List<String> lazyOrderedKeys = []
    private List<Object> lazyOrderedValues = []

    MoArray(String variable, Map varBindings) {
        throwUnlessGenericIndexed(variable)

        this.sliceName = variable
        this.bindings = varBindings
        this.keys = variables.matches(bindings, variable)
    }

    List orderedKeys() {
        if (lazyOrderedKeys.isEmpty())
            this.lazyOrderedKeys = keys.sort(customSorter)

        lazyOrderedKeys
    }

    List orderedValues() {
        if (lazyOrderedValues.isEmpty()) {
            List okeys = orderedKeys()
            this.lazyOrderedValues = okeys.collect { k -> bindings[k] }
        }
        lazyOrderedValues
    }

    boolean isEmpty() {
        keys.isEmpty()
    }

    int size() {
        keys.size()
    }

    /**
     * Use brackets to access this method, like a[i] instead of a.getAt(i)
     * @param index
     * @return
     */
    Object getAt(int index) {
        orderedValues()
        lazyOrderedValues[index]
    }

    /**
     * Use brackets to access this method, like a[i]=3 instead of a.putAt(i, 3)
     * @param index
     * @param value
     */
    void putAt(int index, Object value) {
        throwIfInvalidIndex(index)
        String newVar = variables.generateOneIndexed(sliceName, index)
        bindings.put(newVar, value)
        keys.add(newVar)
    }

    void set(List values) {
        List newVars = variables.generateIndexed(sliceName, values.size())

        keys.each { k -> bindings.remove(k) }
        keys.clear()
        lazyOrderedKeys.clear()
        lazyOrderedKeys.clear()

        for (int i = 0; i< values.size(); i++) {
            bindings.put(newVars[i], values[i])
            keys.add(newVars[i])
        }
    }

    private void throwIfInvalidIndex(int index) {
        if (index >= keys.size()) {
            throw new PlasticException(ExceptId, "Index ${index} is too large for array ${this.sliceName} (that has size ${this.size()})")
        }
    }

    private void throwUnlessGenericIndexed(String variable) {
        if (!variables.isGenericIndexed(variable))
            throw new PlasticException(ExceptId, "Expected a genericly indexed variable name (like \${abc[*]}) instead of ${variable}")
    }
}
