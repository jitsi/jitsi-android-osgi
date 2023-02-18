/*
 * Copyright @ 2023 - present 8x8, Inc.
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

import static java.lang.StackWalker.Option.*;

import java.io.*;
import java.lang.StackWalker.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;
import java.util.stream.*;
import lombok.*;
import org.apache.commons.io.*;
import org.apache.commons.lang3.*;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.*;
import org.osgi.framework.*;

public class BundleClassLoader
    extends URLClassLoader
{
    private static final Logger LOGGER = Logger.getLogger(BundleClassLoader.class.getName());

    private final ClassLoader parent;

    private final Map<URI, List<File>> nativeLibraries = new HashMap<>();

    private File nativeLibTempDir;

    public BundleClassLoader(ClassLoader parent)
    {
        // Do not set a parent classloader to break the parent-first paradigm.
        // We need our classloader first to be able to intercept the findLibrary
        // call even if a class would actually be available in the parent.
        super(Stream
            .of(System.getProperty("java.class.path").split(System.getProperty("path.separator")))
            .map(
                cp -> {
                    try
                    {
                        return new File(cp).toURI().toURL();
                    }
                    catch (MalformedURLException e)
                    {
                        throw new RuntimeException(e);
                    }
                }).toArray(URL[]::new), null);
        this.parent = parent;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name))
        {
            try
            {
                // Everything java.* is definitely not from our jars (or
                // build directories) on the classpath, so try the parent
                // first. If not found on the classpath, try delegating to
                // the parent anyway.
                return name.startsWith("java.")
                    ? parent.loadClass(name)
                    : super.loadClass(name);
            }
            catch (ClassNotFoundException cnf)
            {
                return parent.loadClass(name);
            }
        }
    }

    @Override
    protected String findLibrary(String libname)
    {
        var walker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);
        var callerOpt = walker.walk(
            frames -> frames
                .dropWhile(f -> f.getDeclaringClass().getPackageName().startsWith("java.")
                    || f.getDeclaringClass() == getClass())
                .findFirst()
                .map(StackFrame::getDeclaringClass));
        if (callerOpt.isEmpty())
        {
            return null;
        }

        var caller = callerOpt.get();
        URI callerUrl;
        try
        {
            callerUrl = caller.getProtectionDomain().getCodeSource().getLocation().toURI();
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }

        var libs = nativeLibraries.computeIfAbsent(callerUrl, key -> {
            var paths = loadNativeLibraries(caller);
            var found = false;
            for (var path : paths)
            {
                if (System.mapLibraryName(libname).equals(new File(path).getName()))
                {
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                return null;
            }

            if (nativeLibTempDir == null)
            {
                nativeLibTempDir = new File(SystemUtils.getJavaIoTmpDir(), "jitsi-native-" + UUID.randomUUID());
                if (!nativeLibTempDir.mkdirs())
                {
                    LOGGER.log(Level.SEVERE, "Could not create temp dir {0} for native libs",
                        nativeLibTempDir.getAbsolutePath());
                    nativeLibTempDir = null;
                    return null;
                }
            }

            // extract all defined dlls, they might depend on each other
            var libPaths = new ArrayList<File>(paths.size());
            for (var path : paths)
            {
                var libUrl = caller.getResource("/" + path);
                if (libUrl != null)
                {
                    try
                    {
                        var dest = new File(nativeLibTempDir, new File(path).getName());
                        IOUtils.copy(libUrl, dest);
                        libPaths.add(dest);
                    }
                    catch (IOException e)
                    {
                        LOGGER.log(Level.SEVERE, "Could not extract lib", e);
                    }
                }
            }

            return libPaths;
        });

        if (libs != null)
        {
            for (var lib : libs)
            {
                if (System.mapLibraryName(libname).equals(lib.getName()))
                {
                    return lib.getAbsolutePath();
                }
            }
        }

        return null;
    }

    private List<String> loadNativeLibraries(Class<?> caller)
    {
        URL mfResource = null;
        try
        {
            var classSource = caller.getProtectionDomain().getCodeSource().getLocation();
            var resources = caller.getClassLoader().getResources("META-INF/MANIFEST.MF").asIterator();
            while (resources.hasNext())
            {
                var res = resources.next();
                var resPath = res.toString();
                if (resPath.startsWith("jar:"))
                {
                    resPath = resPath.substring(4);
                }
                var resPos = resPath.indexOf('!');
                if (resPos > -1)
                {
                    resPath = resPath.substring(0, resPos);
                }
                if (resPath.equals(classSource.toString()))
                {
                    mfResource = res;
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        if (mfResource == null)
        {
            return Collections.emptyList();
        }

        try (var s = mfResource.openStream())
        {
            if (s == null)
            {
                return Collections.emptyList();
            }

            var mf = new Manifest(s);
            var nativeCodeNames = mf.getMainAttributes().getValue(Constants.BUNDLE_NATIVECODE);

            // split by comma -> contains multiple libs with filters
            var spaceIgnore = StringMatcherFactory.INSTANCE.charSetMatcher('\r', '\n', ' ');
            var filterSt = new StringTokenizer(nativeCodeNames, ',', '"');
            filterSt.setIgnoredMatcher(spaceIgnore);

            var nativeLibs = new ArrayList<NativeLibs>();
            for (var filter : filterSt.getTokenList())
            {
                var paths = new ArrayList<String>();
                String osName = "";
                String processor = "";

                // split by semicolon -> contains the libs and the filter
                var clauseSt = new StringTokenizer(filter, ';');
                clauseSt.setIgnoredMatcher(spaceIgnore);
                for (var clause : clauseSt.getTokenList())
                {
                    // library paths
                    if (clause.indexOf('=') == -1)
                    {
                        paths.add(clause.charAt(0) == '/'
                            ? clause.substring(1)
                            : clause);
                    }
                    else
                    {
                        // filter clauses
                        var filterClauseSt = new StringTokenizer(clause, '=', '"');
                        filterClauseSt.setIgnoredMatcher(spaceIgnore);
                        var filterClauseTokens = filterClauseSt.getTokenArray();
                        switch (filterClauseTokens[0])
                        {
                        case Constants.BUNDLE_NATIVECODE_OSNAME:
                            osName = filterClauseTokens[1].toLowerCase(Locale.ROOT);
                            break;
                        case Constants.BUNDLE_NATIVECODE_PROCESSOR:
                            processor = filterClauseTokens[1].toLowerCase(Locale.ROOT);
                            break;
                        }
                    }
                }

                nativeLibs.add(new NativeLibs(paths, osName, processor));
            }

            var paths = new ArrayList<String>();
            for (var nl : nativeLibs)
            {
                if (!nl.matchesThisOs())
                {
                    continue;
                }
                if (!nl.matchesThisProcessor())
                {
                    continue;
                }

                paths.addAll(nl.paths);
            }

            return paths;
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "Failed to extract manifest", e);
        }

        return Collections.emptyList();
    }

    @RequiredArgsConstructor
    private static class NativeLibs
    {
        private final List<String> paths;

        private final String osName;

        private final String processor;

        boolean matchesThisOs()
        {
            switch (osName)
            {
            case "linux":
                return SystemUtils.IS_OS_UNIX;
            case "win32":
                return SystemUtils.IS_OS_WINDOWS;
            case "macos":
            case "macosx":
                return SystemUtils.IS_OS_MAC;
            }

            return false;
        }

        boolean matchesThisProcessor()
        {
            switch (processor)
            {
            case "x86":
            case "pentium":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
                return SystemUtils.OS_ARCH.equals("x86");
            case "x86-64":
            case "amd64":
            case "em64t":
            case "x86_64":
                return SystemUtils.OS_ARCH.equals("amd64");
            case "aarch64":
            case "arm64":
                return SystemUtils.OS_ARCH.equals("aarch64");
            case "powerpc-64-le":
            case "ppc64le":
            case "ppc64el":
                return SystemUtils.OS_ARCH.equals("ppc64le")
                    || SystemUtils.OS_ARCH.equals("ppc64el")
                    || (SystemUtils.OS_ARCH.equals("ppc64")
                    && "little".equalsIgnoreCase(System.getProperty("sun.cpu.endian")));
            }
            return false;
        }
    }
}
