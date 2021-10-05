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

import org.osgi.framework.*;

import java.util.*;

/**
 *
 * @author Lyubomir Marinov
 */
public class ServiceRegistrationImpl<S>
    implements ServiceRegistration<S>
{
    private static final Comparator<String> CASE_INSENSITIVE_COMPARATOR
        = new Comparator<String>()
        {
            public int compare(String s1, String s2)
            {
                return s1.compareToIgnoreCase(s2);
            }
        };

    private static final Map<String, Object> EMPTY_PROPERTIES
        = newCaseInsensitiveMapInstance();

    private final BundleImpl bundle;

    private final String[] classNames;

    private final Map<String, Object> properties;

    private final S service;

    private final long serviceId;

    private final ServiceReferenceImpl serviceReference
        = new ServiceReferenceImpl();

    public ServiceRegistrationImpl(
            BundleImpl bundle,
            long serviceId,
            String[] classNames,
            S service,
            Dictionary<String, ?> properties)
    {
        this.bundle = bundle;
        this.serviceId = serviceId;
        this.classNames = classNames;
        this.service = service;

        if ((properties == null) || properties.isEmpty())
                this.properties = EMPTY_PROPERTIES;
        else
        {
            Enumeration<String> keys = properties.keys();
            Map<String, Object> thisProperties
                = newCaseInsensitiveMapInstance();

            while (keys.hasMoreElements())
            {
                String key = keys.nextElement();

                if (Constants.OBJECTCLASS.equalsIgnoreCase(key)
                        || Constants.SERVICE_ID.equalsIgnoreCase(key))
                    continue;
                else if (thisProperties.containsKey(key))
                    throw new IllegalArgumentException(key);
                else
                    thisProperties.put(key, properties.get(key));
            }

            this.properties
                = thisProperties.isEmpty()
                    ? EMPTY_PROPERTIES
                    : thisProperties;
        }
    }

    @Override
    public ServiceReference<S> getReference()
    {
        return serviceReference;
    }

    public ServiceReference<S> getReference(Class<S> clazz)
    {
        return serviceReference;
    }

    private static Map<String, Object> newCaseInsensitiveMapInstance()
    {
        return new TreeMap<>(String::compareToIgnoreCase);
    }

    @Override
    public void setProperties(Dictionary properties)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void unregister()
    {
        bundle.getFramework().unregisterService(bundle, this);
    }

    class ServiceReferenceImpl implements ServiceReference<S>
    {
        @Override
        public int compareTo(Object other)
        {
            Long otherServiceId = ((ServiceRegistrationImpl<S>) other).serviceId;

            return otherServiceId.compareTo(
                ServiceRegistrationImpl.this.serviceId);
        }

        @Override
        public Dictionary<String, Object> getProperties()
        {
            synchronized (properties)
            {
                Dictionary<String, Object> dict = new Hashtable<>(properties.size());
                for (Map.Entry<String, Object> e : properties.entrySet())
                {
                    dict.put(e.getKey(), e.getValue());
                }
                return dict;
            }
        }

        @Override
        public <A> A adapt(Class<A> type)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle getBundle()
        {
            return bundle;
        }

        @Override
        public Object getProperty(String key)
        {
            Object value;

            if (Constants.OBJECTCLASS.equalsIgnoreCase(key))
                value = classNames;
            else if (Constants.SERVICE_ID.equalsIgnoreCase(key))
                value = serviceId;
            else
                synchronized (properties)
                {
                    value = properties.get(key);
                }
            return value;
        }

        @Override
        public String[] getPropertyKeys()
        {
            synchronized (properties)
            {
                String[] keys = new String[2 + properties.size()];
                int index = 0;

                keys[index++] = Constants.OBJECTCLASS;
                keys[index++] = Constants.SERVICE_ID;

                for (String key : properties.keySet())
                    keys[index++] = key;
                return keys;
            }
        }

        Object getService()
        {
            return service;
        }

        @Override
        public Bundle[] getUsingBundles()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isAssignableTo(Bundle bundle, String className)
        {
            // TODO Auto-generated method stub
            return false;
        }
    }
}
