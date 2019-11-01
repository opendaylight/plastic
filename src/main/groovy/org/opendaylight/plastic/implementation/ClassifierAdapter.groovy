
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


class ClassifierAdapter extends PlanningClassifier {

    private final SimpleClassifier inner;

    ClassifierAdapter(SimpleClassifier inner) {
        this.inner = checkNotNull(inner);
    }

    @Override
    TranslationPlanLite classify(Schema parsedPayload, TranslationPlanLite plan) {
        plan.resolveUsing(inner.classify(parsedPayload.getParsed()));
        return plan;
    }
}
