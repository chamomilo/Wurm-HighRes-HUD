package org.highresfocusbar.client;

import org.highreshud.client.HighResHudResources;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HighResFocusBarResources {
    private static final Logger LOG = Logger.getLogger("HighResSelectBar");
    private static boolean attempted;
    private static boolean loaded;

    private HighResFocusBarResources() {
    }

    public static synchronized void ensurePackLoaded() {
        if (attempted) return;
        attempted = true;
        loaded = HighResHudResources.ensurePackLoaded();
    }

    public static File resolve(String path) {
        File file = new File(path == null ? "" : path);
        if (!file.isAbsolute()) file = new File(System.getProperty("user.dir"), path);
        return file.getAbsoluteFile();
    }

    static synchronized boolean isLoaded() {
        return loaded;
    }
}
