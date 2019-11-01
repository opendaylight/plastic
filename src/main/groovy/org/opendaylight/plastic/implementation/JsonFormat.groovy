
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.json.JsonBuilder
import groovy.json.JsonException
import groovy.json.JsonSlurper

import java.util.regex.Pattern


class JsonFormat implements Format {

    static class JSONParseException extends PlasticException {

        JSONParseException(String message) {
            super("PLASTIC-MALFORMED-DATA",message)
        }
    }

    static final Pattern beginningXml = Pattern.compile("^\\s*<");
    static final String FORMATKEY = "json"

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
        if (strm.available() == 0)
            strm = asStream("{}") // support for empty defaults

        try {
            inflate(new JsonSlurper().parse(strm))
        }
        catch(JsonException e)
        {
            strm.reset()
            Scanner scanner = new Scanner(strm);
            if(scanner.findWithinHorizon(beginningXml,0)!=null)
                throw new JSONParseException("Cannot parse XML as JSON")

            throw new VersionedSchemaParsed.MalformedException(FORMATKEY, e)
        }
    }

    private Object inflate(Object object) {
        // The "parsed" object is often a LazyMap which clones as the value NULL
        // Just accessing any key will instantiate it (determined by looking at
        // implementation of parsed.buildIfNeeded()

        if (object instanceof Map)
            object.containsKey("foobar")

        object
    }

    Object parse(String payload) {
        parse(asStream(payload))
    }

    // This only really works for objects which came from parsed JSON (by this class)
    // originally, otherwise there could be a lot more types to deal with below.
    //
    @Override
    Object clone(Object original) {

        // the branches below that just return original are because they are immutable

        if (original instanceof Map) {
            Map cloned = [:]
            original.each { k,v ->
                cloned[k] = clone(v)
            }
            cloned
        }
        else if (original instanceof List) {
            List cloned = []
            original.each { e ->
                cloned.add(clone(e))
            }
            cloned
        }
        else if (original instanceof Integer) {
            new Integer(original.intValue())
        }
        else if (original instanceof BigDecimal) {
            original
        }
        else if (original instanceof Boolean) {
            new Boolean(original.booleanValue())
        }
        else if (original instanceof String) {
            original
        }
        else if (original instanceof Cloneable)
            original.clone()
        else
            original.toString()
    }

    /* Unused reference code - cannot serialize everything in a JSON-originated tree

    private Object deepCopyUsingSerialize(Object original) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ObjectOutputStream oos = new ObjectOutputStream(baos)
        oos.writeObject(original);
        oos.flush()

        ByteArrayInputStream bain = new ByteArrayInputStream(baos.toByteArray())
        ObjectInputStream ois = new ObjectInputStream(bain)
        return ois.readObject()
    }
    */

    @Override
    String serialize(Object source) {
        if (source == null)
            return "null"
        else
            new JsonBuilder(source).toPrettyString()
    }

    @Override
    Object deserialize(String source) {
        return parse(asStream(source))
    }

    @Override
    Aggregator createAggregator() {
        return new JsonAggregator()
    }

    private InputStream asStream(String source) {
        new ByteArrayInputStream(source == null ? "".getBytes() : source.getBytes())
    }
}
