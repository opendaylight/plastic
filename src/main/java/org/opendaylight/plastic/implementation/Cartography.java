
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

public interface Cartography {

    /**
     * An abstract representation of empty defaults that should be recognized
     * by the various parsers for JSON, XML, and others
     */
    String EMPTY_DEFAULTS = "";

    /**
     * Translate the given JSON/XML payload according to the given schemas and return
     * the result (or throw an exception)
     *
     * @param input is the input schema information for a pre-existing schema definition
     * @param output is the output schema information for a pre-existing schema definition
     * @param payload is the string version of a payload in one of the supported formats
     * @return an instance of the output schema populated with appropriate values
     */
    String translate(VersionedSchema input, VersionedSchema output, String payload);

    /**
     * Similar to translate above but takes a map of variable-name-to-values that specifies
     * defaults for variables. This map can have 0 or more entries.
     *
     * @param input (see above)
     * @param output (see above)
     * @param payload (see above)
     * @param defaults a JSON encoded map keyed by variable name with values
     * @return (see above)
     */
    String translateWithDefaults(VersionedSchema input, VersionedSchema output, String payload, String defaults);

    /**
     * Close any internal queues in preparation for quitting.
     */
    void close();
}
