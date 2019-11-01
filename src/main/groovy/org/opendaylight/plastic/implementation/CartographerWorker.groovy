
/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.plastic.implementation

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static com.google.common.base.Preconditions.checkNotNull

class CutOutTheMiddle {

    final int margin = 25
    final int size
    final int lower
    final int upper
    AtomicInteger index = new AtomicInteger(0)

    CutOutTheMiddle(int length) {
        this.size = length
        this.lower = margin
        this.upper = size - margin
    }

    CutOutTheMiddle increment() {
        index.incrementAndGet()
        this
    }

    boolean should() {
        int i = index.get()
        i <= lower || i >= upper
    }
}

class CartographyWorkerLogger {

    Logger logger = LoggerFactory.getLogger(CartographerWorker.class)


    void translating(VersionedSchema unresolvedInput, VersionedSchema output, String payload, String defaults) {
        if (logger.isDebugEnabled()) {
            final int MAX = 100
            int pLen = (payload.length() > MAX) ? MAX : payload.length()
            int dLen = (defaults.length() > MAX) ? MAX : defaults.length()

            logger.debug("Plastic translation")
            logger.debug("\tIn: " + unresolvedInput)
            logger.debug("\tOut: " + output)
            logger.debug("\tPayload(truncated): " + payload.substring(0, pLen))

            if (defaults.length() > 0)
                logger.debug("\tDefaults(truncated): " + defaults.substring(0, dLen))
        }
    }

    void foundParentPlan(TranslationPlanLite resolvedPlan) {
        if (logger.isDebugEnabled()) {
            TranslationPlanLite[] children = resolvedPlan.getParentRole().childPlans()

            logger.debug("Plastic parent plan. In: "
                    + resolvedPlan.firstSchema().toString()
                    + " Out: "
                    + resolvedPlan.lastSchema().toString()
                    + " Children: "
                    + children.length
            )

            CutOutTheMiddle cut = new CutOutTheMiddle(resolvedPlan.getParentRole().childPlans().length)

            for (TranslationPlanLite child : resolvedPlan.getParentRole().childPlans()) {
                if (cut.increment().should()) {
                    logger.debug("Plastic child plan. "
                            + "Name: "
                            + child.getChildRole().getName()
                            + " In: "
                            + child.firstSchema().toString()
                            + " Out: "
                            + child.lastSchema().toString()
                    )
                }
            }
        }
    }

    void completedChild(TranslationPlanLite resolvedChild, Schema childResult) {
        if (logger.isDebugEnabled()) {
            logger.debug("Child completed. "
                    + "Name: "
                    + resolvedChild.getChildRole().getName()
                    + " Result: "
                    + childResult.toShortString(512)
            )
        }
    }

    void finalPayload(Schema payload) {
        if (logger.isDebugEnabled())
            logger.debug("Reassembled payload: {}", payload.toShortString(512))
    }

    void showConcurrency(int maxConcurrency) {
        if (maxConcurrency > 1)
            logger.debug("Translate has max concurrency of {}", maxConcurrency)
    }

    void showThreading(int threads) {
        if (threads > 1)
            logger.debug("Translate will be using {} inner child threads", threads)
    }
}

class CartographerWorker implements Cartography, AutoCloseable {

    private final CartographyWorkerLogger logger = new CartographyWorkerLogger()

    private final Poller poller
    private final ClassCacheClearer morphersClearer
    private final ClassCacheClearer classifiersClearer
    private final ClassCacheClearer libraryClearer

    private final LibraryLoader libraryLoader
    private final ClassifierResolver classifierLocator
    private final CachingSchemaSource schemaSource
    private final PlanResolution resolver

    // Using fixedThreadPool seemed to give better scheduling results than GPARS parallel array

    private final int thds = new ThreadingEnv().numUsableThreads()
    private final ExecutorService executor = Executors.newFixedThreadPool(thds)

    private final AppContext appProps

    CartographerWorker() {
        this(new SearchPath(), 0)
    }

    CartographerWorker(SearchPath path) {
        this(path, 0)
    }

    CartographerWorker(SearchPath path, int pollingInterval) {
        this(new AppContext(path), new FilenamesCaches(path, "lib", "classifiers", "morphers", "schemas"), new Poller(pollingInterval), null, null)
    }

    CartographerWorker(FilenamesCaches caches, Poller poller, PlanResolution resolver, ClassifierResolver locator) {
        this(new AppContext(), caches, poller, resolver, locator)
    }

    CartographerWorker(AppContext appProps, FilenamesCaches caches, Poller poller, PlanResolution resolver, ClassifierResolver locator) {

        this.appProps = appProps

        logger.showThreading(thds)

        GroovyClassLoader gcl = new GroovyClassLoader(getClass().getClassLoader())

        this.schemaSource = new CachingSchemaSource(new SchemaLoader(caches.get("schemas")))
        MorpherLoader mloader = new MorpherLoader(appProps, caches.get("morphers"), gcl)
        this.resolver = (resolver == null) ? new PlanResolution(schemaSource, mloader) : resolver

        ClassifierLoader cloader = new ClassifierLoader(appProps, caches.get("classifiers"), gcl)
        this.libraryLoader = new LibraryLoader(caches.getRootFor("lib"), gcl)
        this.classifierLocator = (locator == null) ? cloader : locator

        this.libraryClearer = new ClassCacheClearer(gcl, caches.getRootFor("lib"), { libraryLoader.clear() })
        this.morphersClearer = new ClassCacheClearer(gcl, caches.getRootFor("morphers"))
        this.classifiersClearer = new ClassCacheClearer(gcl, caches.getRootFor("classifiers"))

        this.poller = poller
        this.poller.register(caches)
        this.poller.register(libraryClearer)
        this.poller.register(morphersClearer)
        this.poller.register(classifiersClearer)
        this.poller.register(libraryLoader)
        this.poller.register(schemaSource)
        this.poller.register(mloader)
        this.poller.register(cloader)
        this.poller.start()

        this.poller.waitTillInitialized()
    }

    @Override
    void close() {
        poller.close()

        executor.shutdownNow();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    @Override
    String translate(VersionedSchema unresolvedInput, VersionedSchema output, String payload) {
        return translateWithDefaults(unresolvedInput, output, payload, EMPTY_DEFAULTS)
    }

    @Override
    String translateWithDefaults(VersionedSchema unresolvedInput, VersionedSchema output, String payload, String defaults) {

        checkNotNull(unresolvedInput)
        checkNotNull(output)
        checkNotNull(payload)
        checkNotNull(defaults)

        poller.lock()

        logger.showConcurrency(poller.maxConcurrency())
        logger.translating(unresolvedInput, output, payload, defaults)

        try {
            Schema parsedPayload = resolver.createSchema(unresolvedInput, payload)
            Schema parsedDefaults = resolver.createSimilarSchema(unresolvedInput, defaults)

            TranslationPlanLite plan = new TranslationPlanLite(unresolvedInput, output)
            TranslationPlanLite resolvedPlan = plan.resolve(classifierLocator, parsedPayload)

            if (resolvedPlan.hasParentRole()) {
                logger.foundParentPlan(resolvedPlan)

                ParentRole parentRole = resolvedPlan.getParentRole()

                TranslationPlanLite[] childPlans = parentRole.childPlans()
                int numChildren = childPlans.length

                CountDownLatch latch = new CountDownLatch(numChildren)

                List<Exception> thrown = Collections.synchronizedList(new ArrayList<Exception>())
                Map<String,Schema> childResults = new ConcurrentHashMap<>()

                CutOutTheMiddle filteredLogging = new CutOutTheMiddle(numChildren)

                childPlans.each { TranslationPlanLite myChildPlan ->

                    Runnable childTask = new Runnable() {

                        @Override
                        void run()
                        {
                            try {
                                TranslationPlanLite resolvedChild = myChildPlan.resolve(classifierLocator)
                                resolvedChild.validate()
                                myChildPlan.validateLineage(resolvedChild)

                                MapTask task = resolver.lookupMappings(resolvedChild)
                                Schema childResult = task.map(resolvedChild.getChildRole().payload(), parsedDefaults)

                                if (filteredLogging.increment().should())
                                    logger.completedChild(resolvedChild, childResult)
                                String cname = resolvedChild.getChildRole().getName()
                                if (childResults.containsKey(cname))
                                    throw new PlasticException("PLASTIC-DUP-CHILD-KEY",
                                            "The following child translation plan key is not unique ${cname}. This is a classifier logic error.")
                                childResults.put(cname, childResult)
                            }
                            catch(Exception e) {
                                thrown.add(e)
                            }
                            finally {
                                latch.countDown()
                            }
                        }
                    }

                    executor.submit(childTask)
                }

                latch.await()

                if (!thrown.isEmpty()) {
                    throw new RuntimeException("Child plans threw ${thrown.size()} exceptions - the first one is ...", thrown.get(0))
                }

                parsedPayload.inject(childResults)
                logger.finalPayload(parsedPayload)
            }

            resolvedPlan.validate()
            MapTask task = resolver.lookupMappings(resolvedPlan)

            Schema parsedOutput = task.map(parsedPayload, parsedDefaults)
            return parsedOutput.emit()
        }
        finally {
            poller.unlock()
        }
    }
}
