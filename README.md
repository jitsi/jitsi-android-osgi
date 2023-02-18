# Jitsi Android OSGi

This is a not-implementation of an OSGi framework. The name originally comes
from the now abandoned jitsi-android project. jitsi-android-osgi is used
by [Jigasi](https://github.com/jitsi/jigasi).

Jitsi never really cared about the modularization features of OSGi (i.e. bundle
isolation), but rather the service manager. Jigasi uses non OSGi-aware libraries
and this not-really-OSGi framework does not care about manifests. "Bundles" must
be manually listed and the "framework" must be told to start them.

## Example Usage
```java
// list all activators here
var activators = List.of(MyBundle.class);

// then prepare the "framework"
var options = Map.ofConstants.FRAMEWORK_BEGINNING_STARTLEVEL, "2");
Framework fw = new FrameworkImpl(options, Main.class.getClassLoader());
fw.init();
var bundleContext = fw.getBundleContext();
for (Class<? extends BundleActivator> activator : activators) {
    var url = activator.getProtectionDomain().getCodeSource().getLocation().toString();
    var bundle = bundleContext.installBundle(url);
    var startLevel = bundle.adapt(BundleStartLevel.class);
    startLevel.setStartLevel(2);
    var bundleActivator = bundle.adapt(BundleActivatorHolder.class);
    bundleActivator.addBundleActivator(activator);
}

// start and wait until the shutdown
fw.start();
fw.waitForStop(0);
```

## Native Libraries from Bundles

To use bundles that declare native libraries in their manifest and are loading
them via `System.loadLibrary(String)`, use the `BundleClassLoader` to run the
framework init code. They should then work like in a full OSGi implementation.
