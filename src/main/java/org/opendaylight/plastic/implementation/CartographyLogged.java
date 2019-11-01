
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

public class CartographyLogged implements Cartography {

    BetterLogger log;
    Cartography inner;

    CartographyLogged (Cartography inner) {
        this(inner, new BetterLogger(Cartography.class));
    }

    CartographyLogged(Cartography inner, BetterLogger logger) {
        this.inner = inner;
        this.log = logger;

        log.initialized();
    }

    @Override
    public String translate(VersionedSchema input, VersionedSchema output, String payload) {
        try {
            log.startedTranslate(input, output, payload);
            return inner.translate(input, output, payload);
        }
        finally {
            log.endedTranslate();
        }
    }

    @Override
    public String translateWithDefaults(VersionedSchema input, VersionedSchema output, String payload, String defaults) {
        try {
            log.startedTranslate(input, output, payload, defaults);
            return inner.translateWithDefaults(input, output, payload, defaults);
        }
        finally {
            log.endedTranslate();
        }
    }

    @Override
    public void close() {
        inner.close();
    }
}
