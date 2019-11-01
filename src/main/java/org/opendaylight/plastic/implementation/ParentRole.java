
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

import java.util.ArrayList;
import java.util.List;

public class ParentRole implements TranslationPlanLite.Role {

    private List<TranslationPlanLite> children = new ArrayList<>();

    public void addChild(TranslationPlanLite child) {
        validChild(child);
        children.add(child);
    }

    private void validChild(TranslationPlanLite child) {
        if(child == null)
            throw new IllegalArgumentException("Cannot add a NULL child translation plan to parent.");
        if (!child.hasChildRole())
            throw new IllegalArgumentException("Cannot add a translation plan to parent unless it has a child role.");
    }

    public TranslationPlanLite[] childPlans() {
        return children.toArray(new TranslationPlanLite[0]);
    }
}
