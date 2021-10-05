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
import org.osgi.framework.*;

import java.io.*;
import java.util.*;

/**
 *
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
        // TODO Auto-generated method stub
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
                (filter == null) ? null : createFilter(filter));
    }

    @Override
    public Filter createFilter(String filter)
        throws InvalidSyntaxException
    {
        return FrameworkUtil.createFilter(filter);
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(
            String className,
            String filter)
        throws InvalidSyntaxException
    {
        return getServiceReferences(className, filter, false);
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle[] getBundles()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getDataFile(String filename)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProperty(String key)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> S getService(ServiceReference<S> reference)
    {
        return
            (S) ((ServiceRegistrationImpl.ServiceReferenceImpl) reference)
                .getService();
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz)
    {
        return getServiceReference(clazz, clazz.getName());
    }

    private <S> ServiceReference<S> getServiceReference(
            Class<S> clazz,
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
            // No InvlidSyntaxException is expected because the filter is null.
            serviceReferences = null;
        }

        return
            ((serviceReferences == null) || (serviceReferences.length == 0))
                ? null
                : serviceReferences[0];
    }

    @Override
    public ServiceReference<?> getServiceReference(String className)
    {
        return getServiceReference(Object.class, className);
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(
            Class<S> clazz,
            String filter)
        throws InvalidSyntaxException
    {
        return getServiceReferences(clazz, clazz.getName(), filter, true);
    }

    private <S> Collection<ServiceReference<S>> getServiceReferences(
            Class<S> clazz,
            String className,
            String filter,
            boolean checkAssignable)
        throws InvalidSyntaxException
    {
        return
            framework.getServiceReferences(
                    getBundle(),
                    clazz,
                    className,
                    (filter == null) ? null : createFilter(filter),
                    checkAssignable);
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(
            String className,
            String filter)
        throws InvalidSyntaxException
    {
        return getServiceReferences(className, filter, true);
    }

    private <S> ServiceReference<S>[] getServiceReferences(
            String className,
            String filter,
            boolean checkAssignable)
        throws InvalidSyntaxException
    {
        Collection<ServiceReference<Object>> serviceReferences
            = getServiceReferences(
                    Object.class,
                    className,
                    filter,
                    checkAssignable);

        return serviceReferences.toArray(new ServiceReference[0]);
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
        return framework.installBundle(getBundle(), location, input);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(
            Class<S> clazz,
            S service,
            Dictionary<String, ?> properties)
    {
        return
            registerService(
                    clazz,
                    new String[] { clazz.getName() }, service, properties);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz,
        ServiceFactory<S> factory, Dictionary<String, ?> properties)
    {
        throw new UnsupportedOperationException();
    }

    private <S> ServiceRegistration<S> registerService(
            Class<S> clazz,
            String[] classNames,
            S service,
            Dictionary<String, ?> properties)
    {
        return
            framework.registerService(
                    getBundle(),
                    clazz,
                    classNames, service, properties);
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
        return registerService(Object.class, classNames, service, properties);
    }

    @Override
    public void removeBundleListener(BundleListener listener)
    {
        framework.removeBundleListener(getBundle(), listener);
    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeServiceListener(ServiceListener listener)
    {
        framework.removeServiceListener(getBundle(), listener);
    }

    @Override
    public boolean ungetService(ServiceReference<?> reference)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(
        ServiceReference<S> reference)
    {
        throw new UnsupportedOperationException();
    }
}
