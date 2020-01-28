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

import java.util.regex.Matcher
import java.util.regex.Pattern

/*
 * This class represents a wild card matching feature that is a bit easier (but less flexible) than Java Regex.
 * Note that client logic should use usesWildCarding(...) to avoid the slow regex logic powering this feature.
 * See unit tests for practical usage examples.
 */
@CompileStatic
class WildCardMatcher implements VariablesFetcher {

    static boolean usesWildcarding(String candidate) {
        candidate != null && candidate.startsWith("|") && candidate.endsWith("|")
    }

    static class NoWildcardingFound extends PlasticException {
        NoWildcardingFound(String candidate) {
            super("PLASTIC-NOT-WILDCARD", "the following value does not use wildcard matching: "+candidate)
        }
    }

    static class NoMatchFound extends PlasticException {
        NoMatchFound(String template, Object candidate) {
            super("PLASTIC-NO-MATCH", "could not use $template to match against $candidate")
        }
    }

    private static final Pattern REGEX_SPECIAL_CHARS = Pattern.compile('[{}()\\[\\].+?^$\\\\|]'); // No * as it is ours

    private static String escape(String unprotected) {

        return REGEX_SPECIAL_CHARS.matcher(unprotected).replaceAll('\\\\$0');
    }

    final String template
    final Variables variables
    final Pattern pattern
    final RegexReady regexVars

    WildCardMatcher(String template) {
        if (!usesWildcarding(template))
            throw new NoWildcardingFound(template)

        this.template = template
        this.variables = new Variables(template)
        this.regexVars = asPattern(template)
        this.pattern = Pattern.compile(regexVars.regex) // throw PatternSyntaxException
    }

    private RegexReady asPattern(String wild) {
        RegexReady regex = new RegexReady()
        String inner = wild.substring(1,wild.length()-1)

        for (String rawName : variables.getRawNames()) {
            inner = inner.replace(rawName, "PLACEHOLDER")
            regex.add(variables.unadorn(rawName))
        }

        String rx = escape(inner).replace('PLACEHOLDER', "(.+?)")
        rx = '^' + rx.replace('*', '\\b.*\\b') + '$'
        rx = rx.replace(')(', ')\\b(') // two variables back-to-back split on word boundary
        regex.setRegex(rx)
        regex
    }

    Variables getVariables() {
        variables
    }

    @Override
    Bindings fetch(Object candidate) {
        if (candidate instanceof String || candidate instanceof GString) {
            candidate = candidate == null ? "" : candidate

            Matcher matcher = pattern.matcher((String)candidate)
            if (matcher.matches()) {
                Bindings result = new Bindings()

                for (int i = 0; i < regexVars.count(); i++) {
                    result.set(regexVars.getName(i), matcher.group(i + 1))
                }

                return result
            }
        }

        throw new NoMatchFound(template, candidate)
    }

    @Override
    List<String> names() {
        variables.names().asList()
    }

    String toString() {
        "[ $template ]"
    }

    static class RegexReady {

        String regex = ""
        List<String> registerNames = new ArrayList<>()
        List<String> registerValues = new ArrayList<>()

        void setRegex(String regex) {
            this.regex = regex
        }

        void add(String registerName) {
            registerNames.add(registerName);
            registerValues.add(null)
        }

        int count() {
            registerNames.size()
        }

        String getName(int index) {
            registerNames.get(index)
        }
    }
}