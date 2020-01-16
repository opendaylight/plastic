
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import spock.lang.Specification

class PlanResolutionSpec extends Specification {

    VersionedSchema someSchema = new VersionedSchema("nui-create-epnm", "2.1", "JSON")
    TranslationPlanLite somePlan = new TranslationPlanLite(someSchema, someSchema)

    SchemaSource mockSchemaSource = Mock() {
        createSchema(_) >> new Schema(someSchema, "")
        createSchema(_,_) >> new Schema(someSchema, "")
    }
    MorpherLoader mockMorpherLoader = Mock()

    PlanResolution instance = new PlanResolution(mockSchemaSource, mockMorpherLoader)

    def "resolving includes locating each schema"() {
        when:
        instance.lookupMappings(somePlan)
        then:
        2 * instance.schemaSource.createSchema(_) >> new Schema(someSchema, "")
    }

    def "resolving includes locating default morphers"() {
        when:
        instance.lookupMappings(somePlan)
        then:
        2 * instance.morpherLoader.locateImplicitly(*_)
    }

    def "resolving skips default morphers if all explicit morphers present"() {
        given:
        List<String> morphers = ["morpher-A", "morpher-B", "morpher-C", "morpher-D", "morpher-E"]
        int count = morphers.size()
        TranslationPlanLite explicitPlan = new TranslationPlanLite(someSchema, someSchema, morphers)
        when:
        instance.lookupMappings(explicitPlan)
        then:
        count * instance.morpherLoader.locateExplicitly(*_) >> Mock(Morpher)
        0 * instance.morpherLoader.locateImplicitly(*_)
    }

    def "resolving skips explicit morphers if all supplied morphers are default morphers"() {
        given:
        TranslationPlanLite explicitPlan = new TranslationPlanLite(someSchema, someSchema)
        explicitPlan.addMorpher(TranslationPlanLite.DEFAULT_INPUT_MORPHER)
        explicitPlan.addMorpher(TranslationPlanLite.DEFAULT_OUTPUT_MORPHER)
        when:
        instance.lookupMappings(explicitPlan)
        then:
        0 * instance.morpherLoader.locateExplicitly(*_)
        2 * instance.morpherLoader.locateImplicitly(*_) >> Mock(Morpher)
    }

    def "resolving can handle a mixture of default morpher and explicit morphers"() {
        given:
        List<String> morphers = ["morpher-A", "morpher-B", "morpher-C", "morpher-D", "morpher-E"]
        int count = morphers.size()
        and:
        TranslationPlanLite explicitPlan = new TranslationPlanLite(someSchema, someSchema, morphers)
        explicitPlan.addMorpher(TranslationPlanLite.DEFAULT_OUTPUT_MORPHER)
        when:
        instance.lookupMappings(explicitPlan)
        then:
        count * instance.morpherLoader.locateExplicitly(*_) >> Mock(Morpher)
        1 * instance.morpherLoader.locateImplicitly(*_) >> Mock(Morpher)
    }
}
