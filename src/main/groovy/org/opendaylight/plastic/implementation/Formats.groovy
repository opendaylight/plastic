
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

class Formats {

    static def KNOWNS = [new JsonFormat(), new XmlFormat(), new ChunkyJsonFormat() ]

    static class UnsupportedFormat extends PlasticException {
        String[] expected
        String found

        UnsupportedFormat(Set<String> expected, String found) {
            super("PLASTIC-UNSUPPORTED-FORMAT", "Unsupported format found: "+found)
            this.expected = expected
            this.found = found
        }
    }
    static class DuplicateFormat extends PlasticException {
        String[] expected
        String found

        DuplicateFormat(Set<String> existing, String found) {
            super("PLASTIC-DUPLICATE-FORMAT", "Duplicate format found: "+found)
            this.expected = existing
            this.found = found
        }
    }

    private Map supporteds = [:]

    Formats() {
        KNOWNS.each { addSupported(it) }
    }

    void addSupported(Format format) {
        if (supports(format.formatKey()))
            throw new DuplicateFormat(supporteds.keySet(), format.formatKey())
        supporteds[format.formatKey()] = format
    }

    boolean supports(String formatKey) {
        supporteds.containsKey(formatKey)
    }

    Format lookup(String formatKey) {
        validate(formatKey)
        supporteds[formatKey]
    }

    private void validate(String formatKey) {
        if (!supports(formatKey))
            throw new UnsupportedFormat(supporteds.keySet(), formatKey)
    }

    Format mustLookup(String formatKey) {
        validate(formatKey)
        lookup(formatKey)
    }
}
