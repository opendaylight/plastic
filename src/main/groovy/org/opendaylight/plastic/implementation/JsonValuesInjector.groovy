
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.collect.ImmutableList
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
            super("PLASTIC-WHAT_PAR-TYPE", "The following should have been a list or map but wasn't: "+parent)
        }
    }

    static class Triple {

        Object model
        String genericVar
        List<String> matchedVars

        Triple(Object model, String generic, List<String> matches) {
            this.model = model
            this.genericVar = generic
            this.matchedVars = matches
        }
        List modelAsList() {
            model instanceof List ? (List)model : (List)null
        }

        List<String> indices() {
            Variables vars = new Variables(matchedVars)
            vars.extractIndices()
        }
    }

    static class JsonParentContainers {

        static final String notGeneric = null
        static final List<String> noMatches = ImmutableList.of()

        Stack<Triple> parentCollections = new Stack<>()

        def push(Object model) {
            // Hotspot: used to use new Triple(model, null, [])
            parentCollections.push (new Triple(model, notGeneric, noMatches))
        }

        def pop() {
            parentCollections.pop()
        }

        Triple peek() {
            parentCollections.isEmpty() ? null : parentCollections[-1]
        }

        def markForExpansion(String genericVar, List<String> matched) {
            for (int i = parentCollections.size()-1; i >= 0; i--) {
                if (parentCollections[i].model instanceof List) {
                    Triple triple = parentCollections[i]
                    triple.genericVar = genericVar
                    triple.matchedVars = matched
                    return
                }
            }

            throw new IndexedButNoContainingArrayException(genericVar)
        }

        boolean isTosMarkedForExpansion() {
            !parentCollections.isEmpty() && parentCollections[-1].genericVar != null
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

    def inject(Map values, Object model, Set danglingInputs, Set danglingOutputs) {

        def expectedInputVars = values.keySet() as Set
        def parentCollections = new JsonParentContainers()
        def todos = new TodoList()
        def foundInputVars = [] as Set

        // some classifiers are doing multi-level child translations so that the model is truncated
        // by markers until the variable is actually injected. so walking cannot get to those
        // parts of the model yet. so we loop until we aren't making progress or we hit the finish
        // line.

        final int MAXDEPTH = 5 // do we have the guts to just make this infinite?

        for (int passes = 0; passes < MAXDEPTH; passes++) {
            int preFound = foundInputVars.size()

            walkTheModel(values, model, parentCollections, danglingOutputs, foundInputVars, todos)

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
            if (!foundInputVars.contains(expected))
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

    private def walkTheModel(Map inValues, Object model,
                             JsonParentContainers parentCollections,
                             Set danglingOutputVars, Set foundInputVars,
                             TodoList todos) {

        boolean  doPushPop = isCollection(model)

        if (doPushPop)
            parentCollections.push(model)

        model.eachWithIndex{ obj, int i ->
            if (isCollection(obj)) {
                this.walkTheModel(inValues, obj, parentCollections, danglingOutputVars, foundInputVars, todos)
                return
            }

            def schemaValue = value(obj)

            if (isCollection(schemaValue)) {
                this.walkTheModel(inValues, schemaValue, parentCollections, danglingOutputVars, foundInputVars, todos)
                return
            } else {
                Variables vars = new Variables(schemaValue.toString())
                if (vars.isPresent()) {
                    def replaced = schemaValue
                    vars.toEach { String var, String val ->
                        def matchedVars = vars.matches(inValues, var)
                        if (!matchedVars.isEmpty()) {
                            if (vars.isGenericIndexed(var)) {
                                parentCollections.markForExpansion(var, matchedVars)
                            }
                            else {
                                foundInputVars.add(var)
                                replaced = this.replace(vars.adornPattern(var), inValues[var], replaced)
                            }
                        }
                        else if (vars.isGenericIndexed(var)) {
                            // it is an indexed variable and has no bound values, so the "expansion"
                            // of it is to remove it from the array or use a blank value otherwise
                            String abandoned = vars.adorn(var)
                            todos.add({ abandonVariable(model, abandoned) })
                        }
                        else {
                            danglingOutputVars.add(var)
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
                Variables vars = new Variables("")

                Triple tos = parentCollections.peek()
                List marked = tos.modelAsList()

                marked.each {
                    todos.add({
                        marked.remove(0)
                    })
                }

                marked.each { member ->

                    // this indexed variable is a sub-member of an array of objects
                    // (should not be an array of arrays)

                    if (isCollection(member)) {
                        List matchedIndices = tos.indices()
                        matchedIndices.each { String index ->
                            def cloned = deepCopy(member)
                            recursivelyReplace(cloned,
                                    vars.genericIndexPattern(),
                                    index)
                            todos.add({
                                this.walkTheModel(inValues, cloned, parentCollections, danglingOutputVars,
                                        foundInputVars, todos)
                                marked.add(cloned)
                            })
                        }
                    }

                    // this indexed variable is a member of an array of scalars, so
                    // add de-indexed variables the end of array then walk the
                    // array again (to handle multiple variables in a single slot

                    else {
                        String templateValue = member.toString()
                        String genericSrc = tos.genericVar

                        List<String> matched = tos.matchedVars
                        matched.each { String varName ->
                            todos.add({
                                String specific = templateValue.replace(genericSrc, varName)
                                marked.add(specific);
                            })
                        }
                        todos.add({
                            this.walkTheModel(inValues, marked, parentCollections, danglingOutputVars,
                                    foundInputVars, todos)
                        })
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

    def recursivelyReplace(Object model, String from, String to) {
        if(!isCollection(model))
            throw new CollectionExpectedException(model)
        doRecursivelyReplace(null, null, model, from, to)
    }

    private def doRecursivelyReplace(Object parentMapOrList, Object keyOrIndex, Object model, String from, String to) {
        if (model instanceof List) {
            ((List)model).eachWithIndex { obj,i ->
                doRecursivelyReplace(model, i, obj, from, to)
            }
        }
        else if (model instanceof Map) {
            ((Map)model).each { k,v ->
                doRecursivelyReplace(model, k, v, from, to)
            }
        }
        else {
            def val = value(model)
            def replacement = replace(from, to, val)
            setListOrMapValue(parentMapOrList, keyOrIndex, replacement)
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

    // pattern is the regex version of the variable
    // value is the replacement value
    // output is the original that needs replacing

    private def replace(String pattern, value, output) {
        if (value == null)
            return ""

        if (output instanceof String || output instanceof GString) {

            if (isCollection(value))
                return value

            def replacementValue = (value instanceof Boolean) ? Boolean.toString(value) : value.toString()

            // This little bit of WTF gets around replaceAll's inability
            // to deal with the replacement value (which is not a regex)
            // containing $ characters (it thinks it is some kind of regex
            // register substitution). Fix is to replace $ with \$

            // String replace is the possible hotspot based on profiling
            String wtf = replacementValue.replace("\$", "\\\$")
            output = ((String)output).replaceAll(pattern, wtf)

            if (output == "${value}")
                // return native type in return model as it is the complete value
                // * a concession to retaining JSON types; this uses the incoming type
                //   as the desired outgoing type since the outgoing 'type' must be a
                //   string to hold the variable and still be parsable JSON
                return value
        }

        return output
    }
}
