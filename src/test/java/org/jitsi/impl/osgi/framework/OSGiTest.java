package org.jitsi.impl.osgi.framework;

import org.jitsi.impl.osgi.framework.launch.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(JUnit4.class)
public class OSGiTest
{
    private final static String[][] bundles = new String[][]
        {
            {
                "org/jitsi/impl/osgi/framework/Bundle1"
            },
            {
                "org/jitsi/impl/osgi/framework/Bundle2"
            },
            {
                "org/jitsi/impl/osgi/framework/Bundle3"
            },
        };

    @Test
    public void osgiLauncherTest()
    {
        OSGiLauncher launcher = new OSGiLauncher(bundles);

        MockBundleActivator activator1 = new MockBundleActivator();

        MockBundleActivator activator2 = new MockBundleActivator();

        launcher.start(activator1);

        // Wait for the activator to be started
        assertTrue(activator1.waitToBeStarted());

        assertNotNull(Bundle1.bundleContext);
        assertNotNull(Bundle2.bundleContext);
        assertTrue(Bundle3.waitToBeStarted());

        launcher.start(activator2);

        // It should be started
        assertTrue(activator2.waitToBeStarted());

        // Now shutdown after first instance bundles are
        launcher.stop(activator1);
        assertTrue(activator1.waitToBeStopped());

        // Bunt the rest should still be running
        assertNotNull(Bundle1.bundleContext);
        assertNotNull(Bundle2.bundleContext);
        assertNotNull(Bundle3.bundleContext);
        assertTrue(activator2.isRunning());

        // Now remove the last instance bundle and all bundles should stop
        launcher.stop(activator2);
        assertTrue(activator2.waitToBeStopped());
        assertFalse(activator1.isRunning());
        assertNull(Bundle1.bundleContext);
        assertNull(Bundle2.bundleContext);
        assertNull(Bundle3.bundleContext);
    }
}
