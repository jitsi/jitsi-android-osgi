package org.jitsi.impl.osgi.framework;

import org.osgi.framework.*;

/**
 *
 */
public class Bundle2
    implements BundleActivator
{
    public static BundleContext bundleContext;

    @Override
    public void start(BundleContext context)
        throws Exception
    {
        bundleContext = context;
    }

    @Override
    public void stop(BundleContext context)
        throws Exception
    {
        bundleContext = null;
    }
}
