
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

public class Factory {

    public Cartography buildWorker() {
        return new CartographerWorker();
    }

    public Cartography buildWorkerWithLogging() {
        return new CartographyLogged(buildWorker());
    }

    public CartographyService buildWorkerService() {
        return new CartographyServiceAdapter(buildWorkerWithLogging());
    }

    public CartographyService build() {
        return buildWorkerService();
    }
}
