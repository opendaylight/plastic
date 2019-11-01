
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.json.JsonOutput


class ChunkyJsonFormat implements Format {

    static final char DASH = '-'
    static final char NEWLINE = '\n'
    static final String LEADER = "${DASH}\n"
    static final String TRAILER = "\n${DASH}"
    static final String SEPARATOR = "\n${DASH}\n"
    static final String NAKED_SEPARATOR = "${DASH}"

    static final JsonFormat JsonFormatting = new JsonFormat()
    static final ChunkyJsonAggregator Aggregator = new ChunkyJsonAggregator()

    static final String FORMATKEY = "cjson"

    @Override
    String formatKey() {
        return FORMATKEY
    }

    @Override
    boolean matches(String key) {
        return key && key.equalsIgnoreCase(FORMATKEY)
    }

    @Override
    Object parse(InputStream strm) {
        def results = []
        if (strm.available() > 0) {
            List<String> chunks = Aggregator.deAggregate(strm.text)
            chunks.each {
                String s ->
                    Object o = JsonFormatting.parse(s)
                    results.add(o)
            }
        }
        results
    }

    Object parse(String payload) {
        parse(asStream(payload))
    }

    @Override
    Object clone(Object original) {
        if (original instanceof List) {
            original.collect { JsonFormatting.clone(it) }
        }
        else
            throw new PlasticException("PLASTIC-CAT-JSON-CLONE", "Cannot clone non-chunky JSON: ${original.class.simpleName}")
    }

    @Override
    String serialize(Object original) {
        if (original == null)
            return "null"

        if (original instanceof List)
            return serializeList((List)original)

        if (original instanceof Map)
            return serializeMap((Map)original)

        throw new PlasticException("PLASTIC-CAT-JSON-SER", "Cannot serialize non-chunky JSON: ${original.class.simpleName}")
    }

    String serializeMap(Map<String, Object> defaults) {
        String json = JsonOutput.toJson(defaults)
        LEADER +
        json +
        TRAILER
    }

    String serializeList(List original) {
        LEADER +
        original.collect { JsonFormatting.serialize(it) }.join(SEPARATOR) +
        TRAILER
    }

    @Override
    Object deserialize(String source) {
        return parse(asStream(source))
    }

    @Override
    Aggregator createAggregator() {
        return new ChunkyJsonAggregator()
    }

    private InputStream asStream(String source) {
        new ByteArrayInputStream(source == null ? "".getBytes() : source.getBytes())
    }
}
