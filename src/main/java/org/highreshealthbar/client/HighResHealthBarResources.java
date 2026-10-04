package org.highreshealthbar.client;

import org.highreshud.client.HighResHudResources;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HighResHealthBarResources {
    private static final Logger LOG = Logger.getLogger(HighResHealthBarResources.class.getName());
    private static boolean attempted;
    private static boolean loaded;

    private HighResHealthBarResources() {
    }

    public static synchronized void ensurePackLoaded() {
        if (attempted) return;
        attempted = true;

        loaded = HighResHudResources.ensurePackLoaded();
    }

    static synchronized boolean isLoaded() {
        return loaded;
    }
}
