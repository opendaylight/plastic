
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.test

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class ThreadStorm {

    static class Report {
        final float elapsedSecs
        final float avgElapsedSecs
        final float avgInFlight
        final int maxInFlight

        Report(long startedNanos, long endedNanos,
               int numThreads, int concurrency,
               int maxInProgress, int sumInProgress,
               List<Float> durations) {
            this.elapsedSecs = 1.0*(endedNanos-startedNanos)/1000.0/1000.0/1000.0
            this.maxInFlight = maxInProgress
            this.avgInFlight = 1.0*sumInProgress/numThreads
            this.avgElapsedSecs = durations.isEmpty() ? 0.0 : (durations.sum()/durations.size())
        }

        @Override
        String toString() {
            "Elapsed-Seconds: ${elapsedSecs}, Avg-Elapsed Secs: ${avgElapsedSecs}, Average-Active-Threads: ${avgInFlight}, Max-Active-Threads: ${maxInFlight}"
        }
    }

    final int concurrency
    final int nthreads

    ThreadStorm(int nthreads) {
        this(nthreads, nthreads)
    }

    ThreadStorm(int nthreads, int concurrency) {
        this.nthreads = nthreads
        this.concurrency = concurrency

        if (nthreads % concurrency != 0)
            throw new IllegalArgumentException("Concurrency ${concurrency} must evenly divide into number of threads ${nthreads}")
    }

    ThreadStorm setConcurrency(int c) {
        new ThreadStorm(this.nthreads, c)
    }

    ThreadStorm serially() {
        this.setConcurrency(1)
    }

    ThreadStorm concurrently() {
        this.setConcurrency(this.nthreads)
    }

    Report run(Closure cls) {
        ExecutorService exec = Executors.newFixedThreadPool(concurrency)

        CountDownLatch overallLatch = new CountDownLatch(nthreads)
        List<Float> durations = Collections.synchronizedList(new ArrayList<Float>())

        long startedAt = System.nanoTime()

        AtomicInteger inProgress = new AtomicInteger(0)
        AtomicInteger inProgressMax = new AtomicInteger(0)
        AtomicInteger inProgressSum = new AtomicInteger(0)

        nthreads.times {
            exec.submit({
                inProgress.incrementAndGet()
                long myStartedAt = System.nanoTime()
                cls()
                long myEndedAt = System.nanoTime()
                float durationSecs = (myEndedAt-myStartedAt)/1000.0/1000.0/1000.0
                durations.add(durationSecs)
                int inFlightNow = inProgress.get()
                if (inFlightNow > inProgressMax.get())
                    inProgressMax.set(inFlightNow)
                inProgress.decrementAndGet()
                inProgressSum.addAndGet(inFlightNow)
                overallLatch.countDown()
            })
        }

        overallLatch.await()

        long endedAt = System.nanoTime()

        new Report(startedAt, endedAt, nthreads, concurrency, inProgressMax.get(), inProgressSum.get(), durations)
    }
}
