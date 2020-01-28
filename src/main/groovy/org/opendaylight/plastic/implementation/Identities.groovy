
/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.plastic.implementation

/**
 * Class provides a long id as a surrogate for the reference to the object.
 * Different references will always have different ids.
 * The same reference will always have the same id.
 */

class Identities {
    private IdentityHashMap<Object,Long> references = new IdentityHashMap()
    private long nextId = 0

    long get(Object obj) {
        Long id = references.get(obj);
        if (id == null)
            references.put(obj, id = nextId++);
        return id;
    }
}
