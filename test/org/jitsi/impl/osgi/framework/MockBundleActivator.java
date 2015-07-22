package org.jitsi.impl.osgi.framework;

import org.osgi.framework.*;

/**
 * @author Pawel Domas
 */
public class MockBundleActivator
    implements BundleActivator
{
    private BundleContext context;

    @Override
    public synchronized void start(BundleContext context)
        throws Exception
    {
        this.context = context;

        this.notifyAll();
    }

    @Override
    public synchronized void stop(BundleContext context)
        throws Exception
    {
        this.context = null;

        this.notifyAll();
    }

    public boolean isRunning()
    {
        return context != null;
    }

    public synchronized boolean waitToBeStarted()
    {
        if (isRunning())
        {
            return true;
        }

        try
        {
            this.wait();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        return isRunning();
    }

    public synchronized boolean waitToBeStopped()
    {
        if (!isRunning())
        {
            return true;
        }

        try
        {
            this.wait();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        return !isRunning();
    }
}
