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
import org.highresfightinghud.client.CombatFocusState;
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
import org.highreshud.client.ui.HudSkin;
import org.highreshud.client.ui.HudCaptionGroup;
import org.chamomilo.wurm.ui.v1.UiRect;
import org.chamomilo.wurm.ui.v1.UiColor;
import org.chamomilo.wurm.ui.v1.UiPainter;
import org.chamomilo.wurm.ui.v1.UiBackground;
import org.chamomilo.wurm.ui.v1.UiScale;

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
    private final ChamomiloUiV1Canvas ui = new ChamomiloUiV1Canvas(this);
    private final HighResHudActionButton noTargetButton;
    private final HighResHudActionButton focusButton;
    private final CombatFocusState focusState = new CombatFocusState();
    private final CombatKnowledgeBook knowledgeBook = new CombatKnowledgeBook(
            Paths.get(HighResFightingHudSettings.knowledgeDirectory));
    private final CombatExamineQueue examineQueue = new CombatExamineQueue();

    private final List<StaticComponent> modeButtons = new ArrayList<>();
    private final List<StaticComponent> attackButtons = new ArrayList<>();
    private final List<StaticComponent> positionControls = new ArrayList<>();
    private final List<StaticComponent> specialButtons = new ArrayList<>();
    private final List<HighResCombatHint> combatHints = new ArrayList<>();
    private List<StaticComponent> nativeAdvanced;
    private AttackButtonComponent nativeFocus;
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
        this.noTargetButton = new HighResHudActionButton(this,
                NO_TARGET_WIDTH, NO_TARGET_HEIGHT,
                new HudCaptionGroup(NO_TARGET_WIDTH, NO_TARGET_HEIGHT,
                        HudSkin.COMPACT, "No target"),
                "No target", this::clearCombatTarget);
        this.focusButton = new HighResHudActionButton(this,
                FOCUS_BUTTON_WIDTH, FOCUS_BUTTON_HEIGHT,
                new HudCaptionGroup(FOCUS_BUTTON_WIDTH, FOCUS_BUTTON_HEIGHT,
                        HudSkin.COMPACT, "Combat focus"),
                "Combat focus", this::attemptFocus);
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

    /** Selection transfers only while the combat card is actually displayed. */
    public long presentedTargetId() {
        return contentVisible && target != null && owner.isComponentEnabled(this)
                ? targetId : NO_TARGET;
    }

    private void clearCombatTarget() {
        if (rawTarget() == null) return;
        lastPortraitClick = 0L;
        draggingPortrait = false;
        HighResHudApi.runAs(ActionOrigin.HUD, () -> {
            owner.sendCombatAction(PlayerAction.NO_TARGET.getId());
            owner.setTargetCreature(-1L, null);
        });
        if (portrait != null) portrait.setSubject(null);
        syncTarget();
        syncLayout();
    }

    public boolean serverText(String messageTitle, String message) {
        focusState.observeMessage(message);
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
        noTargetButton.caption("No target", next != null);
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
        noTargetButton.setLocation(nextX + NO_TARGET_X, nextY + NO_TARGET_Y,
                NO_TARGET_WIDTH, NO_TARGET_HEIGHT);
        focusButton.setLocation(nextX + FOCUS_BUTTON_X, nextY + FOCUS_BUTTON_Y,
                FOCUS_BUTTON_WIDTH, FOCUS_BUTTON_HEIGHT);
        if (fightOptions != null) {
            fightOptions.setPosition(nextX + COMBAT_X, nextY + COMBAT_Y);
        }
        if (resolveNativeChildren()) layoutNativeControls();
        syncFocusButton();
    }

    @Override
    public void setLocation(int nextX, int nextY, int ignoredWidth,
                            int ignoredHeight) {
        this.x = nextX;
        this.y = nextY;
        syncLayout();
    }

    @Override
    protected void renderComponent(Queue queue, float ignoredAlpha) {
        final float alpha = 1.0f;
        syncTarget();
        syncLayout();
        if (!contentVisible) return;
        if (portrait != null) portrait.requestPreview();

        org.highreshud.client.ui.HudSkin.fightingBack(ui.begin(queue), x, y);
        UiPainter.background(ui, UiBackground.LEATHER, UiScale.BASE, 1.0f,
                x + 5, y + 5, TARGET_WIDTH - 5, TARGET_HEIGHT - 5);
        renderPortrait(queue);
        renderIdentity(queue);
        renderHealth(queue);
        renderProgress(queue);
        renderStatus(queue);
        renderTargetFrame(queue);
        noTargetButton.render(queue, 1.0f);
        renderFocus(queue);
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
        HudSkin.targetFront(ui.begin(queue), x, y, hasActionProgress());
    }

    private boolean hasActionProgress() {
        return !progressState.progress().title.isEmpty();
    }

    private UiRect targetCell(int row) {
        return HudSkin.targetCell(x, y, hasActionProgress(), row);
    }

    private void renderIdentity(Queue queue) {
        String name = targetName();
        if (name.isEmpty()) name = "No combat target";
        UiRect cell = targetCell(0);
        paintCentered(title, queue, fit(title, name, cell.width - 12),
                cell.x + 6, centeredLowerEdge(cell.y, cell.height, title.getHeight()),
                cell.width - 12, 0.94f, 0.86f, 0.68f);
    }

    private void renderHealth(Queue queue) {
        CreatureCellRenderable creature = target instanceof CreatureCellRenderable
                ? (CreatureCellRenderable) target : null;
        if (creature == null) return;
        float raw = clamp(creature.getPercentHealth() / 100f);
        smoothedHealth = FocusMath.smoothHealth(smoothedHealth, raw);
        UiRect cell = targetCell(1);
        int gx = cell.x, gy = cell.y, gw = cell.width;
        renderGauge(queue, gx, gy, gw, cell.height,
                smoothedHealth, 0.055f, 0.61f, 0.18f);
        paintCentered(small, queue,
                "Health: " + Math.round(clamp(smoothedHealth) * 100f) + "%",
                gx, centeredLowerEdge(gy, cell.height, small.getHeight()),
                gw, 0.94f, 0.92f, 0.84f);
    }

    private void renderProgress(Queue queue) {
        FocusBarState.Progress progress = progressState.progress();
        if (progress.title.isEmpty()) return;
        UiRect cell = targetCell(2);
        int gx = cell.x, gy = cell.y, gw = cell.width;
        float[] colour = progress.changeColor
                ? new float[]{0.58f, 0.23f, 0.10f}
                : new float[]{0.10f, 0.43f, 0.72f};
        renderGauge(queue, gx, gy, gw, cell.height,
                progress.value, colour[0], colour[1], colour[2]);
        paintCentered(small, queue, fit(small, progress.title, gw - 8),
                gx, centeredLowerEdge(gy, cell.height, small.getHeight()),
                gw, 0.94f, 0.92f, 0.84f);
    }

    private void renderStatus(Queue queue) {
        if (!(target instanceof CreatureCellRenderable)) return;
        CreatureCellRenderable creature = (CreatureCellRenderable) target;
        String label = CreatureRelationState.label(creature);
        boolean hostile = CreatureRelationState.isHostile(creature);
        UiRect cell = targetCell(hasActionProgress() ? 3 : 2);
        paintShadowed(small, queue, fit(small, label, cell.width - 12),
                cell.x + 6, centeredLowerEdge(cell.y, cell.height, small.getHeight()),
                hostile ? 0.96f : 0.48f,
                hostile ? 0.42f : 0.78f,
                hostile ? 0.22f : 0.42f);
    }

    public void focusOptionsReceived() {
        focusState.combatChanged(nativeFighting());
        focusState.optionsReceived(System.nanoTime());
    }

    public void focusPositionReceived(byte stance) { focusState.positionReceived(stance); }
    public void focusLevelReceived(byte level, String message) { focusState.levelReceived(level, message); }

    private boolean stunned() {
        try { return Boolean.TRUE.equals(fieldValue("stunned")); }
        catch (Exception ignored) { return false; }
    }

    private boolean specialAvailable(StaticComponent component) {
        return nativeFighting() && !stunned() && nativeAdvanced != null
                && nativeAdvanced.contains(component)
                && component instanceof AttackButtonComponent
                && !((AttackButtonComponent) component).hidden;
    }

    private String focusStatus() {
        boolean specials = false;
        // Shield bash alone does not prove the initial engagement threshold.
        for (int i = 1; i < specialButtons.size(); i++) specials |= specialAvailable(specialButtons.get(i));
        String action = owner.getActionString();
        return focusState.readiness(target != null, stunned(), nativeFocus != null && !nativeFocus.hidden,
                specials, action != null && action.toLowerCase(Locale.ROOT).contains("focusing"), System.nanoTime());
    }

    private void syncFocusButton() {
        focusState.combatChanged(nativeFighting());
        focusButton.caption("Combat focus", CombatFocusState.ready(focusStatus()));
    }

    private void attemptFocus() {
        syncFocusButton();
        if (!CombatFocusState.ready(focusStatus())) return;
        focusState.attemptSent(System.nanoTime());
        focusButton.caption("Combat focus", false);
        HighResHudApi.runAs(ActionOrigin.HUD, () -> owner.sendCombatAction(nativeFocus.command));
    }

    private void renderFocus(Queue queue) {
        HudSkin.panel(ui.begin(queue), x + FOCUS_X, y + FOCUS_Y, FOCUS_WIDTH, FOCUS_HEIGHT);
        focusButton.render(queue, 1.0f);
        String level = "Focus " + focusState.level() + "/5";
        int baseline = y + FOCUS_BUTTON_Y + (FOCUS_BUTTON_HEIGHT + Math.max(bold.getHeight(), regular.getHeight())) / 2;
        paintShadowed(bold, queue, level, x + FOCUS_TEXT_X, baseline,
                0.94f, 0.86f, 0.68f);
        String status = focusStatus();
        boolean ready = CombatFocusState.ready(status);
        int statusX = x + FOCUS_TEXT_X + bold.getWidth(level) + FOCUS_TEXT_GAP;
        int available = x + FOCUS_X + FOCUS_WIDTH - 8 - statusX;
        paintShadowed(regular, queue, fit(regular, status, available), statusX, baseline,
                ready ? 0.48f : 0.83f, ready ? 0.78f : 0.65f, 0.42f);
    }

    private int availableAttackStances() {
        if (!nativeFighting() || stunned() || !resolveNativeChildren()) return 0;
        int mask = 0;
        for (int i = 0; i < attackButtons.size(); i++) {
            StaticComponent button = attackButtons.get(i);
            if (button instanceof AttackButtonComponent && !((AttackButtonComponent) button).hidden)
                mask |= 1 << AttackStanceModel.idAt(i);
        }
        return mask;
    }

    private CombatKnowledge.Snapshot combatRecommendation() {
        return knowledgeBook.snapshot(combatContextKey(), playerStance(), availableAttackStances());
    }

    private void renderAnalysis(Queue queue) {
        int ax = x + ANALYSIS_X;
        int ay = y + ANALYSIS_Y;
        org.highreshud.client.ui.HudSkin.panel(ui.begin(queue), ax, ay, ANALYSIS_WIDTH, ANALYSIS_HEIGHT);
        if (targetProfile == null) {
            paintCentered(regular, queue, "TARGET ANALYSIS", ax, ay + ANALYSIS_TEXT_INSET + regular.getHeight(),
                    ANALYSIS_WIDTH, 0.88f, 0.77f, 0.58f);
            paintCentered(small, queue, "Select a creature to build its model",
                    ax, ay + 62, ANALYSIS_WIDTH,
                    0.70f, 0.63f, 0.52f);
            return;
        }

        CombatKnowledge.Snapshot overall = knowledgeBook.snapshot(
                targetProfile.knowledgeKey(), playerStance());
        CombatKnowledge.Snapshot stats = combatRecommendation();
        int top = ay + ANALYSIS_TEXT_INSET;
        top = analysisRow(queue, regular,
                "KNOWLEDGE " + overall.studyPercent + "%  ·  KILLS "
                        + overall.kills + "  ·  SAMPLES "
                        + stats.outgoingAttempts + "/" + overall.outgoingAttempts,
                ax, top, 0.91f, 0.80f, 0.60f);
        top = analysisRow(queue, small,
                "Hit " + probability(stats.hitChance)
                        + "  Parry " + probability(stats.parryChance),
                ax, top, 0.86f, 0.80f, 0.69f);
        top = analysisRow(queue, small,
                "Glance " + probability(stats.glanceChance)
                        + "  Dmg index avg/max "
                        + decimal(stats.meanDamageIndex) + "/"
                        + decimal(stats.maximumDamageIndex),
                ax, top, 0.86f, 0.80f, 0.69f);
        top = analysisRow(queue, bold,
                stats.bestStance < 0 ? "BEST — no attack available" : "BEST " + AttackStanceModel.label(stats.bestStance)
                        + "  +" + Math.round(stats.projectedGainPercent)
                        + "%  (n=" + stats.bestStanceSamples + ")",
                ax, top, 0.96f, 0.72f, 0.30f);
        top = analysisRow(queue, small,
                "Enemy stance " + AttackStanceModel.label(targetStance())
                        + "  ·  incoming " + overall.incomingDamageTypes,
                ax, top, 0.78f, 0.75f, 0.66f);
        String shield = loadout.hasShield()
                ? "Shield " + probability(stats.shieldBlockChance) : "Shield not equipped";
        analysisRow(queue, small, shield + " · " + equipmentLine(), ax, top, 0.78f, 0.75f, 0.66f);
    }

    private int analysisRow(Queue queue, TextFont font, String text, int ax, int top, float red, float green, float blue) {
        paintShadowed(font, queue, fit(font, text, ANALYSIS_WIDTH - 2 * ANALYSIS_TEXT_INSET - 1),
                ax + ANALYSIS_TEXT_INSET, top + font.getHeight(), red, green, blue);
        return top + font.getHeight() + ANALYSIS_ROW_GAP;
    }

    private void renderCombatSections(Queue queue) {
        section(queue, x + COMBAT_X, y + COMBAT_Y,
                COMBAT_SIZE, COMBAT_HEIGHT, "ATTACK ZONE");
        section(queue, x + MODE_X, y + MODE_Y,
                MODE_WIDTH, MODE_HEIGHT, "MODE");
        section(queue, x + POSITION_X, y + POSITION_Y,
                POSITION_WIDTH, POSITION_HEIGHT, "POSITION");
        section(queue, x + SPECIAL_X, y + SPECIAL_Y,
                SPECIAL_WIDTH, SPECIAL_HEIGHT, "SPECIAL MOVES");
        paintShadowed(bold, queue, "Distance", x + DISTANCE_LABEL_X, y + POSITION_TEXT_Y, .86f, .80f, .69f);
        paintShadowed(bold, queue, targetDistance(), x + DISTANCE_VALUE_X, y + POSITION_TEXT_Y,
                .94f, .90f, .77f);
        paintShadowed(bold, queue, "Footing", x + FOOTING_LABEL_X, y + POSITION_TEXT_Y, .86f, .80f, .69f);
        if (targetProfile == null) return;
        CombatKnowledge.Snapshot stats = combatRecommendation();
        if (stats.bestStance >= 0) {
            int bestGrid = AttackStanceModel.gridIndex(stats.bestStance);
            int bx = x + FightingHudLayout.stanceCellX(bestGrid, STANCE_ICON);
            int by = y + FightingHudLayout.stanceCellY(bestGrid, STANCE_ICON);
            rim(queue, bx - 2, by - 2, STANCE_ICON + 4, STANCE_ICON + 4,
                    2, 0.92f, 0.66f, 0.20f, 0.96f);
        }
        int currentGrid = AttackStanceModel.gridIndex(playerStance());
        int cx = x + FightingHudLayout.stanceCellX(currentGrid, STANCE_ICON);
        int cy = y + FightingHudLayout.stanceCellY(currentGrid, STANCE_ICON);
        rim(queue, cx - 1, cy - 1, STANCE_ICON + 2, STANCE_ICON + 2,
                1, 0.34f, 0.74f, 0.95f, 0.94f);
    }

    private String targetDistance() {
        World world = owner == null ? null : owner.getWorld();
        if (world == null || !(target instanceof CreatureCellRenderable)) return "—";
        CreatureCellRenderable creature = (CreatureCellRenderable) target;
        float px = world.getPlayerPosX(), py = world.getPlayerPosY();
        float tx = creature.getXPos(), ty = creature.getYPos();
        if (!Float.isFinite(px) || !Float.isFinite(py) || !Float.isFinite(tx) || !Float.isFinite(ty)) return "—";
        return Integer.toString(org.highresfocusbar.client.FocusMath.horizontalDistance(px, py, tx, ty));
    }

    private void section(Queue queue, int sx, int sy, int sw, int sh,
                         String label) {
        org.highreshud.client.ui.HudSkin.panel(ui.begin(queue), sx, sy, sw, sh);
        paintCentered(title, queue, label, sx, sy + SECTION_TITLE_Y, sw,
                0.86f, 0.75f, 0.56f);
    }

    private void renderFightOptions(Queue queue, float alpha) {
        if (fightOptions == null) return;
        if (!resolveNativeChildren()) {
            paintCentered(small, queue, "Combat controls unavailable", x + SPECIAL_X,
                    y + SPECIAL_Y + 35, SPECIAL_WIDTH, .70f, .63f, .52f);
            return;
        }
        layoutNativeControls();
        renderControls(queue, alpha, modeButtons);
        if (nativeFighting()) {
            renderControls(queue, alpha, attackButtons);
            for (StaticComponent position : positionControls) position.render(queue, 1.0f);
        }
        for (int i = 0; i < specialButtons.size(); i++) combatHints.get(i).renderButton(queue);
    }

    private void renderControls(Queue queue, float alpha,
                                List<StaticComponent> components) {
        for (StaticComponent component : components) {
            if (component == null) continue;
            if (component instanceof AttackButtonComponent) {
                AttackButtonComponent action = (AttackButtonComponent) component;
                boolean unavailable = action.hidden || components == specialButtons && !specialAvailable(component);
                UiPainter.button(ui.begin(queue), unavailable ? .5f : 1f, 0f,
                        org.highreshud.client.ui.HudSkin.COMPACT, 1.0f,
                        component.x, component.y, component.width, component.height);
                int cx = component.x, cy = component.y, cw = component.width, ch = component.height;
                boolean savedHidden = action.hidden;
                try {
                    // Native icon, availability/difficulty colors and input remain native.
                    component.setLocation(cx + 3, cy + 3, cw - 6, ch - 6);
                    if (unavailable) action.hidden = true;
                    component.render(queue, 1.0f);
                } finally { action.hidden = savedHidden; component.setLocation(cx, cy, cw, ch); }
            } else {
                org.highreshud.client.ui.HudSkin.panel(ui.begin(queue), component.x,
                        component.y, component.width, component.height);
                component.render(queue, 1.0f);
            }
        }
    }

    @SuppressWarnings("unchecked")
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
            nativeFocus = (AttackButtonComponent) componentField("focusB");
            nativeAdvanced = (List<StaticComponent>) fieldValue("advancedComponents");
            focusState.levelReceived(((Number) declaredFieldValue(nativeFocus, "focusLevel")).byteValue(),
                    (String) declaredFieldValue(nativeFocus, "focusLevelMessage"));
            specialButtons.add(componentField("shieldBash"));
            positionControls.add(componentField("distanceMeter"));
            positionControls.add(componentField("balanceMeter"));
            Object specialMoves = fieldValue("specialMoves");
            for (int i = 0; i < Array.getLength(specialMoves); i++) {
                specialButtons.add(asComponent(Array.get(specialMoves, i)));
            }
            nativeChildrenResolved = complete(modeButtons)
                    && complete(attackButtons) && complete(positionControls) && complete(specialButtons)
                    && nativeFocus != null && nativeAdvanced != null;
            if (nativeChildrenResolved) {
                for (int i = 0; i < specialButtons.size(); i++) {
                    final int index = i;
                    final StaticComponent button = specialButtons.get(i);
                    combatHints.add(new HighResCombatHint(this, button, () -> specialAvailable(button),
                            () -> new String[]{index == 0 ? "Shield bash: shield attack; server controls readiness."
                                    : "Weapon special move: unlocked by the server during combat.",
                                    specialAvailable(button) ? "Ready" : index == 0
                                            ? "Unavailable: shield missing, cooldown, or combat state."
                                            : "Unavailable until the server grants this move."}, true));
                }
                for (StaticComponent position : positionControls) combatHints.add(new HighResCombatHint(this,
                        position, () -> false, () -> new String[]{position instanceof DistMeterComponent
                            ? "Position indicator: relative weapon range, not an action."
                            : "Position indicator: terrain and balance advantage, not an action."}));
            }
            return nativeChildrenResolved;
        } catch (Throwable ignored) {
            modeButtons.clear();
            attackButtons.clear();
            positionControls.clear();
            specialButtons.clear();
            combatHints.clear();
            nativeChildrenUnavailable = true;
            return false;
        }
    }

    private Object fieldValue(String name) throws Exception {
        return declaredFieldValue(fightOptions, name);
    }

    private static Object declaredFieldValue(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
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
        positionControls.get(0).setSize(false);
        positionControls.get(0).setLocation(x + RANGE_ICON_X, y + POSITION_ICON_Y, POSITION_ICON_SIZE, POSITION_ICON_SIZE);
        positionControls.get(1).setSize(false);
        positionControls.get(1).setLocation(x + FOOTING_ICON_X, y + POSITION_ICON_Y, POSITION_ICON_SIZE, POSITION_ICON_SIZE);
        for (int i = 0; i < specialButtons.size(); i++) {
            StaticComponent component = specialButtons.get(i);
            component.setSize(true);
            component.setLocation(x + specialCellX(i), y + specialCellY(),
                    SPECIAL_BUTTON_SIZE, SPECIAL_BUTTON_SIZE);
        }
    }

    @Override
    public StaticComponent getComponentAt(int mouseX, int mouseY) {
        if (!contentVisible || !contains(mouseX, mouseY)) return null;
        if (noTargetButton.contains(mouseX, mouseY)) return noTargetButton;
        if (focusButton.contains(mouseX, mouseY)) return focusButton;
        if (resolveNativeChildren()) {
            for (HighResCombatHint hint : combatHints) {
                StaticComponent nested = hint.at(mouseX, mouseY);
                if (nested != null) return nested;
            }
            if (stunned()) return this;
            StaticComponent nested = componentAt(modeButtons, mouseX, mouseY);
            if (nested != null) return nested;
            if (nativeFighting()) {
                nested = componentAt(attackButtons, mouseX, mouseY);
                if (nested != null) return nested;
            }
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
                clearCombatTarget();
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
        if (noTargetButton.contains(mouseX, mouseY)) {
            pickData.addText("Clear the current combat target");
            return;
        }
        if (inside(mouseX, mouseY, x + FOCUS_X, y + FOCUS_Y, FOCUS_WIDTH, FOCUS_HEIGHT)) {
            pickData.addText("Combat focus level " + focusState.level() + "/5");
            pickData.addText(focusState.levelMessage());
            pickData.addText(focusStatus());
            pickData.addText("~ marks estimated initial engagement; the server decides whether the attempt succeeds.");
            pickData.addText("Manual combat controls required. Focusing temporarily stops weapon attacks.");
            return;
        }
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
        UiRect well = new UiRect(gx, gy, gw, gh);
        HudSkin.gauge(ui.begin(queue), well, value, new UiColor(red, green, blue));
        HudSkin.glass(ui, well);
        int filled = Math.round(clamp(value) * gw);
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
