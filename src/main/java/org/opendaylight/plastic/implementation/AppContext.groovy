/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Simple properties management class that handles locating, reading, and comparison.
 * Note that property values coming through this class are treated as strings.
 * Note that if there are multiple property files, the last one wins.
 */
class AppContext {

    private static Logger logger = LoggerFactory.getLogger(AppContext)

    private static List<String> defaultNames = [ 'plastic.properties', 'cartographer.properties' ]

    private final SearchPath path
    private final Properties props

    AppContext() {
        this(null)
    }

    AppContext(SearchPath path) {
        this.props = new Properties()
        this.path = path
        reread()
    }

    private void reread() {
        if (path != null) {
            props.clear()

            defaultNames.each { name ->
                String found = path.findOrElse(name, null)
                if (found != null) {
                    logger.debug("Reading application context properties from: {}", found)
                    File pf = new File(found)
                    pf.withInputStream { strm ->
                        props.load(strm)
                    }
                }
            }
        }
    }

    private boolean isEqualTo(Map other) {
        props.equals(other)
    }

    String getOrElse(String propertyName, String defaultValue) {
        props.containsKey(propertyName) ? props.get(propertyName) : defaultValue
    }

    void addAll(AppContext appProperties) {
        appProperties.props .each { k,v ->
            props.put(k, v)
        }
    }

    Map<String, String> asMap() {
        Map<String, String> results = new HashMap<>()
        results.putAll(props)
        results
    }
}
