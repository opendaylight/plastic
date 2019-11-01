/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

import org.opendaylight.plastic.implementation.SimpleClassifier


class AppPropsClassifier extends SimpleClassifier {

    Map context

    void setContext(Map context) {
        this.context = context
    }

    @Override
    String classify(Object payload) {
        payload.'classifier-says' = context['classifier-status'] ? context['classifier-status'] : "classifier-not-ok"
        "app-props-in"
    }
}
