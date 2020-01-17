
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.base.Splitter
import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class Variables {

    static class Finding {
        final String raw
        final Object value
        Finding(String raw, Object value) {
            this.raw = raw
            this.value = value
        }
    }

    private static final Pattern envelopePattern = ~/\$\{(.+?)\}/

    private static final String[] emptyStrings = new String[0]

    static String adorn(String candidate) {
        StringBuilder sb = new StringBuilder("\${")
        sb.append(candidate)
        sb.append("}")
        sb.toString()
    }

    static String unadorn(String candidate) {
        if (candidate.startsWith('${') && candidate.endsWith('}'))
            candidate.substring(2, candidate.length()-1)
        else
            candidate
    }

    static boolean isAdorned(String candidate) {
        candidate.startsWith('${') && candidate.endsWith('}')
    }

    static String patternQuoted(String candidate) {
        Pattern.quote(candidate)
    }

    static String adornedAndQuoted(String candidate) {
        patternQuoted(adorn(candidate))
    }

    // Hotspot: don't use regex

    static boolean isGenericIndexed(String candidate) {
        candidate.contains("[*]")
    }

    static String genericIndex() {
        return "[*]"
    }

    // Hotspot: don't use regex

    static boolean isIndexed(String candidate) {
        for (int left = 0; left > -1; ) {
            left = candidate.indexOf('[', left)
            if (left > -1) {
                int right = candidate.indexOf(']', left)
                if (right > -1 && right > (left+1)) {
                    for (int i = left+1; i< right; i++) {
                        int ch = candidate.charAt(i)
                        if ('0123456789*'.indexOf(ch) > -1)
                            return true
                    }
                }
                if (right > -1)
                    left = right
                else
                    left++
            }
        }
        false
    }

    static String extractIndex(String candidate) {
        int left = candidate.indexOf('[')
        int right = candidate.indexOf(']')
        (left < right) ? candidate.substring(left, right+1) : ""
    }

    static String generifyIndex(String candidate) {
        if (!isGenericIndexed(candidate)) {

        // avoiding regex for speed

            int left = candidate.indexOf('[')
            int right = candidate.lastIndexOf(']')
            if (left >= 0 && right >= 0 && right > left) {
                StringBuilder sb = new StringBuilder()
                sb.append(candidate.substring(0, left + 1))
                sb.append('*')
                sb.append(candidate.substring(right))
                return sb.toString()
            }
        }

        candidate
    }

    // Hotspot

    static boolean matches(String specificVar, String genericVar) {
        if (specificVar == genericVar)
            return true

        if (isGenericIndexed(genericVar) && isIndexed(specificVar)) {
            int gvi = genericVar.indexOf('[')
            int svi = specificVar.indexOf('[')

            if (gvi == svi)
                return genericVar.substring(0, gvi) == specificVar.substring(0, svi)
        }

        false
    }

    static List<String> matches(Map<String,Object> inputs, String possibleVariableName) {

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

    static List generateManyIndexed(String candidate, int count) {
        List result = []
        if (isGenericIndexed(candidate) && count > 0) {
            for (i in (0..count-1)) {
                result.add(candidate.replace("*", Integer.toString(i)))
            }
        }
        result
    }

    static String generateAsIndexed(String candidate, int index) {
        if (isGenericIndexed(candidate) && index >= 0) {
            return candidate.replace("*", Integer.toString(index))
        }
        null
    }

    // Not used currently but left as a reference to compare with adhocParse(...)
    //
    static private Map<String,Finding> regexParse(String candidates) {
        def results = [:]
        def matcher = candidates =~ envelopePattern
        while (matcher.find()) {
            String blob = matcher.group(1)
            String[] parts = blob.split('=', 2)
            parts[0] = parts[0].trim()
            results.put(parts[0], new Finding (adorn(parts[0]), parts.length > 1 ? parts[1] : null))
        }
        results
    }

    static private Map<String,Finding> adhocParse(String candidates) {
        def results = [:]
        for (String candidate : splitApart(candidates.trim())) {
            if (isAdorned(candidate)) {
                String blob = unadorn(candidate)
                String[] parts = blob.split('=', 2)
                parts[0] = parts[0].trim()
                results.put(parts[0], new Finding (adorn(parts[0]), parts.length > 1 ? parts[1] : null))
            }
        }
        results
    }

    static List<String> splitApart(String candidates) {
        List<String> results = new ArrayList<>()
        final int len = candidates.length()
        int left = 0
        while (left < len) {
            int begin = candidates.indexOf('${', left)
            if (begin > -1) {
                int end = candidates.indexOf('}', begin)
                if (end > -1) {
                    if (left != begin)
                        results.add(candidates.substring(left, begin))
                    results.add(candidates.substring(begin, end+1))
                    left = end+1
                }
                else {
                    results.add(candidates.substring(begin))
                    left = len
                }
            }
            else {
                results.add(candidates.substring(left))
                left = begin = len
            }
        }
        return results
    }

    // -------------------------------------------------------------------------------------------

    private String raw = ""
    private Map<String,Finding> foundNames = [:]
    private String first = ""

    Variables() {
        this(emptyStrings)
    }

    Variables(String candidates) {
        foundNames = parse(candidates)
        initialized()
    }

    Variables(String[] candidates) {
        candidates.each { String s -> foundNames[s] = (Finding)null }
        initialized()
    }

    Variables(List<String> candidates) {
        candidates.each { String s -> foundNames[s] = (Finding)null }
        initialized()
    }

    void toEach(Closure cs) {
        foundNames.each { Object k, Finding f -> cs(k,f.value) }
    }

    private void initialized() {
        raw = join()
        first = foundNames.isEmpty() ? "" : foundNames.keySet().iterator().next()
    }

    // Note underlying LinkedHashMap is preserving key ordering, which is relied on by wildcarding feature
    //
    Map<String,Finding> parse(String candidates) {

        if (candidates.length() < 4) // must be at least length of ${a}
            return [:]

        // Hotspot: avoid using regex parse if possible

        // For speed, special case a simple single variable

        if (candidates.startsWith('${') && candidates.endsWith('}')) {
            int anotherDollar = candidates.indexOf('$', 1)
            if (anotherDollar < 0) {
                String inner = candidates.substring(2, candidates.length()-1)
                String[] parts = inner.split('=', 2)
                parts[0] = parts[0].trim()
                return [ (parts[0]): new Finding(candidates, parts.length > 1 ? (Object)parts[1] : (Object)null) ]
            }
        }

        adhocParse(candidates)
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

    Set<String> names() {
        foundNames.keySet()
    }

    String get() {
        first
    }

    List<String> extractIndices() {
        foundNames.keySet().collect { extractIndex(it) }.grep { it }
    }

    boolean hasValue(String candidate) {
        if (foundNames.containsKey(candidate))
            return (foundNames[candidate].value != null)

        String generic = generifyIndex(candidate)
        if (foundNames.containsKey(generic))
            return (foundNames[generic].value != null)

        false
    }

    // Underlying LinkedHashMap means variable names come out in the order they were inserted

    List<String> getRawNames() {
        List<String> results = []
        foundNames.values().each { Finding f ->
            results.add(f.raw)
        }
        results
    }

    String getValue(String candidate) {
        foundNames.containsKey(candidate) ? foundNames[candidate].value : null
    }

    @Override
    int hashCode() {
        raw.hashCode()
    }

    private String join() {
        foundNames.keySet().join(", ")
    }
}
