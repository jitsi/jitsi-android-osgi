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
package org.jitsi.impl.osgi.framework.launch;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import org.jitsi.impl.osgi.framework.*;
import org.jitsi.impl.osgi.framework.startlevel.*;
import org.osgi.framework.Filter;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.framework.startlevel.*;

/**
 * @author Lyubomir Marinov
 * @author Pawel Domas
 */
public class FrameworkImpl
    extends BundleImpl
    implements Framework
{
    /**
     * The logger
     */
    private final Logger logger
        = Logger.getLogger(FrameworkImpl.class.getName());

    private final List<BundleImpl> bundles = new LinkedList<>();

    private final Map<String, String> configuration;

    private EventDispatcher eventDispatcher;

    private final FrameworkStartLevelImpl frameworkStartLevel
        = new FrameworkStartLevelImpl(this);

    private long nextBundleId = 1;

    private long nextServiceId = 1;

    private final List<ServiceRegistrationImpl<?>> serviceRegistrations
        = new LinkedList<>();

    private final Object stopEvent = new Object();

    private final List<FrameworkListener> frameworkListeners =
        Collections.synchronizedList(new ArrayList<>());

    public FrameworkImpl(Map<String, String> configuration,
        ClassLoader classLoader)
    {
        super(null, 0, null, classLoader);

        this.configuration = configuration;

        bundles.add(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A adapt(Class<A> type)
    {
        if (FrameworkStartLevel.class.equals(type))
        {
            return (A) frameworkStartLevel;
        }

        return super.adapt(type);
    }

    public void addBundleListener(BundleImpl origin, BundleListener listener)
    {
        if (eventDispatcher != null)
        {
            eventDispatcher.addListener(origin, BundleListener.class, listener,
                null);
        }
    }

    public void addServiceListener(
        BundleImpl origin,
        ServiceListener listener,
        Filter filter)
    {
        if (eventDispatcher != null)
        {
            eventDispatcher.addListener(origin, ServiceListener.class, listener,
                filter);
        }
    }

    public void addFrameworkListener(FrameworkListener listener)
    {
        frameworkListeners.add(listener);
    }

    public void fireBundleEvent(int type, Bundle bundle)
    {
        fireBundleEvent(type, bundle, bundle);
    }

    private void fireBundleEvent(int type, Bundle bundle, Bundle origin)
    {
        if (eventDispatcher != null)
        {
            eventDispatcher.fireBundleEvent(
                new BundleEvent(type, bundle, origin));
        }
    }

    private void fireFrameworkEvent(int type, FrameworkListener... listeners)
    {
        FrameworkEvent event = new FrameworkEvent(type, this, null);
        if (listeners != null && listeners.length != 0)
        {
            for (FrameworkListener listener : listeners)
            {
                try
                {
                    listener.frameworkEvent(event);
                }
                catch (Exception t)
                {
                    logger.log(Level.SEVERE, "Error firing framework event", t);
                }
            }
        }

        for (FrameworkListener listener : frameworkListeners)
        {
            try
            {
                listener.frameworkEvent(event);
            }
            catch (Exception t)
            {
                logger.log(Level.SEVERE, "Error firing framework event", t);
            }
        }
    }

    private void fireServiceEvent(int type, ServiceReference<?> reference)
    {
        if (eventDispatcher != null)
        {
            eventDispatcher.fireServiceEvent(new ServiceEvent(type, reference));
        }
    }

    public BundleImpl getBundle(long id)
    {
        if (id == 0)
        {
            return this;
        }
        else
        {
            synchronized (this.bundles)
            {
                for (BundleImpl bundle : this.bundles)
                {
                    if (bundle.getBundleId() == id)
                    {
                        return bundle;
                    }
                }
            }
            return null;
        }
    }

    private List<BundleImpl> getBundlesByStartLevel(int startLevel)
    {
        List<BundleImpl> bundles = new LinkedList<>();

        synchronized (this.bundles)
        {
            for (BundleImpl bundle : this.bundles)
            {
                BundleStartLevel bundleStartLevel
                    = bundle.adapt(BundleStartLevel.class);

                if (bundleStartLevel != null
                    && bundleStartLevel.getStartLevel() == startLevel)
                {
                    bundles.add(bundle);
                }
            }
        }
        return bundles;
    }

    @SuppressWarnings("unchecked")
    public <S> Collection<ServiceReference<S>> getServiceReferences(
        BundleImpl origin,
        String className,
        Filter filter,
        boolean checkAssignable)
        throws InvalidSyntaxException
    {
        Filter classNameFilter
            = FrameworkUtil.createFilter(
            '('
                + Constants.OBJECTCLASS
                + '='
                + (className == null ? '*' : className)
                + ')');

        List<ServiceReference<S>> serviceReferences = new LinkedList<>();
        synchronized (serviceRegistrations)
        {
            for (var serviceRegistration : serviceRegistrations)
            {
                ServiceReference<S> serviceReference
                    = (ServiceReference<S>) serviceRegistration.getReference();

                if (classNameFilter.match(serviceReference)
                    && (filter == null
                    || filter.match(serviceReference)))
                {
                    serviceReferences.add(serviceReference);
                }
            }
        }

        return serviceReferences;
    }

    @Override
    public FrameworkImpl getFramework()
    {
        return this;
    }

    private long getNextBundleId()
    {
        return nextBundleId++;
    }

    @Override
    public void init()
        throws BundleException
    {
        init((FrameworkListener[]) null);
    }

    @Override
    public void init(FrameworkListener... listeners)
        throws BundleException
    {
        if (listeners != null)
        {
            frameworkListeners.addAll(Arrays.asList(listeners));
        }

        setState(STARTING);
    }

    public Bundle installBundle(BundleImpl origin, String location)
        throws BundleException
    {
        if (location == null)
        {
            throw new BundleException("location");
        }

        BundleImpl bundle = null;
        boolean fireBundleEvent = false;

        synchronized (bundles)
        {
            for (BundleImpl existing : bundles)
            {
                if (existing.getLocation().equals(location))
                {
                    bundle = existing;
                    break;
                }
            }
            if (bundle == null)
            {
                bundle
                    = new BundleImpl(
                    getFramework(),
                    getNextBundleId(),
                    location,
                    classLoader);
                bundles.add(bundle);
                fireBundleEvent = true;
            }
        }

        if (fireBundleEvent)
        {
            fireBundleEvent(BundleEvent.INSTALLED, bundle, origin);
        }

        return bundle;
    }

    public <T> ServiceRegistration<T> registerService(
        BundleImpl origin,
        String[] classNames,
        T service,
        Dictionary<String, ?> properties)
    {
        if (classNames == null || classNames.length == 0)
        {
            throw new IllegalArgumentException("classNames");
        }
        else if (service == null)
        {
            throw new IllegalArgumentException("service");
        }
        else
        {
            Class<?> serviceClass = service.getClass();

            if (!ServiceFactory.class.isAssignableFrom(serviceClass))
            {
                ClassLoader classLoader = serviceClass.getClassLoader();

                for (String className : classNames)
                {
                    boolean illegalArgumentException = true;
                    Throwable cause = null;

                    try
                    {
                        if (Class.forName(className, false, classLoader)
                            .isAssignableFrom(serviceClass))
                        {
                            illegalArgumentException = false;
                        }
                    }
                    catch (ClassNotFoundException | LinkageError eiie)
                    {
                        cause = eiie;
                    }
                    if (illegalArgumentException)
                    {
                        throw new IllegalArgumentException(className, cause);
                    }
                }
            }
        }

        long serviceId;

        synchronized (serviceRegistrations)
        {
            serviceId = nextServiceId++;
        }

        ServiceRegistrationImpl<T> serviceRegistration
            = new ServiceRegistrationImpl<>(
            origin,
            serviceId,
            classNames, service, properties);

        synchronized (serviceRegistrations)
        {
            serviceRegistrations.add(serviceRegistration);
        }
        fireServiceEvent(
            ServiceEvent.REGISTERED,
            serviceRegistration.getReference());
        return serviceRegistration;
    }

    public void removeBundleListener(BundleImpl origin, BundleListener listener)
    {
        if (eventDispatcher != null)
        {
            eventDispatcher.removeListener(
                origin,
                BundleListener.class,
                listener);
        }
    }

    public void removeServiceListener(
        BundleImpl origin,
        ServiceListener listener)
    {
        if (eventDispatcher != null)
        {
            eventDispatcher.removeListener(
                origin,
                ServiceListener.class,
                listener);
        }
    }

    public void removeFrameworkListener(FrameworkListener listener)
    {
        frameworkListeners.remove(listener);
    }

    @Override
    public void start(int options)
        throws BundleException
    {
        int state = getState();

        if (state == INSTALLED || state == RESOLVED)
        {
            init();
            state = getState();
        }

        if (state == STARTING)
        {
            int startLevel = 1;
            if (configuration != null)
            {
                var s =
                    configuration.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
                if (s != null)
                {
                    try
                    {
                        startLevel = Integer.parseInt(s);
                    }
                    catch (NumberFormatException nfe)
                    {
                        // ignore, keep default start level
                    }
                }
            }

            var startLevelAwaiter = new Semaphore(0);
            frameworkStartLevel.setStartLevel(startLevel,
                event -> startLevelAwaiter.release());
            while (frameworkStartLevel.getStartLevel() < startLevel)
            {
                try
                {
                    startLevelAwaiter.acquire();
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            setState(ACTIVE);
            fireFrameworkEvent(FrameworkEvent.STARTED);
        }
    }

    public void startLevelChanged(
        int oldStartLevel, int newStartLevel,
        FrameworkListener... listeners)
    {
        if (oldStartLevel < newStartLevel)
        {
            for (BundleImpl bundle : getBundlesByStartLevel(newStartLevel))
            {
                try
                {
                    BundleStartLevel bundleStartLevel
                        = bundle.adapt(BundleStartLevel.class);
                    int options = START_TRANSIENT;

                    bundle.start(options);
                }
                catch (Exception t)
                {
                    logger.log(Level.SEVERE, "Error changing start level", t);
                }
            }
        }

        fireFrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, listeners);
    }

    public void startLevelChanging(int oldStartLevel, int newStartLevel)
    {
        if (oldStartLevel > newStartLevel)
        {
            for (BundleImpl bundle : getBundlesByStartLevel(oldStartLevel))
            {
                try
                {
                    bundle.stop(STOP_TRANSIENT);
                }
                catch (Exception t)
                {
                    logger.log(Level.SEVERE, "Error changing start level", t);
                }
            }
        }
    }

    @Override
    protected void stateChanged(int oldState, int newState)
    {
        switch (newState)
        {
        case RESOLVED:
            eventDispatcher = null;
            break;
        case STARTING:
            eventDispatcher = new EventDispatcher();
            break;
        }

        super.stateChanged(oldState, newState);
    }

    @Override
    public void stop(int options)
        throws BundleException
    {
        logger.fine("Stopping framework");
        setState(STOPPING);
        logger.fine("Waiting for framework stop event");
        frameworkStartLevel.internalSetStartLevel(0,
            event ->
            {
                setState(RESOLVED);
                fireFrameworkEvent(FrameworkEvent.STOPPED);
                synchronized (stopEvent)
                {
                    stopEvent.notifyAll();
                }
            });
    }

    public void unregisterService(
        BundleImpl origin,
        ServiceRegistration<?> serviceRegistration)
    {
        boolean removed;

        synchronized (serviceRegistrations)
        {
            removed = serviceRegistrations.remove(serviceRegistration);
        }

        if (removed)
        {
            fireServiceEvent(
                ServiceEvent.UNREGISTERING,
                serviceRegistration.getReference());
        }
        else
        {
            throw new IllegalStateException("serviceRegistrations");
        }
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices()
    {
        ServiceReference<?>[] references
            = new ServiceReference<?>[serviceRegistrations.size()];

        for (int i = 0; i < serviceRegistrations.size(); i++)
        {
            references[i] = serviceRegistrations.get(i).getReference();
        }

        return references;
    }

    @Override
    public FrameworkEvent waitForStop(long timeout)
        throws InterruptedException
    {
        var start = System.nanoTime();
        var timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeout);
        synchronized (stopEvent)
        {
            if (timeout > 0 && (System.nanoTime() - start) > timeoutNanos)
            {
                return new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, this,
                    null);
            }

            while (getState() != RESOLVED)
            {
                stopEvent.wait(timeout);
            }
        }

        return new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
    }
}
