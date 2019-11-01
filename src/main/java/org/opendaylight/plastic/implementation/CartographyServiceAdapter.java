
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;


import static org.opendaylight.plastic.implementation.Cartography.EMPTY_DEFAULTS;

/*
 * JSON/RPC has a limitation that classes cannot be used as method arguments,
 * so this class is adapting the primitives into real class arguments and
 * passing them on.
 */
public class CartographyServiceAdapter implements CartographyService {

    private final Cartography cartography;

    public CartographyServiceAdapter(Cartography cartography) {
        this.cartography = cartography;
    }

    public String translate(String inSchemaName, String inVersion, String inType,
                     String outSchemaName, String outVersion, String outType,
                     String payload)
    {
        return translate(inSchemaName, inVersion, inType, outSchemaName, outVersion, outType, payload, EMPTY_DEFAULTS);
    }

    @Override
    public String translate(String inSchemaName, String inVersion, String inType, String outSchemaName, String outVersion, String outType, String payload, String defaults) {
        VersionedSchema in = new VersionedSchema(inSchemaName, inVersion, inType);
        VersionedSchema out = new VersionedSchema(outSchemaName, outVersion, outType);
        return cartography.translateWithDefaults(in, out, payload, defaults);
    }

    @Override
    public void close() throws Exception {
        cartography.close();
    }
}
