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
import org.jitsi.impl.osgi.framework.startlevel.*;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.*;

import java.io.*;
import java.net.*;
import java.security.cert.*;
import java.util.*;
import java.util.logging.*;

/**
 *
 * @author Lyubomir Marinov
 * @author Pawel Domas
 */
public class BundleImpl
    implements Bundle
{
    /**
     * The Logger
     */
    private Logger logger = Logger.getLogger(BundleImpl.class.getName());

    private BundleActivator bundleActivator;

    private BundleContext bundleContext;

    private final long bundleId;

    private BundleStartLevel bundleStartLevel;

    private final FrameworkImpl framework;

    private final String location;

    protected final ClassLoader classLoader;

    private int state = INSTALLED;

    public BundleImpl(FrameworkImpl framework, long bundleId, String location, ClassLoader classLoader)
    {
        this.framework = framework;
        this.bundleId = bundleId;
        this.location = location;
        this.classLoader = classLoader;
    }

    public <A> A adapt(Class<A> type)
    {
        Object adapt;

        if (BundleStartLevel.class.equals(type))
        {
            if (getBundleId() == 0)
                adapt = null;
            else
                synchronized (this)
                {
                    if (bundleStartLevel == null)
                        bundleStartLevel = new BundleStartLevelImpl(this);

                    adapt = bundleStartLevel;
                }
        }
        else
            adapt = null;

        @SuppressWarnings("unchecked")
        A a = (A) adapt;

        return a;
    }

    public int compareTo(Bundle other)
    {
        long thisBundleId = getBundleId();
        long otherBundleId = other.getBundleId();

        if (thisBundleId < otherBundleId)
            return -1;
        else if (thisBundleId == otherBundleId)
            return 0;
        else
            return 1;
    }

    public Enumeration<URL> findEntries(
            String path,
            String filePattern,
            boolean recurse)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public BundleContext getBundleContext()
    {
        switch (getState())
        {
        case STARTING:
        case ACTIVE:
        case STOPPING:
            return bundleContext;
        default:
            return null;
        }
    }

    public long getBundleId()
    {
        return bundleId;
    }

    public File getDataFile(String filename)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public URL getEntry(String path)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration<String> getEntryPaths(String path)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public FrameworkImpl getFramework()
    {
        return framework;
    }

    public Dictionary<String, String> getHeaders()
    {
        return getHeaders(null);
    }

    public Dictionary<String, String> getHeaders(String locale)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public long getLastModified()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getLocation()
    {
        return
            (getBundleId() == 0) ? Constants.SYSTEM_BUNDLE_LOCATION : location;
    }

    public ServiceReference<?>[] getRegisteredServices()
    {
        return framework.getRegisteredServices();
    }

    public URL getResource(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration<URL> getResources(String name)
        throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceReference<?>[] getServicesInUse()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
            int signersType)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getState()
    {
        return state;
    }

    public String getSymbolicName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Version getVersion()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasPermission(Object permission)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Class<?> loadClass(String name)
        throws ClassNotFoundException
    {
        return classLoader.loadClass(name);
    }

    protected void setBundleContext(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    protected void setState(int state)
    {
        int oldState = getState();

        if (oldState != state)
        {
            this.state = state;

            int newState = getState();

            if (oldState != newState)
                stateChanged(oldState, newState);
        }
    }

    public void start()
        throws BundleException
    {
        start(0);
    }

    public void start(int options)
        throws BundleException
    {
        if (getState() == UNINSTALLED)
            throw new IllegalStateException("Bundle.UNINSTALLED");

        BundleStartLevel bundleStartLevel = adapt(BundleStartLevel.class);
        FrameworkStartLevel frameworkStartLevel
            = getFramework().adapt(FrameworkStartLevel.class);

        if ((bundleStartLevel != null)
                && (bundleStartLevel.getStartLevel()
                        > frameworkStartLevel.getStartLevel()))
        {
            if ((options & START_TRANSIENT) == START_TRANSIENT)
                throw new BundleException("startLevel");
            else
                return;
        }

        if (getState() == ACTIVE)
            return;

        if (getState() == INSTALLED)
            setState(RESOLVED);

        setState(STARTING);

        String location = getLocation();

        if (location != null)
        {
            BundleActivator bundleActivator = null;
            Throwable exception = null;

            try
            {
                bundleActivator
                    = (BundleActivator)
                        loadClass(location.replace('/', '.')).newInstance();

                bundleActivator.start(getBundleContext());
            }
            catch (Throwable t)
            {
                logger.log(Level.SEVERE,
                           "Error starting bundle: "+bundleActivator, t);

                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
                else
                    exception = t;
            }

            if (exception == null)
                this.bundleActivator = bundleActivator;
            else
            {
                setState(STOPPING);
                setState(RESOLVED);
                getFramework().fireBundleEvent(BundleEvent.STOPPED, this);
                throw new BundleException("BundleActivator.start", exception);
            }
        }

        if (getState() == UNINSTALLED)
            throw new IllegalStateException("Bundle.UNINSTALLED");

        setState(ACTIVE);
    }

    protected void stateChanged(int oldState, int newState)
    {
        switch (newState)
        {
        case ACTIVE:
            getFramework().fireBundleEvent(BundleEvent.STARTED, this);
            break;
        case RESOLVED:
            setBundleContext(null);
            break;
        case STARTING:
            setBundleContext(new BundleContextImpl(getFramework(), this));

            /*
             * BundleEvent.STARTING is only delivered to
             * SynchronousBundleListeners, it is not delivered to
             * BundleListeners.
             */
            break;
        case STOPPING:
            /*
             * BundleEvent.STOPPING is only delivered to
             * SynchronousBundleListeners, it is not delivered to
             * BundleListeners.
             */
            break;
        }
    }

    public void stop()
        throws BundleException
    {
        stop(0);
    }

    public void stop(int options)
        throws BundleException
    {
        boolean wasActive = false;

        switch (getState())
        {
        case ACTIVE:
            wasActive = true;
        case STARTING:
            setState(STOPPING);

            Throwable exception = null;

            if (wasActive && (bundleActivator != null))
            {
                try
                {
                    bundleActivator.stop(getBundleContext());
                }
                catch (Throwable t)
                {
                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                    else
                        exception = t;
                }
                this.bundleActivator = null;
            }

            if (getState() == UNINSTALLED)
                throw new BundleException("Bundle.UNINSTALLED");

            setState(RESOLVED);
            getFramework().fireBundleEvent(BundleEvent.STOPPED, this);

            if (exception != null)
                throw new BundleException("BundleActivator.stop", exception);
            break;

        case UNINSTALLED:
            throw new IllegalStateException("Bundle.UNINSTALLED");
        default:
            break;
        }
    }

    public void uninstall()
        throws BundleException
    {
        // TODO Auto-generated method stub
    }

    public void update()
        throws BundleException
    {
        update(null);
    }

    public void update(InputStream input)
        throws BundleException
    {
        // TODO Auto-generated method stub
    }
}
