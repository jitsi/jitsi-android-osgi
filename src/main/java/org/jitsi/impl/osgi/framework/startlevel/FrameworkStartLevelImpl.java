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
package org.jitsi.impl.osgi.framework.startlevel;

import java.util.concurrent.*;
import org.jitsi.impl.osgi.framework.launch.*;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.*;

/**
 * @author Lyubomir Marinov
 */
public class FrameworkStartLevelImpl
    implements FrameworkStartLevel
{
    private final FrameworkImpl framework;

    private int initialBundleStartLevel = 0;

    private int startLevel;

    public FrameworkStartLevelImpl(FrameworkImpl framework)
    {
        this.framework = framework;
    }

    @Override
    public FrameworkImpl getBundle()
    {
        return framework;
    }

    @Override
    public int getInitialBundleStartLevel()
    {
        int initialBundleStartLevel = this.initialBundleStartLevel;

        if (initialBundleStartLevel == 0)
        {
            initialBundleStartLevel = 1;
        }
        return initialBundleStartLevel;
    }

    @Override
    public synchronized int getStartLevel()
    {
        return startLevel;
    }

    public void internalSetStartLevel(
        int startLevel,
        FrameworkListener... listeners)
    {
        if (startLevel < 0)
        {
            throw new IllegalArgumentException("startLevel");
        }

        ForkJoinPool.commonPool().execute(new Command(startLevel, listeners));
    }

    @Override
    public void setInitialBundleStartLevel(int initialBundleStartLevel)
    {
        if (initialBundleStartLevel <= 0)
        {
            throw new IllegalArgumentException("initialBundleStartLevel");
        }

        this.initialBundleStartLevel = initialBundleStartLevel;
    }

    @Override
    public void setStartLevel(int startLevel, FrameworkListener... listeners)
    {
        if (startLevel == 0)
        {
            throw new IllegalArgumentException("startLevel");
        }

        internalSetStartLevel(startLevel, listeners);
    }

    private class Command
        implements Runnable
    {
        private final FrameworkListener[] listeners;

        private final int startLevel;

        public Command(int startLevel, FrameworkListener... listeners)
        {
            this.startLevel = startLevel;
            this.listeners = listeners;
        }

        @Override
        public void run()
        {
            int startLevel = getStartLevel();
            if (startLevel < this.startLevel)
            {
                for (int intermediateStartLevel = startLevel + 1;
                    intermediateStartLevel <= this.startLevel;
                    intermediateStartLevel++)
                {
                    int oldStartLevel = getStartLevel();

                    framework.startLevelChanging(
                        oldStartLevel, intermediateStartLevel
                    );
                    synchronized (FrameworkStartLevelImpl.this)
                    {
                        FrameworkStartLevelImpl.this.startLevel =
                            intermediateStartLevel;
                    }
                    framework.startLevelChanged(
                        oldStartLevel, intermediateStartLevel,
                        listeners);
                }
            }
            else if (this.startLevel < startLevel)
            {
                for (int intermediateStartLevel = startLevel;
                    intermediateStartLevel > this.startLevel;
                    intermediateStartLevel--)
                {
                    int oldStartLevel = getStartLevel();
                    int newStartLevel = intermediateStartLevel - 1;

                    framework.startLevelChanging(
                        oldStartLevel, newStartLevel
                    );
                    synchronized (FrameworkStartLevelImpl.this)
                    {
                        FrameworkStartLevelImpl.this.startLevel = newStartLevel;
                    }
                    framework.startLevelChanged(
                        oldStartLevel, newStartLevel,
                        listeners);
                }
            }
            else
            {
                framework.startLevelChanging(startLevel, startLevel);
                framework.startLevelChanged(startLevel, startLevel, listeners);
            }
        }
    }
}
