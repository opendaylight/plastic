
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation


class PlasticException extends RuntimeException {
    PlasticException(String id, String msg) {
        super(id+": "+msg)
    }

    PlasticException(String id, String msg, Exception e) {
        super(id+": "+msg, e)
    }
}
