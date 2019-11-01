
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;


interface Format {
    /**
     * @return a unique key for this format
     */
    String formatKey()

    /**
     *
     * @param case-insensitive candidate key
     * @return if this format is identified by the candiate key
     */
    boolean matches(String key)

    /**
     *
     * @param input stream of text to parse
     * @return parsed object (or throw)
     */
    Object parse(InputStream inStrm)

    /**
     *
     * @param object to clone
     * @return deep copy of object
     */
    Object clone(Object other)

    /**
     *
     * @param object to serialized
     * @return serialization of object (or throw)
     */
    String serialize(Object source)

    /**
     *
     * @param source serialization
     * @return fully realized object (or throw)
     */
    Object deserialize(String source)

    /**
     *
     * @return new aggregator for this format
     */
    Aggregator createAggregator()
}
