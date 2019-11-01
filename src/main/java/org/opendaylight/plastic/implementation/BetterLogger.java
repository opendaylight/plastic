
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BetterLogger {

    Logger log;

    public <T> BetterLogger(Class<T> clazz) {
        log = LoggerFactory.getLogger(clazz.getName());
    }

    public void initialized() {
        String level = "";
        if (log.isDebugEnabled())
            level = "DEBUG";
        else if (log.isTraceEnabled())
            level = "TRACE";
        else if (log.isWarnEnabled())
            level = "WARN";
        else if (log.isErrorEnabled())
            level = "ERROR";
        else if (log.isInfoEnabled())
            level = "INFO";

        log.info("Logging initialized (name: "+log.getName()+" level: "+level+")");
    }

    public void startedTranslate(VersionedSchema input, VersionedSchema output, String payload) {
        log.info("Received translate request: input("+input+") output("+output+")");
    }

    public void startedTranslate(VersionedSchema input, VersionedSchema output, String payload, String defaults) {
        log.info("Received translate (with defaults) request: input("+input+") output("+output+")");
    }

    public void endedTranslate() {
        log.info("Finished mapping");
    }

    public void closed() {
        log.info("Logging closed");
    }
}
