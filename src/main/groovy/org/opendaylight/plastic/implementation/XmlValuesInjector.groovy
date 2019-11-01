
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import java.util.regex.Pattern


class XmlValuesInjector {

    static class DangingInputsException extends PlasticException {

        Set danglings

        DangingInputsException(Set danglings) {
            super("PLASTIC-DANGLING-INPUTS", "The following input variables were not matched to output variables: " + danglings)
            this.danglings = danglings
        }
    }

    static class DangingOutputsException extends PlasticException {

        Set danglings

        DangingOutputsException(Set danglings) {
            super("PLASTIC-DANGLING-OUTPUTS", "The following output variables were not matched to input variables: " + danglings)
            this.danglings = danglings
        }
    }

    Node inject(Map values, Node model) {

        def expectedInputVars = values.keySet() as Set
        def foundInputVars = [] as Set
        def danglingOutputVars = [] as Set

        NodeList nodes = model.depthFirst()
        nodes.each { n ->
            if (n instanceof Node) {
                Node node = (Node) n
                String text = node.localText().isEmpty() ? "" : node.localText().get(0)
                Variables vars = new Variables(text)
                if (vars.isPresent()) {
                    def replaced = text
                    vars.toEach { v,val ->
                        if (values.containsKey(v)) {
                            Object value = values[v] == null ? "" : values[v] // could be string, boolean, int, ...
                            foundInputVars.add(v)
                            String fullVar = Pattern.quote(vars.adorn(v))
                            if (fullVar == text)
                                replaced = value // preserves original type
                            else
                                replaced = replaced.replaceAll(fullVar, value.toString())
                        }
                        else {
                            danglingOutputVars.add(v)
                        }
                    }

                    if (text != replaced) {
                        node.value().set(0, replaced)
                    }
                }
            }
        }

        reportDanglingOutputs(danglingOutputVars)
        reportDanglingInputs(expectedInputVars - foundInputVars)

        model
    }

    private void reportDanglingInputs(Set danglingInputVars) {
        if (!danglingInputVars.isEmpty()) {
            throw new DangingInputsException(danglingInputVars)
        }
    }

    private void reportDanglingOutputs(Set danglingOutputVars) {
        if (!danglingOutputVars.isEmpty()) {
            throw new DangingOutputsException(danglingOutputVars)
        }
    }
}
