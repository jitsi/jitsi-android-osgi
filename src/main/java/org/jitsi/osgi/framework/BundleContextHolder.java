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
package org.jitsi.osgi.framework;

import org.osgi.framework.*;

/**
 * Represents an accessor to a <tt>BundleContext</tt>. When the accessor
 * acquires a <tt>BundleContext</tt> or looses it, it notifies the
 * <tt>BundleActivator</tt>s registered with it.
 *
 * @author Lyubomir Marinov
 */
public interface BundleContextHolder
{
    /**
     * Registers a <tt>BundleActivator</tt> with this instance to be notified
     * when this instance acquires a <tt>BundleContext</tt> or looses it.
     * 
     * @param bundleActivator the <tt>BundlerActivator</tt> to register with
     * this instance
     */
    public void addBundleActivator(BundleActivator bundleActivator);

    /**
     * Gets the <tt>BundleContext</tt> in which this instance has been started.
     *
     * @return the <tt>BundleContext</tt> in which this instance has been
     * started or <tt>null</tt> if this instance has not been started in a
     * <tt>BundleContext</tt>
     */
    public BundleContext getBundleContext();

    /**
     * Unregisters a <tt>BundleActivator</tt> with this instance to no longer be
     * notified when this instance acquires a <tt>BundleContext</tt> or looses
     * it.
     * 
     * @param bundleActivator the <tt>BundlerActivator</tt> to unregister with
     * this instance
     */
    public void removeBundleActivator(BundleActivator bundleActivator);
}
