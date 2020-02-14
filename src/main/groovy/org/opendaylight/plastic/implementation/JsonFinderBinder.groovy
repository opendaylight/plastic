
/*
 * Copyright (c) 2019-2020 Lumina Networks, Inc. All rights reserved.
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

    static class NonArrayedContext extends PlasticException {
        String offender

        NonArrayedContext(String offendingVarName) {
            super("PLASTIC-NO-ARRAY-CONTEXT", "The following arrayed variable was used without enough surrounding arrays: $offendingVarName")
            this.offender = offendingVarName
        }
    }

    private static final Pattern LONEDOT_REGEX = ~/(?<!\\)\./

    Bindings process(Object model, Object payload) {
        Preconditions.checkNotNull(model)
        Preconditions.checkNotNull(payload)

        Map<String,VariablesFetcher> pathVars = [:] // path associated to binder
        Map<String,Object> defaults = [:] // variable name to variable value
        buildPathsToVariables(model, pathVars, defaults)

        Map<String,Object> boundVars = fetchVarToValues(pathVars, payload)

        Bindings bindings = new Bindings(boundVars)
        bindings.applyDefaults(defaults)
        bindings
    }

    void buildPathsToVariables(Object model, Map<String,VariablesFetcher> seenPaths, Map<String,Object> seenVars) {
        if (model instanceof Map) {
            pathsFromMap(model, seenPaths, seenVars, "")
        } else if (model instanceof List) {
            pathsFromList(model, seenPaths, seenVars, "[]")
        } else {
            throw new UnsupportedModel("Given in-memory model is not a recognized type", model)
        }
    }

    private void pathsFromMap(Map<String,Object> map, Map<String,VariablesFetcher> seenPaths, Map<String,Object> seenVars, String path) {
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

    private void pathsFromList(List<Object> list, Map<String,VariablesFetcher> seenPaths, Map<String,Object> seenVars, String path) {
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

    private void pathsFromLeaf(String fullPath, String value, Map<String,VariablesFetcher> seenPaths, Map<String,Object> seenVars, String path) {
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
                seenPaths.put(fullPath, new SimpleVariableFetcher(vars.names()))
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

    // TODO: remove unused class

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
        private VariablesFetcher fetcher

        private Stack<Long> activeIndices = new Stack<>()
        private Stack<Map<String,Schemiterator>> activeIterators = new Stack<>()
        private Map<String,Schemiterator> highWaterMarks = new HashMap<>()

        void useFetcher(VariablesFetcher fetcher) {
            this.fetcher = fetcher
        }

        void recordFind(Object value) {
            // fetcher: ADDR[^][*]  value: 1.2.3.4
            Bindings results = fetcher.fetch(nullPrimitiveOrCollection(value))
            for (String varName : results.bindings().keySet()) {
                String newKey = assignFromIndices(varName)
                boundVars[newKey] = results.get(varName)
            }
        }

        String assignFromIndices(String varName) {
            if (Variables.isGenericIndexed(varName)) {
                Schemiterator myIterator = ensureIterator(varName)
                myIterator.setCurrentFromIndices(activeIndices)
                return myIterator.adornedValue()
            }
            else {
                return varName
            }
        }

        Schemiterator ensureIterator(String varName) {
            if (activeIterators.isEmpty())
                throw new NonArrayedContext(varName)

            Map<String, Schemiterator> tos = activeIterators.peek()
            if (tos.containsKey(varName))
                return tos.get(varName)
            else {
                Schemiterator result = new Schemiterator(varName)
                tos.put(varName, result)
                return result
            }
        }

        void unUseFetcher() {
            for (String varName : fetcher.names()) {
                if (!highWaterMarks.containsKey(varName)) {

                    // We hit a variable that was defined in the input schema, but was never found in the
                    // payload. We want to make an entry for that name with a null value so it will not be
                    // ignored but will be seen as having no value.
                    //
                    // But we do NOT want this behavior for arrayed variables because they can be bound to
                    // empty arrays, so aren't found by definition and do not want them seen as missing.

                    if (!Variables.isGenericIndexed(varName) && !boundVars.containsKey(varName)) {
                        recordFind(null)
                    }
                }
                else {
                    Schemiterator iterator = highWaterMarks.get(varName)
                    iterator.writeSpecTo(boundVars)
                }
            }

            fetcher = null
        }

        void pushIteration() {
            activeIndices.push(0L)
            activeIterators.push(new HashMap<>());
        }

        void popIteration() {
            if (!activeIterators.isEmpty()) {
                for (Map.Entry<String,Schemiterator> entry : activeIterators.peek().entrySet()) {
                    mergeIntoHighwater (entry.key, entry.value)
                }
                activeIterators.pop()
            }

            if (!activeIndices.isEmpty())
                activeIndices.pop()
        }

        private void mergeIntoHighwater(String varName, Schemiterator iterator) {
            iterator.rerangeUsingCurrent()
            if (highWaterMarks.containsKey(varName)) {
                Schemiterator cousin = highWaterMarks.get(varName)
                highWaterMarks.put(varName, cousin.mergeWith(iterator))
            }
            else
                highWaterMarks.put(varName, iterator)
        }

        void incrementIteration() {
            if (!activeIndices.isEmpty())
                activeIndices.push(activeIndices.pop()+1)
        }

        Map<String,Object> bindings() {
            boundVars
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
    Map<String,Object> fetchVarToValues(Map<String,VariablesFetcher> pathVars, Object payload) {

        Recorder recorder = new Recorder()

        pathVars.each { String path, VariablesFetcher binder ->
            recorder.useFetcher(binder)
            getPathValue(path, payload, recorder)
            recorder.unUseFetcher()
        }

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

    private void getElementValue(List<String> remainingPath, Object element, Recorder ifoundit) {

        if (remainingPath.size() == 0) {
            ifoundit.recordFind(element)
            return
        }

        String firstTerm = remainingPath[0]
        List<String> remainingTerms = remainingPath.drop(1)
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
            ifoundit.pushIteration()
            int len = nextElements.size()
            for (int i = 0; i< len; i++) {
                Object nElement = nextElements[i]
                getElementValue(remainingTerms, nElement, ifoundit)
                if (i != len-1)
                    ifoundit.incrementIteration()
            }
            ifoundit.popIteration()
        }
        else {
            for (Object nElement : nextElements) {
                getElementValue(remainingTerms, nElement, ifoundit)
            }
        }
    }
}
