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
