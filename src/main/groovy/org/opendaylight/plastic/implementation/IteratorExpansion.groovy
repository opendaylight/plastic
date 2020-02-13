
/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

/**
 * Class is responsible for taking pre-discovered iterators and using those to replicate
 * any list contents.
 *
 * The resulting model IS MODIFIED IN PLACE with lists having repeated contents and any
 * generic variables replaced with specific variables (ie, ABC[*] becomes ABC[0] ... ABC[N]
 *
 * This is memory model (lists and maps) specific and would need abstraction to deal with
 * other memory models like XML nodes
 *
 */
package org.opendaylight.plastic.implementation

import groovy.transform.CompileStatic

@CompileStatic
class IteratorExpansion {

    static class CollectionExpectedException extends PlasticException {
        CollectionExpectedException(Object model) {
            super("PLASTIC-COLL-NEEDED", "The following model was not recognized as a map or list: "+model)
        }
    }

    static class UnexpectedParentCollectionTypeException extends PlasticException {
        UnexpectedParentCollectionTypeException(Object parent) {
            super("PLASTIC-WHAT-PAR-TYPE", "The following should have been a list or map but wasn't: "+parent)
        }
    }

    private IteratorFlows flows
    private int recursionDepth = 0 // interactive debugging aid
    private List<List> recursedParents = new ArrayList<>()
    private Map<String,List> abandoning = new HashMap<>()

    IteratorExpansion(IteratorFlows flows) {
        this.flows = flows
    }

    void processModel(Object model) {
        _processModel(model)

        abandoning.each { varName, parent ->
            abandonVariable(parent, varName)
        }
    }

    private void _processModel(Object model) {

        recursionDepth++

        if (isList(model)) {
            List marked = (List) model // reference is identity!
            recursedParents.add(marked)

            Schemiterator iterator = flows.getOutputIterator(marked)
            if (iterator.effectiveDimensions() > 0) {

                List prototypes = []
                prototypes.addAll(marked)
                marked.clear()

                while(!iterator.isDone()) {
                    Map<String, String> specificVars = iterator.replaceables()
                    prototypes.each { member ->

                        // the indexed variable is a sub-member of an array-of-objects or an array-of-arrays

                        if (isCollection(member)) {
                            IdentityHashMap<List,List> oldToNew = new IdentityHashMap<>()
                            Object cloned = deepCopy(member, oldToNew)
                            flows.shareIterators(oldToNew)
                            recursivelyReplace(cloned, specificVars)

                            // now clone chunk should no longer have any generic variables AT THIS LEVEL - all
                            // should be specifically indexed, but there may be generic variables in children

                            marked.add(cloned)

                            // recursion must happen in the context of the iterator incrementing to have the
                            // indexes advance correctly

                            _processModel(cloned)
                        }

                        // this indexed variable is a member of an array of scalars, so
                        // add de-indexed variables the end of array then walk the
                        // array again

                        else {
                            String templateValue = member.toString()
                            String specific = templateValue

                            // handle multiple variables in the same value

                            for (Map.Entry<String, String> entry : specificVars) {
                                specific = replace(entry.key, entry.value, specific)
                            }

                            // We might have a specific-indexed variable that just might not have
                            // a binding - this can happen for non-rectangular indices, so don't add it

                            boolean abandon = false

                            if (Variables.isIndexed(specific) && Variables.isSingular(specific)) {
                                if (!flows.isBound(Variables.unadorn(specific))) {
                                    abandon = true
                                }
                            }
                            // newly created value should no longer have any generic indices - all should be specific

                            if (!abandon)
                                marked.add(specific);
                        }
                    }

                    iterator.increment()
                }

                // Because we are re-using iterators rather than creating copies...

                iterator.reset()

                recursedParents.remove(recursedParents.size()-1)

                // List was expanded and recursed because it was in use directly or indirectly by an iterator,
                // so do not recurse again on it

                return
            }
        }

        // If this is a list... then it had no expansion or it is a list with expansion but no iterator
        // from the input bindings

        model.each { obj ->

            // For array.each, the obj can be a map, array, or a scalar

            if (isCollection(obj)) {
                _processModel(obj)
                return
            }

            // For map.each, the obj will always be a map entry (string key, value of map, array, scalar)

            def schemaValue = asValue(obj)

            if (isCollection(schemaValue)) {
                _processModel(schemaValue)
                return
            }

            // For scalar (aka leaf) value...

            if (schemaValue instanceof String) {
                String valueString = (String) schemaValue
                if (Variables.mightBeIndexed(valueString)) {

                    // We have a generic indexed variable that was not expandable because
                    // it had no binding - this can happen for empty arrays, so just remove it

                    if (Variables.isGenericIndexed(valueString)) {
                        abandoning.put(valueString, recursedParents[-1])
                    }
                }
            }
        }

        recursionDepth--
        if (isList(model))
            recursedParents.remove(recursedParents.size()-1)
    }

    private static boolean isCollection(Object model) {
        (model instanceof Map) || (model instanceof List)
    }

    private static boolean isList(Object model) {
        model instanceof List
    }

    private Object deepCopy(Object object, IdentityHashMap<List,List> oldNew) {
        if (object instanceof Map) {
            Map result = [:]
            ((Map)object).each { k,v -> result[k] = deepCopy(v, oldNew) }
            return result
        }
        if (object instanceof List) {
            List result = []
            oldNew.put(object, result)
            ((List)object).each { v -> result.add(deepCopy(v, oldNew)) }
            return result
        }

        return object.getClass().newInstance(object)
    }

    void recursivelyReplace(Object model, Map<String,String> fromTo) {
        if(!isCollection(model))
            throw new CollectionExpectedException(model)
        doRecursivelyReplace(null, null, model, fromTo)
    }

    private def doRecursivelyReplace(Object parentMapOrList, Object keyOrIndex, Object model, Map<String,String> fromTo) {
        if (model instanceof List) {
            ((List)model).eachWithIndex { obj,i ->
                doRecursivelyReplace(model, i, obj, fromTo)
            }
        }
        else if (model instanceof Map) {
            ((Map)model).each { k,v ->
                doRecursivelyReplace(model, k, v, fromTo)
            }
        }
        else {
            def val = asValue(model)
            for (Map.Entry<String,String> entry : fromTo) {
                val = replace(entry.key, entry.value, val)
            }
            setListOrMapValue(parentMapOrList, keyOrIndex, val)
        }
    }

    static private Object replace(String varName, Object varValue, Object output) {

        // Not sure this really makes sense. Shouldn't we just treat the replacement value
        // as empty and continue? Needs looking at.

        if (varValue == null)
            return ""

        // Trying to replace a portion of something with a collection doesn't make sense, so just
        // return the collection (ie, a full replacement, which does make sense)

        if (isCollection(varValue))
            return varValue

        if (output instanceof String || output instanceof GString) {
            String sOutput = (String) output
            String sValue = varValue.toString()

            sOutput = sOutput.replace(varName, sValue)

            // If this is a complete replacement, then retain the original type from the value.
            // Do not stringify it. A partial replacement by definition should result in a string.

            if (sOutput == sValue)
                output = varValue
            else
                output = sOutput
        }

        return output
    }

    static private void setListOrMapValue(Object mapOrList, Object keyOrIndex, Object value) {
        if (mapOrList instanceof List)
            ((List)mapOrList)[(int)keyOrIndex] = value
        else if (mapOrList instanceof Map)
            ((Map)mapOrList)[keyOrIndex] = value
        else
            throw new UnexpectedParentCollectionTypeException(mapOrList)
    }

    static private Object asValue(Object obj) {
        Object value = obj

        if (obj instanceof Map.Entry) {
            value = ((Map.Entry)obj).getValue()
        }

        if (obj instanceof String || obj instanceof GString || obj instanceof Number || obj instanceof Boolean ) {
            value = obj.toString()
        }

        value
    }

    // Remove any elements with the given value from the parent structure, wherever it occurs. If the
    // element is a member of an array, then remove that slot. If it is a member of a map, then
    // replace its value with a blank string
    //
    private void abandonVariable(Object parent, String element) {
        if (parent instanceof Map) {
            ((Map)parent).each { k,v ->
                if (v.equals(element))
                    setListOrMapValue(parent, k, "")
                else if (v instanceof Map || v instanceof List)
                    abandonVariable(v, element)
            }
        }
        else if (parent instanceof List) {
            ((List)parent).removeAll { e ->
                if (element.equals(e))
                    return true
                else if (e instanceof Map || e instanceof List)
                    abandonVariable(e, element)
                return false
            }
        }

        parent
    }
}

