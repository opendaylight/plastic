
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import org.opendaylight.plastic.implementation.author.InputsAndOutputs
import org.opendaylight.plastic.implementation.author.Logging

// A base class for all external morphers that provides support methods.
// To use this, just inherit from this base class in your own morphers.

class ExtendedBasicMorpher implements InputsAndOutputs, Logging {

    ExtendedBasicMorpher() {
        createLogger(ExtendedBasicMorpher)
    }

    void ignoreUnusedInputs(String... variables) {
        if (variables.length == 0)
            ignoredInputs.add("*")
        else
            ignoredInputs.addAll(variables)
    }

    void ignoreUnusedOutputs(String... variables) {
        if (variables.length == 0)
            ignoredOutputs.add("*")
        else
            ignoredOutputs.addAll(variables)
    }

    void optionalInputs(String... variables) {
        if (variables.length == 0)
            optioned.add("*")
        else
            optioned.addAll(variables)
    }

    String urlEncode(String unencoded) {
        URLEncoder.encode(unencoded, "UTF-8")
    }

    String timeFromEpoch(String epoch, String zoneOffset) {
        Times.fromEpochWithOffset(epoch, zoneOffset)
    }

    String timeWithoutZoneOffset(String ambiguous) {
        Times.fromAmbiguous(ambiguous, Times.Ambiguities.MissingT, Times.Ambiguities.MissingZone)
    }
}
