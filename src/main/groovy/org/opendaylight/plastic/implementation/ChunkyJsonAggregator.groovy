
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


/**
 * This is a concatentated JSON aggregator. Its output, unlike JsonAggregator, is not valid JSON.
 * It is here for deaggregation speed and will likely replace JsonAggregator. Note that the separators
 * cannot syntactically occur in JSON itself.
 */
class ChunkyJsonAggregator extends Aggregator {

    static class BadChunkyJson extends PlasticException {
        BadChunkyJson(String bad) {
            super("PLASTIC-BAD-CAT-JSON", "Payload does not conform to 'chunky JSON' format: ")
        }
    }


    ChunkyJsonAggregator() {
        super(ChunkyJsonFormat.LEADER, ChunkyJsonFormat.SEPARATOR, ChunkyJsonFormat.TRAILER)
    }

    @Override
    protected List<String> realDeAggregate(String multiPayload) {
        List<String> chunks = []
        StringBuilder chunk = new StringBuilder()

        if (!multiPayload.startsWith(ChunkyJsonFormat.LEADER))
            throw new BadChunkyJson(multiPayload)

        multiPayload.eachLine {
            String s ->
                if (s.startsWith(ChunkyJsonFormat.NAKED_SEPARATOR) && s.trim() == ChunkyJsonFormat.NAKED_SEPARATOR) {
                    if (chunk.length() > 0) {
                        chunks.add(chunk.toString())
                        chunk.setLength(0)
                    }
                }
                else {
                    if (chunk.length() > 0)
                        chunk.append(ChunkyJsonFormat.NEWLINE)
                    chunk.append(s)
                }
        }

        if (!multiPayload.endsWith(ChunkyJsonFormat.TRAILER))
            throw new BadChunkyJson(multiPayload)

        chunks
    }

    @Override
    String serializeDefaults(Map<String, Object> defaults) {
        ChunkyJsonFormat format = new ChunkyJsonFormat()
        format.serializeMap(defaults)
    }
}
