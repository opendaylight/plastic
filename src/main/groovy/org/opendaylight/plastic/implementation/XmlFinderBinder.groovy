
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


class XmlFinderBinder {

    Bindings process(Node model, Node payload) {
        Map varPaths = [:]
        Map defaults = [:]
        buildVariablesToPaths(model, varPaths, defaults)
        Map boundVars = fetchVarToValues(varPaths, payload)

        // TODO: seems like this rummaging around can be moved into Bindings
        Bindings bindings = new Bindings(boundVars)
        varPaths.each { k,p ->
            if (!boundVars.containsKey(k) || boundVars[k] == null) {
                boundVars[k] = defaults[k]
                bindings.defaultWasUsed(k)
            }
        }
        bindings
    }

    void buildVariablesToPaths(Node model, Map seenPaths, Map seenVals)
    {
        NodeList nodes = model.depthFirst()
        nodes.each { n ->
            if (n instanceof Node) {
                Node node = (Node) n

                Variables vars = new Variables(node.text())
                if (vars.isPresent()) {
                    def path = formulatePath(node)
                    vars.toEach { v,val ->
                        seenPaths[v] = path
                        seenVals[v] = val
                    }
                }
            }
        }
    }

    private String formulatePath(Node node) {
        if (node) {
            Node parent = node.parent()
            if (parent && parent != node) {
                return formulatePath(parent) + '.' + node.name()
            } else {
                return node.name()
            }
        }

        ""
    }

    private Map fetchVarToValues(Map varToPaths, Node searchHere) {
        Map varToValues = [:]
        Map pathsToVars = invertMap(varToPaths)

        NodeList nodes = searchHere.depthFirst()
        nodes.each { n ->
            if (n instanceof Node) {
                Node node = (Node) n

                def path = formulatePath(node)
                if (pathsToVars.containsKey(path)) {
                    def val = node.text()
                    pathsToVars[path].each { var -> varToValues[var] = val }
                }
            }
        }
        varToValues
    }

    private Map invertMap(Map inMap) {
        HashMap result = [:]
        inMap.each { k,v ->
            if (!result.containsKey(v))
                result[v] = []
            result[v].add(k)
        }
        result
    }
}
