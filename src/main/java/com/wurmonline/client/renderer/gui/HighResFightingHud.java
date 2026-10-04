package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.World;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.resources.textures.IconLoader;
import com.wurmonline.client.resources.textures.ResourceTexture;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.client.resources.textures.Texture;
import com.wurmonline.client.settings.WindowPosition;
import com.wurmonline.shared.constants.PlayerAction;
import org.highresfightinghud.client.AttackStanceModel;
import org.highresfightinghud.client.CombatExamineQueue;
import org.highresfightinghud.client.CombatKnowledge;
import org.highresfightinghud.client.CombatKnowledgeBook;
import org.highresfightinghud.client.CombatObservation;
import org.highresfightinghud.client.CombatObservationParser;
import org.highresfightinghud.client.CreatureProfile;
import org.highreshud.client.state.CreatureRelationState;
import org.highresfightinghud.client.EquipmentLoadout;
import org.highresfightinghud.client.FightingFonts;
import org.highresfightinghud.client.FightingHudLayout;
import org.highresfightinghud.client.FocusBarState;
import org.highresfightinghud.client.FocusMath;
import org.highresfightinghud.client.HighResFightingHudSettings;
import org.highresfightinghud.client.ShieldArc;
import org.highresfightinghud.client.portrait.FocusPortraitController;
import org.highreshud.core.ActionOrigin;
import org.highreshud.core.HighResHudApi;
import org.highreshud.client.HiddenExamineCoordinator;
import org.highreshud.client.HighResHudRuntime;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.highresfightinghud.client.FightingHudLayout.*;
import static org.highresfightinghud.client.FocusBarLayout.*;

/** One unified target card, learned PvE model, and native combat control HUD. */
public final class HighResFightingHud extends StaticComponent
        implements WindowSerializer {
    private static final long DOUBLE_CLICK_NANOS = 380_000_000L;
    private static final long NO_TARGET = Long.MIN_VALUE;
    private static final long SHINE_PERIOD = 4_800_000_000L;
    private static final float BACKING_RED = 9f / 255f;
    private static final float BACKING_GREEN = 3f / 255f;
    private static final float BACKING_BLUE = 0f;
    private static final float RAIL_RED = 109f / 255f;
    private static final float RAIL_GREEN = 95f / 255f;
    private static final float RAIL_BLUE = 83f / 255f;
    private static final int STANCE_ICON = 38;
    private static final int MODE_ICON = 28;
    private static final int AUX_ICON = 24;

    private final HeadsUpDisplay owner;
    private final TargetWindow targetWindow;
    /* Keep this typed as StaticComponent to avoid resolving the hooked class. */
    private final StaticComponent fightOptions;
    private static volatile Method getFightingMethod;
    private final FocusPortraitController portrait;
    private final FocusBarState progressState = new FocusBarState();
    private final DragController dragger;
    private final TextFont regular;
    private final TextFont bold;
    private final TextFont title;
    private final TextFont small;
    private final ResourceTexture frame;
    private final CombatKnowledgeBook knowledgeBook = new CombatKnowledgeBook(
            Paths.get(HighResFightingHudSettings.knowledgeDirectory));
    private final CombatExamineQueue examineQueue = new CombatExamineQueue();

    private final List<StaticComponent> modeButtons = new ArrayList<>();
    private final List<StaticComponent> attackButtons = new ArrayList<>();
    private final List<StaticComponent> auxiliaryButtons = new ArrayList<>();
    private boolean nativeChildrenResolved;
    private boolean nativeChildrenUnavailable;
    private PickableUnit target;
    private long targetId = NO_TARGET;
    private long lastTargetId = NO_TARGET;
    private long pendingCorpseId = -1L;
    private CreatureProfile targetProfile;
    private CreatureProfile lastTargetProfile;
    private float smoothedHealth = -1f;
    private boolean contentVisible;
    private boolean wasFighting;
    private long lastPortraitClick;
    private boolean draggingPortrait;
    private int portraitDragX;
    private int portraitDragY;
    private EquipmentLoadout loadout = EquipmentLoadout.EMPTY;
    private boolean loadoutInitialized;

    public HighResFightingHud(HeadsUpDisplay owner, TargetWindow targetWindow,
                              FocusPortraitController portrait) {
        super("highres-Fighting HUD", 404, 120, PANEL_WIDTH, PANEL_HEIGHT);
        this.owner = owner;
        this.targetWindow = targetWindow;
        this.fightOptions = nativeFightWindow(owner);
        this.portrait = portrait;
        this.dragger = new DragController(this);
        this.regular = FightingFonts.regular();
        this.bold = FightingFonts.bold();
        this.title = FightingFonts.title();
        this.small = FightingFonts.small();
        this.text = regular;
        this.textBold = bold;
        this.frame = texture("img.highresfightinghud.frame");
        activateKnowledgeBook();
        syncTarget();
        syncLayout();
    }

    public StaticComponent nativeFightWindow() {
        return fightOptions;
    }

    public boolean ownsNativeFightWindow(WurmComponent candidate) {
        return candidate != null && candidate == fightOptions;
    }

    @Override
    public void gameTick() {
        activateKnowledgeBook();
        syncTarget();
        boolean fighting = nativeFighting();
        syncEquipment(fighting && wasFighting);
        if (fighting && !wasFighting) queueOpeningExamines();
        wasFighting = fighting;
        examineQueue.tick(System.nanoTime(), new CombatExamineQueue.Sender() {
            @Override
            public boolean examine(long examineTargetId) {
                HiddenExamineCoordinator.Claim claim =
                        HighResHudRuntime.claimHiddenExamine(
                                "fightingHud", examineTargetId);
                if (claim == HiddenExamineCoordinator.Claim.WAIT) return false;
                if (claim == HiddenExamineCoordinator.Claim.SEND) {
                    HighResHudApi.runAs(ActionOrigin.INTERNAL,
                            () -> owner.sendAction(PlayerAction.EXAMINE,
                                    examineTargetId));
                }
                return true;
            }
        });
        if (HighResFightingHudSettings.collectCombatKnowledge) {
            knowledgeBook.tick(System.nanoTime());
        }
        if (fightOptions != null) fightOptions.gameTick();
        syncLayout();
    }

    public void combatFightingChanged(boolean fighting) {
        syncTarget();
        syncEquipment(false);
        if (fighting && !wasFighting) queueOpeningExamines();
        wasFighting = fighting;
        syncLayout();
    }

    /** Makes the registered HUD visible as soon as Wurm assigns a target. */
    public void targetChanged() {
        syncTarget();
        if (target != null && !owner.isComponentEnabled(this)) {
            owner.toggleComponent(this, true);
        }
        syncLayout();
    }

    public boolean serverText(String messageTitle, String message) {
        if (examineQueue.observe(messageTitle, message)) return true;
        if (!HighResFightingHudSettings.collectCombatKnowledge
                || targetProfile == null) return false;
        CombatObservation observation = CombatObservationParser.parse(
                messageTitle, message, targetName());
        if (observation == null) return false;
        String creatureKey = targetProfile.knowledgeKey();
        knowledgeBook.observe(creatureKey, observation,
                playerStance(), targetStance(), shieldEligible());
        knowledgeBook.observe(combatContextKey(), observation,
                playerStance(), targetStance(), shieldEligible());
        return false;
    }

    public void progressChanged(float progress, String label,
                                boolean changeColor) {
        PickableUnit current = rawTarget();
        if (current != null && nativeFighting()) {
            if (progressState.selectionId() != current.getId()) {
                progressState.selectionChanged(current.getId());
            }
            progressState.mirrorProgress(progress, label, changeColor);
        }
    }

    public boolean mouseWheeledAt(HeadsUpDisplay source, int mouseX,
                                  int mouseY, int wheelDelta) {
        return source == owner && contentVisible && portrait != null
                && wheelDelta != 0 && portraitAt(mouseX, mouseY)
                && portrait.zoom(wheelDelta);
    }

    private void activateKnowledgeBook() {
        if (!HighResFightingHudSettings.collectCombatKnowledge) return;
        World world = owner == null ? null : owner.getWorld();
        // HUD.init runs before login has attached ServerConnection to World.
        // World.getUsername() delegates to that connection and throws while
        // it is absent, preventing this component from reaching HUD Settings.
        if (world == null || world.getServerConnection() == null) return;
        try {
            String server = world.getServerName();
            String character = world.getUsername();
            if (server == null || server.trim().isEmpty()
                    || character == null || character.trim().isEmpty()) return;
            knowledgeBook.activate(server, character);
        } catch (RuntimeException ignored) {
            // Login is still assembling World; gameTick retries safely.
        }
    }

    private void syncTarget() {
        PickableUnit next = rawTarget();
        long nextId = next == null ? NO_TARGET : next.getId();
        if (nextId != targetId) {
            targetId = nextId;
            target = next;
            smoothedHealth = -1f;
            progressState.clearProgress();
            if (next == null) {
                progressState.clearActions();
                targetProfile = null;
                examineQueue.clearTarget();
            } else {
                progressState.selectionChanged(nextId);
                targetProfile = CreatureProfile.fromName(targetName());
                lastTargetProfile = targetProfile;
                lastTargetId = nextId;
                if (HighResFightingHudSettings.collectCombatKnowledge) {
                    knowledgeBook.recordEncounter(targetProfile.knowledgeKey());
                }
                examineQueue.schedule(CombatExamineQueue.Kind.TARGET,
                        nextId, targetName());
            }
        } else {
            target = next;
        }
        if (portrait != null) portrait.setSubject(next);
        contentVisible = next != null || nativeFighting();
    }

    private PickableUnit rawTarget() {
        CreatureCellRenderable creature = targetWindow == null
                ? null : targetWindow.creature;
        return creature == null || creature.isItem() ? null : creature;
    }

    private void syncEquipment(boolean scheduleChanges) {
        World world = owner == null ? null : owner.getWorld();
        InventoryMetaWindowView equipment = world == null
                || world.getInventoryManager() == null ? null
                : world.getInventoryManager().getPlayerEquipment();
        EquipmentLoadout next = EquipmentLoadout.inspect(equipment);
        if (loadoutInitialized && next.signature().equals(loadout.signature())) {
            return;
        }
        EquipmentLoadout previous = loadout;
        loadout = next;
        if (scheduleChanges && loadoutInitialized) {
            if (previous.weaponId != next.weaponId
                    || previous.weaponQuality != next.weaponQuality
                    || previous.weaponRarity != next.weaponRarity) {
                examineQueue.schedule(CombatExamineQueue.Kind.WEAPON,
                        next.weaponId, next.weaponName);
            }
            if (previous.shieldId != next.shieldId
                    || previous.shieldQuality != next.shieldQuality
                    || previous.shieldRarity != next.shieldRarity) {
                examineQueue.schedule(CombatExamineQueue.Kind.SHIELD,
                        next.shieldId, next.shieldName);
            }
        }
        loadoutInitialized = true;
    }

    private void queueOpeningExamines() {
        syncEquipment(false);
        examineQueue.schedule(CombatExamineQueue.Kind.WEAPON,
                loadout.weaponId, loadout.weaponName);
        examineQueue.schedule(CombatExamineQueue.Kind.SHIELD,
                loadout.shieldId, loadout.shieldName);
        if (target != null) {
            examineQueue.schedule(CombatExamineQueue.Kind.TARGET,
                    target.getId(), targetName());
        }
    }

    private void syncLayout() {
        if (!contentVisible) {
            super.setLocation(x, y, 0, 0);
            return;
        }
        int nextX = Math.max(0, Math.min(x,
                Math.max(0, SCREEN_WIDTH - PANEL_WIDTH)));
        int nextY = Math.max(0, Math.min(y,
                Math.max(0, SCREEN_HEIGHT - PANEL_HEIGHT)));
        super.setLocation(nextX, nextY, PANEL_WIDTH, PANEL_HEIGHT);
        if (fightOptions != null) {
            fightOptions.setPosition(nextX + COMBAT_X, nextY + COMBAT_Y);
        }
        if (resolveNativeChildren()) layoutNativeControls();
    }

    @Override
    public void setLocation(int nextX, int nextY, int ignoredWidth,
                            int ignoredHeight) {
        this.x = nextX;
        this.y = nextY;
        syncLayout();
    }

    @Override
    protected void renderComponent(Queue queue, float alpha) {
        syncTarget();
        syncLayout();
        if (!contentVisible) return;
        if (portrait != null) portrait.requestPreview();

        fillRect(queue, 0.055f, 0.034f, 0.018f, 0.99f,
                x, y, PANEL_WIDTH, PANEL_HEIGHT);
        rim(queue, x, y, PANEL_WIDTH, PANEL_HEIGHT, 2,
                RAIL_RED, RAIL_GREEN, RAIL_BLUE, 0.96f);
        fillRect(queue, 0.092f, 0.061f, 0.036f, 1f,
                x, y, TARGET_WIDTH, TARGET_HEIGHT);
        renderPortrait(queue);
        renderTargetFrame(queue);
        renderIdentity(queue);
        renderHealth(queue);
        renderProgress(queue);
        renderStatus(queue);
        renderAnalysis(queue);
        renderCombatSections(queue);
        renderFightOptions(queue, alpha);
    }

    private void renderPortrait(Queue queue) {
        int px = x + PORTRAIT_X;
        int py = y + PORTRAIT_Y;
        fillRect(queue, 0.058f, 0.039f, 0.024f, 1f,
                px, py, PORTRAIT_SIZE, PORTRAIT_SIZE);
        Texture live = portrait == null ? null : portrait.texture();
        if (live != null && portrait.hasLiveModel()) {
            float[] crop = portrait.crop();
            Renderer.texturedQuadAlphaBlend(queue, live,
                    1f, 1f, 1f, 1f, px, py, PORTRAIT_SIZE, PORTRAIT_SIZE,
                    crop[0], 1f - crop[1], crop[2], -crop[3]);
            return;
        }
        Texture fallback = portrait == null ? null : portrait.fallbackTexture();
        if (fallback == null && target != null) {
            fallback = target.getIconTexture();
            if (fallback == null && target.getIconId() >= 0) {
                fallback = IconLoader.getIcon(Short.valueOf(target.getIconId()));
            }
        }
        if (fallback != null) {
            Renderer.texturedQuadAlphaBlend(queue, fallback,
                    1f, 1f, 1f, 1f, px, py, PORTRAIT_SIZE, PORTRAIT_SIZE,
                    0f, 0f, 1f, 1f);
        } else {
            paintCentered(bold, queue, "?", px, py + 53, PORTRAIT_SIZE,
                    0.77f, 0.64f, 0.42f);
        }
    }

    private void renderTargetFrame(Queue queue) {
        if (frame == null) return;
        Renderer.texturedQuadAlphaBlend(queue, frame,
                1f, 1f, 1f, 1f, x, y, TARGET_WIDTH, TARGET_HEIGHT,
                0f, 0f, 1f, 1f);
    }

    private void renderIdentity(Queue queue) {
        String name = targetName();
        if (name.isEmpty()) name = "No combat target";
        paintCentered(title, queue, fit(title, name, INFO_WIDTH - 12),
                x + INFO_X + 6,
                centeredLowerEdge(y + HEADER_TEXT_Y,
                        HEADER_TEXT_HEIGHT, title.getHeight()),
                INFO_WIDTH - 12, 0.94f, 0.86f, 0.68f);
    }

    private void renderHealth(Queue queue) {
        CreatureCellRenderable creature = target instanceof CreatureCellRenderable
                ? (CreatureCellRenderable) target : null;
        if (creature == null) return;
        float raw = clamp(creature.getPercentHealth() / 100f);
        smoothedHealth = FocusMath.smoothHealth(smoothedHealth, raw);
        int gx = x + INFO_X + 4;
        int gy = y + HEALTH_GAUGE_Y;
        int gw = INFO_WIDTH - 8;
        renderGauge(queue, gx, gy, gw, GAUGE_HEIGHT,
                smoothedHealth, 0.055f, 0.61f, 0.18f);
        paintCentered(small, queue,
                "Health: " + Math.round(clamp(smoothedHealth) * 100f) + "%",
                gx, centeredLowerEdge(gy, GAUGE_HEIGHT, small.getHeight()),
                gw, 0.94f, 0.92f, 0.84f);
    }

    private void renderProgress(Queue queue) {
        FocusBarState.Progress progress = progressState.progress();
        if (progress.title.isEmpty()) return;
        int gx = x + INFO_X + 4;
        int gy = y + PROGRESS_GAUGE_Y;
        int gw = INFO_WIDTH - 8;
        float[] colour = progress.changeColor
                ? new float[]{0.58f, 0.23f, 0.10f}
                : new float[]{0.10f, 0.43f, 0.72f};
        renderGauge(queue, gx, gy, gw, GAUGE_HEIGHT,
                progress.value, colour[0], colour[1], colour[2]);
        paintCentered(small, queue, fit(small, progress.title, gw - 8),
                gx, centeredLowerEdge(gy, GAUGE_HEIGHT, small.getHeight()),
                gw, 0.94f, 0.92f, 0.84f);
    }

    private void renderStatus(Queue queue) {
        if (!(target instanceof CreatureCellRenderable)) return;
        CreatureCellRenderable creature = (CreatureCellRenderable) target;
        String descriptor = targetProfile == null ? "unknown"
                : targetProfile.descriptor();
        String label = descriptor + " · "
                + CreatureRelationState.label(creature);
        boolean hostile = CreatureRelationState.isHostile(creature);
        paintShadowed(small, queue, fit(small, label, INFO_WIDTH - 12),
                x + INFO_X + 6,
                centeredLowerEdge(y + STATUS_TEXT_Y,
                        STATUS_TEXT_HEIGHT, small.getHeight()),
                hostile ? 0.96f : 0.48f,
                hostile ? 0.42f : 0.78f,
                hostile ? 0.22f : 0.42f);
    }

    private void renderAnalysis(Queue queue) {
        int ax = x + ANALYSIS_X;
        int ay = y + ANALYSIS_Y;
        fillRect(queue, BACKING_RED, BACKING_GREEN, BACKING_BLUE, 0.98f,
                ax, ay, ANALYSIS_WIDTH, ANALYSIS_HEIGHT);
        rim(queue, ax, ay, ANALYSIS_WIDTH, ANALYSIS_HEIGHT, 1,
                RAIL_RED, RAIL_GREEN, RAIL_BLUE, 0.90f);
        if (targetProfile == null) {
            paintCentered(regular, queue, "TARGET ANALYSIS", ax, ay + 18,
                    ANALYSIS_WIDTH, 0.88f, 0.77f, 0.58f);
            paintCentered(small, queue, "Select a creature to build its model",
                    ax, ay + 62, ANALYSIS_WIDTH,
                    0.70f, 0.63f, 0.52f);
            return;
        }

        CombatKnowledge.Snapshot overall = knowledgeBook.snapshot(
                targetProfile.knowledgeKey(), playerStance());
        CombatKnowledge.Snapshot stats = knowledgeBook.snapshot(
                combatContextKey(), playerStance());
        paintShadowed(regular, queue,
                "KNOWLEDGE " + overall.studyPercent + "%  ·  KILLS "
                        + overall.kills + "  ·  SAMPLES "
                        + stats.outgoingAttempts + "/" + overall.outgoingAttempts,
                ax + 8, ay + 16, 0.91f, 0.80f, 0.60f);
        paintShadowed(small, queue,
                "Hit " + probability(stats.hitChance)
                        + "  Parry " + probability(stats.parryChance),
                ax + 8, ay + 34, 0.86f, 0.80f, 0.69f);
        paintShadowed(small, queue,
                "Glance " + probability(stats.glanceChance)
                        + "  Dmg index avg/max "
                        + decimal(stats.meanDamageIndex) + "/"
                        + decimal(stats.maximumDamageIndex),
                ax + 8, ay + 50, 0.86f, 0.80f, 0.69f);
        paintShadowed(bold, queue,
                "BEST " + AttackStanceModel.label(stats.bestStance)
                        + "  +" + Math.round(stats.projectedGainPercent)
                        + "%  (n=" + stats.bestStanceSamples + ")",
                ax + 8, ay + 68, 0.96f, 0.72f, 0.30f);
        paintShadowed(small, queue,
                "Enemy stance " + AttackStanceModel.label(targetStance())
                        + "  ·  incoming " + overall.incomingDamageTypes,
                ax + 8, ay + 85, 0.78f, 0.75f, 0.66f);
        String shield = loadout.hasShield()
                ? "Shield " + probability(stats.shieldBlockChance)
                : "Shield not equipped";
        paintShadowed(small, queue, shield + "  ·  " + equipmentLine(),
                ax + 8, ay + 102, 0.78f, 0.75f, 0.66f);
    }

    private void renderCombatSections(Queue queue) {
        section(queue, x + COMBAT_X, y + COMBAT_Y,
                COMBAT_SIZE, COMBAT_SIZE, "ATTACK ZONE");
        section(queue, x + MODE_X, y + MODE_Y,
                MODE_WIDTH, MODE_HEIGHT, "MODE");
        section(queue, x + AUX_X, y + AUX_Y,
                AUX_WIDTH, AUX_HEIGHT, "TACTICS");
        if (targetProfile == null) return;
        CombatKnowledge.Snapshot stats = knowledgeBook.snapshot(
                combatContextKey(), playerStance());
        int bestGrid = AttackStanceModel.gridIndex(stats.bestStance);
        int bx = x + FightingHudLayout.stanceCellX(bestGrid, STANCE_ICON);
        int by = y + FightingHudLayout.stanceCellY(bestGrid, STANCE_ICON);
        rim(queue, bx - 2, by - 2, STANCE_ICON + 4, STANCE_ICON + 4,
                2, 0.92f, 0.66f, 0.20f, 0.96f);
        int currentGrid = AttackStanceModel.gridIndex(playerStance());
        int cx = x + FightingHudLayout.stanceCellX(currentGrid, STANCE_ICON);
        int cy = y + FightingHudLayout.stanceCellY(currentGrid, STANCE_ICON);
        rim(queue, cx - 1, cy - 1, STANCE_ICON + 2, STANCE_ICON + 2,
                1, 0.34f, 0.74f, 0.95f, 0.94f);
    }

    private void section(Queue queue, int sx, int sy, int sw, int sh,
                         String label) {
        fillRect(queue, BACKING_RED, BACKING_GREEN, BACKING_BLUE, 0.98f,
                sx, sy, sw, sh);
        rim(queue, sx, sy, sw, sh, 1,
                RAIL_RED, RAIL_GREEN, RAIL_BLUE, 0.90f);
        paintCentered(small, queue, label, sx, sy + 14, sw,
                0.86f, 0.75f, 0.56f);
    }

    private void renderFightOptions(Queue queue, float alpha) {
        if (fightOptions == null) return;
        if (!resolveNativeChildren()) {
            fightOptions.render(queue, alpha);
            return;
        }
        layoutNativeControls();
        renderControls(queue, alpha, modeButtons);
        if (nativeFighting()) {
            renderControls(queue, alpha, attackButtons);
            renderControls(queue, alpha, auxiliaryButtons);
        }
    }

    private void renderControls(Queue queue, float alpha,
                                List<StaticComponent> components) {
        for (StaticComponent component : components) {
            if (component != null) component.render(queue, alpha);
        }
    }

    private boolean resolveNativeChildren() {
        if (nativeChildrenResolved) return true;
        if (nativeChildrenUnavailable || fightOptions == null) return false;
        try {
            modeButtons.add(componentField("fightAgressive"));
            modeButtons.add(componentField("fightNormal"));
            modeButtons.add(componentField("fightDefensive"));
            modeButtons.add(componentField("toggleRanged"));
            Object stances = fieldValue("combatStances");
            for (int i = 0; i < AttackStanceModel.count(); i++) {
                attackButtons.add(asComponent(Array.get(stances,
                        AttackStanceModel.idAt(i))));
            }
            auxiliaryButtons.add(componentField("focusB"));
            auxiliaryButtons.add(componentField("shieldBash"));
            auxiliaryButtons.add(componentField("distanceMeter"));
            auxiliaryButtons.add(componentField("balanceMeter"));
            Object specialMoves = fieldValue("specialMoves");
            for (int i = 0; i < Array.getLength(specialMoves); i++) {
                auxiliaryButtons.add(asComponent(Array.get(specialMoves, i)));
            }
            nativeChildrenResolved = complete(modeButtons)
                    && complete(attackButtons) && complete(auxiliaryButtons);
            return nativeChildrenResolved;
        } catch (Throwable ignored) {
            modeButtons.clear();
            attackButtons.clear();
            auxiliaryButtons.clear();
            nativeChildrenUnavailable = true;
            return false;
        }
    }

    private Object fieldValue(String name) throws Exception {
        Field field = fightOptions.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(fightOptions);
    }

    private StaticComponent componentField(String name) throws Exception {
        return asComponent(fieldValue(name));
    }

    private static StaticComponent asComponent(Object value) {
        return value instanceof StaticComponent ? (StaticComponent) value : null;
    }

    private static boolean complete(List<StaticComponent> components) {
        if (components.isEmpty()) return false;
        for (StaticComponent component : components) {
            if (component == null) return false;
        }
        return true;
    }

    private void layoutNativeControls() {
        for (int i = 0; i < attackButtons.size(); i++) {
            StaticComponent component = attackButtons.get(i);
            component.setSize(true);
            component.setLocation(
                    x + FightingHudLayout.stanceCellX(i, STANCE_ICON),
                    y + FightingHudLayout.stanceCellY(i, STANCE_ICON),
                    STANCE_ICON, STANCE_ICON);
        }
        int modeGap = 3;
        int modeContent = MODE_ICON * 4 + modeGap * 3;
        int modeTop = y + MODE_Y + 18
                + Math.max(0, (MODE_HEIGHT - 18 - modeContent) / 2);
        for (int i = 0; i < modeButtons.size(); i++) {
            StaticComponent component = modeButtons.get(i);
            component.setSize(true);
            component.setLocation(x + MODE_X + (MODE_WIDTH - MODE_ICON) / 2,
                    modeTop + i * (MODE_ICON + modeGap),
                    MODE_ICON, MODE_ICON);
        }
        int columns = 7;
        int gap = 3;
        int contentWidth = columns * AUX_ICON + (columns - 1) * gap;
        int left = x + AUX_X + Math.max(2,
                (AUX_WIDTH - contentWidth) / 2);
        for (int i = 0; i < auxiliaryButtons.size(); i++) {
            StaticComponent component = auxiliaryButtons.get(i);
            component.setSize(true);
            component.setLocation(left + (i % columns) * (AUX_ICON + gap),
                    y + AUX_Y + 15 + (i / columns) * (AUX_ICON + 2),
                    AUX_ICON, AUX_ICON);
        }
    }

    @Override
    public StaticComponent getComponentAt(int mouseX, int mouseY) {
        if (!contentVisible || !contains(mouseX, mouseY)) return null;
        if (resolveNativeChildren()) {
            StaticComponent nested = componentAt(modeButtons, mouseX, mouseY);
            if (nested != null) return nested;
            if (nativeFighting()) {
                nested = componentAt(attackButtons, mouseX, mouseY);
                if (nested != null) return nested;
                nested = componentAt(auxiliaryButtons, mouseX, mouseY);
                if (nested != null) return nested;
            }
        } else if (fightOptions != null
                && fightOptions.contains(mouseX, mouseY)) {
            StaticComponent nested = fightOptions.getComponentAt(mouseX, mouseY);
            if (nested != null && nested != fightOptions) return nested;
        }
        return this;
    }

    private static StaticComponent componentAt(List<StaticComponent> values,
                                               int mouseX, int mouseY) {
        for (StaticComponent component : values) {
            if (component != null && component.contains(mouseX, mouseY)) {
                StaticComponent nested = component.getComponentAt(mouseX, mouseY);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    @Override
    protected void leftPressed(int mouseX, int mouseY, int buttons) {
        if (portraitAt(mouseX, mouseY)) {
            long now = System.nanoTime();
            if (now - lastPortraitClick <= DOUBLE_CLICK_NANOS) {
                lastPortraitClick = 0L;
                draggingPortrait = false;
                owner.sendCombatAction(PlayerAction.NO_TARGET.getId());
                owner.setTargetCreature(-1L, null);
                if (portrait != null) portrait.setSubject(null);
                syncTarget();
                syncLayout();
                return;
            }
            lastPortraitClick = now;
            draggingPortrait = portrait != null;
            portraitDragX = mouseX;
            portraitDragY = mouseY;
            return;
        }
        dragger.leftPressed(mouseX, mouseY, buttons);
    }

    @Override
    protected void mouseDragged(int mouseX, int mouseY) {
        if (draggingPortrait && portrait != null) {
            portrait.rotate(mouseX - portraitDragX, mouseY - portraitDragY);
            portraitDragX = mouseX;
            portraitDragY = mouseY;
            return;
        }
        dragger.mouseDragged(mouseX, mouseY);
    }

    @Override
    protected void leftReleased(int mouseX, int mouseY) {
        draggingPortrait = false;
        dragger.leftReleased(mouseX, mouseY);
    }

    @Override
    protected void rightPressed(int mouseX, int mouseY, int buttons) {
        if (portraitAt(mouseX, mouseY) && target != null) {
            owner.popupRequested(mouseX, mouseY, targetName(), target.getId());
            return;
        }
        final WurmPopup popup = new WurmPopup(
                "highResFightingHudMenu", "highres-Fighting HUD",
                mouseX, mouseY);
        popup.addSeparator();
        dragger.addContextMenuEntry(popup);
        if (portrait != null) {
            popup.addButton(popup.new WPopupLiveButton("Resume portrait motion") {
                @Override
                protected void handleLeftClick() {
                    portrait.resumeMotion();
                }
            });
        }
        owner.showPopupComponent(popup);
    }

    @Override
    public void pick(PickData pickData, int mouseX, int mouseY) {
        if (!contentVisible || !contains(mouseX, mouseY)) return;
        if (portraitAt(mouseX, mouseY)) {
            pickData.addText("Target portrait: drag to rotate, wheel to zoom");
            pickData.addText("Double-click to clear target");
            pickData.addText("Right-click for the native target menu");
            return;
        }
        if (inside(mouseX, mouseY, x + ANALYSIS_X, y + ANALYSIS_Y,
                ANALYSIS_WIDTH, ANALYSIS_HEIGHT)) {
            pickData.addText("Statistics are learned per server, character,"
                    + " and creature type");
            pickData.addText("Ranges are 95% confidence intervals");
            pickData.addText("Damage index is inferred from combat text");
            return;
        }
        pickData.addText(target == null
                ? "highres-Fighting HUD" : targetName());
    }

    /** Records the kill and keeps Wurm's useful corpse replacement selection. */
    public void keepCorpseForKilledCreature(long killedCreatureId,
                                             long corpseId) {
        if (FocusMath.shouldSelectCorpse(killedCreatureId, NO_TARGET,
                targetId, NO_TARGET, lastTargetId)) {
            pendingCorpseId = corpseId;
        }
        if ((killedCreatureId == targetId || killedCreatureId == lastTargetId)
                && lastTargetProfile != null
                && HighResFightingHudSettings.collectCombatKnowledge) {
            knowledgeBook.recordKill(lastTargetProfile.knowledgeKey());
            knowledgeBook.flush();
        }
        if (killedCreatureId == targetId || killedCreatureId == lastTargetId) {
            owner.setTargetCreature(-1L, null);
            if (portrait != null) portrait.setSubject(null);
            syncTarget();
            syncLayout();
        }
    }

    public void renderableAdded(CellRenderable renderable) {
        if (renderable == null || renderable.getId() != pendingCorpseId
                || owner.getSelectBar() == null) return;
        pendingCorpseId = -1L;
        owner.getSelectBar().setSelected(renderable);
    }

    @Override
    public void restorePositionHints(WindowPosition position) {
        setPosition(position.x, position.y);
        dragger.setDisabled((position.flags & 1) != 0);
        owner.toggleComponent(this, (position.flags & 2) == 0);
    }

    @Override
    public WindowPosition createPositionHints() {
        int flags = dragger.isDisabled() ? 1 : 0;
        if (!owner.isComponentEnabled(this)) flags |= 2;
        return new WindowPosition(x, y, flags);
    }

    private int playerStance() {
        World world = owner == null ? null : owner.getWorld();
        return world == null || world.getPlayer() == null ? 0
                : world.getPlayer().getCurrentFightStance();
    }

    private int targetStance() {
        return target instanceof CreatureCellRenderable
                ? ((CreatureCellRenderable) target).getCurrentFightStance() : 0;
    }

    private boolean shieldEligible() {
        if (!loadout.hasShield() || !(target instanceof CreatureCellRenderable)) {
            return false;
        }
        World world = owner == null ? null : owner.getWorld();
        CreatureCellRenderable creature = (CreatureCellRenderable) target;
        return world != null && ShieldArc.eligible(true,
                creature.getLengthFromPlayer(), world.getPlayerRotX(),
                world.getPlayerPosX(), world.getPlayerPosY(),
                creature.getXPos(), creature.getYPos());
    }

    private String equipmentLine() {
        if (loadout.weaponId < 0L) return "weapon unknown";
        String rarity = loadout.weaponRarity <= 0 ? ""
                : " r" + loadout.weaponRarity;
        CombatExamineQueue.Snapshot examined = examineQueue.snapshot(
                CombatExamineQueue.Kind.WEAPON);
        String effects = examined == null
                || examined.targetId != loadout.weaponId
                || examined.effects.isEmpty() ? ""
                : " fx" + examined.effects.size();
        return limit(loadout.weaponName, 22) + rarity + " ql"
                + Math.round(loadout.weaponQuality) + effects;
    }

    private String combatContextKey() {
        return targetProfile == null ? "unknown|loadout:" + loadout.modelKey()
                : targetProfile.knowledgeKey() + "|loadout:" + loadout.modelKey();
    }

    private String targetName() {
        if (targetWindow != null && targetWindow.targetName != null
                && !targetWindow.targetName.trim().isEmpty()) {
            return targetWindow.targetName.trim();
        }
        if (target instanceof CreatureCellRenderable) {
            String name = ((CreatureCellRenderable) target)
                    .getCreatureData().getName();
            if (name != null && !name.trim().isEmpty()) return name.trim();
        }
        return target == null || target.getHoverName() == null
                ? "" : target.getHoverName().trim();
    }

    private boolean portraitAt(int mouseX, int mouseY) {
        return inside(mouseX, mouseY, x + PORTRAIT_X, y + PORTRAIT_Y,
                PORTRAIT_SIZE, PORTRAIT_SIZE);
    }

    private static String probability(CombatKnowledge.Probability value) {
        if (value.samples == 0) return "-- [--]";
        return Math.round(value.value * 100.0) + "% ["
                + Math.round(value.lower * 100.0) + '-'
                + Math.round(value.upper * 100.0) + "]";
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String limit(String value, int maximum) {
        String clean = clean(value);
        return clean.length() <= maximum ? clean
                : clean.substring(0, Math.max(0, maximum - 3)) + "...";
    }

    private void renderGauge(Queue queue, int gx, int gy, int gw, int gh,
                             float value, float red, float green, float blue) {
        int filled = Math.round(clamp(value) * gw);
        if (filled <= 0) return;
        fillRect(queue, red, green, blue, 1f, gx, gy, filled, gh);
        fillRect(queue, Math.min(1f, red + 0.22f),
                Math.min(1f, green + 0.20f),
                Math.min(1f, blue + 0.16f), 0.68f,
                gx, gy, filled, 1);
        fillRect(queue, red * 0.46f, green * 0.46f, blue * 0.46f, 0.86f,
                gx, gy + gh - 1, filled, 1);
        if (!HighResFightingHudSettings.animateGaugeShine || filled < 5) return;
        long offset = (gx * 31L + gy * 17L) * 1_000_000L;
        double angle = Math.floorMod(System.nanoTime() + offset, SHINE_PERIOD)
                / (double) SHINE_PERIOD * Math.PI * 2.0;
        float breathe = (float) ((Math.sin(angle) + 1.0) * 0.5);
        fillRect(queue, 1f, 0.98f, 0.90f,
                0.045f + breathe * 0.035f,
                gx + 1, gy + 1, Math.max(0, filled - 2),
                Math.max(2, gh / 3));
    }

    private void rim(Queue queue, int rx, int ry, int rw, int rh,
                     int thickness, float red, float green,
                     float blue, float alpha) {
        if (rw <= 0 || rh <= 0) return;
        fillRect(queue, red, green, blue, alpha, rx, ry, rw, thickness);
        fillRect(queue, red, green, blue, alpha,
                rx, ry + rh - thickness, rw, thickness);
        fillRect(queue, red, green, blue, alpha, rx, ry, thickness, rh);
        fillRect(queue, red, green, blue, alpha,
                rx + rw - thickness, ry, thickness, rh);
    }

    private static ResourceTexture texture(String mapping) {
        try {
            return ResourceTextureLoader.getNowrapLinearTexture(mapping);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static StaticComponent nativeFightWindow(HeadsUpDisplay owner) {
        if (owner == null) return null;
        try {
            Field field = owner.getClass().getDeclaredField("fightWindow");
            field.setAccessible(true);
            Object value = field.get(owner);
            return value instanceof StaticComponent
                    ? (StaticComponent) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean nativeFighting() {
        if (fightOptions == null) return false;
        try {
            Method method = getFightingMethod;
            if (method == null) {
                method = fightOptions.getClass().getMethod("getFighting");
                method.setAccessible(true);
                getFightingMethod = method;
            }
            return Boolean.TRUE.equals(method.invoke(fightOptions));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void paintShadowed(TextFont font, Queue queue, String value,
                                      int px, int baseline, float red,
                                      float green, float blue) {
        paint(font, queue, value, px + 1, baseline + 1,
                0.014f, 0.010f, 0.007f, 0.98f);
        paint(font, queue, value, px, baseline, red, green, blue, 1f);
    }

    private static void paintCentered(TextFont font, Queue queue, String value,
                                      int px, int baseline, int available,
                                      float red, float green, float blue) {
        int left = px + Math.max(0,
                (available - font.getWidth(value) - 1) / 2);
        paintShadowed(font, queue, value, left, baseline, red, green, blue);
    }

    private static void paint(TextFont font, Queue queue, String value,
                              int px, int baseline, float red, float green,
                              float blue, float alpha) {
        if (value == null || value.isEmpty()) return;
        font.moveTo(px, baseline);
        font.paint(queue, value, red, green, blue, alpha);
    }

    private static String fit(TextFont font, String value, int maximumWidth) {
        String clean = clean(value);
        if (font.getWidth(clean) <= maximumWidth) return clean;
        String suffix = "...";
        int end = clean.length();
        while (end > 0 && font.getWidth(clean.substring(0, end) + suffix)
                > maximumWidth) end--;
        return clean.substring(0, end) + suffix;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) return 0f;
        return Math.max(0f, Math.min(1f, value));
    }

    private static boolean inside(int mouseX, int mouseY,
                                  int left, int top, int width, int height) {
        return mouseX >= left && mouseX < left + width
                && mouseY >= top && mouseY < top + height;
    }
}
