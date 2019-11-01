
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

import java.nio.file.Path

// A collection of test classes to short-circuit using the file system to locate classifiers, morphers, and schemas

class ShortCircuit {

    static class TestClassifierResolver implements ClassifierResolver {

        Map<String, PlanningClassifier> classifiers = Maps.newHashMap()

        @Override
        TranslationPlanLite resolve(TranslationPlanLite plan, Schema parsedPayload) {
            String classifierName = plan.getClassifierName()
            if (classifiers.containsKey(classifierName))
                classifiers.get(classifierName).classify(parsedPayload, plan)
            else
                throw new IllegalArgumentException(classifierName)
        }

        TestClassifierResolver addSimple(String name, SimpleClassifier classifier) {
            addDeluxe(name, new ClassifierAdapter(classifier))
            this
        }

        TestClassifierResolver addDeluxe(String name, PlanningClassifier classifier) {
            classifiers.put(name, classifier)
            this
        }
    }

    static class TestSchemaLoader extends SchemaLoader {

        Map<String,String> streams = Maps.newHashMap()

        TestSchemaLoader() {
            super(new FilenamesCache())
        }

        InputStream locate(VersionedSchema target) {
            if (streams.containsKey(target.name))
                return new ByteArrayInputStream(streams.get(target.name).getBytes())
            else
                throw new IllegalArgumentException(target.name)
        }

        TestSchemaLoader add(String name, String content) {
            streams.put(name, content)
            this
        }
    }

    static class TestMorpherFactory extends MorpherLoader {

        Map<String,Morpher> morphers = Maps.newHashMap()

        TestMorpherFactory() {
            super(new AppContext(), new FilenamesCache("."), new GroovyClassLoader(TestMorpherFactory.class.getClassLoader()))
        }

        Morpher locateImplicitly(VersionedSchema schema) {
            if (morphers.containsKey(schema.name))
                return morphers.get(schema.name)
            else
                return new NopMorpher(schema, new Object(), "dummy-file-name")
        }

        Morpher locateExplicitly(VersionedSchema schema, String morpherName) {
            if (morphers.containsKey(schema.name))
                return morphers.get(schema.name)
            else
                return new NopMorpher(schema, new Object(), "dummy-file-name")
        }

        TestMorpherFactory add(String name, Morpher morpher) {
            morphers.put(name, morpher)
            this
        }
    }

    static class TestPlanResolution extends PlanResolution {
        TestPlanResolution(SchemaLoader modelStreamLocator, MorpherLoader morpherFactory) {
            super(new CachingSchemaSource(modelStreamLocator), morpherFactory)
        }
    }

    static Path createTempDirs(Object parent, String... children) {
        File fchild = File.createTempFile("cart-unit-test-3",".tmp")
        File tmpDir = fchild.parentFile
        fchild.delete()

        File resultDir = new File(tmpDir, parent)
        resultDir.mkdir()
        resultDir.deleteOnExit()

        for (String child : children) {
            fchild = new File(resultDir, child)
            fchild.mkdir()
            fchild.deleteOnExit()
        }

        resultDir.toPath()
    }

    static SearchPath standardSearchPath(String root) {
        new SearchPath(createTempDirs(root, "lib", "classifiers", "morphers", "schemas"))
    }

    static FilenamesCaches useStandardCaches() {
        String uuid = UUID.randomUUID().toString()
        new FilenamesCaches(standardSearchPath("cart-root-${uuid}"), "lib", "classifiers", "morphers", "schemas")
    }
}
