
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation.author

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap

/*
 * A tally is a set of named counters. The counter names are arbitrary. There is a built-in
 * "completion" counter as well with its own count and own manipulation.
 *
 * The concept is to allow clients to use these names as reasons for partial failure or
 * success and to get a summary of that activity later. So a client might say A and B
 * happened, then a completion was done. Later it might say C happened then a completion
 * happened. Or maybe just a completion happened without incident.
 *
 * A tally is supposed to have a unique name, though no error will indicate duplicates.
 * There is an optional concept of a parent tally that has children, so they can be
 * arranged in heirarchies. Given any child, the top level parent can be found. The
 * names/counts/completions can be aggregated upwards to any point, but this usually
 * is to the topmost level.
 *
 * The heirarchy is expected to be built initially and remain static, hence there is
 * no multithreading safety for this. The actually accounting is multithreaded.
 *
 * The tally result can be consumed as a map or as a summary string.
 *
 * Example usage
 *
 * static Tally myTally = new Tally(...)
 *
 *    ...
 *    if(badConditionA)
 *        myTally.account("conditionA")
 *    ...
 *    if(badConditionB)
 *        myTally.account("conditionB")
 *    ...
 *    myTally.completed()
 *    ...
 *    myTally.writeToLogEvery(5)
 */
class Tally {

    static Logger logger = LoggerFactory.getLogger(Tally)

    static class Lineage {
        final Tally parent
        final Tally child

        Lineage(Tally parent, Tally child) {
            this.parent = parent
            this.child = child
        }
    }

    static class Counter {
        int value = 0;
        Counter(int starting) {
            this.value = starting
        }
    }

    static class Watcher {
        void completed(int completions) {}
    }

    // Periodically report to log every "modulus" completions
    // Use 0 to disable
    // See unit tests for examples
    //
    static class PeriodicLogger extends Watcher {

        final Tally target
        final int modulus

        PeriodicLogger(Tally target, int modulus) {
            if (modulus < 0)
                throw new IllegalArgumentException("Cannot have negative auto completion number: ${modulus}")

            this.target = target
            this.modulus = modulus
        }

        void completed(int completions) {
            if (modulus > 0 && completions % modulus == 0)
                target.writeToLog()
        }
    }

    // Periodically report to log every C**2 completions, capped to a limit (modulus after that)
    // Use 0 to disable
    // See unit tests for examples
    //
    static class LogarithmicLogger extends Watcher {

        final Tally target
        final int limit

        int current
        PeriodicLogger periodic

        LogarithmicLogger(Tally target, int limit) {
            if (limit < 0)
                throw new IllegalArgumentException("Cannot have negative cap: ${limit}")

            this.target = target
            this.limit = limit
            this.current = 1
        }

        void completed(int completions) {
            if (limit > 0) {
                if (periodic)
                    periodic.completed(completions)
                else {
                    if (completions >= current) {
                        current = (2 * current > limit) ? limit : 2 * current
                        target.writeToLog()
                        if (current == limit)
                            this.periodic = newPeriodic(current)
                    }
                }
            }
        }

        PeriodicLogger newPeriodic(int modulus) {
            new PeriodicLogger(target, modulus)
        }
    }

    private String name = "Anonymous"
    private int completions = 0
    private Map<String,Counter> tallies = new ConcurrentHashMap<>()

    private Tally parent = null
    private List<Lineage> children = new ArrayList<>()

    private Watcher watcher = new Watcher()

    Tally(String name) {
        this.name = name
    }

    Tally(Object that) {
        this(that.class.simpleName)
    }

    Tally(Class clazz) {
        this(clazz.simpleName)
    }

    String getName() {
        this.name
    }

    Map<String,Integer> asMap() {
        Map<String,Integer> result = [:]
        tallies.each { k,v -> result.put(k, v.value)}
        result.put('completions', completions)
        result
    }

    Tally findTally(String target) {
        if (name.equals(target))
            return this

        Tally found = null

        children.find { lineage ->
            Tally candidate = lineage.child.findTally(target)
            if (candidate)
                found = candidate
            found
        }

        found
    }

    void adopt(Tally... childs) {
        childs.each { child ->
            child.parent = this
            children.add(new Lineage(this, child))
        }
    }

    Tally parentmost() {
        Tally parentMost = this;
        while(parentMost.parent)
            parentMost = parentMost.parent
        parentMost
    }

    synchronized Tally sumUpwards() {
        Tally parentMost = parentmost()
        Tally result = new Tally(parentMost.name)
        parentMost.sumDownwards(result)
        result
    }

    private void sumDownwards(Tally into) {
        into.completions += completions
        tallies.each { key,counter ->
            into.ensureCounter(key)
            into.tallies[key] = Integer.valueOf(counter.value)
        }
        children.each { lineage ->
             lineage.child.sumDownwards(into)
        }
    }

    private void ensureCounter(String key) {
        if (!tallies.containsKey(key))
            tallies[key] = new Counter(0)
    }

    synchronized Tally account(String... whys) {
        whys.each { why ->
            if (tallies.containsKey(why))
                tallies[why].value++
            else
                tallies[why] = new Counter(1)
        }
        this
    }

    synchronized Tally completed() {
        completions++
        watcher.completed(completions)
        this
    }

    synchronized String toString() {
        StringBuilder buffer = new StringBuilder()

        buffer.append("[")
        buffer.append("Completions:")
        buffer.append(completions)

        tallies.sort().each { k,v ->
            buffer.append(", ")
            buffer.append(k)
            buffer.append(":")
            buffer.append(v.value)
        }

        buffer.append("]")
        buffer.toString()
    }

    Tally writeToLog() {
        logger.info("Tally ${name} -> ${this.toString()}")
        this
    }

    Tally writeToLogEvery(int howManyCompletions) {
        watcher = new PeriodicLogger(this, howManyCompletions)
        this
    }

    Tally writeToLogLogarithmicly(int cap) {
        watcher = new PeriodicLogger(this, cap)
        this
    }
}
