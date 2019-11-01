
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

import java.util.regex.Pattern

@CompileStatic
class Variables {

    void toEach(Closure cs) {
        foundNames.each { Object k, Object v -> cs(k,v) }
    }

    private static final Pattern envelopePattern = ~/\$\{(.+?)\}/
    private static final Pattern indexPattern = ~/\[(\d+|\*)\]/
    private static final Pattern genericIndexPattern = ~/\[\*\]/
    private static final String genericIndex = "[*]"

    private static final String[] emptyStrings = new String[0]

    private String raw = ""
    private Map<String,Object> foundNames = [:]
    private String first = ""

    Variables() {
        this(emptyStrings)
    }

    Variables(String candidates) {
        foundNames = parse(candidates)
        initialized()
    }

    Variables(String[] candidates) {
        candidates.each { String s -> foundNames[s] = null }
        initialized()
    }

    Variables(List<String> candidates) {
        candidates.each { String s -> foundNames[s] = null }
        initialized()
    }

    private void initialized() {
        raw = join()
        first = foundNames.isEmpty() ? "" : foundNames.keySet().iterator().next()
    }

    private Map<String,Object> parse(String candidates) {

        if (candidates.length() == 0)
            return [:]

        // Hotspot: avoid using regex parse if possible

        if (candidates.length() > 3) {
            if (candidates.startsWith('${') && candidates.endsWith('}')) {
                int anotherDollar = candidates.indexOf('$', 1)
                if (anotherDollar < 0) {
                    candidates = candidates.substring(2, candidates.length()-1)
                    String[] parts = candidates.split('=', 2)
                    return [ (parts[0].trim()): parts.length > 1 ? (Object)parts[1] : (Object)null ]
                }
            }
        }

        regexParse(candidates)
    }

    private Map regexParse(String candidates) {
        def results = [:]
        def matcher = candidates =~ envelopePattern
        while (matcher.find()) {
            String blob = matcher.group(1)
            String[] parts = blob.split('=', 2)
            results.put(parts[0].trim(), parts.length > 1 ? parts[1] : null)
        }
        results
    }

    String raw() {
        raw
    }

    boolean isPresent() {
        !foundNames.isEmpty()
    }

    boolean hasMultiple() {
        foundNames.size() > 1
    }

    String get() {
        first
    }

    String adorn(String candidate) {
        StringBuffer sb = new StringBuffer("\${")
        sb.append(candidate)
        sb.append("}")
        sb.toString()
    }
    // Matcher is the hotspot based on profiling
    boolean isIndexed(String candidate) {
        if (candidate.contains('[')) {
            def matcher = candidate =~ indexPattern
            while (matcher.find()) {
                return true
            }
        }
        false
    }

    boolean isGenericIndexed(String candidate) {
        // Hotspot: used to use matcher
        candidate.contains(genericIndex)
    }

    boolean matches(String specificVar, String genericVar) {
        if (specificVar == genericVar)
            return true

        if (isGenericIndexed(genericVar) && isIndexed(specificVar)) {
            int gvi = genericVar.indexOf('[')
            int svi = specificVar.indexOf('[')

            if (gvi == svi)
                // String comparison is the hotspot based on profiling
                return genericVar.substring(0, gvi) == specificVar.substring(0, svi)
        }

        false
    }

    List<String> matches(Map<String,Object> inputs, String possibleVariableName) {

        boolean varIsGenericIndexed = isGenericIndexed(possibleVariableName)

        List<String> results = []

        if (varIsGenericIndexed) {
            inputs.each { String key, Object val ->
                if (matches(key, possibleVariableName))
                    results.add(key)
            }
        }
        else if (inputs.containsKey(possibleVariableName)) {
            results.add(possibleVariableName)
        }

        results
    }

    String generifyIndex(String withIndex) {
        withIndex.replaceAll(/\[\d+\]/, "[*]")
    }

    String patterned(String candidate) {
        Pattern.quote(candidate)
    }

    String adornPattern(String candidate) {
        patterned(adorn(candidate))
    }

    String genericIndexPattern() {
        return genericIndexPattern
    }

    String extractIndex(String candidate) {
        int left = candidate.indexOf('[')
        int right = candidate.indexOf(']')
        (left < right) ? candidate.substring(left, right+1) : ""
    }

    List extractIndices() {
        foundNames.keySet().collect { extractIndex(it) }.grep { it }
    }

    List generateIndexed(String candidate, int count) {
        List result = []
        if (isGenericIndexed(candidate) && count > 0) {
            for (i in (0..count-1)) {
                result.add(candidate.replace("*", Integer.toString(i)))
            }
        }
        result
    }

    String generateOneIndexed(String candidate, int index) {
        if (isGenericIndexed(candidate) && index >= 0) {
            return candidate.replace("*", Integer.toString(index))
        }
        null
    }

    boolean hasValue(String candidate) {
        if (foundNames.containsKey(candidate))
            return (foundNames[candidate] != null)

        // avoiding regex for speed

        int left = candidate.indexOf('[')
        int right = candidate.lastIndexOf(']')
        if (left >= 0 && right >= 0 && right > left) {
            StringBuilder sb = new StringBuilder()
            sb.append(candidate.substring(0, left+1))
            sb.append('*')
            sb.append(candidate.substring(right))
            String generic = sb.toString()
            if (foundNames.containsKey(generic))
                return (foundNames[generic] != null)
        }
        false
    }

    String getValue(String candidate) {
        foundNames[candidate]
    }

    @Override
    int hashCode() {
        raw.hashCode()
    }

    private String join() {
        foundNames.keySet().join(", ")
    }
}
