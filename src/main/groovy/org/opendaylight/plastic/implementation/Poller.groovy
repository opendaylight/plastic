
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock


/**
 * A simple single thread-based poller that will periodically call back every registered
 * pollee. The polling interval/delay is expressed in seconds. A value of 0 will disable
 * polling, although pollees are guaranteed to be called for their initial phase 0, even
 * in this case.
 */
class Poller {

    private static Logger logger = LoggerFactory.getLogger(Poller)

    private static final int PERSECOND = 1000

    private Thread pollingThread

    private final int pollingSeconds
    private final List<Pollee> pollees = Lists.newArrayList()
    private final Closure didPollee
    private final AtomicInteger pass = new AtomicInteger(0)

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock()
    private final Lock readerLock = rwLock.readLock()
    private final Lock writerLock = rwLock.writeLock()

    private final AtomicInteger concurrentMax = new AtomicInteger(0)
    private final AtomicInteger skipped = new AtomicInteger(0)

    // Hook for testability
    //
    def sleeper = { long millisecs ->
        sleep(millisecs)
    }

    Poller() {
        this(10, { Pollee pollee, int pass -> })
    }

    Poller(int pollingSecs) {
        this(pollingSecs, { Pollee pollee, int pass -> })
    }

    protected Poller(int seconds, Closure polleeCallback) {
        pollingSeconds = (seconds > 0) ? seconds : 0
        didPollee = polleeCallback
    }

    void register(Pollee pollee) {
        pollees.add(pollee)
    }

    protected int getPasses() {
        pass.intValue()
    }

    protected int countPastSuppressions() {
        skipped.intValue()
    }

    void lock() {
        this.readerLock.lock()
        int inprogress = rwLock.getReadLockCount()
        if (inprogress > concurrentMax.get())
            concurrentMax.set(inprogress)
    }

    void unlock() {
        this.readerLock.unlock()
    }

    int maxConcurrency() {
        concurrentMax.get()
    }

    void start() {
        pollingThread = Thread.startDaemon {
            while(!Thread.currentThread().isInterrupted()) {
                int waiting = rwLock.getQueueLength()
                int inprogress = rwLock.getReadLockCount()
                if (waiting == 0 && inprogress == 0) {
                    writerLock.lock()
                    try {
                        logger.debug("Poller starting pass ${pass.get()}")
                        doOnePass()
                    }
                    finally {
                        writerLock.unlock()
                    }
                }
                else {
                    logger.debug("Poller deferring (waiting count: ${waiting}, in-progress: ${inprogress})")
                    skipped.incrementAndGet()
                }

                if (pollingSeconds == 0)
                    Thread.currentThread().interrupt()
                else if(!Thread.currentThread().isInterrupted())
                    sleeper(pollingSeconds*PERSECOND)
            }
        }
    }

    private void doOnePass() {
        pollees.each { Pollee pollee ->
            pollee.phase(pass.intValue())
            didPollee(pollee, pass.intValue())
        }
        pass.incrementAndGet()
    }

    void waitTillInitialized() {
        while(getPasses() == 0) {
            sleep(1)
        }
    }

    void close() {
        pollingThread.interrupt()
    }
}
