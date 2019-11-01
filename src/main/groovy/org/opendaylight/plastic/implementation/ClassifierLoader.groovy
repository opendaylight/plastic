
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.collect.Maps
import groovy.transform.PackageScope

class ClassifierLoader implements ClassifierResolver, Pollee {

    static class ClassifierNotFoundException extends PlasticException {
        ClassifierNotFoundException(String classifierTag, String classifierFileName) {
            super("PLASTIC-CLASSFR-MISSING", "The classifier ${classifierTag} does not have a corresponding file ${classifierFileName}")
        }
    }

    static class MalformedClassifierException extends PlasticException {
        MalformedClassifierException(String classifierName) {
            super("PLASTIC-MALF-CLASSFR", "The following file does not appear to be a valid classifier: ${classifierName}")
        }
    }

    static class DuplicateClassifierClassException extends PlasticException {
        DuplicateClassifierClassException(Class clazz, File file) {
            super("PLASTIC-DUP-CLASSIFIER", "The class ${clazz.getName()} is a duplicate, previously found in file ${file.absolutePath}")
        }
    }

    static class UnknownClassiferClass extends PlasticException {
        UnknownClassiferClass(Classifier classifier) {
            super("PLASTIC-UNKOWN-CLSFR-CLASS", "The class ${classifier.class.simpleName} is a surprise classifier class")
        }
    }

    final FilenamesCache classifiers
    final GroovyClassLoader gcl
    final Map<String,String> seen // <Class-Name,File-Name> (don't reference Class directly)
    final GroovyClassParser gparser // private instance because classifiers are unique population
    final AppContext appProps

    ClassifierLoader(AppContext appProps, FilenamesCache classifiers, GroovyClassLoader gcl) {
        this(appProps, classifiers, gcl, Maps.newConcurrentMap())
    }

    @PackageScope
    ClassifierLoader(AppContext appProps, FilenamesCache classifiers, GroovyClassLoader gcl, Map<String,String> seen) {
        this.appProps = appProps
        this.classifiers = classifiers
        this.gcl = gcl
        this.seen = seen
        this.gparser = new GroovyClassParser(gcl)
    }

    TranslationPlanLite resolve(TranslationPlanLite plan, Schema parsedPayload) {
        if (plan.isUnresolved()) {
            Classifier classifier = locateClassifier(plan)
            addProperties(classifier)
            PlanningClassifier deluxe = wrap(classifier)
            plan = deluxe.classify(parsedPayload, plan)
        }
        plan
    }

    private void addProperties(Classifier classifier) {
        if (classifier.metaClass.respondsTo(classifier, "setContext", Map)) {
            classifier.setContext(appProps.asMap())
        }
    }

    private PlanningClassifier wrap(Classifier classifier) {
        if (classifier instanceof PlanningClassifier)
            return (PlanningClassifier) classifier
        if (classifier instanceof SimpleClassifier)
            return new ClassifierAdapter((SimpleClassifier)classifier)

        throw new UnknownClassiferClass(classifier)
    }

    protected Classifier locateClassifier(TranslationPlanLite plan) {
        String classifierName = plan.getClassifierName()
        File classifierFile = locateClassifierFile(classifierName)
        return asClassifier(classifierFile)
    }

    private File locateClassifierFile(String simpleName) {
        String classifierFileName = computeName(simpleName)
        File classifierFile = new File(classifierFileName)
        if (!classifierFile.exists())
            throw new ClassifierNotFoundException(simpleName, classifierFileName)
        classifierFile
    }

    private String computeName(String classifier) {
        String base = "${classifier}.groovy"
        String result = classifiers.getFile(base)
        result == null ? base : result
    }

    private Classifier asClassifier(File classifierFile) {
        Class clazz = gparser.parseClass(classifierFile)

        String newKey = clazz.getName()
        String newValue = classifierFile.absolutePath

        boolean hasKey = seen.containsKey(newKey)
        boolean hasValue = hasKey && seen[newKey].equals(newValue)

        if ((!hasKey && hasValue) || (hasKey && !hasValue))
            throw new DuplicateClassifierClassException(clazz, classifierFile)

        seen.put(newKey, newValue)
        Object classifierObj = clazz.newInstance()

        if (classifierObj instanceof Classifier)
            return (Classifier) classifierObj

        throw new MalformedClassifierException(newValue)
    }

    @Override
    void phase(int i) {
        gparser.phase(i)
    }
}
