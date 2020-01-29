/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation

import com.google.common.base.Preconditions

class SimpleVariableFetcher implements VariablesFetcher {

    final List<String> varNames = new ArrayList<>()

    SimpleVariableFetcher(String name) {
        Preconditions.checkArgument(name != null && !name.isEmpty())
        varNames.add(name)
    }

    SimpleVariableFetcher(Set<String> names) {
        Preconditions.checkArgument(names != null && !names.isEmpty())
        this.varNames.addAll(names)
    }

    @Override
    Bindings fetch(Object candidateValue) {
        Bindings results = new Bindings()
        for (String name : varNames) {
            results.set(name, candidateValue)
        }
        results
    }

    // TODO: maybe use ImmutableList

    @Override
    List<String> names() {
        varNames
    }

    @Override
    String toString() {
        varNames.toString()
    }
}
