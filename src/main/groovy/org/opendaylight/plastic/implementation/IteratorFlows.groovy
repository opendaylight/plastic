
/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.annotations.VisibleForTesting
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

/**
 * Class is responsible for taking the bindings in (values and iterator specs), instantiating
 * input iterators, then walking the model to calculate the output iterators. These will be
 * bound in parent-child relationships and will have range and dimension compatibility.
 *
 * Client logic can look up the results based on the lists found in the model itself.
 *
 * This is JSON memory model specific and would need abstraction to deal with XML
 */

@CompileStatic
class IteratorFlows {

    static class IndexedButNoContainingArrayException extends PlasticException {
        IndexedButNoContainingArrayException(String varName) {
            super("PLASTIC-INDEX-WO-ARRAY", "The following indexed variable is not in an array: "+varName)
        }
    }

    static class ListInfo {
        long id
        List list
        Schemiterator iterator

        ListInfo(long id, List model) {
            this.id = id
            this.list = model
            this.iterator = new Schemiterator(0)
        }
    }

    Map inValues
    Map<String, Schemiterator> inputIterators

    Identities listIds = new Identities()
    Stack<ListInfo> parentLists = new Stack<>()
    Map<Long, Schemiterator> outputIterators = new HashMap<>()

    IteratorFlows(Map inValues) {
        this.inValues = inValues
        this.inputIterators = Schemiterator.instantiateIterators(inValues)
    }

    void processModel(Object model) {

        boolean doPushOrPop = isList(model)

        if (doPushOrPop)
            parentLists.push(new ListInfo(listIds.get(model), (List)model))

        model.each { obj ->

            // For array.each, the obj can be a map, array, or a scalar

            if (isCollection(obj)) {
                processModel(obj)
                return
            }

            // For map.each, the obj will always be a map entry (string key plus a value of map, array, scalar)

            def schemaValue = asValue(obj)

            if (isCollection(schemaValue)) {
                processModel(schemaValue)
                return
            }

            // For leaf values...

            Variables vars = new Variables(schemaValue.toString())
            if (vars.isPresent()) {
                vars.toEach { String var, String val ->
                    if (Variables.isGenericIndexed(var)) {
                        Schemiterator iteratorForVar = inputIterators.get(var)
                        if (iteratorForVar != null) {
                            verifyDepth(iteratorForVar)
                            ListInfo tos = parentLists.peek()

                            // Note we are converting from the name used on input, like abc[*][*],
                            // to the name used in the output schema, which could be different,
                            // like abc[^][*]

                            tos.iterator.mergeFromUsingName(iteratorForVar, var)
                        }
                    }
                }
            }
        }

        if (doPushOrPop) {
            ListInfo info = parentLists.peek()
            int depth = info.iterator.effectiveDimensions()
            if (depth > 0) {
                outputIterators.put(info.id, info.iterator)
                if (depth > 1) {
                    List<Schemiterator> stackedIterators = iteratorStack()
                    info.iterator.flowOut(stackedIterators)
                }
            }
            parentLists.pop()
        }
    }

    int numberOfVariables() {
        inValues.size()-inputIterators.size()
    }

    boolean isBound(String varName) {
        inValues.containsKey(varName)
    }

    static private Object asValue(Object obj) {
        def value = obj

        if (obj instanceof Map.Entry) {
            value = ((Map.Entry)obj).getValue()
        }

        if (obj instanceof String || obj instanceof GString || obj instanceof Number || obj instanceof Boolean ) {
            value = obj.toString()
        }

        value
    }

    // Return input (bound) iterator for given variable name or null if not found
    //
    Schemiterator getInputIterator(String varName) {
        inputIterators.get(varName)
    }

    Schemiterator getOutputIterator(List list) {
        long id = listIds.get(list)
        Schemiterator result = outputIterators.get(id)
        result ? result : new Schemiterator(0)
    }

    // Note that these are SHARED iterators, so no concurrent use and be sure to reset
    // them after use. May revisit later, being sure to clone parent iterators too.
    //
    void shareIterators(IdentityHashMap<List, List> oldToNew) {
        oldToNew.each { oldL,newL ->
            long newId = listIds.get(newL)
            long oldId = listIds.get(oldL)
            outputIterators.put(newId, outputIterators.get(oldId))
        }
    }

    // Remove any iterators associated with the new ids.
    //
    void unshareIterators(IdentityHashMap<List, List> oldToNew) {
        oldToNew.each { oldL,newL ->
            long newId = listIds.get(newL)
            outputIterators.remove(newId)
        }
    }

    private boolean isList(Object model) {
        model instanceof List
    }

    private boolean isCollection(Object model) {
        (model instanceof Map) || (model instanceof List)
    }

    private void verifyDepth(Schemiterator iterator) {
        int atLeast = iterator.effectiveDimensions()
        if (parentLists.size() < atLeast) {
            throw new IndexedButNoContainingArrayException(iterator.allNames())
        }
    }

    private List<Schemiterator> iteratorStack() {
        List<Schemiterator> results = new ArrayList<>()
        for (int i = 0; i< parentLists.size(); i++) {
            results.add(parentLists.get(i).iterator)
        }
        results
    }

    @VisibleForTesting
    @PackageScope
    Map<Long, Schemiterator> findings() {
        outputIterators
    }
}
