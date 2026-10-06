package org.highreshud.client;

import com.wurmonline.client.renderer.gui.ChamomiloUpdateWindow;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.WurmComponent;
import org.chamomilo.wurm.update.ModUpdate;
import org.chamomilo.wurm.update.SharedUpdateCoordinator;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
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

import java.awt.Desktop;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Single mod-loader entry point for all High-res HUD components. */
public final class HighResHudMod implements WurmClientMod, Configurable,
        PreInitable, Initable, ModListener {
    public static final String VERSION = "0.1.2";
    private static final String DEFAULT_PACK =
            "mods/highres-hud/highres-hud-resources-" + VERSION + ".jar";
    private static final String DEFAULT_PROFILES =
            "mods/highres-hud/portrait-profiles.properties";
    private static final Logger LOG = Logger.getLogger("HighResHud");
    private static final SharedUpdateCoordinator.Host UPDATE_HOST =
            new SharedUpdateCoordinator.Host() {
                @Override
                public void updatesReady(List<ModUpdate> updates) {
                    availableUpdates = updates == null
                            ? Collections.<ModUpdate>emptyList() : updates;
                }

                @Override
                public void checkFailed(String repository, Throwable failure) {
                    LOG.log(Level.FINE, "GitHub update check failed for "
                            + repository + "; continuing without notification",
                            failure);
                }
            };

    private static volatile HeadsUpDisplay updateHud;
    private static volatile List<ModUpdate> availableUpdates;
    private static volatile ChamomiloUpdateWindow updateWindow;
    private static volatile boolean updateNotificationShown;

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
        UnifiedHookInstaller.install();
    }

    @Override
    public void init() {
        SharedUpdateCoordinator.registerHost("highres-hud", UPDATE_HOST);
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

    static void updaterHudReady(HeadsUpDisplay hud) {
        if (hud == null) return;
        updateHud = hud;
        SharedUpdateCoordinator.startOnce();
    }

    static void updaterFrame(HeadsUpDisplay hud) {
        if (hud == null || hud != updateHud) return;
        showAvailableUpdates();
    }

    private static synchronized void showAvailableUpdates() {
        List<ModUpdate> updates = availableUpdates;
        HeadsUpDisplay hud = updateHud;
        if (updates == null || updates.isEmpty() || hud == null
                || updateNotificationShown) {
            return;
        }
        for (ModUpdate update : updates) {
            LOG.warning(update.getNotificationText());
        }
        try {
            ChamomiloUpdateWindow window = new ChamomiloUpdateWindow(
                    hud, updates,
                    "Chamomilo mod updates",
                    "New versions are available for installed Chamomilo mods.",
                    "Download",
                    "Open this release in your browser: {0}",
                    "Later",
                    "Close this notification until the next game start.",
                    HighResHudMod::openDownload,
                    HighResHudMod::dismissUpdateWindow);
            addComponent(hud, window);
            updateWindow = window;
            updateNotificationShown = true;
        } catch (Throwable failure) {
            LOG.log(Level.WARNING,
                    "Unable to show the aggregate update notification", failure);
        }
    }

    private static void openDownload(ModUpdate update) {
        if (update == null) return;
        String url = update.getDownloadUrl();
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("Desktop browsing is unavailable");
            }
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable failure) {
            LOG.log(Level.WARNING, "Unable to open release URL " + url, failure);
        }
    }

    private static synchronized void dismissUpdateWindow() {
        ChamomiloUpdateWindow window = updateWindow;
        updateWindow = null;
        HeadsUpDisplay hud = updateHud;
        if (hud == null || window == null) return;
        try {
            Method hide = ReflectionUtil.getMethod(HeadsUpDisplay.class,
                    "hideComponent", new Class[]{WurmComponent.class});
            ReflectionUtil.callPrivateMethod(hud, hide, window);
        } catch (Throwable failure) {
            LOG.log(Level.FINE, "Unable to hide update notification", failure);
        }
    }

    private static void addComponent(HeadsUpDisplay hud,
                                     WurmComponent component)
            throws ReflectiveOperationException {
        Method add = ReflectionUtil.getMethod(HeadsUpDisplay.class,
                "addComponent", new Class[]{WurmComponent.class});
        ReflectionUtil.callPrivateMethod(hud, add, component);
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
