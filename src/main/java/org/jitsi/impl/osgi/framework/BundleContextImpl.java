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
import java.util.*;
import org.jitsi.impl.osgi.framework.launch.*;
import org.osgi.framework.*;

/**
 * @author Lyubomir Marinov
 */
public class BundleContextImpl
    implements BundleContext
{
    private final BundleImpl bundle;

    private final FrameworkImpl framework;

    public BundleContextImpl(FrameworkImpl framework, BundleImpl bundle)
    {
        this.framework = framework;
        this.bundle = bundle;
    }

    @Override
    public void addBundleListener(BundleListener listener)
    {
        framework.addBundleListener(getBundle(), listener);
    }

    @Override
    public void addFrameworkListener(FrameworkListener listener)
    {
        framework.addFrameworkListener(listener);
    }

    @Override
    public void addServiceListener(ServiceListener listener)
    {
        try
        {
            addServiceListener(listener, null);
        }
        catch (InvalidSyntaxException ise)
        {
            // Since filter is null, there should be no InvalidSyntaxException.
        }
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter)
        throws InvalidSyntaxException
    {
        framework.addServiceListener(
            getBundle(),
            listener,
            filter == null ? null : createFilter(filter));
    }

    @Override
    public Filter createFilter(String filter)
        throws InvalidSyntaxException
    {
        return FrameworkUtil.createFilter(filter);
    }

    @Override
    public BundleImpl getBundle()
    {
        return bundle;
    }

    @Override
    public Bundle getBundle(long id)
    {
        return framework.getBundle(id);
    }

    @Override
    public Bundle getBundle(String location)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle[] getBundles()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getDataFile(String filename)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProperty(String key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> S getService(ServiceReference<S> reference)
    {
        return
            ((ServiceRegistrationImpl<S>.ServiceReferenceImpl) reference)
                .getService();
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz)
    {
        return getServiceReferenceInternal(clazz.getName());
    }

    @Override
    public ServiceReference<?> getServiceReference(String className)
    {
        return getServiceReferenceInternal(className);
    }

    @SuppressWarnings("unchecked")
    private <S> ServiceReference<S> getServiceReferenceInternal(
        String className)
    {
        ServiceReference<S>[] serviceReferences;

        try
        {
            serviceReferences =
                (ServiceReference<S>[]) getServiceReferences(className, null);
        }
        catch (InvalidSyntaxException ise)
        {
            // No InvalidSyntaxException is expected because the filter is null.
            serviceReferences = null;
        }

        return
            serviceReferences == null || serviceReferences.length == 0
                ? null
                : serviceReferences[0];
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(
        Class<S> clazz,
        String filter)
        throws InvalidSyntaxException
    {
        return getServiceReferences(clazz.getName(), filter, true);
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(
        String className,
        String filter)
        throws InvalidSyntaxException
    {
        return getServiceReferences(className, filter, true)
            .toArray(new ServiceReference<?>[0]);
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(
        String className,
        String filter)
        throws InvalidSyntaxException
    {
        return getServiceReferences(className, filter, false)
            .toArray(new ServiceReference<?>[0]);
    }

    private <S> Collection<ServiceReference<S>> getServiceReferences(
        String className,
        String filter,
        boolean checkAssignable)
        throws InvalidSyntaxException
    {
        return
            framework.getServiceReferences(
                getBundle(),
                className,
                filter == null ? null : createFilter(filter),
                checkAssignable);
    }

    @Override
    public Bundle installBundle(String location)
        throws BundleException
    {
        return installBundle(location, null);
    }

    @Override
    public Bundle installBundle(String location, InputStream input)
        throws BundleException
    {
        return framework.installBundle(getBundle(), location);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(
        Class<S> clazz,
        S service,
        Dictionary<String, ?> properties)
    {
        return framework.registerService(
            getBundle(), new String[] { clazz.getName() }, service, properties);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz,
        ServiceFactory<S> factory, Dictionary<String, ?> properties)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceRegistration<?> registerService(
        String className,
        Object service,
        Dictionary<String, ?> properties)
    {
        return registerService(new String[] { className }, service, properties);
    }

    @Override
    public ServiceRegistration<?> registerService(
        String[] classNames,
        Object service,
        Dictionary<String, ?> properties)
    {
        return framework.registerService(
            getBundle(), classNames, service, properties);
    }

    @Override
    public void removeBundleListener(BundleListener listener)
    {
        framework.removeBundleListener(getBundle(), listener);
    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener)
    {
        framework.removeFrameworkListener(listener);
    }

    @Override
    public void removeServiceListener(ServiceListener listener)
    {
        framework.removeServiceListener(getBundle(), listener);
    }

    @Override
    public boolean ungetService(ServiceReference<?> reference)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(
        ServiceReference<S> reference)
    {
        throw new UnsupportedOperationException();
    }
}
