/* Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation.author


import org.opendaylight.plastic.implementation.ChildRole
import org.opendaylight.plastic.implementation.ParentRole
import org.opendaylight.plastic.implementation.Schema
import org.opendaylight.plastic.implementation.TranslationPlanLite
import org.opendaylight.plastic.implementation.VersionedSchema

import static com.google.common.base.Preconditions.checkState


// Gives classifier writers a library of helpers for working with plans

class Plans {

    static VersionedSchema asSchema(String... schemaInfos) {
        checkState(schemaInfos.length == 3);
        return new VersionedSchema(schemaInfos[0],schemaInfos[1],schemaInfos[2]);
    }

    static TranslationPlanLite newParent(VersionedSchema schemaIn, VersionedSchema schemaOut) {
        TranslationPlanLite plan = newPlan(schemaIn, schemaOut);
        asParentPlan(plan);
        return plan;
    }

    static TranslationPlanLite newPlan(VersionedSchema schemaIn, VersionedSchema schemaOut) {
        return new TranslationPlanLite(schemaIn, schemaOut);
    }

    static void asParentPlan(TranslationPlanLite parent) {
        ParentRole parentRole = new ParentRole();
        parent.setRole(parentRole);
    }

    static TranslationPlanLite asChildPlan(TranslationPlanLite child,
                                           String name,
                                           Schema branch) {
        ChildRole childRole = new ChildRole(name, branch);
        child.setRole(childRole);
        return child;
    }

    static void realizeChildPlan(TranslationPlanLite child,
                                 String childName,
                                 Schema root,
                                 List parent,
                                 int childIndex) {

        ClaimCheck checker = new ClaimCheck(childName, root, parent, childIndex)
        asChildPlan(child, checker.name, checker.branch)
        checker.swap()
    }

    static void realizeChildPlan(TranslationPlanLite child,
                                 String childName,
                                 Schema root,
                                 Map parent,
                                 String childKey) {

        ClaimCheck checker = new ClaimCheck(childName, root, parent, childKey)
        asChildPlan(child, checker.name, checker.branch)
        checker.swap()
    }

    // Client logic drills into the payload and hands reference to the immediate parent container
    //
    static List<TranslationPlanLite> realizeChildPlans(Schema root,
                                                       List parent,
                                                       String marker,
                                                       VersionedSchema childInput,
                                                       VersionedSchema childOutput) {
        if (marker == null || marker.isEmpty())
            throw new IllegalArgumentException("Cannot create child translation plans for array using an empty marker/placeholder")
        if (parent == null)
            throw new IllegalArgumentException("Cannot create child translation plans for array with unexpected NULL parent (marker name is $marker)")

        List<TranslationPlanLite> results = new ArrayList<>()
        for (int i = 0; i< parent.size(); i++) {
            TranslationPlanLite child = newPlan(childInput, childOutput)
            Plans.realizeChildPlan(child, marker, root, parent, i)
            results.add(child)
        }
        results
    }

    // Client logic specifies the parent container by a path, enabling an abstract way to reach containers
    // inside of other containers - it is simpler and avoids explicit iteration by client logic
    //
    static List<TranslationPlanLite> realizeChildPlans(Schema root,
                                                       String parentPath,
                                                       String marker,
                                                       VersionedSchema childInput,
                                                       VersionedSchema childOutput) {
        if (marker == null || marker.isEmpty())
            throw new IllegalArgumentException("Cannot create child translation plans for array using an empty marker/placeholder")
        if (parentPath == null || parentPath.isEmpty())
            throw new IllegalArgumentException("Cannot create child translation plans for array using an empty parent path (marker name is $marker)")

        List<TranslationPlanLite> results = new ArrayList<>()

        int pass = 0
        walk(root.getParsed(), parentPath) { List parent, int index ->
            String cmarker = "${marker}[$pass]"
            TranslationPlanLite child = newPlan(childInput, childOutput)
            Plans.realizeChildPlan(child, cmarker, root, parent, index)
            results.add(child)
            pass++
        }

        results
    }

    static void walk(Object root, String path, Closure cls) {
        WalkToList walker = new WalkToList(path)
        walker.walk(root, cls)
    }

    static class WalkToList {

        static class WalkerException extends RuntimeException {
            WalkerException(String msg) {
                super(msg)
            }
        }

        final String originalPath
        final String[] originalParts

        WalkToList(String path) {
            this.originalPath = path

            String tmp = path;
            if (tmp.startsWith("[*]"))
                tmp = tmp.replaceFirst("\\[\\*\\]", "*")
            tmp = tmp.replace("[", ".").replace("].",".")
            this.originalParts = tmp.split("(\\.)+")
        }

        void walk(Object starting, Closure cls) {
            walk(starting, starting, 0, cls)
        }

        Object walk(Object root, Object current, int pathPosition, Closure cls) {
            if (current == null)
                throw new WalkerException("Could not use path ($originalPath) on a NULL path segment")

            boolean isLast = pathPosition == originalPath.length()-1
            boolean isPast = pathPosition >= originalPath.length()

            if (isPast)
                return current
            if (!isLast && !(current instanceof List) && !(current instanceof Map))
                throw new WalkerException("Using a path (${originalPath}) is not supported for the following: ${current}")

            Object here = current

            for (int i = pathPosition; i < originalParts.length; i++) {
                String pathComponent = originalParts[i]
                isLast = (i == originalParts.length - 1)

                if (pathComponent == '*') {
                    if (!(here instanceof List)) {
                        throw new WalkerException("Encountered a object that is not a list via path component \'${pathComponent}\' along the path ${originalPath} for object ${root}")
                    }
                }

                if (here instanceof List) {
                    List list = (List) here
                    if (isLast) {
                        list.eachWithIndex { m,j ->
                            cls(list, j)
                        }
                    } else {
                        list.each { m ->
                            here = walk(root, m, i + 1, cls)
                        }
                    }
                    break
                }
                else if (here instanceof Map) {
                    if (isLast) {
                        Object value = here[pathComponent]
                        if (value instanceof List) {
                            List list = (List) value
                            for (int j = 0; j< list.size(); j++) {
                                cls(list, j)
                            }
                        }
                        else {
                            throw new WalkerException("Path ended unexpectedly on a non-list for the path ${originalPath} for object ${root}")
                        }
                    }
                    else {
                        here = here[pathComponent]
                    }
                }
                else {
                    throw new WalkerException("Path hit a non-collection at the path component \'${pathComponent}\' along the path ${originalPath} for object ${root}")
                }
            }

            here
        }
    }
}
