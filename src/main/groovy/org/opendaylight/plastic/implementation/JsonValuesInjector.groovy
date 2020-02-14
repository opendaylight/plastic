
/*
 * Copyright (c) 2019-2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * This is easily the most complex code in Plastic.
 *
 * - Read the tutorial, particularly on arrayed variables (aka generic variables)
 * - Look closely at the unit tests to see how the logic works from the outside
 *
 * Here are some important concepts to help understanding of the logic
 *
 * - Use of recursion to walk the output schema
 * - Recursing into a data structure
 * - Returning out of a data structure
 * - Delaying structure additions/deletions to avoid concurrent modifications
 * - Iterators that have multiple explicit dimensions
 * - Iterators that have implicit (borrowed) dimensions
 * - Recursing into the first use of an interator
 * - Recursing out of the dimensional depth of an iterator
 */

@CompileStatic
class JsonValuesInjector {

    static Logger logger = LoggerFactory.getLogger(JsonValuesInjector)

    static class IndexedButNoContainingArrayException extends PlasticException {
        IndexedButNoContainingArrayException(String varName) {
            super("PLASTIC-INDEX-WO-ARRAY", "The following indexed variable is not in an array: "+varName)
        }
    }

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

    static class Triple {

        Object model
        Map<String,Schemiterator> iterators = new HashMap<>()

        Triple(Object model) {
            this.model = model
        }
        List modelAsList() {
            model instanceof List ? (List)model : (List)null
        }

        void add(Schemiterator iterator) {
            iterator.addToMap(iterators)
        }

        Schemiterator iterator() {
            Schemiterator result = null
            for (Schemiterator iterator : iterators.values()) {
                if (result == null)
                    result = iterator
                else
                    result = result.mergeWith(iterator)
            }
            result
        }
    }

    // A stack of collections (List and Map) that are "in scope" as the recursion
    // walks the model. Note that nothing is really done with Maps, so they could
    // be removed by refactoring.

    static class ParentContainers {

        Stack<Triple> parentCollections = new Stack<>()

        def push(Object model) {
            // Hotspot
            parentCollections.push (new Triple(model))
        }

        def pop() {
            parentCollections.pop()
        }

        Triple peek() {
            peek(0)
        }

        Triple peek(int depth) {
            parentCollections[-1-depth]
        }

        def markForExpansion(Schemiterator iterator) {

            // TODO: cache this search for speedup

            for (int i = parentCollections.size()-1; i >= 0; i--) {
                if (parentCollections[i].model instanceof List) {
                    Triple triple = parentCollections[i]
                    triple.add(iterator)
                    return
                }
            }

            throw new IndexedButNoContainingArrayException(iterator.allNames())
        }

        boolean isTosMarkedForExpansion() {
            !parentCollections.isEmpty() && !parentCollections[-1].iterators.isEmpty()
        }

        int size() {
            parentCollections.size()
        }
    }

    static class TodoList {

        List<Closure> todos = new ArrayList<>()

        TodoList add(Closure cl) {
            todos.add(cl)
            return this
        }

        // Note that nested arrays will cause more calls to add() after the
        // doAll() is invoked. So avoid concurrent modification exceptions by
        // looping this way (size will be updated)
        //
        TodoList doAll() {
            for (int i = 0; i< todos.size(); i++) {
                Closure cl = todos[i]
                cl()
            }
            return this
        }

        TodoList clear() {
            todos.clear()
            return this
        }
    }

    Object inject(Map values, Object model, Set danglingInputs, Set danglingOutputs) {

        IteratorFlows flows = new IteratorFlows(values)
        flows.processModel(model)
        int numVariables = flows.numberOfVariables()

        IteratorExpansion expansion = new IteratorExpansion(flows)
        expansion.processModel(model)

        Set<String> expectedInputVars = (Set<String>) values.keySet()
        ParentContainers parentCollections = new ParentContainers()
        TodoList todos = new TodoList()
        Set<String> foundInputVars = [] as Set

        // some classifiers are doing multi-level child translations so that the model is truncated
        // by markers until the variable is actually injected. so walking cannot get to those
        // parts of the model yet. so we loop until we aren't making progress or we hit the maximum
        // depth.

        final int MAXDEPTH = 5 // do we have the guts to just make this infinite?

        for (int passes = 0; passes < MAXDEPTH; passes++) {
            int preFound = foundInputVars.size()

            IterativeReplacement replacer = new IterativeReplacement(values, danglingOutputs, foundInputVars)
            replacer.processModel(model)

            /*
            walkTheModel(values, effectives, model, parentCollections, danglingOutputs, foundInputVars, todos)

            // Have to actually modify the model after the traversal, otherwise
            // you get a concurrent modification exception (you can't iterate and
            // modify at the same time)

            todos.doAll().clear()
*/

            int postFound = foundInputVars.size()

            // This test for termination is not precise. If the morpher logic is ignoring some inputs
            // then the found values will never equal the given input values. This means we could do
            // one extra pass.

            boolean stalledOut = postFound == preFound
            boolean foundEverything = postFound >= numVariables

            if (stalledOut || foundEverything) {
                if (passes > 1)
                    logger.debug("Used injection passes of: {}", passes)
                break;
            }
        }

        // Hotspot: used to use danglingInputs.addAll(expectedInputVars - foundInputVars)

        expectedInputVars.each { expected ->
            if (!Variables.isInternal(expected) && !foundInputVars.contains(expected))
                danglingInputs.add(expected)
        }

        model
    }

    boolean isCollection(Object model) {
        (model instanceof Map) || (model instanceof List)
    }

    boolean isList(Object model) {
        model instanceof List
    }

    // Remove any elements with the given value from the parent structure,
    // whereever it occurs, recursively searching. If the element is a member
    // of an array, then remove that slot. If it is a member of a map, then
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

    // Need to walk the model breadth first because we need to see all the generic variables used at this
    // depth and get them to mark the same parent before going deeper. This allows all the iterators in use
    // at any level to be present when a child that has a generic variable is being processed.
    //
    private def walkTheModel(Map inValues, IteratorFlows effectives, Object model,
                             ParentContainers parentCollections,
                             Set danglingOutputVars, Set foundInputVars,
                             TodoList todos) {

        boolean  doPushPop = isCollection(model)

        if (doPushPop)
            parentCollections.push(model)

        model.eachWithIndex{ obj, int i ->

            // For array.each, the obj can be a map, array, or a scalar

            if (isCollection(obj)) {
                this.walkTheModel(inValues, effectives, obj, parentCollections, danglingOutputVars, foundInputVars, todos)
                return
            }

            // For map.each, the obj will always be a map entry (string key, value of map, array, scalar)

            def schemaValue = asValue(obj)

            if (isCollection(schemaValue)) {
                this.walkTheModel(inValues, effectives, schemaValue, parentCollections, danglingOutputVars, foundInputVars, todos)
                return
            }

            // For scalar values...

            Variables vars = new Variables(schemaValue.toString())
            if (vars.isPresent()) {
                def replaced = schemaValue
                vars.toEach { String var, String val ->

                    if (Variables.isGenericIndexed(var)) {
                        Schemiterator iteratorForVar = effectives.getInputIterator(var)
                        if (iteratorForVar == null) {
                            // it is an indexed variable and has no bound values, so the "expansion"
                            // of it is to remove it from the array or use a blank value otherwise
                            String abandoned = Variables.adorn(var)
                            todos.add({ abandonVariable(model, abandoned) })
                        }
                    }
                    else {
                        if (inValues.containsKey(var)) {
                            foundInputVars.add(var)
                            replaced = this.replace(Variables.adorn(var), inValues[var], replaced)
                        }
                        else {
                            danglingOutputVars.add(var)
                        }
                    }
                }

                if (replaced != schemaValue) {
                    if (obj instanceof Map.Entry)
                        ((Map.Entry)obj).setValue(replaced)
                    else if (model instanceof List)
                        ((ArrayList)model).set(i, replaced)
                }
            }
        }

        if (doPushPop) {
            if (isList(model)) {
                List marked = (List) model
                Schemiterator iterations = effectives.getOutputIterator(marked)
                if (iterations.effectiveDimensions() > 0) {

                    // We need to (later) remove the original generic members that are acting as prototypes

                    marked.each {
                        todos.add({
                            marked.remove(0)
                        })
                    }

                    // Figure out how many effective loops are required to be done to the set of prototypes

                    while (!iterations.isDone()) {
                        Map<String, String> specificVars = iterations.replaceables()
                        marked.each { member ->
                            // the indexed variable is a sub-member of an array of objects or an array of arrays

                            if (isCollection(member)) {
                                def cloned = deepCopy(member)
                                recursivelyReplace(cloned, specificVars)

                                // now clone chunk should no longer have any generic variables at this level - all
                                // should be specifically indexed. there may be generic variables in children

                                this.walkTheModel(inValues, effectives, cloned, parentCollections, danglingOutputVars,
                                        foundInputVars, todos)

                                todos.add({
                                    marked.add(cloned)
                                })
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

                                // newly create value should no longer have any generic indices - all should be specific

                                todos.add({
                                    marked.add(specific);
                                })

                                // TODO: look into this as inefficient (walking once for each array member rather than the array itself
                                // convert specific variable name to final value from bindings

                                todos.add({
                                    this.walkTheModel(inValues, effectives, marked, parentCollections, danglingOutputVars,
                                            foundInputVars, todos)
                                })
                            }
                        }

                        iterations.increment()
                    }
                }
            }

            parentCollections.pop()
        }
    }

    private Object deepCopy(Object object) {
        if (object instanceof Map) {
            Map result = [:]
            ((Map)object).each { k,v -> result[k] = deepCopy(v) }
            return result
        }
        if (object instanceof List) {
            List result = []
            ((List)object).each { v -> result.add(deepCopy(v)) }
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

    void setListOrMapValue(Object mapOrList, Object keyOrIndex, Object value) {
        if (mapOrList instanceof List)
            ((List)mapOrList)[(int)keyOrIndex] = value
        else if (mapOrList instanceof Map)
            ((Map)mapOrList)[keyOrIndex] = value
        else
            throw new UnexpectedParentCollectionTypeException(mapOrList)
    }

    static def asValue(Object obj) {
        def value

        if (obj instanceof Map.Entry) {
            value = ((Map.Entry)obj).getValue()
        }

        if (obj instanceof String || obj instanceof GString || obj instanceof Number || obj instanceof Boolean ) {
            value = obj.toString()
        }

        value
    }

    private Object replace(String varName, Object varValue, Object output) {

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
}
