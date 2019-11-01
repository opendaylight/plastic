
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation;

import java.util.HashMap;
import java.util.Map;

public class FilenamesCaches implements Pollee {

    Map<String,FilenamesCache> caches = new HashMap<>();

    public FilenamesCaches(SearchPath path, String... dirs) {
        for(String dir : dirs) {
            caches.put(dir, new FilenamesCache(path.find(dir)));
        }
    }

    @Override
    public void phase(int i) {
        for(FilenamesCache cache : caches.values()) {
            cache.scan();
        }
    }

    public FilenamesCache get(String dir) {
        return caches.get(dir);
    }

    public String getRootFor(String dir) {
        return get(dir) == null ? "" : get(dir).root();
    }
}
