
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

public interface CartographyService extends AutoCloseable {

    String translate(String inSchemaName, String inVersion, String inType,
                     String outSchemaName, String outVersion, String outType,
                     String payload);

    String translate(String inSchemaName, String inVersion, String inType,
                     String outSchemaName, String outVersion, String outType,
                     String payload, String defaults);

}
