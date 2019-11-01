
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

class PayloadAggregationSpec extends Specification {
    PayloadAggregation instance = new PayloadAggregation()

    List<VersionedSchemaRaw> items = []

    def "empty collections cannot be aggregated"() {
        when:
        instance.aggregate([])
        then:
        thrown(PayloadAggregation.AggregationEmpty)
    }

    def "heterogenous collections of schema types cannot be aggregated"() {
        given:
        def jsonItem = new VersionedSchemaRaw("abc", "1.0", "json", "")
        def xmlItem = new VersionedSchemaRaw("def", "1.0", "xml", "")
        when:
        instance.aggregate([ jsonItem, xmlItem ])
        then:
        thrown(PayloadAggregation.AggregationSchemaMismatch)
    }

    def "homogenous inline collections can be aggregated"() {
        given:
        String payload = "{ \"abc\": 1 }"
        and:
        def jsonItem = new VersionedSchemaRaw("abc", "1.0", "cjson", payload)
        def jsonItem2 = new VersionedSchemaRaw("def", "1.0", "cjson", payload)
        and:
        Aggregator aggregator = new ChunkyJsonAggregator()
        aggregator.add(payload)
        aggregator.add(payload)
        String expected = aggregator.emit()
        when:
        VersionedSchemaRaw result = instance.aggregate([ jsonItem, jsonItem2 ])
        then:
        result.raw == expected
    }

    def "homogenous inline collections can be de-aggregated"() {
        given:
        String item = "{ \"abc\": 1 }"
        Aggregator aggregator = new ChunkyJsonAggregator()
        aggregator.add(item)
        String payload = aggregator.emit()
        def schema = new VersionedSchema("some-schema", "1.0", "cjson")
        when:
        List<String> result = instance.deAggregate(schema, payload)
        then:
        result == [ item ]
    }

    def "homogenous wrapped collections can be aggregated"() {
        given:
        String payload = "<abc>1</abc>"
        and:
        def xmlItem1 = new VersionedSchemaRaw("abc", "1.0", "xml", payload)
        def xmlItem2 = new VersionedSchemaRaw("def", "1.0", "xml", payload)
        and:
        Aggregator aggregator = new XmlAggregator()
        aggregator.add(payload)
        aggregator.add(payload)
        String expected = aggregator.emit()
        when:
        VersionedSchemaRaw result = instance.aggregate([ xmlItem1, xmlItem2 ])
        then:
        result.raw == expected
    }

    def "collections can be aggregated via suppliers"() {
        given:
        String payload = "{ \"abc\": 1 }"
        and:
        def jsonItem = new VersionedSchemaRaw("abc", "1.0", "json", payload)
        def jsonItem2 = new VersionedSchemaRaw("def", "1.0", "json", payload)
        and:
        VersionedSchemaRawSupplier supplier1 = new VersionedSchemaRawSupplier() {
            @Override
            VersionedSchemaRaw get() {
                return jsonItem
            }
        }
        VersionedSchemaRawSupplier supplier2 = new VersionedSchemaRawSupplier() {
            @Override
            VersionedSchemaRaw get() {
                return jsonItem2
            }
        }
        and:
        Aggregator aggregator = new JsonAggregator()
        aggregator.add(payload)
        aggregator.add(payload)
        String expected = aggregator.emit()
        when:
        VersionedSchemaRaw result = instance.aggregateViaSuppliers([ supplier1, supplier2 ])
        then:
        result.raw == expected
    }
}
