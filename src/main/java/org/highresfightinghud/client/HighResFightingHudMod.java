package org.highresfightinghud.client;

import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.HighResFightingHud;
import com.wurmonline.client.renderer.gui.HudSettings;
import com.wurmonline.client.renderer.gui.SelectBar;
import com.wurmonline.client.renderer.gui.TargetWindow;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.client.settings.SavePosManager;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.highresfightinghud.client.portrait.FocusPortraitController;
import org.highreshud.client.state.ActionProgressState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Fighting-HUD component adapter owned by the unified High-res HUD entry point. */
public final class HighResFightingHudMod {
    private static final Logger LOG = Logger.getLogger("HighResFightingHud");
    private static volatile Field targetWindowField;
    private static volatile HighResFightingHudMod instance;

    private final ActionProgressState actionProgress;
    private final CombatSelectionGuard selectionGuard =
            new CombatSelectionGuard();
    private volatile HeadsUpDisplay hud;
    private volatile HighResFightingHud panel;
    private volatile FocusPortraitController targetPortrait;

    public HighResFightingHudMod(ActionProgressState actionProgress) {
        if (actionProgress == null) throw new IllegalArgumentException("actionProgress");
        this.actionProgress = actionProgress;
        instance = this;
    }

    public void configure(Properties properties) {
        HighResFightingHudSettings.configure(properties);
    }

    public void init() {
        LOG.info("High-res Fighting HUD component initialized");
    }

    public static void hudReady(HeadsUpDisplay readyHud) {
        HighResFightingHudMod mod = instance;
        if (mod == null) return;
        try {
            mod.attach(readyHud);
        } catch (Throwable error) {
            LOG.log(Level.SEVERE,
                    "Unable to attach high-res Fighting HUD", error);
        }
    }

    private synchronized void attach(HeadsUpDisplay newHud) throws Exception {
        if (hud == newHud && panel != null) return;
        HighResFightingHudResources.ensurePackLoaded();
        hud = newHud;
        targetPortrait = createPortrait();

        Field targetField = targetWindowField;
        if (targetField == null) {
            targetField = field(HeadsUpDisplay.class, "targetWindow");
            targetWindowField = targetField;
        }
        TargetWindow targetWindow = targetField == null ? null
                : (TargetWindow) targetField.get(newHud);
        panel = new HighResFightingHud(newHud, targetWindow, targetPortrait);

        newHud.getComponents().remove(panel.nativeFightWindow());
        Method add = ReflectionUtil.getMethod(HeadsUpDisplay.class,
                "addComponent", new Class[]{WurmComponent.class});
        ReflectionUtil.callPrivateMethod(newHud, add, panel);

        Field settingsField = HeadsUpDisplay.class
                .getDeclaredField("hudSettings");
        settingsField.setAccessible(true);
        HudSettings settings = (HudSettings) settingsField.get(newHud);
        settings.registerComponent("highres-Fighting HUD", panel);
        settings.setAvailable(panel, true);
        settings.setEnabled(panel,
                HighResFightingHudSettings.enabledByDefault);
        if (panel.nativeFightWindow() != null) {
            settings.setAvailable(panel.nativeFightWindow(), false);
            settings.setEnabled(panel.nativeFightWindow(), false);
        }

        Field saveField = HeadsUpDisplay.class
                .getDeclaredField("savePosManager");
        saveField.setAccessible(true);
        ((SavePosManager) saveField.get(newHud)).registerAndRefresh(
                panel, "highresfightinghud");

        panel.targetChanged();

        if (HighResFightingHudSettings.enabledByDefault) {
            Method show = ReflectionUtil.getMethod(HeadsUpDisplay.class,
                    "showComponent", new Class[]{WurmComponent.class});
            ReflectionUtil.callPrivateMethod(newHud, show, panel);
        }
        LOG.info("Registered one HUD element: highres-Fighting HUD");
    }

    public static void progressChanged(SelectBar source, float progress,
                                       String title, boolean changeColor) {
        HighResFightingHudMod mod = instance;
        if (mod == null || mod.hud == null
                || mod.hud.getSelectBar() != source) return;
        HighResFightingHud current = mod.panel;
        if (current != null) {
            current.progressChanged(progress,
                    mod.actionProgress.enhance(title), changeColor);
        }
    }

    public static void targetChanged(HeadsUpDisplay source) {
        HighResFightingHudMod mod = instance;
        HighResFightingHud current = mod == null ? null : mod.panel;
        if (mod != null && mod.hud == source && current != null) {
            current.targetChanged();
        }
    }

    public static boolean serverText(String title, String message) {
        HighResFightingHudMod mod = instance;
        HighResFightingHud current = mod == null ? null : mod.panel;
        return current != null && current.serverText(title, message);
    }

    public static boolean selectionSuppressed(SelectBar source) {
        HighResFightingHudMod mod = instance;
        return mod != null && mod.hud != null
                && mod.hud.getSelectBar() == source
                && mod.selectionGuard.shouldSuppress(System.nanoTime());
    }

    public static void combatFightingChanged(boolean fighting) {
        HighResFightingHudMod mod = instance;
        if (mod == null) return;
        boolean entered = mod.selectionGuard.update(
                fighting, System.nanoTime());
        if (entered && mod.hud != null) mod.hud.setSelected(null);
        HighResFightingHud current = mod.panel;
        if (current != null) current.combatFightingChanged(fighting);
    }

    public static boolean redirectNativeFightWindow(HeadsUpDisplay source,
                                                     WurmComponent component) {
        HighResFightingHudMod mod = instance;
        HighResFightingHud current = mod == null ? null : mod.panel;
        return mod != null && mod.hud == source && current != null
                && current.ownsNativeFightWindow(component);
    }

    public static void creatureReplacedByCorpse(long killedCreatureId,
                                                 long corpseId) {
        HighResFightingHudMod mod = instance;
        HighResFightingHud current = mod == null ? null : mod.panel;
        if (current != null) {
            current.keepCorpseForKilledCreature(killedCreatureId, corpseId);
        }
    }

    public static void renderableAdded(CellRenderable renderable) {
        HighResFightingHudMod mod = instance;
        HighResFightingHud current = mod == null ? null : mod.panel;
        if (current != null) current.renderableAdded(renderable);
    }

    public static void beginPortraitFrame(HeadsUpDisplay currentHud) {
        HighResFightingHudMod mod = instance;
        if (mod != null && mod.hud == currentHud
                && mod.targetPortrait != null) {
            mod.targetPortrait.beginRender();
        }
    }

    public static void endPortraitFrame(HeadsUpDisplay currentHud) {
        HighResFightingHudMod mod = instance;
        if (mod != null && mod.hud == currentHud
                && mod.targetPortrait != null) {
            mod.targetPortrait.endRender();
        }
    }

    public static boolean portraitMouseWheeled(HeadsUpDisplay currentHud,
                                               int mouseX, int mouseY,
                                               int wheelDelta) {
        HighResFightingHudMod mod = instance;
        HighResFightingHud current = mod == null ? null : mod.panel;
        return mod != null && mod.hud == currentHud && current != null
                && current.mouseWheeledAt(
                currentHud, mouseX, mouseY, wheelDelta);
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field result = type.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException | SecurityException error) {
            LOG.log(Level.SEVERE, "Wurm client field contract changed: "
                    + type.getName() + '.' + name, error);
            return null;
        }
    }

    private static FocusPortraitController createPortrait() {
        try {
            return new FocusPortraitController(
                    HighResFightingHudSettings.portraitRenderSize);
        } catch (Throwable error) {
            LOG.log(Level.SEVERE,
                    "Target portrait disabled; static fallback remains", error);
            return null;
        }
    }
}
