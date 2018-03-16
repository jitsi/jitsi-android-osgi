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

import org.jitsi.impl.osgi.framework.launch.*;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.framework.startlevel.*;

import java.util.*;

/**
 * Represents the entry point of the OSGi environment.
 *
 * @author Lyubomir Marinov
 * @author Pawel Domas
 */
public class OSGiLauncher
{
    /**
     * The <tt>BundleContextHolder</tt> which will allow non-OSGi bundles to
     * track the availability of an OSGi <tt>BundleContext</tt>.
     */
    private final BundleContextHolderImpl bundleContextHolder
        = new BundleContextHolderImpl();

    /**
     * The locations of the OSGi bundles (or rather of the class files of their
     * <tt>BundleActivator</tt> implementations) comprising Jitsi Videobridge.
     * An element of the <tt>BUNDLES</tt> array is an array of <tt>String</tt>s
     * and represents an OSGi start level.
     */
    private final java.lang.String[][] bundles;

    private final ClassLoader classLoader;

    /**
     * The <tt>org.osgi.framework.launch.Framework</tt> instance which
     * represents the launched OSGi instance.
     */
    private Framework framework;

    /**
     * Creates new instance of <tt>OSGiLauncher</tt> that will launch OSGi
     * system with specified bundles set.
     *
     * @param bundles two dimension array that specifies bundles activator
     *                classes that will be launched. First dimension is the run
     *                level, second dimension is list of activators for given
     *                level.
     */
    public OSGiLauncher(String[][] bundles, ClassLoader classLoader)
    {
        this.bundles = bundles;
        this.classLoader = classLoader;
    }

    /**
     * Starts the OSGi implementation all {@link #bundles}.
     */
    private void start()
    {
        /*
         * The documentation of AbstractComponent#start() says that it gets
         * called once for each host that this Component connects to and that
         * extending classes should take care to avoid double initialization.
         */
        if (framework != null)
            return;

        FrameworkFactory frameworkFactory = new FrameworkFactoryImpl(classLoader);
        Map<String, String> configuration = new HashMap<String, String>();

        configuration.put(
                Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
                Integer.toString(bundles.length));

        Framework framework = frameworkFactory.newFramework(configuration);
        boolean started = false;

        try
        {
            framework.init();

            BundleContext bundleContext = framework.getBundleContext();

            for (int startLevelMinus1 = 0;
                    startLevelMinus1 < bundles.length;
                    startLevelMinus1++)
            {
                int startLevel = startLevelMinus1 + 1;

                for (String location : bundles[startLevelMinus1])
                {
                    Bundle bundle = bundleContext.installBundle(location);

                    if (bundle != null)
                    {
                        BundleStartLevel bundleStartLevel
                            = bundle.adapt(BundleStartLevel.class);

                        if (bundleStartLevel != null)
                            bundleStartLevel.setStartLevel(startLevel);
                    }
                }
            }

            this.framework = framework;

            framework.start();
            started = true;

            bundleContextHolder.start(bundleContext);
        }
        catch (Exception be)
        {
            throw new RuntimeException(be);
        }
        finally
        {
            if (!started && (this.framework == framework))
                this.framework = null;
        }
    }

    /**
     * Starts the OSGi implementation and the bundles.
     *
     * @param bundleActivator
     */
    public synchronized void start(BundleActivator bundleActivator)
    {
        start();

        bundleContextHolder.addBundleActivator(bundleActivator);
    }

    /**
     * Stops the Jitsi Videobridge bundles and the OSGi implementation.
     */
    private void stop()
    {
        if (framework != null)
        {
            boolean waitForStop = false;

            try
            {
                framework.stop();
                waitForStop = true;
            }
            catch (BundleException be)
            {
                throw new RuntimeException(be);
            }

            if (waitForStop)
            {
                /*
                 * The Framework#stop() method has been successfully invoked.
                 * However, it returns immediately and the very execution occurs
                 * asynchronously. Wait for the asynchronous execution to
                 * complete.
                 */
                waitForStop();
            }

            framework = null;

            try
            {
                bundleContextHolder.stop(null);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Stops all bundles and the OSGi implementation.
     *
     * @param bundleActivator
     */
    public synchronized void stop(BundleActivator bundleActivator)
    {
        bundleContextHolder.removeBundleActivator(bundleActivator);
        if (bundleContextHolder.getBundleActivatorCount() < 1)
            stop();
    }

    /**
     * Waits for {@link #framework} to stop if it has not stopped yet.
     */
    private void waitForStop()
    {
        boolean interrupted = false;

        try
        {
            while (framework != null)
            {
                int state = framework.getState();

                if ((state == Bundle.ACTIVE)
                        || (state == Bundle.STARTING)
                        || (state == Bundle.STOPPING))
                {
                    try
                    {
                        Thread.sleep(20);
                    }
                    catch (InterruptedException ie)
                    {
                        interrupted = true;
                    }
                    continue;
                }
                else
                {
                    break;
                }
            }
        }
        finally
        {
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }
}
