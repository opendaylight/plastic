
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation.author


import org.opendaylight.plastic.implementation.Schema

// Gives classifier writers a way to replace chunks of a json tree
// with a variable name

/*
 * This class is used to swap in a implementation variable name into a structure,
 * modifying the structure in-place and returning the snipped out value. The
 * snipped value is usually an object that is a member of an array or a map.
 * If the object is a scalar, it is wrapped into an array (because it needs to
 * be a legal JSON structure).
 */

class ClaimCheck {

    String baseName
    Schema root
    Schema branch
    String name

    def getter = { null }
    def setter = { v -> }

    ClaimCheck(String claimCheckBaseName, Schema root,
               List parent, int childIndex) {
        mustBeList(parent)

        getter = { parent[childIndex] }
        setter = { v -> parent[childIndex] = v }

        initialize(claimCheckBaseName, root, childIndex.toString(), getter)
    }

    ClaimCheck(String claimCheckBaseName, Schema root,
               Map parent, String childKey) {
        mustBeMap(parent)

        getter = { parent[childKey] }
        setter = { v ->
            parent[v] = [:]
            parent.remove(childKey)
        }

        initialize(claimCheckBaseName, root, childKey, getter)
    }

    private void initialize(String claimCheckBaseName, Schema theRoot, String key, Closure fetch) {
        this.baseName = claimCheckBaseName
        this.name = "${baseName}[${key}]"
        this.root = theRoot

        Object replacee = fetch()
        this.branch = theRoot.cloneWith(isScalar(replacee) ? [ replacee ] : replacee)
    }

    private void mustBeMap(Map map) {
        if (!(map instanceof Map))
            throw new IllegalArgumentException("Expected JSON map, not ${map}")
    }

    private void mustBeList(List list) {
        if (!(list instanceof List))
            throw new IllegalArgumentException("Expected JSON list, not ${list}")
    }

    private boolean isScalar(Object obj) {
        !(obj instanceof Map) && !(obj instanceof List)
    }

    String getName() {
        this.name
    }

    private String getVariableName() {
        "\${${name}}"
    }

    Schema getBranch() {
        branch
    }

    void swap() {
        setter(variableName)
    }
}
