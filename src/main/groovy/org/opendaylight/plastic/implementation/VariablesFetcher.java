/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation;

import java.util.List;

/*
 * Defines a mechanism for retrieving variable bindings from a target string
 */
public interface VariablesFetcher {

    /*
     * Empty or null candidate results in an empty (not null) set of bindings
     * being returned.
     */
    Bindings fetch(Object candidate);

    /*
     * Return the list of variable names associated with this binder
     */
    List<String> names();
}
