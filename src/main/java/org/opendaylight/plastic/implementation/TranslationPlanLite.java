
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

import static com.google.common.base.Preconditions.checkNotNull;


public class TranslationPlanLite extends TranslationPlan<VersionedSchema,String> {

    interface Role {}
    private interface NotChildRole extends Role {}
    private static class NoRole implements Role {}

    static class ResolutionNotPossible extends PlasticException {
        ResolutionNotPossible(String msg) {
            super("PLASTIC-NO-VAR-NAME-SCHEMA", "The following schema name has no classifier name present: "+ msg);
        }
    }

    static class NoMultipleVariablesException extends PlasticException {
        NoMultipleVariablesException(String msg) {
            super("PLASTIC-MULT-VAR-NAME-SCHEMA", "The following schema name has more than one variable: "+ msg);
        }
    }

    static class MalformedReplacementSchemanNamePartException extends PlasticException {
        MalformedReplacementSchemanNamePartException() {
            super("PLASTIC-MALF-SCH-NAM-PART", "The classifier returned a null or blank result");
        }
    }

    static class WrongRoleException extends PlasticException {
        WrongRoleException(Class<?> wanted, Class<?> found) {
            super("PLASTIC-WRONG-ROLE", "Translation plan found in wrong role. Wanted: " +
                    wanted.getSimpleName() +
                    " Found: " +
                    found.getSimpleName()
            );
        }

        WrongRoleException(Class<?> wanted, Role found) {
            this(wanted, found.getClass());
        }
    }

    public static final String DEFAULT_INPUT_MORPHER = "DEFAULT-INPUT-MORPHER-PLACEHOLDER";
    public static final String DEFAULT_OUTPUT_MORPHER = "DEFAULT-OUTPUT-MORPHER-PLACEHOLDER";

    private Role role = new NoRole();

    public TranslationPlanLite(TranslationPlanLite other) {
        other.schemas.forEach((VersionedSchema vs) -> { schemas.add(vs.clone()); });
        this.morphers.addAll(other.morphers);
    }

    public TranslationPlanLite(VersionedSchema input, VersionedSchema output) {
        super(input, output);
    }

    public TranslationPlanLite(VersionedSchema input, VersionedSchema output, List<String> morphers) {
        super(input, output, morphers);
    }

    public boolean isDefaultMorpher(String mname) {
        return mname.equals(DEFAULT_INPUT_MORPHER) || mname.equals(DEFAULT_OUTPUT_MORPHER);
    }

    public boolean isUnresolved() {
        Variables variables = new Variables(firstSchema().getName());
        return variables.isPresent();
    }

    public String getClassifierName() {
        Variables variables = new Variables(firstSchema().getName());
        validateResolvability(variables);
        return variables.get();
    }

    private void validateResolvability(Variables variables) {
        if (!variables.isPresent())
            throw new ResolutionNotPossible(variables.raw());
        if (variables.hasMultiple())
            throw new NoMultipleVariablesException(variables.raw());
    }

    public void resolveUsing(String schemaNamePart) {
        VersionedSchema unresolved = firstSchema();
        Variables variables = new Variables(unresolved.getName());
        validateResolvability(variables);

        String variableName = variables.get();
        if (isNothing(schemaNamePart))
            throw new MalformedReplacementSchemanNamePartException();

        String adorned = variables.adorn(variableName);
        String resolved = replace(unresolved.getName(), adorned, schemaNamePart);
        replaceFirstSchmea(unresolved.rename(resolved));
    }

    private boolean isNothing(String s) {
        return s == null || s.isEmpty() || s.trim().isEmpty();
    }

    // Avoid the special character collision between regex and dollar variables

    private String replace(String source, String from, String to) {
        int here = source.indexOf(from);
        String prefix = source.substring(0, here);
        String suffix = source.substring(here+from.length());
        return prefix+to+suffix;
    }

    @Override
    public void validate() {
        for (VersionedSchema schema : schemas) {
            Variables variables = new Variables(schema.getName());
            if (variables.isPresent())
                throw new TranslationPlanUnresolved(schema.getName());
        }
    }

    public void validateLineage(TranslationPlanLite resolved) {
        if (!role.equals(resolved.role)) {
            throw new PlasticException("PLASTIC-TR-PLAN-UNSUPPORTED",
                    "Translation plan resolution changing plan role is not supported. " +
                            "This is likely an error in your classifier.");
        }
    }

    public TranslationPlanLite resolve(ClassifierResolver classifierLocator, Schema parsedPayload) {
        mustNotBeChild();
        return classifierLocator.resolve(this, parsedPayload);
    }

    public TranslationPlanLite resolve(ClassifierResolver classifierLocator) {
        return classifierLocator.resolve(this, getChildRole().payload());
    }

    public boolean hasChildRole() {
        return role.getClass().equals(ChildRole.class);
    }

    public boolean hasParentRole() {
        return role.getClass().equals(ParentRole.class);
    }

    public void setRole(Role role) {
        this.role = checkNotNull(role);
    }

    public ChildRole getChildRole() {
        mustBeChild();
        return (ChildRole) role;
    }

    private void mustBeChild() {
        if (!role.getClass().equals(ChildRole.class))
            throw new WrongRoleException(ChildRole.class, role);
    }

    private void mustNotBeChild() {
        if (role.getClass().equals(ChildRole.class))
            throw new WrongRoleException(NotChildRole.class, role);
    }

    public ParentRole getParentRole() {
        mustBeParent();
        return (ParentRole) role;
    }

    private void mustBeParent() {
        if (!role.getClass().equals(ParentRole.class))
            throw new WrongRoleException(ParentRole.class, role);
    }

    public TranslationPlanLite addChild(TranslationPlanLite child) {
        getParentRole().addChild(child);
        return this;
    }

    public TranslationPlanLite addChildren(List<TranslationPlanLite> children) {
        for (TranslationPlanLite child : children)
            getParentRole().addChild(child);
        return this;
    }
}
