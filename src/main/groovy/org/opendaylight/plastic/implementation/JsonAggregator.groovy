
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

class JsonAggregator extends Aggregator {
    JsonAggregator() {
        super("[", ",", "]")
    }

    enum State {
        LookingForLeader,
        LookingForChunkOrTrailer,
        CollectingChunk
    }

    // Groovy doesn't have character literals

    static final char LBRACE = '['
    static final char RBRACE = ']'
    static final char LCURLY = '{'
    static final char RCURLY = '}'
    static final char QUOTE = '\"'
    static final char SLASH = '\\'

    /**
     * This is a partial parsing of a JSON string that assumes that JSON objects (as strings)
     * have been bundled into a top level JSON array. It return an empty list for ad hoc JSON.
     *
     * Leader = singular top level [
     * Trailer = singular top level ]
     * Chunk =  { ... } inclusive for any top level object
     *
     * @param multiPayload
     * @return list of unbundled JSON objects (as strings)
     */
    @Override
    protected List<String> realDeAggregate(String multiPayload) {
        List<String> chunks = []

        final int len = multiPayload.length()
        State state = State.LookingForLeader

        int depthBraces = 0
        int depthCurlies = 0

        boolean inQuotes = false

        int mark = -1
        char prevCh = '\0'

        for (int i = 0; i< len; i++) {
            char ch = multiPayload.charAt(i)

            State next = state

            if (!inQuotes) {
                switch (state) {
                    case State.LookingForLeader:
                        if (ch == LBRACE)
                            next = State.LookingForChunkOrTrailer
                        break
                    case State.LookingForChunkOrTrailer:
                        if (ch == RBRACE)
                            next = State.LookingForLeader
                        else if (ch == LCURLY)
                            next = State.CollectingChunk
                        else if (ch == RCURLY)
                            next = State.LookingForChunkOrTrailer
                        break
                    case State.CollectingChunk:
                        if (ch == RCURLY && depthCurlies == 1)
                            next = State.LookingForChunkOrTrailer
                        break
                }

                if (state == State.LookingForChunkOrTrailer && next == State.CollectingChunk) {
                    mark = i
                } else if (state == State.CollectingChunk && next == State.LookingForChunkOrTrailer) {
                    String chunk = multiPayload.substring(mark, i + 1)
                    chunks.add(chunk)
                    mark = -1
                }

                switch (ch) {
                    case LBRACE:
                        depthBraces++
                        break
                    case RBRACE:
                        depthBraces--
                        break
                    case LCURLY:
                        depthCurlies++
                        break
                    case RCURLY:
                        depthCurlies--
                        break
                }
            }

            if (ch == QUOTE && prevCh != SLASH) {
                inQuotes = !inQuotes
            }

            state = next
            prevCh = ch
        }

        chunks
    }

    @Override
    String serializeDefaults(Map<String, Object> defaults) {
        JsonOutput.toJson(defaults)
    }
}
