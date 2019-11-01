
/*
* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package alternative1.classifiers

import org.opendaylight.plastic.implementation.SimpleClassifier

class AbcClassifier extends SimpleClassifier {
    @Override
    String classify(Object parsedPayload) {
        "abc"
    }
}
