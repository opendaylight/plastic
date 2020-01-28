
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.base.Preconditions
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Pattern

@CompileStatic
class JsonFinderBinder {

    static Logger logger = LoggerFactory.getLogger(JsonFinderBinder)

    static class UnsupportedModel extends PlasticException {
        Object model

        UnsupportedModel(String msg, model) {
            super("PLASTIC-UNSUPPORTED-MODEL", msg)
            this.model = model
        }
    }

    static class NoMultipleVariables extends PlasticException {
        String offender

        NoMultipleVariables(String candidate) {
            super("PLASTIC-MULT-IN-VARS", candidate)
            this.offender = candidate
        }
    }

    private static final Pattern LONEDOT_REGEX = ~/(?<!\\)\./

    Bindings process(Object model, Object payload) {
        Preconditions.checkNotNull(model)
        Preconditions.checkNotNull(payload)

        Map<String,VariablesBinder> pathVars = [:] // path associated to binder
        Map<String,Object> defaults = [:] // variable name to variable value
        buildPathsToVariables(model, pathVars, defaults)

        Map<String,Object> boundVars = fetchVarToValues(pathVars, payload)

        Bindings bindings = new Bindings(boundVars)
        bindings.applyDefaults(defaults)
        bindings
    }

    void buildPathsToVariables(Object model, Map<String,VariablesBinder> seenPaths, Map<String,Object> seenVars) {
        if (model instanceof Map) {
            pathsFromMap(model, seenPaths, seenVars, "")
        } else if (model instanceof List) {
            pathsFromList(model, seenPaths, seenVars, "[]")
        } else {
            throw new UnsupportedModel("Given in-memory model is not a recognized type", model)
        }
    }

    private void pathsFromMap(Map<String,Object> map, Map<String,VariablesBinder> seenPaths, Map<String,Object> seenVars, String path) {
        map.each { String key, Object value ->
            if (value instanceof List) {
                pathsFromList((List<Object>)value, seenPaths, seenVars, concatPath(path, protect(key), "[]"))
            }
            else if (value instanceof Map) {
                pathsFromMap((Map<String,Object>)value, seenPaths, seenVars, concatPath(path, protect(key)))
            }
            else if (value instanceof String || value instanceof GString) {
                String fullpath = concatPath(path, protect(key))
                pathsFromLeaf(fullpath, (String) value, seenPaths, seenVars, path)
            }
        }
    }

    private void pathsFromList(List<Object> list, Map<String,VariablesBinder> seenPaths, Map<String,Object> seenVars, String path) {
        list.each { entry ->
            if (entry instanceof Map) {
                pathsFromMap((Map<String,Object>)entry, seenPaths, seenVars, "$path")
            }
            else if (entry instanceof List) {
                pathsFromList((List<Object>)entry, seenPaths, seenVars, concatPath(path, "[]"))
            }
            else if (entry instanceof String || entry instanceof GString) {
                pathsFromLeaf(path, (String) entry, seenPaths, seenVars, path)
            }
        }
    }

    private void pathsFromLeaf(String fullPath, String value, Map<String,VariablesBinder> seenPaths, Map<String,Object> seenVars, String path) {
        if (WildCardMatcher.usesWildcarding(value)) {
            WildCardMatcher wild = new WildCardMatcher(value)
            seenPaths.put(fullPath, wild)
            wild.variables.toEach { String var, Object val ->
                seenVars[var] = val
            }
        }
        else {
            Variables vars = new Variables(value)
            if (vars.hasMultiple())
                logger.warn("PLASTIC-MULT-IN-VARS: {}", vars.raw())

            if (vars.isPresent()) {
                seenPaths.put(fullPath, new SimpleVariableBinder(vars.names()))
                vars.toEach { String var, Object val ->
                    seenVars[var] = val
                }
            }
        }
    }

    private String protect(String term) {
        term.contains('.') ? term.replace(".", "\\.") : term
    }

    @PackageScope
    String concatPath(String... terms) {
        // Hotspot: used to use terms.grep { it }.join(".")
        StringBuilder buffer = new StringBuilder()
        for (String term : terms) {
            if (term != null && !term.isEmpty()) {
                if (buffer.length() > 0)
                    buffer.append('.')
                buffer.append(term)
            }
        }
        buffer.toString()
    }

    // For performance, avoid Integer boxing

    static class NamedCounters {
        static class Counter {
            int count = 0
        }

        Map<String,Counter> counters = new HashMap<>()

        private void ensure(String key) {
            if (!counters.containsKey(key))
                counters[key] = new Counter()
        }

        private void increment(String key) {
            ensure(key)
            counters[key].count += 1
        }

        private int get(String key) {
            ensure(key)
            counters[key].count
        }
    }

    static class Recorder {

        private Map<String,Object> boundVars = [:]
        private NamedCounters seenVarCounts = new NamedCounters()
        private List<String> currentKeys = []
        private Map<String,Integer> sizes = [:]
        private VariablesBinder binder

        void useBinder(VariablesBinder binder) {
            this.binder = binder
            this.currentKeys = binder.names()
        }

        void recordFind(Object value) {
            Bindings results = binder.fetch(nullPrimitiveOrCollection(value))
            for (String currentKey : currentKeys) {
                int count = seenVarCounts.get(currentKey)
                String newKey = currentKey.replace("[*]", "[${count}]")
                boundVars[newKey] = results.get(currentKey)
                seenVarCounts.increment(currentKey)
                if (currentKey != newKey)
                    sizes[currentKey] = count+1
            }
        }

        void unUseBinder() {
            Variables vars = new Variables()
            for (String currentKey : currentKeys) {
                if (seenVarCounts.get(currentKey) == 0) {
                    // An arrayed variable will have a key like "abc[*]" and if there is no dimension to the
                    // array (which is fine), it won't be found. We don't want to create a fake entry for [0]
                    // so protect against that.
                    if (!vars.isGenericIndexed(currentKey)) {
                        recordFind(null)
                    }
                }
            }

            binder = null
            currentKeys.clear()
        }

        Map<String,Object> bindings() {
            boundVars
        }

        void validateConsistentArrayed() {
            if (!hasUniformValues(sizes)) {
                logger.debug("PLASTIC-ARRAY-SIZES: fyi the asterisk was bound to arrays of different sizes: " +  sizes.toString())
            }
        }

        boolean hasUniformValues(Map target) {
            Iterator<Map.Entry> iterator = target.entrySet().iterator()
            Object firstValue = iterator.hasNext() ? iterator.next()?.getValue() : null
            Object different = target.find {
                if (it.value == null && firstValue != null)
                    true
                else if (it.value != null && firstValue == null)
                    true
                else if (it.value == null && firstValue == null)
                    false
                else
                    !it.value.equals(firstValue)
            }
            different == null
        }

        boolean hasUniformValues() {
            hasUniformValues(sizes)
        }

        // Explicitly converting scalar values to strings below to avoid automatic
        // representation of large integers as fixed point exponential format or
        // numeric 0 eventually being an empty string.
        //
        private static Object nullPrimitiveOrCollection(Object value) {
            if (value == null)
                return value

            // Hotspot: used to use switch statement
            // For speed, ordered by likelihood of match

            if (value.class == String)
                return value
            if (value.class == GString)
                return value
            if (value.class == Integer)
                return value
            if (value.class == Long)
                return value
            if (value.class == BigDecimal)
                return value
            if (value instanceof List)
                return value
            if (value instanceof Map)
                return value
            if (value.class == Double)
                return value
            if (value.class == Boolean)
                return value
            if (value.class == Short)
                return value
            if (value.class == Float)
                return value
            if (value.class == Byte)
                return value
            if (value.class == Character)
                return value

            // Hotspot: used to use ""+value
            value.toString()
        }
    }

    @PackageScope
    Map<String,Object> fetchVarToValues(Map<String,VariablesBinder> pathVars, Object payload) {

        Recorder recorder = new Recorder()

        pathVars.each { String path, VariablesBinder binder ->
            recorder.useBinder(binder)
            getPathValue(path, payload, recorder)
            recorder.unUseBinder()
        }

        recorder.validateConsistentArrayed()
        recorder.bindings()
    }

    @PackageScope
    void getPathValue(String path, Object payload, Recorder ifoundit) {
        List<String> terms
        if (path.contains("\\."))
            terms = LONEDOT_REGEX.split(path, -2).collect { String s -> s.replace("\\.", ".") }
        else
            terms = path.tokenize('.')
        getElementValue(terms, payload, ifoundit)
    }

    private boolean getElementValue(List<String> terms, Object element, Recorder ifoundit) {

        if (terms.size() == 0) {
            ifoundit.recordFind(element)
            return true
        }

        String firstTerm = terms[0]
        List<String> remainingTerms = terms.drop(1)
        List<Object> nextElements = []

        if (element instanceof Map) {
            nextElements.add(element[firstTerm])
        } else if (element instanceof List) {
            element.each {
                if (it instanceof Map) {
                    nextElements.add(it)
                }
                else if (it instanceof List) {
                    nextElements.add(it)
                }
                else {
                    nextElements.add(it)
                }
            }
        }

        if (firstTerm == '[]') {
            boolean found = false
            for (Object nElement : nextElements) {
                if (getElementValue(remainingTerms, nElement, ifoundit))
                    found = true
            }
            return found
        }
        else {
            for (Object nElement : nextElements) {
                if (getElementValue(remainingTerms, nElement, ifoundit))
                    return true
            }
            return false
        }
    }
}
