
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

import java.util.ArrayList;
import java.util.List;


public class PayloadAggregation {

    static private class AggregationSchemaMismatch extends PlasticException {
        AggregationSchemaMismatch(VersionedSchema expected, VersionedSchema found) {
            this(expected.getType(), found.getType());
        }

        AggregationSchemaMismatch(String expected, String found) {
            super("PLASTIC-AGG-SCHEMA-MATCH",
                    "Payload aggregation failed homogeneous schema requirement - expected:"
                            + expected +" found: " +found);
        }
    }

    static private class AggregationEmpty extends PlasticException {
        AggregationEmpty() {
            super("PLASTIC-AGG-EMTPY", "Payload aggregation failed because incoming collection was empty");
        }
    }

    private final Formats formats;

    public PayloadAggregation() {
        this(new Formats());
    }

    PayloadAggregation(Formats formats) {
        this.formats = formats;
    }

    public VersionedSchemaRaw aggregateViaSuppliers(List<? extends VersionedSchemaRawSupplier> suppliers) throws PlasticException {
        List<VersionedSchemaRaw> inner = new ArrayList<>();
        for (VersionedSchemaRawSupplier supplier : suppliers) {
            inner.add(supplier.get());
        }
        return aggregate(inner);
    }

    public VersionedSchemaRaw aggregate (List<VersionedSchemaRaw> incoming) throws PlasticException {
        mustNotBeEmpty(incoming);
        mustBeSameSchema(incoming);

        VersionedSchemaRaw first = incoming.get(0);
        Aggregator aggregator = createAggregator(first);

        for (VersionedSchemaRaw vsr : incoming) {
            aggregator.add(vsr.getRaw());
        }

        return first.cloneWith(aggregator.emit());
    }

    private Aggregator createAggregator(VersionedSchemaRaw first) throws PlasticException {
        return createAggregator(first.getSchema());
    }

    public Aggregator createAggregator(String schemaType) throws PlasticException {
        return formats.mustLookup(schemaType).createAggregator();
    }

    public Aggregator createAggregator(VersionedSchema schema) throws PlasticException {
        return formats.mustLookup(schema.getType()).createAggregator();
    }

    private void mustNotBeEmpty(List<VersionedSchemaRaw> incoming) {
        if (incoming.isEmpty())
            throw new AggregationEmpty();
    }

    private void mustBeSameSchema(List<VersionedSchemaRaw> incoming) {
        if (!incoming.isEmpty()) {
            VersionedSchemaRaw first = incoming.get(0);

            for (VersionedSchemaRaw vsr : incoming) {
                if (!vsr.matches(first))
                    throw new AggregationSchemaMismatch(first.getSchema(), vsr.getSchema());
            }
        }
    }

    public List<String> deAggregate (VersionedSchema schema, String payload) {
        Aggregator aggregator = createAggregator(schema);
        return aggregator.deAggregate(payload);
    }
}
