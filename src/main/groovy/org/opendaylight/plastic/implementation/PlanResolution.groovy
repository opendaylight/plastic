
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import groovy.transform.PackageScope

class PlanResolution {

    SchemaSource schemaSource
    MorpherLoader morpherLoader

    @PackageScope
    PlanResolution(SchemaSource schemaSource, MorpherLoader morpherLoader) {
        this.schemaSource = schemaSource
        this.morpherLoader = morpherLoader
    }

    Schema createSchema(VersionedSchema inSchema, String raw) {
        return schemaSource.createSchema(inSchema, raw)
    }

    Schema createSimilarSchema(VersionedSchema reference, String content) {
        return schemaSource.createSimilar(reference, content)
    }

    MapTask lookupMappings(TranslationPlanLite plan) {

        TranslationPlan<Schema,Morpher> boundPlan = new TranslationPlan<>()

        plan.eachSchema {
            VersionedSchema schema ->
                boundPlan.addSchema(schemaSource.createSchema(schema))
        }

        if (plan.hasMorphers()) {
            VersionedSchema schema = plan.firstSchema()
            plan.eachMorpher {
                String mname ->
                    Morpher morpher
                    if (plan.isDefaultMorpher(mname))
                        morpher = morpherLoader.locateImplicitly(schema)
                    else
                        morpher = morpherLoader.locateExplicitly(schema, mname)
                    boundPlan.addMorpher(morpher)
            }
        }
        else {
            plan.eachSchema {
                VersionedSchema schema ->
                    Morpher morpher = morpherLoader.locateImplicitly(schema)
                    boundPlan.maybeAddMorpher(morpher)
            }
        }

        return new MapTask(boundPlan)
    }
}
