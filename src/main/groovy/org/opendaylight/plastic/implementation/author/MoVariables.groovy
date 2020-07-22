
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation.author

import org.opendaylight.plastic.implementation.Schemiterator
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
        List newKeys = variables.generateManyIndexed(newSlice, len)
        MoVariables self = this
        newKeys.each { k -> self[k] = defValue }
        new MoArray(newSlice, bindings)
    }

    MoArray newArray(String newSlice, int len) {
        newArray(newSlice, len, "")
    }

    MoArray newArray(String newSlice, List values) {
        MoArray array = newArray(newSlice, values.size(), "")
        array.set(values)
        array
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

    String dump() {
        bindings.toMapString()
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
    private Schemiterator iterator

    // TODO: i don't believe that we need lazyOrderedKeys anymore after using sizeFromKeys() - remove it

    private List<String> lazyOrderedKeys = []
    private List<Object> lazyOrderedValues = []

    MoArray(String variable, Map varBindings) {
        throwUnlessGenericIndexed(variable)

        this.sliceName = variable
        this.bindings = varBindings
        this.keys = variables.matches(bindings, variable)

        ensureIterator()
    }

    private void ensureIterator() {
        if (Schemiterator.hasSpec(sliceName, bindings)) {
            iterator = new Schemiterator(sliceName)
            iterator.readSpecFrom(bindings)
        }
        else
            updateIterator()
    }

    // Only support for single dimension - if more wanted, then client
    // must write ad hoc logic
    //
    // For older client logic, make sure there is an iterator spec in the
    // bindings, or none of the array logic will work
    //
    private void updateIterator() {
        iterator = new Schemiterator(sliceName, sizeFromKeys(keys))
        iterator.writeSpecTo(bindings)
    }

    private int sizeFromKeys(Set<String> theKeys) {
        int largestFoundIndex = -1
        for (String key : theKeys) {
            int index = Variables.extractIndexAsInt(key, -1)
            if (index > largestFoundIndex)
                largestFoundIndex = index
        }
        largestFoundIndex == -1 ? 0 : (largestFoundIndex+1)
    }

    String getName() {
        sliceName
    }

    List orderedKeys() {
        if (lazyOrderedKeys.isEmpty())
            this.lazyOrderedKeys = keys.sort(customSorter)

        lazyOrderedKeys
    }

    List orderedValues() {
        if (lazyOrderedValues.isEmpty()) {
            int len = size()
            for (int i = 0; i< len; i++) {
                String key = Variables.generateAsIndexed(sliceName, i)
                lazyOrderedValues.add(bindings[key])
            }
        }
        lazyOrderedValues
    }

    List values() {
        orderedValues()
    }

    boolean isEmpty() {
        keys.isEmpty()
    }

    int size() {
        iterator.size()
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
        forcePutAt(index, value)
    }

    void set(List values) {
        List newVars = variables.generateManyIndexed(sliceName, values.size())

        keys.each { k -> bindings.remove(k) }
        keys.clear()
        lazyOrderedKeys.clear()
        lazyOrderedValues.clear()

        for (int i = 0; i< values.size(); i++) {
            bindings.put(newVars[i], values[i])
            keys.add(newVars[i])
        }

        updateIterator()
    }

    void add(Object value) {
        int size = bindings.size();
        size-- // do not count the iterator
        forcePutAt(size, value)

        iterator = new Schemiterator(sliceName, keys.size())
        iterator.writeSpecTo(bindings)
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

    private void forcePutAt(int index, Object value) {
        String newVar = variables.generateAsIndexed(sliceName, index)
        bindings.put(newVar, value)
        keys.add(newVar)
    }

    String dump() {
        StringBuilder sb = new StringBuilder()
        int len = size()
        for (int i = 0; i< len; i++) {
            if (sb.size() != 0)
                sb.append(', ')
            sb.append(getAt(i)?.toString())
        }
        sb.append((sb.size() == 0) ? ']' : ' ]')
        sb.insert(0, "$sliceName [ ")
        sb.toString()
    }
}
