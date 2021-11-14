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

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.cert.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;
import org.apache.commons.io.file.*;
import org.apache.commons.io.filefilter.*;
import org.jitsi.impl.osgi.framework.launch.*;
import org.jitsi.impl.osgi.framework.startlevel.*;
import org.jitsi.osgi.framework.*;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.*;

/**
 * @author Lyubomir Marinov
 * @author Pawel Domas
 */
public class BundleImpl
    implements Bundle, BundleActivatorHolder
{
    private static final Logger logger =
        Logger.getLogger(BundleImpl.class.getName());

    private final List<Class<? extends BundleActivator>>
        bundleActivatorClasses = new ArrayList<>();

    private final List<BundleActivator> bundleActivators = new ArrayList<>();

    private BundleContext bundleContext;

    private final long bundleId;

    private final BundleStartLevel bundleStartLevel =
        new BundleStartLevelImpl(this);

    private final FrameworkImpl framework;

    private final String location;

    protected final ClassLoader classLoader;

    private volatile int state = INSTALLED;

    public BundleImpl(FrameworkImpl framework, long bundleId, String location,
        ClassLoader classLoader)
    {
        this.framework = framework;
        this.bundleId = bundleId;
        this.location = location;
        this.classLoader = classLoader;
    }

    @SuppressWarnings("unchecked")
    public <A> A adapt(Class<A> type)
    {
        if (BundleStartLevel.class.equals(type))
        {
            if (getBundleId() == 0)
            {
                return null;
            }
            else
            {
                return (A) bundleStartLevel;
            }
        }
        else if (BundleActivatorHolder.class.equals(type))
        {
            return (A) this;
        }

        return null;
    }

    public int compareTo(Bundle other)
    {
        long thisBundleId = getBundleId();
        long otherBundleId = other.getBundleId();
        return Long.compare(thisBundleId, otherBundleId);
    }

    public Enumeration<URL> findEntries(
        String path,
        String filePattern,
        boolean recurse)
    {
        File f;
        try
        {
            f = new File(new URI(location));
        }
        catch (URISyntaxException e)
        {
            logger.log(Level.SEVERE,
                "Could not get bundle URI from " + location, e);
            return null;
        }
        if (f.exists())
        {
            if (path.startsWith("/"))
            {
                path = path.substring(1);
            }
            if (!path.endsWith("/"))
            {
                path += "/";
            }

            Iterator<URL> matches;
            if (f.isFile() && f.getName().endsWith(".jar"))
            {
                matches = getJarEntries(path, filePattern, recurse, f);
            }
            else if (f.isDirectory())
            {
                matches = getDirectoryEntries(path, filePattern, f);
            }
            else
            {
                logger.log(Level.SEVERE,
                    location + " is neither a file nor a directory");
                return null;
            }

            if (matches == null)
            {
                return null;
            }

            return new Enumeration<>()
            {
                @Override
                public boolean hasMoreElements()
                {
                    return matches.hasNext();
                }

                @Override
                public URL nextElement()
                {
                    return matches.next();
                }
            };
        }
        return null;
    }

    private Iterator<URL> getJarEntries(String path, String filePattern,
        boolean recurse, File f)
    {
        try
        {
            var z = new ZipFile(f);
            var filter = new WildcardFileFilter(
                filePattern == null ? "*" : filePattern);
            return z.stream()
                .filter(e -> recurse
                    ? e.getName().startsWith(path)
                    : e.getName().equals(path)
                        && filter.accept(new File(e.getName()))
                )
                .map(e -> {
                    try
                    {
                        return new URL(e.getName());
                    }
                    catch (MalformedURLException ex)
                    {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .iterator();
        }
        catch (IOException e)
        {
            logger.log(Level.SEVERE, "Could not open " + location, e);
            return null;
        }
    }

    private Iterator<URL> getDirectoryEntries(String path, String filePattern,
        File f)
    {
        var fileFilter = new WildcardFileFilter(
            filePattern == null ? "*" : filePattern);
        var visitor = AccumulatorPathVisitor.withLongCounters(fileFilter,
            FileFilterUtils.trueFileFilter());
        try
        {
            Files.walkFileTree(new File(f, path).toPath(), visitor);
        }
        catch (IOException e)
        {
            logger.log(Level.SEVERE, "Could not walk files in " + location, e);
            return null;
        }

        if (!visitor.getFileList().isEmpty())
        {
            return visitor.getFileList().stream()
                .map(r ->
                {
                    try
                    {
                        return r.toUri().toURL();
                    }
                    catch (MalformedURLException e)
                    {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .iterator();
        }
        else
        {
            return null;
        }
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
        throw new UnsupportedOperationException();
    }

    public URL getEntry(String path)
    {
        throw new UnsupportedOperationException();
    }

    public Enumeration<String> getEntryPaths(String path)
    {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    public long getLastModified()
    {
        throw new UnsupportedOperationException();
    }

    public String getLocation()
    {
        return
            getBundleId() == 0 ? Constants.SYSTEM_BUNDLE_LOCATION : location;
    }

    public ServiceReference<?>[] getRegisteredServices()
    {
        return framework.getRegisteredServices();
    }

    public URL getResource(String name)
    {
        throw new UnsupportedOperationException();
    }

    public Enumeration<URL> getResources(String name)
    {
        throw new UnsupportedOperationException();
    }

    public ServiceReference<?>[] getServicesInUse()
    {
        throw new UnsupportedOperationException();
    }

    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
        int signersType)
    {
        throw new UnsupportedOperationException();
    }

    public int getState()
    {
        return state;
    }

    public String getSymbolicName()
    {
        throw new UnsupportedOperationException();
    }

    public Version getVersion()
    {
        throw new UnsupportedOperationException();
    }

    public boolean hasPermission(Object permission)
    {
        throw new UnsupportedOperationException();
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
            {
                stateChanged(oldState, newState);
            }
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
        {
            throw new IllegalStateException("Bundle.UNINSTALLED");
        }

        BundleStartLevel bundleStartLevel = adapt(BundleStartLevel.class);
        FrameworkStartLevel frameworkStartLevel
            = getFramework().adapt(FrameworkStartLevel.class);

        if (bundleStartLevel != null
            && bundleStartLevel.getStartLevel()
            > frameworkStartLevel.getStartLevel())
        {
            if ((options & START_TRANSIENT) == START_TRANSIENT)
            {
                throw new BundleException("startLevel");
            }
            else
            {
                return;
            }
        }

        if (getState() == ACTIVE)
        {
            return;
        }

        if (getState() == INSTALLED)
        {
            setState(RESOLVED);
        }

        setState(STARTING);
        try
        {
            for (var activatorClass : bundleActivatorClasses)
            {
                logger.log(Level.INFO,
                    "Starting activator " + activatorClass.getName()
                        + " in " + location);
                var activator =
                    activatorClass.getDeclaredConstructor().newInstance();
                activator.start(getBundleContext());
                bundleActivators.add(activator);
            }
        }
        catch (Exception t)
        {
            logger.log(Level.SEVERE,
                "Error starting bundle: " + getLocation(), t);

            setState(STOPPING);
            setState(RESOLVED);
            getFramework().fireBundleEvent(BundleEvent.STOPPED, this);
            throw new BundleException("BundleActivator.start", t);
        }

        if (getState() == UNINSTALLED)
        {
            throw new IllegalStateException("Bundle.UNINSTALLED");
        }

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

    @SuppressWarnings("fallthrough")
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

            if (wasActive)
            {
                for (var activator : bundleActivators)
                {
                    try
                    {
                        activator.stop(getBundleContext());
                    }
                    catch (Exception t)
                    {
                        exception = t;
                    }
                }
            }

            if (getState() == UNINSTALLED)
            {
                throw new BundleException("Bundle.UNINSTALLED");
            }

            setState(RESOLVED);
            getFramework().fireBundleEvent(BundleEvent.STOPPED, this);
            if (exception != null)
            {
                throw new BundleException("BundleActivator.stop", exception);
            }
            break;

        case UNINSTALLED:
            throw new IllegalStateException("Bundle.UNINSTALLED");
        default:
            break;
        }
    }

    public void uninstall()
    {
        throw new UnsupportedOperationException();
    }

    public void update()
        throws BundleException
    {
        update(null);
    }

    public void update(InputStream input)
        throws BundleException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addBundleActivator(
        Class<? extends BundleActivator> activatorClass)
    {
        bundleActivatorClasses.add(activatorClass);
    }
}
