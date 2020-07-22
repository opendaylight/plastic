
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

import java.util.regex.Pattern

@CompileStatic
class Variables {

    static final char LBRACKET = '['
    static final String[] ILLEGALS = [ "+", "<", ">", "\"", ".", "[", "]", "]", "{", "}", "(", ")", "*", "~", "^", "?", "&", "|", "@", "=", " " ]
    static final String GOODINDICES = "[]*^0123456789"

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

    static boolean basenameContainsIllegals(String candidate) {
        int here = candidate.indexOf((int)LBRACKET)
        if (here >= 0)
            candidate = candidate.substring(0, here)

        for (String c : ILLEGALS) {
            if (candidate.indexOf(c) > -1)
                return true
        }
        false
    }

    static boolean wellFormedIndices(String candidate) {
        int here = candidate.indexOf((int)LBRACKET)
        if (here >= 0) {
            for (int i = here; i< candidate.length(); i++) {
                if (GOODINDICES.indexOf((int)candidate.charAt(i)) < 0)
                    return false
            }
        }
        true
    }

    static String basename(String candidate) {
        int here = candidate.indexOf((int)LBRACKET)
        if (here >= 0)
            candidate.substring(0, here)
        else
            candidate
    }

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

    static boolean isInternal(String candidate) {
        candidate.startsWith('_')
    }

    static String patternQuoted(String candidate) {
        Pattern.quote(candidate)
    }

    // TODO: remove unused
    static String adornedAndQuoted(String candidate) {
        patternQuoted(adorn(candidate))
    }

    static String alternativeName(String candidate) {
        if (candidate) {
            int firstCareted = candidate.indexOf('[^]')
            int lastAsterisked = candidate.lastIndexOf('[*]')
            if (firstCareted < 0 && lastAsterisked > -1) {
                StringBuilder sb = new StringBuilder (candidate.replace('[*]', '[^]'))
                sb.setCharAt(lastAsterisked+1, '*' as char)
                return sb.toString()
            }
            else if (firstCareted > -1 && lastAsterisked > -1) {
                return candidate.replace('[^]', '[*]')
            }
        }

        return candidate
    }

    // Hotspot: don't use regex

    static boolean isGenericIndexed(String candidate) {
        !candidate.startsWith('_') && candidate.indexOf('[') > -1 && (candidate.contains("[*]") || candidate.contains("[^]"))
    }

    static boolean mightBeIndexed(String candidate) {
        candidate != null && candidate.indexOf('[') > -1 && candidate.indexOf(']') > -1
    }

    static boolean isSingular(String candidate) {
        if (candidate == null)
            return false
        if (!candidate.startsWith('${'))
            return false
        if (!candidate.endsWith('}'))
            return false
        if (containsMultiple(candidate, '$'))
            return false
        if (containsMultiple(candidate, '{'))
            return false
        if (containsMultiple(candidate, '}'))
            return false
        true
    }

    static private boolean containsMultiple(String candidate, String target) {
        int first = candidate.indexOf(target)
        if (first < 0)
            return false
        if (first == candidate.length()-target.length()-1)
            return false
        int second = candidate.indexOf(target, first+1)
        second > -1
    }

    static String substring(String varName, String searchHere) {
        StringBuilder sb = new StringBuilder()
        sb.append('${')
        sb.append(varName)
        int left = searchHere.indexOf(sb.toString())
        if (left > -1) {
            int right = searchHere.indexOf('}', left+1)
            if (right > -1)
                return searchHere.substring(left, right+1)
        }
        ""
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
                        if ('0123456789*^'.indexOf(ch) > -1)
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
        int right = candidate.lastIndexOf(']')
        (left < right) ? candidate.substring(left, right+1) : ""
    }

    static int extractIndexAsInt(String candidate, int defaultValue) {
        int result = defaultValue
        String found = extractIndex(candidate)
        if (!found.isEmpty()) {
            try {
                result = Integer.parseInt(found.substring(1, found.size()-1))
            }
            catch (NumberFormatException e) {
                // not sure it makes sense to fuss about this
                // just let the default value be returned
            }
        }
        result
    }

    static String generifyIndex(String candidate) {
        // avoiding regex for speed

        if (!isGenericIndexed(candidate)) {
            StringBuilder sb = new StringBuilder()
            int next = 0

            while (next < candidate.length()) {
                int left = candidate.indexOf('[', next)
                if (left > -1) {
                    int right = candidate.indexOf(']', left)
                    if (right > -1 && right > (left + 1)) {
                        sb.append(candidate.substring(next,left+1))
                        sb.append('*')
                        next = right
                    }
                    else if (right > -1) {
                        sb.append(candidate.substring(left,right))
                        next = right
                    }
                    else {
                        sb.append(candidate.substring(left))
                        next = candidate.length()
                    }
                }
                else {
                    sb.append(candidate.substring(next))
                    next = candidate.length()
                }
            }

            candidate = sb.toString()
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

    static List<String> generateManyIndexed(String candidate, long[] counts) {
        List<String> result = []
        if (isGenericIndexed(candidate) && counts.length > 0) {
            MultiModuloCounter mmc = new MultiModuloCounter(counts)
            if (mmc.isCountable() && !mmc.isZero()) {
                while (true) {
                    long[] indices = mmc.value()
                    String c = candidate
                    for (int i = 0; i < indices.length; i++) {
                        c = replaceFirst(c, '*', Long.toString(indices[i]))
                    }
                    result.add(c)
                    if (mmc.isDone())
                        break
                    mmc.increment()
                }
            }
        }
        result
    }

    private static String replaceFirst(String input, String from, String to) {

        StringBuilder sb = new StringBuilder(input);
        int where = sb.indexOf(from);
        if (where > -1)
            sb.replace(where, where + from.length(), to);

        return sb.toString();
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
    private Map<String,Finding> alternativeNames = [:]
    private String first = ""

    Variables() {
        this(emptyStrings)
    }

    Variables(String candidates) {
        foundNames = parse(candidates)
        validateNames()
        replicateAlternatives()
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

    private void validateNames() {
        foundNames.each { k,v ->
            if (basenameContainsIllegals(k))
                throw new PlasticException("PLASTIC-VAR-NAME-ILLEGAL", "The variable name $k contains one of the illegal characters $ILLEGALS")
            if (!wellFormedIndices(k))
                throw new PlasticException("PLASTIC-VAR-NAME-BAD-GENERIC", "The variable name $k has a badly formed array index")
        }
    }

    void toEach(Closure cs) {
        foundNames.each { Object k, Finding f -> cs(k,f.value) }
    }

    // Now that generic indexes can be composed of * and ^, we don't want to store
    // all permutations, so force everything into a generic key with only *
    //
    private void replicateAlternatives() {
        // modifying while iterating means use a copy
        Set<String> names = new HashSet(foundNames.keySet())
        for (String name : names) {
            String normalizedGeneric = name.replace('[^]', '[*]')
            alternativeNames.put(normalizedGeneric, foundNames[name])
        }
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

        if (alternativeNames.containsKey(generic))
            return (alternativeNames[generic].value != null)

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

    Map<String,String> getNameToRawMapping() {
        Map<String,String> results = [:]
        foundNames.each { String name,Finding f ->
            results.put(name, f.raw)
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
