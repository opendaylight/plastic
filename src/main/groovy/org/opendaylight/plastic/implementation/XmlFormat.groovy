
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.xml.XmlUtil


class XmlFormat implements Format {

    static final String EMPTY_DEFAULTS = "<Map></Map>"
    static final String MAP = "Map"
    static final String ENTRY = "Entry"
    static final String KEY = "Key"
    static final String VALUE = "Value"

    static final String FORMATKEY = "xml"

    @Override
    String formatKey() {
        return FORMATKEY
    }

    @Override
    boolean matches(String key) {
        return key && key.equalsIgnoreCase(FORMATKEY)
    }

    @Override
    Object parse(InputStream stream) {
        if (stream.available() == 0)
            stream = asStream(EMPTY_DEFAULTS) // support for empty defaults

        new XmlParser(false, false).parse(stream)
    }

    Object parse(String content) {
        parse(asStream(content))
    }

    @Override
    Object clone(Object other) {
        Node source = mustBeNode(other)

        // Unfortunately the clone() implementation leaves NULL
        // parent pointers, but we use those to build the path
        // to the element, so clone and set the pointers correctly

        List<Node> srcNodes = source.depthFirst()

        Node results = source.clone()
        List<Node> resultNodes = results.depthFirst()

        Map<Node,Node> cousins = new HashMap<>()

        for (int i = 0; i< srcNodes.size(); i++) {
            cousins.put (resultNodes[i], srcNodes[i])
        }

        Map<Node,Node> revCousins = new HashMap<>()
        cousins.each { Node k, Node v ->
            revCousins.put(v, k)
        }

        resultNodes.each { Node n ->
            if(n.parent() == null) {
                Node myCousin = cousins.get(n)
                Node myCousinsParent = myCousin.parent()
                Node myParent = revCousins.get(myCousinsParent)
                n.setParent(myParent)
            }
        }

        results
    }

    @Override
    String serialize(Object source) {
        mustBeNode(source)
        XmlUtil.serialize(source)
    }

    @Override
    Object deserialize(String source) {
        return parse(asStream(source))
    }

    @Override
    Aggregator createAggregator() {
        return new XmlAggregator()
    }

    Node mustBeNode(Object maybeNode) {
        if (!(maybeNode instanceof Node))
            throw new PlasticException("PLASTIC-PARSED-XML", "Candidate data is not an XML node: "+maybeNode.toString())
        (Node)maybeNode
    }

    private InputStream asStream(String source) {
        new ByteArrayInputStream(source == null ? "".getBytes() : source.getBytes())
    }
}
