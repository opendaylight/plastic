/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package plans.classifiers

import org.opendaylight.plastic.implementation.PlanningClassifier
import org.opendaylight.plastic.implementation.Schema
import org.opendaylight.plastic.implementation.TranslationPlanLite
import org.opendaylight.plastic.implementation.author.Plans


class ArrayPlanningClassifier extends PlanningClassifier {

    @Override
    TranslationPlanLite classify(Schema pvs, TranslationPlanLite incoming) {

        // In this example, we are not really using anything from the incoming plan,
        // as it is just a hard-wired example. In a real instance, we likely would use
        // information from the input and output schema to construct the parent plan.

        TranslationPlanLite parent = Plans.newParent(
                Plans.asSchema("parent-schema-in", "1.0", "json"),
                Plans.asSchema("parent-schema-out", "1.0", "json"))

        List<TranslationPlanLite> children = Plans.realizeChildPlans(
                pvs,
                "a.b.c[*].d",
                "test-marker",
                Plans.asSchema("child-schema-in", "1.0", "json"),
                Plans.asSchema("child-schema-out", "1.0", "json"))
        parent.addChildren(children)

        parent
    }
}
