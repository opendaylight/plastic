
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation.author

import groovy.json.JsonSlurper

class BetterJson {

    static class BetterJsonException extends RuntimeException {
        BetterJsonException(String msg) {
            super(msg)
        }
    }

    protected Object root

    BetterJson(Object payload) {
        this.root = payload
    }

    BetterJson(String raw) {
        this.root = new JsonSlurper().parseText(raw)
    }

    List asList(String... path) {
        Object fetched = (path.length == 0) ? root : fetch(path)
        if (!(fetched instanceof List))
            throw new BetterJsonException("At the end of the JSON path ${this.joined(path)}, found a non-list: ${fetched}")
        fetched
    }

    Map asMap(String... path) {
        Object fetched = (path.length == 0) ? root : fetch(path)
        if (!(fetched instanceof Map))
            throw new BetterJsonException("At the end of the JSON path ${this.joined(path)}, found a non-map: ${fetched}")
        fetched
    }

    Object asObject(String... path) {
        Object fetched = (path.length == 0) ? root : fetch(path)
        fetched
    }

    boolean isList(String... path) {
        Object here = maybeFetch(path)
        here != null && (here instanceof List)
    }

    boolean isNonEmptyList(String... path) {
        Object here = maybeFetch(path)
        here != null && (here instanceof List) && !((List)here).isEmpty()
    }

    boolean isMap(String... path) {
        Object here = maybeFetch(path)
        here != null && (here instanceof Map)
    }

    boolean isNonEmptyMap(String... path) {
        Object here = maybeFetch(path)
        here != null && (here instanceof Map) && !((Map)here).isEmpty()
    }

    boolean isScalar(String... path) {
        Object here = maybeFetch(path)
        here != null && !(here instanceof Map) && !(here instanceof List)
    }

    boolean isObject(String... path) {
        Object here = maybeFetch(path)
        here != null
    }

    Object maybeFetch(String... path) {
        Object here = root
        path.each { p ->
            if (here != null) {
                here = here[p]
            }
        }

        here
    }

    boolean isEmpty(Object tree) {
        if (tree == null)
            return true

        if (tree instanceof List)
            return ((List)tree).isEmpty()
        if (tree instanceof Map)
            return ((Map)tree).isEmpty()

        throw new BetterJsonException("Cannot determine emptiness of non-collection ${tree.toString()} in morpher ${this.class.simpleName}")
    }

    private Object fetch(String... path) {
        if (root == null)
            throw new BetterJsonException("Could not use JSON path (${this.joined(path)} on a NULL root")
        if (path.length == 0)
            return root
        if (!(root instanceof Map))
            throw new BetterJsonException("Using a JSON path (${this.joined(path)}) is not supported for the following: ${this.root}")

        Object here = root

        for (int i = 0; i < path.length; i++) {
            String component = path[i]

            if (here instanceof Map)
                here = here[component]
            else if (here instanceof List && component.isInteger()) {
                List list = (List)here
                int index = component.toInteger().intValue()
                if (index >= list.size())
                    throw new BetterJsonException("Encountered a list index (${index}) out-of-bounds (length is ${list.size()}) along the path ${this.joined(path)}")
                here = list[index]
            }
            else
                here = null

            if (here == null)
                throw new BetterJsonException("Encountered a missing JSON path component \'${component}\' along the path ${this.joined(path)} for object ${this.root}")
        }

        here
    }

    private String joined(String... path) {
        "${path.join('.')}"
    }
}
