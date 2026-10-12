# Building High-res HUD

Use JDK 8 and the bundled Gradle wrapper. Obtain these libraries from your
Wurm Unlimited client and Ago's Client Mod Launcher:

- `client-patched.jar`
- `common.jar`
- `javassist.jar`
- `modlauncher.jar`

Copy `local.properties.example` to `local.properties` and set
`wurmClientLibDir` to the directory containing those libraries. The build only
reads them; it does not install or modify the client.

The shared updater reference is required. By default it is read from
`C:/projects/updater`; override with `-PchamomiloUpdaterReference=/path/to/updater`
or `CHAMOMILO_UPDATER_REFERENCE`. The reference pins the common UI kit and is
clean-built and checked before the consumer is packaged. Its integration checks
the updater version, exact class/resource bytes, bootstrap and ZIP contents.

```text
gradlew clean check dist -PwurmJavaFxJar=/path/to/jfxrt.jar
```

The JavaFX JAR is needed by the native UI verification fixtures. Use the Java 8
runtime supplied with the client. Tests exercise native input, model changes,
localization and layout with GPU drawing substituted for offline rendering.

The installable archive is written to `build/distributions`. It contains the
mod descriptor, two runtime JARs, bundled portrait profiles and `README.txt`.
Client libraries, tests, previews and build files are excluded.
