
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

    static class CollectionUsageInfo {
        Object collection
        int uses

        CollectionUsageInfo(Object collection) {
            this.collection = collection
            this.uses = 0
        }
    }

    private IteratorFlows flows
    private List<List> recursedParentLists = new ArrayList<>()
    private List<CollectionUsageInfo> recursedCollections = new ArrayList<>()
    private Map<String,List> abandonedLeafs = new HashMap<>()

    IteratorExpansion(IteratorFlows flows) {
        this.flows = flows
    }

    void processModel(Object model) {
        _processModel(model)

        abandonedLeafs.each { varName, parent ->
            abandonVariable(parent, varName)
        }
    }

    private CollectionUsageInfo _processModel(Object model) {

        recursedCollections.add(new CollectionUsageInfo(model))

        if (isList(model)) {
            List marked = (List) model // reference is identity!
            recursedParentLists.add(marked)

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

                            IdentityHashMap<List, List> oldClonedToNew = new IdentityHashMap<>()
                            Object cloned = deepCopy(member, oldClonedToNew)

                            // The cloned sublists will be encountered later as part of recursion, so don't
                            // count them here by associating iterators with them (or you'll have too many)

                            flows.shareIterators(oldClonedToNew)

                            Set<String> found = [] as Set
                            recursivelyReplace(cloned, specificVars, found)

                            // We might have a specific-indexed variable that just might not have
                            // a binding - this can happen for non-rectangular indices, so be prepared
                            // to not commit the clone

                            boolean maybeAbandon = found.isEmpty()
                            if (!maybeAbandon) {
                                incrementUseCounts()
                            }

                            // recursion must happen in the context of the iterator incrementing to have the
                            // indexes advance correctly

                            CollectionUsageInfo infoForClone = _processModel(cloned)

                            if (infoForClone.uses == 0) {

                                // Retract the iterators for the abandoned clone - they just add noise

                                flows.unshareIterators(oldClonedToNew)
                            }
                            else {

                                // now clone chunk should no longer have any generic variables AT THIS LEVEL - all
                                // should be specifically indexed, but there may be generic variables in children

                                marked.add(cloned)

                            }
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

                            if (!abandon) {
                                marked.add(specific);
                                incrementUseCounts()
                            }
                        }
                    }

                    iterator.increment()
                }

                // Because we are re-using iterators rather than creating copies...

                iterator.reset()

                recursedParentLists.remove(recursedParentLists.size()-1)
                CollectionUsageInfo result = recursedCollections.remove(recursedCollections.size()-1)

                // List was expanded and recursed because it was in use directly or indirectly by an iterator,
                // so do not recurse again on it

                return result
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
                Variables vars = new Variables((String) schemaValue)
                Map<String, String> nameToRaws = vars.getNameToRawMapping()

                // Just need to figure out if the variable should be abandoned or contribute to keeping it's
                // possibly cloned parent in existence, hence we can break out of the loop early

                for (String vName : vars.names()) {
                    if (Variables.mightBeIndexed(vName)) {
                        // We have a generic indexed variable that was not expandable because
                        // it had no binding - this can happen for empty arrays, so just remove it

                        if (Variables.isGenericIndexed(vName)) {
                            abandonedLeafs.put(nameToRaws.get(vName), recursedParentLists[-1])
                        } else if (Variables.isIndexed(vName)) {
                            if (flows.isBound(Variables.unadorn(vName)))
                                incrementUseCounts()

                            // We have a specific indexed variable that might not have a binding because
                            // the indexes are not rectangular (there are holes in the data)

                            else
                                abandonedLeafs.put(nameToRaws.get(vName), recursedParentLists[-1])
                        }
                    }
                }
            }
        }

        if (isList(model))
            recursedParentLists.remove(recursedParentLists.size()-1)
        recursedCollections.remove(recursedCollections.size()-1)
    }

    private void incrementUseCounts() {
        for (CollectionUsageInfo info : recursedCollections) {
            info.uses++
        }
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

    void recursivelyReplace(Object model, Map<String,String> fromTo, Set<String> found) {
        if(!isCollection(model))
            throw new CollectionExpectedException(model)
        doRecursivelyReplace(null, null, model, fromTo, found)
    }

    private def doRecursivelyReplace(Object parentMapOrList, Object keyOrIndex, Object model,
                                     Map<String,String> fromTo, Set<String> found) {
        if (model instanceof List) {
            ((List)model).eachWithIndex { obj,i ->
                doRecursivelyReplace(model, i, obj, fromTo, found)
            }
        }
        else if (model instanceof Map) {
            ((Map)model).each { k,v ->
                doRecursivelyReplace(model, k, v, fromTo, found)
            }
        }
        else {
            def val = asValue(model)
            for (Map.Entry<String,String> entry : fromTo) {
                val = replace(entry.key, entry.value, val)
                if (flows.isBound(entry.value)) {
                    found.add(entry.value)
                }
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

    // Remove any elements with the given value from the parent structure, wherever it occurs.
    // If the element is a member of an array, then remove that slot.
    // If it is a member of a map, then delete the key
    //
    private void abandonVariable(Object parent, String element) {
        if (parent instanceof Map) {
            String owningKey = ''
            Map map = (Map) parent
            map.each { k,v ->
                if (v.equals(element)) {
                    owningKey = k
                }
                else if ((v instanceof Map) || (v instanceof List))
                    abandonVariable(v, element)
            }
            if (owningKey)
                map.remove(owningKey)
        }
        else if (parent instanceof List) {
            ((List)parent).removeAll { e ->
                if (element.equals(e))
                    return true
                else if (e instanceof Map || e instanceof List) {
                    abandonVariable(e, element)
                    if ((e instanceof Map) && ((Map)e).isEmpty())
                        return true
                }
                return false
            }
        }

        parent
    }
}

