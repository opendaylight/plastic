
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


class XmlAggregator extends Aggregator {

    static final String MAPOPENER = "<${XmlFormat.MAP}>"
    static final String MAPCLOSER = "</${XmlFormat.MAP}>"
    static final String KEYOPENER = "<${XmlFormat.KEY}>"
    static final String KEYCLOSER = "</${XmlFormat.KEY}>"
    static final String VALUEOPENER = "<${XmlFormat.VALUE}>"
    static final String VALUECLOSER = "</${XmlFormat.VALUE}>"

    // NOTE! static references instead of inline to avoid a "bad type on operand stack" groovyc compile error
    static final String XMLOPENER = "<${BIGFINGERPRINT}>"
    static final String XMLCLOSER = "</${BIGFINGERPRINT}>"
    static final String ITEMOPEN = "<${SMALLFINGERPRINT}>"
    static final String ITEMCLOSE = "</${SMALLFINGERPRINT}>"

    enum State {
        LookingForOpener,
        LookingForChunkOrCloser,
        CollectingChunk
    }

    XmlAggregator() {
        super(XMLOPENER, ITEMOPEN, ITEMCLOSE, XMLCLOSER)
    }

    /**
     * This is a partial parsing of a XML that assumes that XML objects have been bundled
     * into a top level collection. It returns an empty list for ad hoc XML.
     *
     * @param multiPayload
     * @return list of unbundled XML
     */
    @Override
    protected List<String> realDeAggregate(String multiPayload) {
        List<String> chunks = []

        State state = State.LookingForOpener
        int mark = 0

        while (mark < multiPayload.length()) {
            switch(state) {
                case State.LookingForOpener:
                    int found = multiPayload.indexOf(XMLOPENER, mark)
                    if (found > -1) {
                        mark = found+XMLOPENER.length()
                        state = State.LookingForChunkOrCloser
                    }
                    break
                case State.LookingForChunkOrCloser:
                    int found = multiPayload.indexOf(ITEMOPEN, mark)
                    if (found > -1) {
                        mark = found+ITEMOPEN.length()
                        state = State.CollectingChunk
                    }
                    else {
                        // with or without XMLCLOSER we are done
                        mark = multiPayload.length()
                    }
                    break
                case State.CollectingChunk:
                    int found = multiPayload.indexOf(ITEMCLOSE, mark)
                    if (found > -1) {
                        chunks.add(multiPayload.substring(mark, found))
                        state = State.LookingForChunkOrCloser
                    }
                    break
            }
        }

        chunks
    }

    @Override
    String serializeDefaults(Map<String, Object> defaults) {
        StringBuilder builder = new StringBuilder()
        builder.append(MAPOPENER)
        for (Map.Entry entry : defaults.entrySet()) {
            builder.append(KEYOPENER)
            builder.append(entry.key.toString())
            builder.append(KEYCLOSER)
            builder.append(VALUEOPENER)
            builder.append(entry.value.toString())
            builder.append(VALUECLOSER)
        }
        builder.append(MAPCLOSER)
        builder.toString()
    }
}
