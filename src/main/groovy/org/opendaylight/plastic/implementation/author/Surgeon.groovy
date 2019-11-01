
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation.author

class Surgeon {

    static class SurgeonException extends RuntimeException {
        SurgeonException(String msg) {
            super(msg)
        }
    }

    protected Object root

    Surgeon(Object root) {
        this.root = root
    }

    // The key used for mapify in wrapping an existing scalar is hard-coded to '???'

    Map mapify(String... path) {
        Object result = forceFit(root, [:], path)
        (Map) result
    }

    List listify(String... path) {
        Object result = forceFit(root, [], path)
        (List) result
    }

    private Object forceFit(Object starting, Object fallback, String... path) {
        if (starting == null)
            throw new SurgeonException("Could not use path (${joined(path)} on a NULL root")
        if (path.length == 0)
            return starting
        if (!(starting instanceof Map) && !(starting instanceof List))
            throw new SurgeonException("Using a path (${joined(path)}) is not supported for the following: ${starting}")
        if (!(fallback instanceof Map) && !(fallback instanceof List))
            throw new SurgeonException("Using a path (${joined(path)}) does not support wrapping using the following: ${fallback}")

        Object here = starting

        for (int i = 0; i < path.length; i++) {
            String component = path[i]
            boolean isLast = (i == path.length-1)

            // path component [] is really a placeholder for a top level list, which has no key, otherwise it is just a noop

            if (component == '[]') {
                if (i != 0) {
                    throw new SurgeonException("Encountered an unexpected implicit array path component '[]' along the path ${joined(path)} for object ${this.root}")
                }
                if (!(here instanceof List)) {
                    throw new SurgeonException("Encountered a object that is not a list via path component \'${component}\' along the path ${joined(path)} for object ${this.root}")
                }
            }
            else {
                if (here instanceof Map) {
                    if (here.containsKey(component)) {
                        Object found = here[component]

                        if (isLast) {
                            if (fallback instanceof List) {
                                if (found instanceof List) {
                                    here = found // here[component]
                                }
                                else {
                                    List wrapped = [found]
                                    here[component] = wrapped
                                    here = wrapped
                                }
                            } else if (fallback instanceof Map) {
                                if (found instanceof Map) {
                                    here = here[component]
                                }
                                else {
                                    Map wrapped = ['???': found]
                                    here[component] = wrapped
                                    here = wrapped
                                }
                            }
                        }
                        else {
                            here = here[component]
                        }
                    }
                    else if (isLast) {
                        here[component] = fallback
                        here = fallback
                    }
                    else {
                        throw new SurgeonException("Could not apply the path component \'${component}\' along the path ${joined(path)} for object ${this.root}")
                    }
                }
                else if (here instanceof List) {
                    List list = (List) here
                    list.each { m ->
                        here = forceFit(m, fallback, path.getAt(i..path.length-1) as String[])
                    }
                    break
                }
                else {
                    here = null
                }

                if (here == null)
                    throw new SurgeonException("Encountered a missing path component \'${component}\' along the path ${joined(path)} for object ${this.root}")
            }
        }

        here
    }

    void placeValue(Object value, String... path) {
        placeValueHere(root, value, path)
    }

    private void placeValueHere(Object starting, Object value, String... path) {
        if (starting == null)
            throw new SurgeonException("Could not use path (${joined(path)} on a NULL root")
        if (path.length == 0)
            return
        if (!(starting instanceof Map) && !(starting instanceof List))
            throw new SurgeonException("Using a path (${joined(path)}) is not supported for the following: ${starting}")

        Object here = starting

        for (int i = 0; i < path.length; i++) {
            String component = path[i]
            boolean isLast = (i == path.length-1)

            if (here instanceof Map) {
                if (here.containsKey(component)) {
                    if (isLast) {
                        here[component] = value
                    }
                    else {
                        here = here[component]
                    }
                }
                else if (isLast) {
                    here[component] = value
                    here = here[component]
                }
                else {
                    throw new SurgeonException("Could not apply the path component \'${component}\' along the path ${joined(path)} for object ${starting}")
                }
            }
            else if (here instanceof List) {
                List list = (List) here
                list.each { m ->
                    here = placeValueHere(m, value, path.getAt(i..path.length-1) as String[])
                }
                break
            }
            else {
                here = null
            }

            if (here == null)
                throw new SurgeonException("Encountered a missing path component \'${component}\' along the path ${joined(path)} for object ${starting}")
        }
    }

    private static String joined(String... path) {
        "${path.join('.')}"
    }
}
