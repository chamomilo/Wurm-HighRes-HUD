package org.highresfocusbar.client;

import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.HighResFocusBar;
import com.wurmonline.client.renderer.gui.HudSettings;
import com.wurmonline.client.renderer.gui.SelectBar;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.client.settings.SavePosManager;
import com.wurmonline.shared.constants.PlayerAction;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.highresfocusbar.client.portrait.FocusPortraitController;
import org.highreshud.client.state.ActionProgressState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Select-bar component adapter owned by the unified High-res HUD entry point. */
public final class HighResFocusBarMod {
    private static final Logger LOG = Logger.getLogger("HighResSelectBar");
    private static volatile Field selectRequestId;
    private static volatile Field selectedUnitField;
    private static volatile Field popupRequestIdField;
    private static volatile HighResFocusBarMod instance;

    private final FocusBarState state = new FocusBarState();
    private final ActionProgressState actionProgress;
    private volatile HeadsUpDisplay hud;
    private volatile HighResFocusBar panel;
    private volatile FocusPortraitController selectedPortrait;

    public HighResFocusBarMod(ActionProgressState actionProgress) {
        if (actionProgress == null) throw new IllegalArgumentException("actionProgress");
        this.actionProgress = actionProgress;
        instance = this;
    }

    public void configure(Properties properties) {
        HighResFocusBarSettings.configure(properties);
    }

    public void init() {
        boolean waypointer = WaypointerBridge.detectAtStartup();
        LOG.info("High res select bar component initialized; Waypointer "
                + (waypointer ? "detected" : "not detected"));
    }

    public static void hudReady(HeadsUpDisplay readyHud) {
        HighResFocusBarMod mod = instance;
        if (mod == null) return;
        try {
            mod.attach(readyHud);
        } catch (Throwable error) {
            LOG.log(Level.SEVERE,
                    "Unable to attach high res select bar", error);
        }
    }

    private synchronized void attach(HeadsUpDisplay newHud) throws Exception {
        if (hud == newHud && panel != null) return;
        HighResFocusBarResources.ensurePackLoaded();
        hud = newHud;
        state.clearActions();
        selectedPortrait = createPortrait(newHud.getWorld());
        panel = new HighResFocusBar(newHud, newHud.getSelectBar(), state,
                selectedPortrait);

        Method add = ReflectionUtil.getMethod(HeadsUpDisplay.class,
                "addComponent", new Class[]{WurmComponent.class});
        ReflectionUtil.callPrivateMethod(newHud, add, panel);

        Field settingsField = HeadsUpDisplay.class
                .getDeclaredField("hudSettings");
        settingsField.setAccessible(true);
        HudSettings settings = (HudSettings) settingsField.get(newHud);
        settings.registerComponent("high res select bar", panel);
        settings.setAvailable(panel, true);
        settings.setEnabled(panel, HighResFocusBarSettings.enabledByDefault);

        Field saveField = HeadsUpDisplay.class
                .getDeclaredField("savePosManager");
        saveField.setAccessible(true);
        ((SavePosManager) saveField.get(newHud)).registerAndRefresh(
                panel, "highresselectbar");

        if (HighResFocusBarSettings.enabledByDefault) {
            Method show = ReflectionUtil.getMethod(HeadsUpDisplay.class,
                    "showComponent", new Class[]{WurmComponent.class});
            ReflectionUtil.callPrivateMethod(newHud, show, panel);
        }
        LOG.info("Registered one HUD element: high res select bar");
    }

    public static void progressChanged(SelectBar source, float progress,
                                       String title, boolean changeColor) {
        HighResFocusBarMod mod = instance;
        if (mod == null || mod.hud == null
                || mod.hud.getSelectBar() != source) return;
        String enhanced = mod.actionProgress.enhance(title);
        HighResFocusBar current = mod.panel;
        if (current != null) {
            current.progressChanged(progress, enhanced, changeColor);
        } else {
            mod.state.mirrorProgress(progress, enhanced, changeColor);
        }
    }

    public static void actionSent(PlayerAction action, int copies) {
        HighResFocusBarMod mod = instance;
        if (mod == null) return;
        if (action != null && action.getId() == PlayerAction.BUTCHER.getId()) {
            HighResFocusBar current = mod.panel;
            if (current != null) current.keepSelectedForReplacement();
        }
        mod.actionProgress.actionSent(action, copies);
    }

    public static void actionTargetsSent(PlayerAction action,
                                         long[] targetIds) {
        if (targetIds == null) return;
        for (long targetId : targetIds) actionTargetSent(action, targetId);
    }

    public static void actionTargetSent(PlayerAction action, long targetId) {
        HighResFocusBarMod mod = instance;
        HighResFocusBar current = mod == null ? null : mod.panel;
        if (current != null) current.actionTargeted(action, targetId);
    }

    public static boolean serverText(String title, String message) {
        HighResFocusBarMod mod = instance;
        HighResFocusBar current = mod == null ? null : mod.panel;
        return current != null && current.serverText(title, message);
    }

    public static PickableUnit selectedWorldOutline() {
        HighResFocusBarMod mod = instance;
        if (mod == null || mod.hud == null) return null;
        try {
            SelectBar source = mod.hud.getSelectBar();
            Field unitField = selectedUnitField;
            if (unitField == null) {
                unitField = field(SelectBar.class, "selectedUnit");
                selectedUnitField = unitField;
            }
            PickableUnit selected = unitField == null || source == null
                    ? null : (PickableUnit) unitField.get(source);
            return SelectedOutlinePickable.wrap(selected);
        } catch (Throwable error) {
            return null;
        }
    }

    public static void actionStarted(String genericName,
                                     float durationSeconds) {
        HighResFocusBarMod mod = instance;
        if (mod != null) {
            mod.actionProgress.actionStarted(genericName, durationSeconds);
        }
    }

    public static boolean popupReceived(HeadsUpDisplay source,
                                        byte responseId, List actions) {
        HighResFocusBarMod mod = instance;
        HighResFocusBar current = mod == null ? null : mod.panel;
        return mod != null && mod.hud == source && current != null
                && current.inspectProbeReceived(responseId, actions);
    }

    public static int popupRequestId(HeadsUpDisplay source) {
        if (source == null) return -1;
        try {
            Field requestField = popupRequestIdField;
            if (requestField == null) {
                requestField = field(HeadsUpDisplay.class, "popupRequestId");
                popupRequestIdField = requestField;
            }
            return requestField == null ? -1 : FocusMath.unsignedRequestId(
                    requestField.getByte(source));
        } catch (Throwable error) {
            return -1;
        }
    }

    @SuppressWarnings("unchecked")
    public static void actionsChanged(SelectBar source, byte responseId,
                                      List actions) {
        HighResFocusBarMod mod = instance;
        if (mod == null || mod.hud == null
                || mod.hud.getSelectBar() != source) return;
        try {
            Field requestField = selectRequestId;
            if (requestField == null) {
                requestField = field(SelectBar.class, "requestId");
                selectRequestId = requestField;
            }
            if (requestField == null) return;
            mod.state.mirrorActions(responseId, requestField.getByte(source),
                    (List<PlayerAction>) actions);
        } catch (Throwable error) {
            LOG.log(Level.WARNING,
                    "Unable to mirror Select Bar actions", error);
        }
    }

    public static void selectionChanged(SelectBar source) {
        HighResFocusBarMod mod = instance;
        if (mod == null || mod.hud == null
                || mod.hud.getSelectBar() != source) return;
        try {
            Field unitField = selectedUnitField;
            if (unitField == null) {
                unitField = field(SelectBar.class, "selectedUnit");
                selectedUnitField = unitField;
            }
            PickableUnit selected = unitField == null ? null
                    : (PickableUnit) unitField.get(source);
            mod.state.selectionChanged(selected == null
                    ? FocusBarState.NO_SELECTION : selected.getId());
        } catch (Throwable error) {
            LOG.log(Level.WARNING,
                    "Unable to mirror Select Bar selection", error);
        }
    }

    public static void beginPortraitFrame(HeadsUpDisplay currentHud) {
        HighResFocusBarMod mod = instance;
        if (mod != null && mod.hud == currentHud) {
            if (mod.panel != null) mod.panel.serviceInspectProbe();
            if (mod.selectedPortrait != null) {
                mod.selectedPortrait.beginRender();
            }
        }
    }

    public static void endPortraitFrame(HeadsUpDisplay currentHud) {
        HighResFocusBarMod mod = instance;
        if (mod != null && mod.hud == currentHud
                && mod.selectedPortrait != null) {
            mod.selectedPortrait.endRender();
        }
    }

    public static boolean portraitMouseWheeled(HeadsUpDisplay currentHud,
                                               int mouseX, int mouseY,
                                               int wheelDelta) {
        HighResFocusBarMod mod = instance;
        HighResFocusBar current = mod == null ? null : mod.panel;
        return mod != null && mod.hud == currentHud && current != null
                && current.mouseWheeledAt(
                currentHud, mouseX, mouseY, wheelDelta);
    }

    /** Event-driven exit repair for Wurm's null-HUD-component mouse path. */
    public static void selectPointerMoved(HeadsUpDisplay currentHud,
                                          int mouseX, int mouseY) {
        HighResFocusBarMod mod = instance;
        HighResFocusBar current = mod == null ? null : mod.panel;
        if (mod != null && mod.hud == currentHud && current != null) {
            current.hudPointerMoved(currentHud, mouseX, mouseY);
        }
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

    private static FocusPortraitController createPortrait(World world) {
        try {
            return new FocusPortraitController(
                    HighResFocusBarSettings.portraitRenderSize, world);
        } catch (Throwable error) {
            LOG.log(Level.SEVERE,
                    "Selected portrait disabled; static fallback remains", error);
            return null;
        }
    }
}
