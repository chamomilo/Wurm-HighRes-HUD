package org.highreshud.client;

import org.gotti.wurmunlimited.modsupport.packs.ModPacks;
import org.highreshealthbar.client.HighResHealthBarSettings;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Loads the combined texture pack exactly once for all three panels. */
public final class HighResHudResources {
    private static final Logger LOG = Logger.getLogger("HighResHud.Resources");
    private static boolean attempted;
    private static boolean loaded;

    private HighResHudResources() {
    }

    public static synchronized boolean ensurePackLoaded() {
        if (attempted) return loaded;
        attempted = true;
        File pack = resolve(HighResHealthBarSettings.resourcePack);
        if (!pack.isFile()) {
            LOG.severe("Unified High-res HUD resource pack is missing: " + pack);
            return false;
        }
        try {
            loaded = ModPacks.addPack(pack, ModPacks.Options.PREPEND,
                    ModPacks.Options.NORELOAD, ModPacks.Options.NOMAPS,
                    ModPacks.Options.NOARMOR);
            LOG.info((loaded ? "Loaded" : "Could not load")
                    + " unified High-res HUD resource pack: " + pack);
        } catch (RuntimeException error) {
            LOG.log(Level.SEVERE,
                    "Unable to load unified High-res HUD resource pack", error);
        }
        return loaded;
    }

    private static File resolve(String path) {
        File file = new File(path == null ? "" : path);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), path);
        }
        return file.getAbsoluteFile();
    }
}
