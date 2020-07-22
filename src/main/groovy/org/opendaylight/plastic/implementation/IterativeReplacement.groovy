
/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

/**
 * Class is responsible for recursing/iterating through the model looking for variables to replace values
 * taking pre-discovered variable bindings
 *
 * The resulting model IS MODIFIED IN PLACE with variable values substituted, like ${ABC} becoming 123
 *
 * This is memory model (lists and maps) specific and would need abstraction to deal with
 * other memory models like XML nodes
 *
 */
package org.opendaylight.plastic.implementation

import groovy.transform.CompileStatic

@CompileStatic
class IterativeReplacement {

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

    Map<String,Object> bindings
    Set<String> danglingOutputs
    Set<String> foundInputs


    IterativeReplacement(Map<String,Object> bindings, Set<String> danglingOutputs, Set<String> foundInputs) {
        this.bindings = bindings
        this.danglingOutputs = danglingOutputs
        this.foundInputs = foundInputs
    }

    void processModel(Object model) {
        if(!isCollection(model))
            throw new CollectionExpectedException(model)
        doRecursivelyReplace(null, null, model, bindings)
    }

    private def doRecursivelyReplace (Object parentMapOrList, Object keyOrIndex, Object model, Map<String,Object> fromTo) {
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

            // The target of the replacement might be a string (or even some other object)
            // If it is a string, the might have zero or more variables within it.

            if (isString(val)) {
                String strVal = (String) val

                // Avoid overhead and optimize for the single variable-as-value

                String varName = Variables.unadorn(strVal)
                if (fromTo.containsKey(varName)) {
                    val = replace(varName, fromTo.get(varName), varName)
                    foundInputs.add(varName)
                }
                else {
                    Variables vars = new Variables(strVal)
                    for (String vName : vars.names()) {
                        // Pick up any missing value from default schema! First case of not using it from input!
                        String value = fromTo.containsKey(vName) ? fromTo.get(vName) : vars.getValue(vName)

                        boolean dangling = (value == null)
                        value = dangling ? "" : value

                        // We need to leave the locating of the raw presence of the variable to the
                        // Variables class because of things like default value syntax.

                        String raw = Variables.substring(vName, strVal)
                        val = replace(raw, value, val)

                        if (dangling)
                            danglingOutputs.add(vName)
                        else
                            foundInputs.add(vName)
                    }
                }
            }

            setListOrMapValue(parentMapOrList, keyOrIndex, val)
        }
    }

    private static boolean isCollection(Object model) {
        (model instanceof Map) || (model instanceof List)
    }

    private static boolean isString(Object model) {
        (model instanceof String) || (model instanceof GString)
    }

    // TODO: some code duplication with IteratorExpansion

    static private Object replace(String varName, Object varValue, Object output) {

        // Not sure this really makes sense. Shouldn't we just treat the replacement value
        // as empty and continue? Needs looking at.

        if (varValue == null)
            return ""

        // Trying to replace a portion of something with a collection doesn't make sense, so just
        // return the collection (ie, a full replacement, which does make sense)

        if (isCollection(varValue))
            return varValue

        if (isString(output)) {
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
        (obj instanceof Map.Entry) ? ((Map.Entry)obj).getValue() : obj
    }
}

