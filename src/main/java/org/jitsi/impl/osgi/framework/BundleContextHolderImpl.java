/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.impl.osgi.framework;

import org.jitsi.osgi.framework.*;

import org.osgi.framework.*;

import java.util.*;
import java.util.logging.*;

/**
 * Provides a base implementation of {@link BundleContextHolder}.
 *
 * @author Lyubomir Marinov
 */
public class BundleContextHolderImpl
    implements BundleActivator,
               BundleContextHolder
{
    /**
     * The logger
     */
    private static final Logger logger
        = Logger.getLogger(BundleContextHolderImpl.class.getName());

    /**
     * An empty array with <tt>BundleActivator</tt> element type. Explicitly
     * defined to reduce unnecessary allocations.
     */
    private static final BundleActivator[] EMPTY_BUNDLE_ACTIVATORS
        = new BundleActivator[0];

    /**
     * The list of <tt>BundleActivator</tt>s registered with this instance to be
     * notified when it acquires a <tt>BundleContext</tt> or looses it.
     */
    private final List<BundleActivator> bundleActivators
        = new ArrayList<BundleActivator>();

    /**
     * The <tt>BundleContext</tt> in which this instance has been started.
     */
    private BundleContext bundleContext;

    /**
     * Initializes a new <tt>BundleContextHolderImpl</tt> instance.
     */
    public BundleContextHolderImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    @java.lang.Override
    public void addBundleActivator(BundleActivator bundleActivator)
    {
        if (bundleActivator == null)
        {
            throw new NullPointerException("bundleActivator");
        }
        else
        {
            synchronized (getSyncRoot())
            {
                // Start new bundle if we are running
                if (!bundleActivators.contains(bundleActivator)
                        && bundleActivators.add(bundleActivator)
                        && (bundleContext != null))
                {
                    try
                    {
                        bundleActivator.start(bundleContext);
                    }
                    catch (Throwable t)
                    {
                        logger.log(Level.SEVERE, t.getMessage(), t);

                        if (t instanceof InterruptedException)
                            Thread.currentThread().interrupt();
                        else if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                    }
                }
            }
        }
    }

    /**
     * Gets the number of <tt>BundleActivator</tt>s registered with this
     * instance to be notified when it acquires a <tt>BundleContext</tt> or
     * looses it.
     *
     * @return the number of <tt>BundleActivator</tt>s registered with this
     * instance to be notified when it acquires a <tt>BundleContext</tt> or
     * looses it
     */
    public int getBundleActivatorCount()
    {
        synchronized (getSyncRoot())
        {
            return bundleActivators.size();
        }
    }

    /**
     * Gets the <tt>BundleActivator</tt>s registered with this instance to be
     * notified when it acquires a <tt>BundleContext</tt> or looses it.
     *
     * @return the <tt>BundleActivator</tt>s registered with this instance to be
     * notified when it acquires a <tt>BundleContext</tt> or looses it
     */
    public BundleActivator[] getBundleActivators()
    {
        synchronized (getSyncRoot())
        {
            return
                bundleActivators.isEmpty()
                    ? EMPTY_BUNDLE_ACTIVATORS
                    : bundleActivators.toArray(EMPTY_BUNDLE_ACTIVATORS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BundleContext getBundleContext()
    {
        synchronized (getSyncRoot())
        {
            return bundleContext;
        }
    }

    /**
     * Gets the <tt>Object</tt> which this instance uses to synchronize the
     * access to its methods.
     *
     * @return the <tt>Object</tt> which this instance uses to synchronize the
     * access to its methods 
     */
    public Object getSyncRoot()
    {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeBundleActivator(BundleActivator bundleActivator)
    {
        if (bundleActivator != null)
        {
            synchronized (getSyncRoot())
            {
                if (bundleActivators.remove(bundleActivator)
                    && (bundleContext != null))
                {
                    try
                    {
                        bundleActivator.stop(bundleContext);
                    }
                    catch (Throwable t)
                    {
                        logger.log(Level.SEVERE, t.getMessage(), t);

                        if (t instanceof InterruptedException)
                            Thread.currentThread().interrupt();
                        else if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                    }
                }
            }
        }
    }

    /**
     * Starts this instance into a specific <tt>BundleContext</tt>. This
     * instance notifies the <tt>BundleActivator</tt>s registered with it that
     * it has acquired a <tt>BundleContext</tt>.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which this instance is
     * starting
     */
    @Override
    public void start(BundleContext bundleContext)
        throws Exception
    {
        synchronized (getSyncRoot())
        {
            this.bundleContext = bundleContext;

            for (BundleActivator bundleActivator : getBundleActivators())
            {
                try
                {
                    bundleActivator.start(bundleContext);
                }
                catch (Throwable t)
                {
                    logger.log(Level.SEVERE, t.getMessage(), t);

                    if (t instanceof InterruptedException)
                        Thread.currentThread().interrupt();
                    else if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                }
            }
        }
    }

    /**
     * Stops this instance into a specific <tt>BundleContext</tt>. This instance
     * notifies the <tt>BundleActivator</tt>s registered with it that it has
     * lost its <tt>BundleContext</tt>.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which this instance is
     * stopping
     */
    @Override
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        synchronized (getSyncRoot())
        {
            try
            {
                for (BundleActivator bundleActivator : getBundleActivators())
                {
                    try
                    {
                        bundleActivator.stop(this.bundleContext);
                    }
                    catch (Throwable t)
                    {
                        logger.log(Level.SEVERE, t.getMessage(), t);

                        if (t instanceof InterruptedException)
                            Thread.currentThread().interrupt();
                        else if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                    }
                }
            }
            finally
            {
                this.bundleContext = null;
            }
        }
    }
}
