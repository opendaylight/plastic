
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
 * This is a class designed to pack or unpack a collection of payloads such that they are both
 * a legally parsible super payload and have "special" structure that allows recognition as
 * being packed and be unpackable.
 *
 * So in a way, this forms a special dialect of the underlying Json/Xml format that is both legal
 * and unpackable without full parsing (due to time cost). The guarantee is that something packed
 * by an aggregator can be unpacked by an aggregator.
 */
abstract class Aggregator {

    static class MissingLeader extends PlasticException {
        MissingLeader(String leader, String candidate) {
            super("PLASTIC-AGG-MISS-LEAD",
                    "The following payload is missing the format leader - expected: "
                            + leader + " found: " + candidate)
        }
    }

    static class MissingTrailer extends PlasticException {
        MissingTrailer(String trailer, String candidate) {
            super("PLASTIC-AGG-MISS-TRAIL",
                    "The following payload is missing the format trailer - expected: "
                            + trailer + " found: "
                            + (candidate.length() > trailer.length()) ?
                            candidate.substring(candidate.length()-trailer.length(), candidate.length()) : candidate)
        }
    }

    static class NullPayload extends PlasticException {
        NullPayload() {
            super("PLASTIC-AGG-NULL-PAYL", "Aggregation encountered a null payload")
        }
    }

    static final String BIGFINGERPRINT = "PlasticAggregation"
    static final String SMALLFINGERPRINT = "PlastAgg"

    private final String leader
    private final String separator1
    private final String separator2
    private final String trailer
    private final StringBuilder buffer
    private final int count

    Aggregator(String leader, String separator, String trailer) {
        this(leader, separator, "", trailer)
    }

    Aggregator(String leader, String separator1, String separator2, String trailer) {
        this.leader = leader
        this.separator1 = separator1
        this.separator2 = separator2
        this.trailer = trailer
        this.buffer = new StringBuilder()
    }

    String emit() {
        buffer.insert(0, leader)
        buffer.append(trailer)
        buffer.toString()
    }

    private boolean isInline() {
        separator2.isEmpty()
    }

    protected Aggregator add(String raw) {
        if (isInline())
            addInline(raw)
        else
            addWrapped(raw)
        this
    }

    protected void addInline(String raw) {
        if (raw != null && !raw.isEmpty()) {
            if (count == 0)
                buffer.append(raw)
            else {
                buffer.append(separator1)
                buffer.append(raw)
            }
            count++
        }
    }

    protected void addWrapped(String raw) {
        if (raw != null && !raw.isEmpty()) {
            buffer.append(separator1)
            buffer.append(raw)
            buffer.append(separator2)
            count++
        }
    }

    List<String> deAggregate(String multiPayload) {
        multiPayload = multiPayload.trim()
        mustNotBeNull(multiPayload)
        mustHaveLeader(multiPayload)
        mustHaveTrailer(multiPayload)

        realDeAggregate(multiPayload)
    }

    private void mustNotBeNull(String candidate) {
        if (candidate == null)
            throw new NullPayload()
    }

    private void mustHaveLeader(String candidate) {
        if (!candidate.startsWith(leader))
            throw new MissingLeader(leader, candidate)
    }

    private void mustHaveTrailer(String candidate) {
        if (!candidate.endsWith(trailer))
            throw new MissingTrailer(trailer, candidate)
    }

    abstract protected List<String> realDeAggregate(String multiPayload)

    abstract String serializeDefaults(Map<String,Object> defaults)
}
