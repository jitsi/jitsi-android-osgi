/*
 * Copyright @ 2018 - present 8x8, Inc.
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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import org.jitsi.impl.osgi.framework.launch.*;
import org.jitsi.osgi.framework.*;
import org.junit.jupiter.api.*;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.framework.startlevel.*;

public class OSGiTest
{
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void osgiLauncherTest() throws BundleException, InterruptedException
    {
        var logger = Logger.getLogger(getClass().getName());

        var options = new HashMap<String, String>();
        options.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "2");
        Framework fw = new FrameworkImpl(options, getClass().getClassLoader());
        fw.init();
        var bundleContext = fw.getBundleContext();
        for (Class<? extends BundleActivator> activator : List.of(
            Bundle1.class,
            Bundle2.class,
            Bundle3.class))
        {
            var url =
                activator.getProtectionDomain().getCodeSource().getLocation()
                    .toString();
            var bundle = bundleContext.installBundle(url);
            var startLevel = bundle.adapt(BundleStartLevel.class);
            startLevel.setStartLevel(2);
            var bundleActivator = bundle.adapt(BundleActivatorHolder.class);
            bundleActivator.addBundleActivator(activator);
        }

        logger.info("Starting framework");
        fw.start();

        logger.info("Waiting for bundle3");
        assertTrue(Bundle3.waitToBeStarted(5000));
        assertNotNull(Bundle1.bundleContext);
        assertNotNull(Bundle2.bundleContext);
        assertNotNull(Bundle3.bundleContext);

        logger.info("Stopping framework");
        fw.stop();

        logger.info("Waiting for framework stop");
        assertEquals(FrameworkEvent.STOPPED, fw.waitForStop(5000).getType());
        assertNull(Bundle1.bundleContext);
        assertNull(Bundle2.bundleContext);
        assertNull(Bundle3.bundleContext);
    }
}
