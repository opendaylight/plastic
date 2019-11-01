
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

    private static final Pattern LONEDOT_REGEX = ~/(?<!\\)\./

    Bindings process(Object model, Object payload) {
        Preconditions.checkNotNull(model)
        Preconditions.checkNotNull(payload)

        Map<String,String> varPaths = [:]
        Map<String,Object> defaults = [:]
        buildVariablesToPaths(model, varPaths, defaults)
        Map<String,Object> boundVars = fetchVarToValues(varPaths, payload)

        // TODO: seems like this rummaging around can be moved into Bindings
        Bindings bindings = new Bindings(boundVars)
        boundVars.each { String k, Object v ->
            if (v == null) {
                boundVars[k] = defaults[k]
                if (boundVars[k] == null) {
                    Variables var = new Variables()
                    String generic = var.generifyIndex(k)
                    boundVars[k] = defaults[generic]
                }
                if (boundVars[k] != null)
                    bindings.defaultWasUsed(k)
            }
        }
        bindings
    }

    void buildVariablesToPaths(Object model, Map<String,String> seenPaths, Map<String,Object> seenVars) {
        if (model instanceof Map) {
            pathsFromMap(model, seenPaths, seenVars, "")
        } else if (model instanceof List) {
            pathsFromList(model, seenPaths, seenVars, "[]")
        } else {
            throw new UnsupportedModel("Model given is not a recognized type", model)
        }
    }

    private void pathsFromMap(Map<String,Object> map, Map<String,String> seenPaths, Map<String,Object> seenVars, String path) {
        map.each { String key, Object value ->
            if (value instanceof List) {
                pathsFromList((List<Object>)value, seenPaths, seenVars, concatPath(path, protect(key), "[]"))
            }
            else if (value instanceof Map) {
                pathsFromMap((Map<String,Object>)value, seenPaths, seenVars, concatPath(path, protect(key)))
            }
            else if (value instanceof String || value instanceof GString) {
                Variables vars = new Variables((String)value)
                vars.toEach { String var, Object val ->
                    seenPaths[(String)var] = concatPath(path, protect(key))
                    seenVars[(String)var] = val
                }
            }
        }
    }

    private void pathsFromList(List<Object> list, Map<String,String> seenPaths, Map<String,Object> seenVars, String path) {
        list.each { entry ->
            if (entry instanceof Map) {
                pathsFromMap((Map<String,Object>)entry, seenPaths, seenVars, "$path")
            }
            else if (entry instanceof List) {
                pathsFromList((List<Object>)entry, seenPaths, seenVars, concatPath(path, "[]"))
            }
            else if (entry instanceof String || entry instanceof GString) {
                Variables vars = new Variables((String)entry)
                vars.toEach { String var, Object val ->
                    seenPaths[var] = path
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
        StringBuffer buffer = new StringBuffer()
        for (String term : terms) {
            if (term != null && !term.isEmpty()) {
                if (buffer.length() > 0)
                    buffer.append('.')
                buffer.append(term)
            }
        }
        buffer.toString()
    }

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
        private String currentKey
        private int currentKeyCount
        private Map<String,Integer> sizes = [:]

        void useKey(String key) {
            this.currentKey = key
            this.currentKeyCount = 0
        }

        void recordFind(Object value) {
            if (currentKey != null) {
                int count = seenVarCounts.get(currentKey)
                String newKey = currentKey.replace("[*]", "[${count}]")
                boundVars[newKey] = nullPrimitiveOrCollection(value)
                seenVarCounts.increment(currentKey)
                currentKeyCount++
                if (currentKey != newKey)
                    sizes[currentKey] = count+1
            }
        }

        void unUseKey() {
            if (currentKey != null) {
                if (currentKeyCount == 0) {
                    // An arrayed variable will have a key like "abc[*]" and if there is no dimension to the
                    // array (which is fine), it won't be found. We don't want to create a fake entry for [0]
                    // so protect against that.
                    Variables vars = new Variables()
                    if (!vars.isGenericIndexed(currentKey)) {
                        recordFind(null)
                    }
                }

                currentKey = null
                currentKeyCount = 0
            }
        }

        Map<String,Object> bindings() {
            boundVars
        }

        void validateConsistentArrayed() {
            if (!hasUniformValues(sizes)) {
                logger.debug("CART-ARRAY-SIZES: the asterisk was bound to arrays of different sizes: " +  sizes.toString())
            }
        }

        private boolean hasUniformValues(Map target) {
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

        // Explicitly converting scalar values to strings below to avoid automatic
        // representation of large integers as fixed point exponential format or
        // numeric 0 eventually being an empty string.
        //
        // This is also done in VersionedSchemaParsed.asDefaults()

        private Object nullPrimitiveOrCollection(Object value) {
            if (value == null)
                return value
            // Hotspot: used to use switch statement
            if (value.class == Integer)
                return value
            if (value.class == Long)
                return value
            if (value.class == Short)
                return value
            if (value.class == Float)
                return value
            if (value.class == Double)
                return value
            if (value.class == BigDecimal)
                return value
            if (value.class == Byte)
                return value
            if (value.class == Boolean)
                return value
            if (value.class == Character)
                return value
            if (value instanceof List)
                return value
            if (value instanceof Map)
                return value

            // Hotspot: used to use ""+value
            value.toString()
        }
    }

    @PackageScope
    Map<String,Object> fetchVarToValues(Map<String,String> varPaths, Object payload) {

        Recorder recorder = new Recorder()

        varPaths.each { String key, String path ->
            recorder.useKey(key)
            getPathValue(path, payload, recorder)
            recorder.unUseKey()
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
