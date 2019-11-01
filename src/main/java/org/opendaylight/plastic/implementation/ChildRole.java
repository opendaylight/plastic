
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

import static com.google.common.base.Preconditions.checkNotNull;


public class ChildRole implements TranslationPlanLite.Role {

    private final Schema payload;
    private final String name;

    public ChildRole(String name, Schema subPayload) {
        this.name = checkNotNull(name);
        this.payload = checkNotNull(subPayload);
    }

    public String getName() {
        return name;
    }

    public Schema payload() {
        return payload;
    }
}
