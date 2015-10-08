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

import org.osgi.framework.*;

/**
 *
 */
public class Bundle3
    implements BundleActivator
{
    public static BundleContext bundleContext;

    @Override
    public void start(BundleContext context)
        throws Exception
    {
        synchronized (Bundle3.class)
        {
            bundleContext = context;

            Bundle3.class.notifyAll();
        }
    }

    public static boolean waitToBeStarted()
    {
        synchronized (Bundle3.class)
        {
            if (bundleContext != null)
                return true;

            try
            {
                Bundle3.class.wait();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            return bundleContext != null;
        }
    }

    @Override
    public void stop(BundleContext context)
        throws Exception
    {
        synchronized (Bundle3.class)
        {
            bundleContext = null;

            Bundle3.class.notifyAll();
        }
    }
}
