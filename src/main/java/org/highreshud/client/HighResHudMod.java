package org.highreshud.client;

import org.chamomilo.wurm.update.SharedUpdateCoordinator;
import org.chamomilo.wurm.update.SharedUpdateHooks;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;
import org.gotti.wurmunlimited.modloader.interfaces.ModListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;
import org.highresfightinghud.client.HighResFightingHudMod;
import org.highresfocusbar.client.HighResFocusBarMod;
import org.highreshealthbar.client.HighResHealthBarMod;
import org.highreshud.client.state.ActionProgressState;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Single mod-loader entry point for all High-res HUD components. */
public final class HighResHudMod implements WurmClientMod, Configurable,
        PreInitable, Initable, ModListener {
    public static final String VERSION = "0.2.3";
    private static final String DEFAULT_PACK =
            "mods/highres-hud/highres-hud-resources-" + VERSION + ".jar";
    private static final String DEFAULT_PROFILES =
            "mods/highres-hud/portrait-profiles.properties";
    private static final Logger LOG = Logger.getLogger("HighResHud");
    private final ActionProgressState actionProgress = new ActionProgressState();
    private final HighResHealthBarMod healthbar = new HighResHealthBarMod();
    private final HighResFocusBarMod selectbar =
            new HighResFocusBarMod(actionProgress);
    private final HighResFightingHudMod fightingHud =
            new HighResFightingHudMod(actionProgress);

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void configure(Properties properties) {
        Properties source = properties == null ? new Properties() : properties;
        healthbar.configure(scoped(source, "healthbar.", false));
        selectbar.configure(scoped(source, "selectbar.", true));
        fightingHud.configure(scoped(source, "fightingHud.", true));
    }

    @Override
    public void preInit() {
        SharedUpdateHooks.install();
        UnifiedHookInstaller.install();
    }

    @Override
    public void init() {
        SharedUpdateHooks.registerHost("highres-hud");
        healthbar.init();
        selectbar.init();
        fightingHud.init();
        LOG.info("Unified High-res HUD " + VERSION
                + " initialized with core API v1");
    }

    @Override
    public void modInitialized(ModEntry<?> entry) {
        try {
            SharedUpdateCoordinator.modInitialized(entry);
        } catch (Throwable failure) {
            LOG.log(Level.WARNING,
                    "Unable to collect mod update metadata", failure);
        }
    }

    private static Properties scoped(Properties source, String prefix,
                                     boolean profiles) {
        Properties result = new Properties();
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (key.startsWith(prefix)) {
                result.setProperty(key.substring(prefix.length()),
                        String.valueOf(entry.getValue()));
            }
        }
        result.setProperty("resourcePack",
                source.getProperty("resourcePack", DEFAULT_PACK));
        if (profiles) {
            result.setProperty("portraitProfilesOverride",
                    source.getProperty("portraitProfilesOverride",
                            DEFAULT_PROFILES));
        }
        return result;
    }
}
