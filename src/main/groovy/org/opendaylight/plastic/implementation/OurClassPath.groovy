
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import com.google.common.collect.Lists

import java.lang.reflect.Method


class OurClassPath {
    GroovyClassLoader gcl
    List<String> jars = Lists.newArrayList();

    OurClassPath(GroovyClassLoader gcl) {
        this.gcl = gcl
    }

    void add(String path) {
        jars.add(path);
    }

    OurClassPath add(Collection<String> paths) {
        paths.each { String path -> jars.add(path) }
        this
    }

    void commit() throws Exception {

        if (!jars.isEmpty()) {
            URLClassLoader urlClassLoader = (URLClassLoader) gcl
            Class<URLClassLoader> urlClass = URLClassLoader.class;

            Method method = urlClass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);

            for (String jar : jars) {
                URI jarUri = new File(jar).toURI();
                method.invoke(urlClassLoader, jarUri.toURL());
            }
        }
    }
}
