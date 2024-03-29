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
 * Represents an accessor to a specialized {@link Bundle}.
 */
public interface BundleActivatorHolder
{
    /**
     * Registers a {@link BundleActivator} with this instance.
     *
     * @param bundleActivator the Activator to register with this bundle.
     */
    void addBundleActivator(Class<? extends BundleActivator> bundleActivator);
}
