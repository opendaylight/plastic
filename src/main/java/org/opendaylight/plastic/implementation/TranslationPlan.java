/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

import com.google.common.collect.Lists;
import groovy.lang.Closure;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;


class TranslationPlan<S,M>
{
    static class TranslationPlanEmpty extends PlasticException {
        TranslationPlanEmpty() {
            super("PLASTIC-TRANS-PLAN-EMPTY", "Translation plan is empty!");
        }
    }

    static class TranslationPlanMissingSchema extends PlasticException {
        TranslationPlanMissingSchema() {
            super("PLASTIC-TRANS-PLAN-MISSING-SCHEMA", "Translation plan is missing a schema!");
        }
    }

    static class TranslationPlanTooManySchemas extends PlasticException {
        TranslationPlanTooManySchemas(int count) {
            super("PLASTIC-TRANS-PLAN-EXTRA-SCHEMAS", "Translation plan expected two schemas but found "+ count);
        }
    }

    static class TranslationPlanUnresolved extends PlasticException {
        TranslationPlanUnresolved(String schemaInfo) {
            super("PLASTIC-TRANS-PLAN-UNRESOLVED", "Translation plan is unresolved: "+schemaInfo);
        }
    }

    protected List<S> schemas = Lists.newArrayList();
    protected List<M> morphers = Lists.newArrayList();

    public TranslationPlan() {
    }

    public TranslationPlan(S input, S output) {
        this.schemas.add(checkNotNull(input));
        this.schemas.add(checkNotNull(output));
    }

    public TranslationPlan(S input, S output, List<M> morphers) {
        this.schemas.add(checkNotNull(input));
        this.schemas.add(checkNotNull(output));
        this.morphers.addAll(morphers);
    }

    public void addSchema(S schema) {
        schemas.add(checkNotNull(schema));
    }

    public void addMorpher(M morpher) {
        morphers.add(checkNotNull(morpher));
    }

    public void maybeAddMorpher(M morpher) {
        if (morpher != null)
            addMorpher(morpher);
    }

    public S firstSchema() {
        return schemas.get(0);
    }

    public S lastSchema() {
        return schemas.get(schemas.size()-1);
    }

    public void replaceFirstSchmea(S replacement) {
        schemas.set(0, checkNotNull(replacement));
    }

    public void eachSchema(Closure cls) {
        for (S schema : schemas) {
            cls.call(schema);
        }
    }

    public List<S> schemas() {
        return schemas;
    }

    public boolean hasMorphers() {
        return !morphers.isEmpty();
    }

    public void eachMorpher(Closure cls) {
        for (M morpher : morphers) {
            cls.call(morpher);
        }
    }

    public List<M> morphers() {
        return morphers;
    }

    public void validate() {
        switch (schemas.size()) {
            case 0:
                throw new TranslationPlanEmpty();
            case 1:
                throw new TranslationPlanMissingSchema();
            case 2:
                break;
            default:
                // for now: only 2
                // future: pipelining translations will have many schemas
                throw new TranslationPlanTooManySchemas(schemas.size());
        }
    }
}
