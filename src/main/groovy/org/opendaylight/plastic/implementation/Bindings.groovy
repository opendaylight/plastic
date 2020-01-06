/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation

import groovy.transform.CompileStatic

/**
 * Encapsulates variable names (keys) with their values (usually Strings but could be Objects).
 * For performance, there is no cloning of the map, so all client usage must be on good behavior.
 */
@CompileStatic
class Bindings {

    private static Variables variables = new Variables()

    private Map<String,Object> bound
    private Set<String> defaultUsed = new HashSet<>()

    Bindings() {
        this.bound = new HashMap<>()
    }

    Bindings(Map<String,Object> bindings) {
        this.bound = bindings
    }

    // Put a null entry in for any variables which are not present
    //
    void addMissingVariables(Set<String> names) {
        names.each { n ->
            if (!bound.containsKey(n))
                bound[n] = null
        }
    }

    // Put any specific values from defaults into any variables which have null values (are missing)
    //
    void applyDefaults(Map<String,Object> defaults) {
        bound.each { String k, Object v ->
            if (v == null) {
                bound[k] = defaults[k]
                if (bound[k] == null) {
                    String generic = variables.generifyIndex(k)
                    bound[k] = defaults[generic]
                }
                if (bound[k] != null)
                    defaultWasUsed(k)
            }
        }
    }

    // Put any specific values from source into any variables which have default values
    //
    void overrideDefaultValuesWith(Map<String,Object> source) {
        source.each { String k,Object v ->
            if (v != null) {
                putIfMissingOrNull(k, v)
                putIfDefault(k, v)
            }
        }
    }

    void defaultWasUsed(String varName) {
        defaultUsed.add(varName)
    }

    Map<String,Object> bindings() {
        return bound
    }

    private void putIfMissingOrNull(String key, Object value) {
        if (!bound.containsKey(key) || bound.get(key) == null)
            bound.put(key, value)
    }

    private void putIfDefault(String key, Object value) {
        if (defaultUsed.contains(key))
            bound.put(key, value)
    }
}
