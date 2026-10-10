package org.highreshud.client;

import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.SelectBar;
import com.wurmonline.client.renderer.gui.StaticComponent;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.shared.constants.PlayerAction;
import com.wurmonline.shared.util.MulticolorLineSegment;
import org.highreshud.core.ActionEvent;
import org.highreshud.core.ActionOrigin;
import org.highreshud.core.HighResHudApi;
import org.highreshud.core.InputRouter;
import org.highreshud.core.LifecycleCoordinator;
import org.highreshud.core.MessageRouter;
import org.highreshud.client.portrait.SharedPortraitCoordinator;
import org.highreshud.client.state.CreatureRelationState;
import org.highresfightinghud.client.HighResFightingHudMod;
import org.highresfocusbar.client.HighResFocusBarMod;
import org.highreshealthbar.client.HighResHealthBarMod;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The only bytecode-facing handler. Every patched Wurm method enters here,
 * then fans out to isolated component adapters and the public core buses.
 */
public final class HighResHudRuntime {
    private static final Logger LOG = Logger.getLogger("HighResHud.Runtime");

    private HighResHudRuntime() {
    }

    public static void resourcesReady() {
        guarded("resources", new Task() {
            @Override public void run() { HighResHudResources.ensurePackLoaded(); }
        });
    }

    public static void hudReady(final HeadsUpDisplay hud) {
        LifecycleCoordinator lifecycle = HighResHudApi.core().lifecycle();
        LifecycleCoordinator.Stage stage = lifecycle.stage();
        if (stage == LifecycleCoordinator.Stage.DISCONNECTING
                || stage == LifecycleCoordinator.Stage.STOPPED) {
            lifecycle.reset();
            stage = lifecycle.stage();
        }
        if (stage == LifecycleCoordinator.Stage.CREATED) lifecycle.hudReady();
        guarded("healthbar.attach", new Task() {
            @Override public void run() { HighResHealthBarMod.hudReady(hud); }
        });
        guarded("selectbar.attach", new Task() {
            @Override public void run() { HighResFocusBarMod.hudReady(hud); }
        });
        guarded("fightingHud.attach", new Task() {
            @Override public void run() { HighResFightingHudMod.hudReady(hud); }
        });
        if (lifecycle.stage() == LifecycleCoordinator.Stage.HUD_READY
                && hud != null && hud.getWorld() != null) {
            lifecycle.sessionReady();
        }
    }

    public static void beginFrame(final HeadsUpDisplay hud) {
        SharedPortraitCoordinator.beginFrame();
        guarded("healthbar.frame.begin", new Task() {
            @Override public void run() { HighResHealthBarMod.beginPortraitFrame(hud); }
        });
        guarded("selectbar.frame.begin", new Task() {
            @Override public void run() { HighResFocusBarMod.beginPortraitFrame(hud); }
        });
        guarded("fightingHud.frame.begin", new Task() {
            @Override public void run() { HighResFightingHudMod.beginPortraitFrame(hud); }
        });
        SharedPortraitCoordinator.dispatch();
    }

    public static void endFrame(final HeadsUpDisplay hud) {
        guarded("healthbar.frame.end", new Task() {
            @Override public void run() { HighResHealthBarMod.endPortraitFrame(hud); }
        });
        guarded("selectbar.frame.end", new Task() {
            @Override public void run() { HighResFocusBarMod.endPortraitFrame(hud); }
        });
        guarded("fightingHud.frame.end", new Task() {
            @Override public void run() { HighResFightingHudMod.endPortraitFrame(hud); }
        });
        guarded("portraits.frame.end", new Task() {
            @Override public void run() { SharedPortraitCoordinator.finishFrame(); }
        });
    }

    public static void mouseMoved(final HeadsUpDisplay hud, final int x,
                                  final int y) {
        HighResHudApi.core().input().route(new InputRouter.Event(
                InputRouter.Kind.MOUSE_MOVE, x, y, 0));
        guarded("selectbar.mouseMoved", new Task() {
            @Override public void run() {
                HighResFocusBarMod.selectPointerMoved(hud, x, y);
            }
        });
    }

    public static boolean mouseWheeled(final HeadsUpDisplay hud, final int x,
                                       final int y, final int delta) {
        if (HighResHudApi.core().input().route(new InputRouter.Event(
                InputRouter.Kind.MOUSE_WHEEL, x, y, delta))) return true;
        return guardedBoolean("selectbar.mouseWheel", new Decision() {
            @Override public boolean get() {
                return HighResFocusBarMod.portraitMouseWheeled(hud, x, y, delta);
            }
        }) || guardedBoolean("fightingHud.mouseWheel", new Decision() {
            @Override public boolean get() {
                return HighResFightingHudMod.portraitMouseWheeled(hud, x, y, delta);
            }
        }) || guardedBoolean("healthbar.mouseWheel", new Decision() {
            @Override public boolean get() {
                return HighResHealthBarMod.portraitMouseWheeled(hud, x, y, delta);
            }
        });
    }

    public static void actionStarted(final String name, final float duration) {
        guarded("shared.actionStarted", new Task() {
            @Override public void run() { HighResFocusBarMod.actionStarted(name, duration); }
        });
    }

    public static void actionSent(long sourceId, final long[] targets,
                                  final PlayerAction action) {
        long[] safeTargets = targets == null ? new long[0] : targets.clone();
        short actionId = action == null ? (short) -1 : action.getId();
        ActionOrigin origin = HighResHudApi.core().actions().currentOrigin();
        if (actionId == PlayerAction.EXAMINE.getId()
                && origin != ActionOrigin.INTERNAL) {
            HiddenExamineCoordinator.visibleExamineStarted();
        }
        HighResHudApi.core().actions().publish(new ActionEvent(sourceId,
                safeTargets, actionId, String.valueOf(action), origin));
        final int copies = safeTargets.length == 0 ? 1 : safeTargets.length;
        guarded("selectbar.action", new Task() {
            @Override public void run() {
                HighResFocusBarMod.actionSent(action, copies);
                HighResFocusBarMod.actionTargetsSent(action, targets);
            }
        });
        guarded("healthbar.action", new Task() {
            @Override public void run() {
                HighResHealthBarMod.vehicleActionSent(targets, action);
            }
        });
    }

    public static void singleActionSent(long sourceId, final long target,
                                        final PlayerAction action) {
        actionSent(sourceId, new long[]{target}, action);
    }

    public static boolean serverText(String title, String message) {
        return routeServerText(title, message, false);
    }

    public static boolean serverMulticolorText(String title, List segments) {
        return routeServerText(title, plainText(segments), true);
    }

    private static boolean routeServerText(String title, String message,
                                           boolean multicolor) {
        boolean consume = HighResHudApi.core().messages().route(
                new MessageRouter.Message(title, message, multicolor));
        boolean hidden = guardedBoolean("selectbar.message", new TextDecision(
                title, message, false));
        hidden |= guardedBoolean("fightingHud.message", new TextDecision(
                title, message, true));
        if (hidden && HiddenExamineCoordinator.mayConsume(System.nanoTime())) {
            HiddenExamineCoordinator.responseObserved(System.nanoTime());
            consume = true;
        }
        return consume;
    }

    public static boolean listenerText(String message) {
        return guardedBoolean("healthbar.fatigue", new Decision() {
            @Override public boolean get() {
                return HighResHealthBarMod.consumeFatigueMessage(message);
            }
        });
    }

    public static boolean titleBml(String bml) {
        return guardedBoolean("healthbar.titleBml", new Decision() {
            @Override public boolean get() {
                return HighResHealthBarMod.captureTitleBml(bml);
            }
        });
    }

    public static boolean popupReceived(final HeadsUpDisplay hud,
                                        final byte responseId,
                                        final List actions) {
        return guardedBoolean("selectbar.popup", new Decision() {
            @Override public boolean get() {
                return HighResFocusBarMod.popupReceived(hud, responseId, actions);
            }
        });
    }

    public static void selectProgress(final SelectBar bar, final float progress,
                                      final String title,
                                      final boolean changeColor) {
        guarded("selectbar.progress", new Task() {
            @Override public void run() {
                HighResFocusBarMod.progressChanged(bar, progress, title, changeColor);
            }
        });
        guarded("fightingHud.progress", new Task() {
            @Override public void run() {
                HighResFightingHudMod.progressChanged(bar, progress, title, changeColor);
            }
        });
    }

    public static void selectActions(final SelectBar bar, final byte responseId,
                                     final List actions) {
        guarded("selectbar.actions", new Task() {
            @Override public void run() {
                HighResFocusBarMod.actionsChanged(bar, responseId, actions);
            }
        });
    }

    public static boolean suppressSelection(final SelectBar bar) {
        return guardedBoolean("fightingHud.selectionGuard", new Decision() {
            @Override public boolean get() {
                return HighResFightingHudMod.selectionSuppressed(bar);
            }
        });
    }

    public static void selectionChanged(final SelectBar bar) {
        guarded("selectbar.selection", new Task() {
            @Override public void run() { HighResFocusBarMod.selectionChanged(bar); }
        });
    }

    public static PickableUnit selectedWorldOutline() {
        try {
            return HighResFocusBarMod.selectedWorldOutline();
        } catch (Throwable error) {
            LOG.log(Level.WARNING, "selectbar outline failed", error);
            return null;
        }
    }

    public static void targetChanged(final HeadsUpDisplay hud) {
        guarded("fightingHud.target", new Task() {
            @Override public void run() { HighResFightingHudMod.targetChanged(hud); }
        });
        guarded("selectbar.combatTarget", new Task() {
            @Override public void run() {
                HighResFocusBarMod.combatTargetPresented(hud,
                        HighResFightingHudMod.presentedTargetId(hud));
            }
        });
    }

    public static boolean redirectFightWindow(final HeadsUpDisplay hud,
                                              final WurmComponent component) {
        return guardedBoolean("fightingHud.window", new Decision() {
            @Override public boolean get() {
                return HighResFightingHudMod.redirectNativeFightWindow(hud, component);
            }
        });
    }

    public static void combatChanged(final boolean fighting) {
        guarded("fightingHud.combat", new Task() {
            @Override public void run() {
                HighResFightingHudMod.combatFightingChanged(fighting);
            }
        });
    }

    public static void focusOptions(final StaticComponent source) {
        guarded("fightingHud.focusOptions", () -> HighResFightingHudMod.focusOptions(source));
    }
    public static void focusPosition(final StaticComponent source, final byte stance) {
        guarded("fightingHud.focusPosition", () -> HighResFightingHudMod.focusPosition(source, stance));
    }
    public static void focusLevel(final StaticComponent source, final byte level, final String message) {
        guarded("fightingHud.focusLevel", () -> HighResFightingHudMod.focusLevel(source, level, message));
    }

    public static void corpseCreated(final long killedId, final long corpseId) {
        guarded("fightingHud.corpse", new Task() {
            @Override public void run() {
                HighResFightingHudMod.creatureReplacedByCorpse(killedId, corpseId);
            }
        });
    }

    public static void renderableAdded(final CellRenderable renderable) {
        guarded("fightingHud.renderable", new Task() {
            @Override public void run() {
                HighResFightingHudMod.renderableAdded(renderable);
            }
        });
    }

    public static void attitudeChanged(final CreatureCellRenderable creature,
                                       final int attitude) {
        guarded("shared.attitude", new Task() {
            @Override public void run() {
                CreatureRelationState.observe(creature, attitude);
            }
        });
    }

    public static boolean inventoryWindow(final HeadsUpDisplay hud,
                                          final InventoryMetaWindowView view) {
        return guardedBoolean("healthbar.inventory", new Decision() {
            @Override public boolean get() {
                return HighResHealthBarMod.inventoryWindowReceived(hud, view);
            }
        });
    }

    public static boolean vehicleEvent(final String context,
                                       final String message) {
        boolean hidden = guardedBoolean("healthbar.vehicleEvent", new Decision() {
            @Override public boolean get() {
                return HighResHealthBarMod.vehicleEventReceived(context, message);
            }
        });
        if (hidden && HiddenExamineCoordinator.mayConsume(System.nanoTime())) {
            HiddenExamineCoordinator.responseObserved(System.nanoTime());
            return true;
        }
        return false;
    }

    public static HiddenExamineCoordinator.Claim claimHiddenExamine(
            String owner, long targetId) {
        return HiddenExamineCoordinator.claim(owner, targetId,
                System.nanoTime());
    }

    private static String plainText(List segments) {
        if (segments == null) return "";
        StringBuilder result = new StringBuilder();
        for (Object value : segments) {
            if (value instanceof MulticolorLineSegment) {
                result.append(((MulticolorLineSegment) value).getText());
            }
        }
        return result.toString();
    }

    private static void guarded(String name, Task task) {
        try {
            task.run();
        } catch (Throwable error) {
            LOG.log(Level.WARNING, name + " failed; vanilla flow continues", error);
        }
    }

    private static boolean guardedBoolean(String name, Decision decision) {
        try {
            return decision.get();
        } catch (Throwable error) {
            LOG.log(Level.WARNING, name + " failed; vanilla flow continues", error);
            return false;
        }
    }

    private interface Task { void run(); }
    private interface Decision { boolean get(); }

    private static final class TextDecision implements Decision {
        private final String title;
        private final String message;
        private final boolean fighting;

        private TextDecision(String title, String message, boolean fighting) {
            this.title = title;
            this.message = message;
            this.fighting = fighting;
        }

        @Override
        public boolean get() {
            return fighting
                    ? HighResFightingHudMod.serverText(title, message)
                    : HighResFocusBarMod.serverText(title, message);
        }
    }
}
