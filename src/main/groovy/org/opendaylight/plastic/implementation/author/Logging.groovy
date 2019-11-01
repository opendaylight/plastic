
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation.author


import org.opendaylight.plastic.implementation.PlasticException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

trait Logging {

    static class ComponentAbortException extends PlasticException {
        ComponentAbortException(String msg) {
            super("PLASTIC-ABORT-EX", "A Plastic component (morpher or classifier) invoked abort: ${msg}")
        }
    }

    Logger logger = LoggerFactory.getLogger(Logging)

    void createLogger(Class clz) {
        logger = LoggerFactory.getLogger(clz)
    }

    void createLogger(String name) {
        logger = LoggerFactory.getLogger(name)
    }

    void debug(String msg, String... others) {
        logger.debug(expand(msg, others))
    }

    void info(String msg, String... others) {
        logger.info(expand(msg, others))
    }

    void warn(String msg, String... others) {
        logger.warn(expand(msg, others))
    }

    void error(String msg, String... others) {
        logger.error(expand(msg, others))
    }

    void abort(String msg, String... others) {
        msg = expand(msg, others)
        logger.error(msg)
        throw new ComponentAbortException(msg)
    }

    private String expand(String msg, String... args) {

        // logger-style convention

        if (msg.contains("{}"))
            msg = msg.replaceAll("\\{\\}", "%s")

        // simple access to string-format convention

        if (msg.contains("%s") || msg.contains("%d") || msg.contains("%i"))
            String.format(msg, args)
        else
            msg + args.join(" ")
    }
}
