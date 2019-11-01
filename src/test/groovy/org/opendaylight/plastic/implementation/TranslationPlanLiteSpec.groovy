
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

class TranslationPlanLiteSpec extends Specification {

    VersionedSchema resolvedIn = new VersionedSchema("foo", "1.0", "json")
    VersionedSchema unresolvedIn = new VersionedSchema("\${some-classifier}", "1.0", "json")
    VersionedSchema schemaOut = new VersionedSchema("foo", "1.0", "json")

    Schema mockSchema = Mock()
    ClassifierLoader mockLocator = Mock()

    def "a resolved plan says it is already resolved"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(resolvedIn.clone(), schemaOut.clone())
        expect:
        !instance.isUnresolved()
    }

    def "an unresolved plan says it is unresolved"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        expect:
        instance.isUnresolved()
    }

    def "an already resolved plan cannot be resolved again"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(resolvedIn.clone(), schemaOut.clone())
        when:
        instance.resolveUsing("bar")
        then:
        thrown(TranslationPlanLite.ResolutionNotPossible)
    }

    def "an unresolved plan can be resolved"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        when:
        instance.resolveUsing("bar")
        then:
        old(instance.isUnresolved()) && !instance.isUnresolved()
    }

    def "a resolved plan is valid"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(resolvedIn.clone(), schemaOut.clone())
        expect:
        instance.validate()
    }

    def "an unresolved plan is not valid"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        when:
        instance.validate()
        then:
        thrown(TranslationPlan.TranslationPlanUnresolved)
    }

    def "an unresolved plan has a name for the classifier to resolve it"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        expect:
        instance.getClassifierName() == "some-classifier"
    }

    def "a plan can be designated as a child plan"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        instance.setRole(new ChildRole("some-path", mockSchema))
        expect:
        instance.hasChildRole()
        !instance.hasParentRole()
    }

    def "a plan can be designated as a parent plan"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        instance.setRole(new ParentRole())
        expect:
        !instance.hasChildRole()
        instance.hasParentRole()
    }

    def "resolving using the implicit payload is for child plans"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        instance.setRole(new ChildRole("some-path", mockSchema))
        when:
        instance.resolve(mockLocator)
        then:
        notThrown()
    }

    def "resolving using the implicit payload is not for parent plans"() {
        given:
        TranslationPlanLite parent = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        parent.setRole(new ParentRole())
        when:
        parent.resolve(mockLocator)
        then:
        thrown(TranslationPlanLite.WrongRoleException)
    }

    def "resolving using the explicit payload is for parent plans"() {
        given:
        TranslationPlanLite parent = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        parent.setRole(new ParentRole())
        when:
        parent.resolve(mockLocator, mockSchema)
        then:
        notThrown(TranslationPlanLite.WrongRoleException)
    }

    def "resolving using the explicit payload is not for child plans"() {
        given:
        TranslationPlanLite instance = new TranslationPlanLite(unresolvedIn.clone(), schemaOut.clone())
        instance.setRole(new ChildRole("some-path", mockSchema))
        when:
        instance.resolve(mockLocator, mockSchema)
        then:
        thrown(TranslationPlanLite.WrongRoleException)
    }
}
