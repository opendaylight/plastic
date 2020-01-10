
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

class ClassifierLoaderSpec extends Specification {

    File createFile (File dir, String name, String contents) {
        File f = new File(dir, name)
        f.createNewFile()
        f.text = contents
        f.deleteOnExit()
        f
    }

    AppContext props = new AppContext()
    SearchPath searcher = new SearchPath("testapplication1.properties")
    FilenamesCache fileCache = new FilenamesCache(searcher.find("classifiers"))

    GroovyClassLoader gcl = new GroovyClassLoader(this.class.classLoader)
    ClassifierLoader instance = new ClassifierLoader(props, fileCache, gcl)

    VersionedSchema fakePayloadSchema = new VersionedSchema("abcdef", "1.0", "json")

    String rawPayload = '{ "msg1": "foo", "msg2": "bar" }'
    Schema payload = new Schema(fakePayloadSchema, rawPayload)

    String clName = "MyTestClassifier"

    String clCode = """
        package unit.test
        import org.opendaylight.plastic.implementation.SimpleClassifier

        class ${clName} extends SimpleClassifier
        {
            String classify(Object parsedPayload)
            {
                "test"
            }
        }
    """

    String dlxCode = """
        import org.opendaylight.plastic.implementation.PlanningClassifier
        import org.opendaylight.plastic.implementation.Schema
        import org.opendaylight.plastic.implementation.TranslationPlanLite

        class ${clName} extends PlanningClassifier
        {
            TranslationPlanLite classify(Schema parsedPayload, TranslationPlanLite plan)
            {
                plan.resolveUsing("test")
                plan
            }
        }
    """

    def setup() {
        fileCache.scan()
    }

    def "a normal (resolved) version schema does not need classification"() {
        given:
        VersionedSchema existingSchema = new VersionedSchema("lmn-input", "1.0", "json")
        TranslationPlanLite existing = new TranslationPlanLite(existingSchema, existingSchema)
        when:
        instance.resolve(existing, payload)
        then:
        existing.firstSchema().equals(existingSchema)
    }

    def "multiple resolving version schemas are not supported"() {
        given:
        VersionedSchema badSchema = new VersionedSchema('${abc}${def}', "1.0", "json")
        TranslationPlanLite bad = new TranslationPlanLite(badSchema, badSchema)
        when:
        instance.resolve(bad, payload)
        then:
        thrown(TranslationPlanLite.NoMultipleVariablesException)
    }

    def "an unresolvable version schema should result in an exception"() {
        given:
        VersionedSchema badSchema = new VersionedSchema('${xyzzy}', "1.0", "json")
        TranslationPlanLite bad = new TranslationPlanLite(badSchema, badSchema)
        when:
        instance.resolve(bad, payload)
        then:
        thrown(ClassifierLoader.ClassifierNotFoundException)
    }

    def "an badly formed classifier is caught"() {
        given:
        VersionedSchema badSchema = new VersionedSchema('${BadlyFormedClassifier}', "1.0", "json")
        TranslationPlanLite bad = new TranslationPlanLite(badSchema, badSchema)
        when:
        instance.resolve(bad, payload)
        then:
        thrown(ClassifierLoader.MalformedClassifierException)
    }

    def "a classifier may not return null or blanks"() {
        given:
        VersionedSchema badSchema = new VersionedSchema('${NullClassifier}', "1.0", "json")
        TranslationPlanLite bad = new TranslationPlanLite(badSchema, badSchema)
        when:
        instance.resolve(bad, payload)
        then:
        thrown(TranslationPlanLite.MalformedReplacementSchemanNamePartException)
    }

    def "a classifier can return a value"() {
        given:
        VersionedSchema testSchema = new VersionedSchema('${AbcClassifier}', "1.0", "json")
        TranslationPlanLite test = new TranslationPlanLite(testSchema, testSchema)
        when:
        instance.resolve(test, payload)
        then:
        test.firstSchema().name == "abc"
    }

    def "a classifier can return a value that is just part of the schema name"() {
        given:
        VersionedSchema testSchema = new VersionedSchema('Xxx${AbcClassifier}Yyy', "1.0", "json")
        TranslationPlanLite test = new TranslationPlanLite(testSchema, testSchema)
        when:
        instance.resolve(test, payload)
        then:
        test.firstSchema().name == "XxxabcYyy"
    }

    def "a classifier can use the parsed payload to calculate a return value"() {
        given:
        VersionedSchema testSchema = new VersionedSchema('${GoodClassifier}', "1.0", "json")
        TranslationPlanLite test = new TranslationPlanLite(testSchema, testSchema)
        when:
        instance.resolve(test, payload)
        then:
        test.firstSchema().name == "foobar"
    }

    def "a duplicate classifier results in an error"() {
        given:
        File classifiersDir = new File(searcher.find("classifiers"))
        createFile(classifiersDir, clName+"1.groovy", clCode)
        createFile(classifiersDir, clName+"2.groovy", clCode)
        fileCache.scan()
        when:
        for (i in (1..2)) {
            VersionedSchema testSchema = new VersionedSchema('${' + clName + i + '}', "1.0", "json")
            TranslationPlanLite test = new TranslationPlanLite(testSchema, testSchema)
            instance.resolve(test, payload)
        }
        then:
        thrown(ClassifierLoader.DuplicateClassifierClassException)
    }

    def "a deluxe classifier is successful"() {
        given:
        File classifiersDir = new File(searcher.find("classifiers"))
        createFile(classifiersDir, clName+"3.groovy", dlxCode)
        fileCache.scan()
        when:
        VersionedSchema testSchema = new VersionedSchema('${' + clName + '3}', "1.0", "json")
        TranslationPlanLite test = new TranslationPlanLite(testSchema, testSchema)
        instance.resolve(test, payload)
        then:
        notThrown(Exception)
    }
}
