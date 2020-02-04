
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
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
        Map<String,Schemiterator> activeIterators = new HashMap<>()

        Triple(Object model) {
            this.model = model
        }
        List modelAsList() {
            model instanceof List ? (List)model : (List)null
        }

        void add(Schemiterator iterator) {
            activeIterators.put(iterator.fullName(), iterator)
        }

        Schemiterator iterator() {
            Schemiterator result = null
            for (Schemiterator iterator : activeIterators.values()) {
                if (result == null)
                    result = iterator
                else
                    result = result.mergeWith(iterator)
            }
            result
        }
    }

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
            parentCollections.isEmpty() ? null : parentCollections[-1]
        }

        def markForExpansion(Schemiterator iterator) {
            for (int i = parentCollections.size()-1; i >= 0; i--) {
                if (parentCollections[i].model instanceof List) {
                    Triple triple = parentCollections[i]
                    triple.add(iterator)
                    return
                }
            }

            throw new IndexedButNoContainingArrayException(iterator.fullName())
        }

        boolean isTosMarkedForExpansion() {
            !parentCollections.isEmpty() && !parentCollections[-1].activeIterators.isEmpty()
        }
    }

    static class TodoList {

        List<Closure> todos = []

        TodoList add(Closure cl) {
            todos.add(cl)
            return this
        }

        TodoList doAll() {
            todos.each { it() }
            return this
        }

        TodoList clear() {
            todos.clear()
            return this
        }
    }

    Object inject(Map values, Object model, Set danglingInputs, Set danglingOutputs) {

        Set<String> expectedInputVars = (Set<String>) values.keySet()
        ParentContainers parentCollections = new ParentContainers()
        TodoList todos = new TodoList()
        Set<String> foundInputVars = [] as Set

        Map<String,Schemiterator> iterators = Schemiterator.instantiateIterators(values)

        // some classifiers are doing multi-level child translations so that the model is truncated
        // by markers until the variable is actually injected. so walking cannot get to those
        // parts of the model yet. so we loop until we aren't making progress or we hit the finish
        // line.

        final int MAXDEPTH = 5 // do we have the guts to just make this infinite?

        for (int passes = 0; passes < MAXDEPTH; passes++) {
            int preFound = foundInputVars.size()

            walkTheModel(values, iterators, model, parentCollections, danglingOutputs, foundInputVars, todos)

            // Have to actually modify the model after the traversal, otherwise
            // you get a concurrent modification exception (you can't iterate and
            // modify at the same time)

            todos.doAll().clear()

            int postFound = foundInputVars.size()

            // This test for termination is not precise. If the morpher logic is ignoring some inputs
            // then the found values will never equal the given input values. This means we could do
            // one extra pass.

            if (postFound == preFound || postFound == values.size()) {
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

    private def walkTheModel(Map inValues, Map<String,Schemiterator> iterators, Object model,
                             ParentContainers parentCollections,
                             Set danglingOutputVars, Set foundInputVars,
                             TodoList todos) {

        boolean  doPushPop = isCollection(model)

        if (doPushPop)
            parentCollections.push(model)

        model.eachWithIndex{ obj, int i ->
            if (isCollection(obj)) {
                this.walkTheModel(inValues, iterators, obj, parentCollections, danglingOutputVars, foundInputVars, todos)
                return
            }

            def schemaValue = value(obj)

            if (isCollection(schemaValue)) {
                this.walkTheModel(inValues, iterators, schemaValue, parentCollections, danglingOutputVars, foundInputVars, todos)
                return
            } else {
                Variables vars = new Variables(schemaValue.toString())
                if (vars.isPresent()) {
                    def replaced = schemaValue
                    vars.toEach { String var, String val ->

                        if (Variables.isGenericIndexed(var)) {
                            Schemiterator iteratorForVar = iterators.get(var)
                            if (iteratorForVar != null) {
                                parentCollections.markForExpansion(iteratorForVar)
                            }
                            else {
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
        }

        if (doPushPop) {
            if (parentCollections.isTosMarkedForExpansion()) {
                Triple tos = parentCollections.peek()
                List marked = tos.modelAsList()

                // We need to (later) remove the original generic members that are acting as prototypes

                marked.each {
                    todos.add({
                        marked.remove(0)
                    })
                }

                // Figure out how many effective loops are required to be done to the set of prototypes

                Schemiterator iterations = tos.iterator()

                while (!iterations.isDone()) {
                    Map<String,String> specificVars = iterations.replaceables()
                    marked.each { member ->
                        // the indexed variable is a sub-member of an array of objects or an array of arrays

                        if (isCollection(member)) {
                            def cloned = deepCopy(member)
                            recursivelyReplace(cloned, specificVars)
                            todos.add({
                                this.walkTheModel(inValues, iterators, cloned, parentCollections,
                                        danglingOutputVars, foundInputVars, todos)
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

                            for (Map.Entry<String,String> entry : specificVars) {
                                specific = replace(entry.key, entry.value, specific)
                            }

                            todos.add({
                                marked.add(specific);
                            })

                            // convert specific variable name to final value from bindings

                            todos.add({
                                this.walkTheModel(inValues, iterators, marked, parentCollections, danglingOutputVars,
                                        foundInputVars, todos)
                            })
                        }
                    }

                    iterations.increment()
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
            def val = value(model)
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

    def value(Object obj) {
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
