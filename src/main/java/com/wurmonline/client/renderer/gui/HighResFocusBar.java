package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.console.ActionClass;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.SubPickableUnit;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.cell.PlayerCellRenderable;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.resources.textures.IconLoader;
import com.wurmonline.client.resources.textures.ResourceTexture;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.client.resources.textures.Texture;
import com.wurmonline.client.settings.WindowPosition;
import com.wurmonline.shared.constants.PlayerAction;
import org.highresfocusbar.client.ActionTooltip;
import org.highreshud.client.state.CreatureRelationState;
import org.highresfocusbar.client.FocusBarState;
import org.highresfocusbar.client.FocusAtlasUv;
import org.highresfocusbar.client.FocusFonts;
import org.highresfocusbar.client.FocusMath;
import org.highresfocusbar.client.HighResFocusBarMod;
import org.highresfocusbar.client.HighResFocusBarSettings;
import org.highresfocusbar.client.SelectedExamineRetry;
import org.highresfocusbar.client.SelectedExamineState;
import org.highresfocusbar.client.SelectedStatusText;
import org.highresfocusbar.client.SelectAutoCloseTimer;
import org.highresfocusbar.client.WaypointerBridge;
import org.highresfocusbar.client.portrait.FocusPortraitController;
import org.highresfocusbar.client.portrait.TilePortraitPreview;
import org.highreshud.core.ActionOrigin;
import org.highreshud.core.HighResHudApi;
import org.highreshud.client.HiddenExamineCoordinator;
import org.highreshud.client.HighResHudRuntime;
import org.highreshud.client.ui.HudCaptionGroup;
import org.highreshud.client.ui.HudSkin;
import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import org.chamomilo.wurm.ui.v1.*;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.highresfocusbar.client.FocusBarLayout.*;

/**
 * Standalone high-resolution Select Bar for the currently selected object.
 * Package placement is intentional because the stock client exposes its
 * Select Bar state to sibling widgets.
 */
public final class HighResFocusBar extends StaticComponent
        implements WindowSerializer {
    public static final int PANEL_WIDTH = 438;
    public static final int PANEL_HEIGHT = 108;
    private static final int BASE_WIDTH = PANEL_WIDTH;
    private static final int FRAME_SOURCE_WIDTH = PANEL_WIDTH;

    private static final int PORTRAIT_X = 13;
    private static final int PORTRAIT_Y = 7;
    private static final int PORTRAIT_WIDTH = 85;
    private static final int PORTRAIT_HEIGHT = 90;
    private static final int INFO_X = 107;
    private static final int INFO_WIDTH = 327;
    private static final int INSPECT_SIZE = 14;
    private static final int WAYPOINT_SIZE = 18;
    private static final int INSPECT_INSET = 1;
    private static final int WAYPOINT_RIGHT_INSET = -6;
    private static final int OVERLAY_TOP_INSET = 1;
    private static final int ACTION_SLOT_WIDTH = ACTION_SLOT_SIZE;
    private static final int ACTION_SLOT_HEIGHT = ACTION_SLOT_SIZE;
    private static final int ACTION_STRIDE = ACTION_SLOT_SIZE + ACTION_SLOT_GAP;
    private static final long SHINE_PERIOD = 4_800_000_000L;
    private static final long SELECTION_GRACE_NANOS = 750_000_000L;
    private static final long ACTION_PRESS_NANOS = 165_000_000L;
    private static final long SILENT_EXAMINE_NANOS = 4_000_000_000L;
    private static final long SILENT_EXAMINE_TAIL_NANOS = 400_000_000L;
    private static final int AUTO_EXAMINE_RETRY_RANGE_METRES = 12;
    private static final float PIN_U = 237f / 1312f;
    private static final float PIN_V = 135f / 1199f;
    private static final float PIN_U_SIZE = 805f / 1312f;
    private static final float PIN_V_SIZE = 805f / 1199f;
    private static final ActionClass[] SELECT_KEYS = new ActionClass[]{
            ActionClass.SELECT_BUTTON_1, ActionClass.SELECT_BUTTON_2,
            ActionClass.SELECT_BUTTON_3, ActionClass.SELECT_BUTTON_4,
            ActionClass.SELECT_BUTTON_5, ActionClass.SELECT_BUTTON_6,
            ActionClass.SELECT_BUTTON_7, ActionClass.SELECT_BUTTON_8
    };

    private final HeadsUpDisplay owner;
    private final SelectBar selectBar;
    private final FocusBarState state;
    private final FocusPortraitController selectedPortrait;
    private final DragController dragger;
    private final TextFont regular;
    private final TextFont bold;
    private final TextFont title;
    private final TextFont small;
    private final ChamomiloUiV1Canvas ui = new ChamomiloUiV1Canvas(this);
    private HudCaptionGroup pagerCaption;
    private int pagerCaptionPages;
    private final ResourceTexture actionAtlas;
    private final ResourceTexture pinIcon;
    private final Map<Short, SelectBarButtonProperty> actionProperties;
    private final SelectAutoCloseTimer autoClose =
            new SelectAutoCloseTimer();

    private boolean showHotkeys = true;
    private boolean showDistance = true;
    private FocusPortraitController draggedPortrait;
    private int portraitDragX;
    private int portraitDragY;
    private float smoothedHealth = -1f;
    private long lastSubjectId = Long.MIN_VALUE;
    private PickableUnit latchedSelectedSubject;
    private long selectedLastSeen;
    private boolean contentVisible;
    private boolean actionsClearedForNoSelection;
    private int pressedActionIndex = -1;
    private int actionPageStart;
    private long pressedActionUntil;
    private long pressedPagerUntil;
    private long pressedInspectUntil;
    private long pressedSelectedWaypointUntil;
    private long inspectSubjectId = Long.MIN_VALUE;
    private int inspectProbeRequestId = -1;
    private PlayerAction resolvedInspectAction;
    private PickableUnit pendingInspectSubject;
    private final SelectedExamineState selectedExamine =
            new SelectedExamineState();
    private final SelectedExamineRetry examineRetry =
            new SelectedExamineRetry();
    private long silentExamineId = Long.MIN_VALUE;
    private long silentExamineUntil;
    private boolean silentExamineResponseStarted;
    private long silentExamineLastLineAt;

    public HighResFocusBar(HeadsUpDisplay owner, SelectBar selectBar,
                           FocusBarState state,
                           FocusPortraitController selectedPortrait) {
        super("high res select bar", 16, 120, PANEL_WIDTH, PANEL_HEIGHT);
        this.owner = owner;
        this.selectBar = selectBar;
        this.state = state;
        this.selectedPortrait = selectedPortrait;
        this.dragger = new DragController(this);
        this.regular = FocusFonts.regular();
        this.bold = FocusFonts.bold();
        this.title = FocusFonts.title();
        this.small = FocusFonts.small();
        this.text = regular;
        this.textBold = bold;
        this.actionAtlas = texture("img.highresselectbar.actions");
        this.pinIcon = texture("img.highresselectbar.pin");
        Map<Short, SelectBarButtonProperty> loaded;
        try {
            loaded = SelectBarXml.load();
        } catch (Throwable ignored) {
            loaded = Collections.emptyMap();
        }
        this.actionProperties = loaded;
        if (selectBar != null && selectBar.selectedUnit != null) {
            state.selectionChanged(selectBar.selectedUnit.getId());
        }
        updatePresentation(selectedSubject());
    }

    private static ResourceTexture texture(String mapping) {
        try {
            return ResourceTextureLoader.getNowrapLinearTexture(mapping);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void gameTick() {
        PickableUnit subject = selectedSubject();
        updatePresentation(subject);
        long now = System.nanoTime();
        long id = subject == null ? Long.MIN_VALUE : subject.getId();
        if (id != lastSubjectId) {
            lastSubjectId = id;
            smoothedHealth = -1f;
            actionPageStart = 0;
            // A newly clicked subject must not inherit the preceding
            // subject's nearly elapsed auto-close deadline.
            autoClose.selectionChanged(now);
        }
        if (autoClose.hasExpired(now)) {
            clearSelection();
            return;
        }
        serviceSelectedExamine(subject);
        observeInspectSubject(subject);
        if (selectedPortrait != null) selectedPortrait.setSubject(subject);
    }

    @Override
    protected void renderComponent(Queue queue, float ignoredAlpha) {
        PickableUnit subject = selectedSubject();
        updatePresentation(subject);
        if (!contentVisible) return;
        requestPortrait(selectedPortrait, subject);

        HudSkin.selectBack(ui.begin(queue), x, y);
        renderPortrait(queue, subject, selectedPortrait, PORTRAIT_X, true);
        renderIdentity(queue, subject);
        renderHealth(queue, subject);
        renderStatus(queue, subject);
        renderInspectButton(queue, subject);
        renderWaypointButton(queue, subject, PORTRAIT_X);
        renderActions(queue);
        renderProgress(queue, state);
        renderFrame(queue);
        renderPinButton(queue);
        renderCloseButton(queue);
        renderDistanceFooter(queue, subject);
    }

    private static void requestPortrait(FocusPortraitController controller,
                                        PickableUnit subject) {
        if (controller == null) return;
        controller.setSubject(subject);
        controller.requestPreview();
    }

    private void renderPortrait(Queue queue, PickableUnit subject,
                                FocusPortraitController controller,
                                int portraitX, boolean selected) {
        int px = x + portraitX;
        int py = y + PORTRAIT_Y;
        fillRect(queue, 0.058f, 0.039f, 0.024f, 1f,
                px, py, PORTRAIT_WIDTH, PORTRAIT_HEIGHT);

        TilePortraitPreview tilePreview = controller == null
                ? null : controller.tilePreview();
        boolean tilePreviewDrawn = renderTilePreview(queue, tilePreview,
                px, py);
        Texture live = controller == null ? null : controller.texture();
        if (live != null && controller.hasLiveModel()) {
            float[] crop = controller.crop();
            Renderer.texturedQuadAlphaBlend(queue, live,
                    1f, 1f, 1f, 1f,
                    px, py, PORTRAIT_WIDTH, PORTRAIT_HEIGHT,
                    crop[0], 1f - crop[1], crop[2], -crop[3]);
            return;
        }
        if (tilePreviewDrawn) return;

        Texture fallback = controller == null ? null : controller.fallbackTexture();
        if (fallback == null && selected && subject != null) {
            fallback = selectBar.selectedIconTexture;
            if (fallback == null && selectBar.selectedIconId >= 0) {
                fallback = IconLoader.getIcon(Short.valueOf(selectBar.selectedIconId));
            }
        }
        if (fallback == null && subject != null) {
            fallback = subject.getIconTexture();
            if (fallback == null && subject.getIconId() >= 0) {
                fallback = IconLoader.getIcon(Short.valueOf(subject.getIconId()));
            }
        }
        if (fallback != null) {
            // Static Wurm icons used to inherit the stock Target Window's
            // 66px size. The focus bar owns a 90px aperture, so fill it.
            Renderer.texturedQuadAlphaBlend(queue, fallback,
                    1f, 1f, 1f, 1f,
                    px, py, PORTRAIT_WIDTH, PORTRAIT_HEIGHT,
                    0f, 0f, 1f, 1f);
        } else {
            if (selected && subject == null) {
                paintCentered(small, queue, "Nothing",
                        px, py + 46, PORTRAIT_WIDTH,
                        0.77f, 0.64f, 0.42f);
                paintCentered(small, queue, "selected",
                        px, py + 59, PORTRAIT_WIDTH,
                        0.77f, 0.64f, 0.42f);
            } else {
                paintCentered(bold, queue, "?",
                        px, py + 53, PORTRAIT_WIDTH,
                        0.77f, 0.64f, 0.42f);
            }
        }
    }

    private static boolean renderTilePreview(Queue queue,
                                             TilePortraitPreview preview,
                                             int px, int py) {
        if (preview == null) return false;
        boolean drawn = false;
        Texture ground = preview.getGroundTexture();
        if (ground != null) {
            Renderer.texturedQuadAlphaBlend(queue, ground,
                    0.88f, 0.88f, 0.88f, 1f,
                    px, py, PORTRAIT_WIDTH, PORTRAIT_HEIGHT,
                    0f, 0f, 1f, 1f);
            drawn = true;
        }

        Texture overlay = preview.getOverlayTexture();
        if (overlay == null
                || preview.getOverlayKind()
                == TilePortraitPreview.OverlayKind.NONE) return drawn;

        boolean crops = preview.getOverlayKind()
                == TilePortraitPreview.OverlayKind.CROP;
        int rows = crops ? 4 : 3;
        int columns = crops ? 3 : 4;
        int baseWidth = crops ? 34 : 29;
        int baseHeight = crops ? 27 : 22;
        for (int row = 0; row < rows; row++) {
            float scale = 0.72f + row * (crops ? 0.10f : 0.13f);
            int spriteWidth = Math.round(baseWidth * scale);
            int spriteHeight = Math.round(baseHeight * scale);
            int rowY = py + 8 + row * (crops ? 18 : 26);
            rowY = Math.min(rowY,
                    py + PORTRAIT_HEIGHT - spriteHeight - 2);
            for (int column = 0; column < columns; column++) {
                int centerX = px + (column + 1) * PORTRAIT_WIDTH
                        / (columns + 1);
                int spriteX = centerX - spriteWidth / 2
                        + ((row + column) % 2 == 0 ? -2 : 2);
                Renderer.texturedQuadAlphaBlend(queue, overlay,
                        1f, 1f, 1f, 0.78f + row * 0.06f,
                        spriteX, rowY, spriteWidth, spriteHeight,
                        preview.getOverlayU(), preview.getOverlayV(),
                        preview.getOverlayWidth(),
                        preview.getOverlayHeight());
            }
        }
        return true;
    }

    private void renderFrame(Queue queue) {
        HudSkin.selectFront(ui.begin(queue), x, y);
    }

    private void renderIdentity(Queue queue, PickableUnit subject) {
        if (subject == null) return;
        String name = subjectName(subject);
        paintShadowed(title, queue, fit(title, name, NAME_TEXT_WIDTH),
                x + NAME_TEXT_X,
                centeredLowerEdge(y + HEADER_TEXT_Y,
                        HEADER_TEXT_HEIGHT, title.getHeight()),
                0.94f, 0.86f, 0.68f);
    }

    private void renderDistanceFooter(Queue queue, PickableUnit subject) {
        if (!showDistance) return;
        int metres = distance(subject);
        String label = "Distance:";
        String value = metres < 0 ? "—" : metres + " m";
        int footerTop = y + PANEL_HEIGHT;
        int baseline = footerTop
                + (DISTANCE_FOOTER_HEIGHT + regular.getHeight()) / 2;
        int gap = 3;
        int totalWidth = regular.getWidth(label) + gap
                + regular.getWidth(value);
        int textX = x + (PANEL_WIDTH - totalWidth) / 2;
        paintShadowed(regular, queue, label, textX, baseline,
                0.76f, 0.76f, 0.72f);
        paintShadowed(regular, queue, value,
                textX + regular.getWidth(label) + gap, baseline,
                0.94f, 0.94f, 0.91f);
    }

    private void renderProgress(Queue queue, FocusBarState ownerState) {
        FocusBarState.Progress progress = ownerState.progress();
        if (progress.title.isEmpty()) return;
        UiRect well = HudSkin.selectProgress(x, y);
        float[] colour = progress.changeColor
                ? new float[]{0.58f, 0.23f, 0.10f}
                : new float[]{0.10f, 0.43f, 0.72f};
        renderGauge(queue, well.x, well.y, well.width, well.height,
                progress.value, colour[0], colour[1], colour[2]);
    }

    private void renderCloseButton(Queue queue) {
        int zoneX = x + CLOSE_ZONE_X;
        int zoneY = y + CLOSE_ZONE_Y;
        int glyphInset = (CLOSE_ZONE_SIZE - CLOSE_GLYPH_SIZE) / 2;
        int bx = zoneX + glyphInset;
        int by = zoneY + glyphInset;
        UiPainter.button(ui.begin(queue), 1f, 0f, HudSkin.COMPACT, 1.0f,
                zoneX, zoneY, CLOSE_ZONE_SIZE, CLOSE_ZONE_SIZE);
        UiIcon.CLOSE.paint(ui, UiColor.TEXT, 1.0f, bx, by, CLOSE_GLYPH_SIZE);
    }

    private void renderPinButton(Queue queue) {
        int zoneX = x + PIN_ZONE_X;
        int zoneY = y + PIN_ZONE_Y;
        boolean pressed = autoClose.isPinned();
        int pressInset = pressed ? 1 : 0;
        UiPainter.button(ui.begin(queue), pressed ? .82f : 1f,
                pressed ? 1f : 0f, HudSkin.COMPACT, 1.0f,
                zoneX, zoneY, PIN_ZONE_SIZE, PIN_ZONE_SIZE);
        if (pinIcon == null) return;
        int glyphInset = (PIN_ZONE_SIZE - PIN_GLYPH_SIZE) / 2 + pressInset;
        int glyphSize = PIN_GLYPH_SIZE - pressInset * 2;
        Renderer.texturedQuadAlphaBlend(queue, pinIcon,
                pressed ? 1f : 0.78f,
                pressed ? 0.90f : 0.74f,
                pressed ? 0.72f : 0.64f, 1f,
                zoneX + glyphInset, zoneY + glyphInset,
                glyphSize, glyphSize,
                PIN_U, PIN_V, PIN_U_SIZE, PIN_V_SIZE);
    }

    private boolean pinButtonAt(int mouseX, int mouseY) {
        return contentVisible && inside(mouseX, mouseY,
                x + PIN_ZONE_X, y + PIN_ZONE_Y,
                PIN_ZONE_SIZE, PIN_ZONE_SIZE);
    }

    private boolean closeButtonAt(int mouseX, int mouseY) {
        return contentVisible && inside(mouseX, mouseY,
                x + CLOSE_ZONE_X, y + CLOSE_ZONE_Y,
                CLOSE_ZONE_SIZE, CLOSE_ZONE_SIZE);
    }

    /** The combat card takes over this creature, including a transient native deselection. */
    public void dismissCombatTarget(long combatTargetId) {
        if (combatTargetId == Long.MIN_VALUE) return;
        PickableUnit selected = selectedSubject();
        if (selected != null && selected.getId() == combatTargetId) clearSelection();
    }

    private void clearSelection() {
        latchedSelectedSubject = null;
        selectedLastSeen = 0L;
        actionsClearedForNoSelection = true;
        state.clearActions();
        if (selectedPortrait != null) selectedPortrait.setSubject(null);
        owner.setSelected(null);
        updatePresentation(null);
    }

    private void renderHealth(Queue queue, PickableUnit subject) {
        CreatureCellRenderable healthSubject = creature(subject);
        float raw = healthSubject == null ? 0f
                : clamp(healthSubject.getPercentHealth() / 100f);
        smoothedHealth = healthSubject == null ? -1f
                : FocusMath.smoothHealth(smoothedHealth, raw);

        UiRect well = HudSkin.selectHealth(x, y);
        int gx = well.x, gy = well.y, gw = well.width;
        if (healthSubject != null) {
            renderGauge(queue, gx, gy, gw, well.height,
                    smoothedHealth, 0.055f, 0.61f, 0.18f);
        }
        String label = healthSubject == null ? ""
                : "Health: " + percent(smoothedHealth);
        if (!label.isEmpty()) {
            paintCentered(small, queue, fit(small, label, gw - 8),
                    gx, centeredLowerEdge(gy, well.height,
                            small.getHeight()), gw,
                    0.94f, 0.92f, 0.84f);
        }
    }

    private void renderInspectButton(Queue queue, PickableUnit subject) {
        PlayerAction inspect = inspectAction();
        if (subject == null || inspect == null) return;
        int bx = x + PORTRAIT_X + INSPECT_INSET;
        int by = y + PORTRAIT_Y + INSPECT_INSET;
        boolean pressed = System.nanoTime() < pressedInspectUntil;
        int offset = pressed ? 1 : 0;
        // Transparent, crisp HUD glyph. Its one-pixel shadow retains
        // readability over pale models without restoring a button tile.
        drawMagnifier(queue, bx + offset + 1, by + offset + 1,
                0.055f, 0.038f, 0.021f, 0.82f);
        drawMagnifier(queue, bx + offset, by + offset,
                pressed ? 0.92f : 0.76f,
                pressed ? 0.67f : 0.55f,
                pressed ? 0.29f : 0.25f, 0.96f);
    }

    private void drawMagnifier(Queue queue, int bx, int by,
                               float red, float green, float blue,
                               float alpha) {
        fillRect(queue, red, green, blue, alpha, bx + 4, by + 1, 4, 1);
        fillRect(queue, red, green, blue, alpha, bx + 2, by + 2, 2, 2);
        fillRect(queue, red, green, blue, alpha, bx + 8, by + 2, 2, 2);
        fillRect(queue, red, green, blue, alpha, bx + 1, by + 4, 1, 4);
        fillRect(queue, red, green, blue, alpha, bx + 10, by + 4, 1, 4);
        fillRect(queue, red, green, blue, alpha, bx + 2, by + 8, 2, 2);
        fillRect(queue, red, green, blue, alpha, bx + 4, by + 10, 4, 1);
        fillRect(queue, red, green, blue, alpha, bx + 8, by + 8, 2, 2);
        fillRect(queue, red, green, blue, alpha, bx + 9, by + 10, 2, 2);
        fillRect(queue, red, green, blue, alpha, bx + 11, by + 12, 3, 2);
    }

    private PlayerAction inspectAction() {
        PickableUnit selected = selectedSubject();
        if (selected != null && selected.getId() == inspectSubjectId
                && resolvedInspectAction != null) {
            return resolvedInspectAction;
        }
        for (PlayerAction action : state.actions()) {
            if (isInspectAction(action)) {
                return action;
            }
        }
        return null;
    }

    private static boolean isInspectAction(PlayerAction action) {
        if (action == null) return false;
        String name = clean(action.getName()).toLowerCase(Locale.ROOT);
        return name.contains("inspect");
    }

    private boolean inspectButtonAt(int mouseX, int mouseY) {
        return inspectAction() != null && selectedSubject() != null
                && inside(mouseX, mouseY,
                x + PORTRAIT_X + INSPECT_INSET,
                y + PORTRAIT_Y + INSPECT_INSET,
                INSPECT_SIZE, INSPECT_SIZE);
    }

    private void renderWaypointButton(Queue queue, PickableUnit subject,
                                      int portraitX) {
        if (subject == null || !WaypointerBridge.isAvailable()) return;
        int bx = x + portraitX + PORTRAIT_WIDTH - WAYPOINT_SIZE
                - WAYPOINT_RIGHT_INSET;
        int by = y + PORTRAIT_Y + OVERLAY_TOP_INSET;
        boolean pressed = System.nanoTime() < pressedSelectedWaypointUntil;
        // Transparent overlay: only a restrained, bold red exclamation mark.
        paintCentered(bold, queue, "!", bx, by + 14,
                WAYPOINT_SIZE,
                pressed ? 0.78f : 0.62f,
                pressed ? 0.16f : 0.12f,
                pressed ? 0.10f : 0.075f);
    }

    private int waypointButtonAt(int mouseX, int mouseY) {
        if (!WaypointerBridge.isAvailable()) return -1;
        PickableUnit selected = selectedSubject();
        if (selected != null && inside(mouseX, mouseY,
                x + PORTRAIT_X + PORTRAIT_WIDTH - WAYPOINT_SIZE
                        - WAYPOINT_RIGHT_INSET,
                y + PORTRAIT_Y + OVERLAY_TOP_INSET,
                WAYPOINT_SIZE, WAYPOINT_SIZE)) {
            return 0;
        }
        return -1;
    }

    private void renderStatus(Queue queue, PickableUnit subject) {
        int sx = x + INFO_X + 6;
        int sy = centeredLowerEdge(y + FOCUS_STATUS_Y,
                FOCUS_STATUS_HEIGHT, small.getHeight());
        CreatureCellRenderable selectedCreature = creature(subject);
        String corpseStatus = FocusMath.corpseStatus(subjectName(subject),
                FocusPortraitController.mapping(subject));
        SelectedExamineState.Snapshot examine = subject == null
                ? null : selectedExamine.get(subject.getId());
        String text = SelectedStatusText.compose(examine, corpseStatus);
        boolean hostile = CreatureRelationState.isHostile(selectedCreature);
        boolean neutralData = !text.isEmpty();
        float red = neutralData ? 0.84f : (hostile ? 0.96f : 0.48f);
        float green = neutralData ? 0.68f : (hostile ? 0.31f : 0.78f);
        float blue = neutralData ? 0.36f : (hostile ? 0.21f : 0.42f);
        paintShadowed(small, queue, fit(small, text, INFO_WIDTH - 12),
                sx, sy, red, green, blue);
    }

    private static CreatureCellRenderable creature(PickableUnit unit) {
        if (!(unit instanceof CreatureCellRenderable)) return null;
        CreatureCellRenderable result = (CreatureCellRenderable) unit;
        return result.isItem() ? null : result;
    }

    private void renderActions(Queue queue) {
        List<PlayerAction> actions = state.actions();
        normalizeActionPage(actions.size());
        long now = System.nanoTime();
        for (int slot = 0; slot < ACTION_VISIBLE_SLOTS; slot++) {
            int index = actionPageStart + slot;
            int sx = x + ACTION_X + slot * ACTION_STRIDE;
            int sy = y + ACTION_Y;
            boolean pressed = index == pressedActionIndex
                    && now < pressedActionUntil;
            int pressOffset = pressed ? 1 : 0;
            if (pressed) {
                fillRect(queue, 0.19f, 0.12f, 0.040f, 0.96f,
                        sx, sy, ACTION_SLOT_WIDTH, ACTION_SLOT_HEIGHT);
            }
            if (index >= actions.size()) continue;
            UiPainter.button(ui.begin(queue), pressed ? .82f : 1f, pressed ? 1f : 0f,
                    HudSkin.COMPACT, 1.0f, sx, sy, ACTION_SLOT_WIDTH, ACTION_SLOT_HEIGHT);
            PlayerAction action = actions.get(index);
            SelectBarButtonProperty property = actionProperties.get(action.getId());
            if (property != null && actionAtlas != null) {
                Renderer.texturedQuadAlphaBlend(queue, actionAtlas,
                        1f,
                        pressed ? 0.86f : 1f,
                        pressed ? 0.48f : 1f, 1f,
                        sx + ACTION_ICON_INSET + pressOffset,
                        sy + ACTION_ICON_INSET + pressOffset,
                        ACTION_ICON_SIZE, ACTION_ICON_SIZE,
                        FocusAtlasUv.iconOffset(property.getX()),
                        FocusAtlasUv.iconOffset(property.getY()),
                        FocusAtlasUv.iconSize(), FocusAtlasUv.iconSize());
            } else {
                paintCentered(bold, queue, "?", sx, sy + 17,
                        ACTION_SLOT_WIDTH, 0.80f, 0.68f, 0.46f);
            }
            if (pressed) {
                rim(queue, sx + 1, sy + 1,
                        ACTION_SLOT_WIDTH - 2, ACTION_SLOT_HEIGHT - 2,
                        2, 1f, 0.72f, 0.20f, 0.92f);
            }
            if (showHotkeys) {
                String key = binding(index);
                if (!key.isEmpty()) {
                    int kw = Math.max(10, small.getWidth(key) + 3);
                    int keyX = sx + ACTION_SLOT_WIDTH - kw - 2;
                    paintCentered(small, queue, key,
                            keyX + pressOffset, sy + 12 + pressOffset, kw,
                            0.96f, 0.84f, 0.48f);
                }
            }
        }
        renderActionPager(queue, actions.size(), now);
    }

    private void renderActionPager(Queue queue, int actionCount, long now) {
        if (actionCount <= ACTION_VISIBLE_SLOTS) return;
        int pages = (actionCount + ACTION_VISIBLE_SLOTS - 1)
                / ACTION_VISIBLE_SLOTS;
        int page = actionPageStart / ACTION_VISIBLE_SLOTS + 1;
        int px = x + ACTION_PAGER_X;
        int py = y + ACTION_Y;
        boolean pressed = now < pressedPagerUntil;
        if (pagerCaption == null || pagerCaptionPages != pages) {
            String[] variants = new String[pages];
            for (int p = 1; p <= pages; p++) variants[p - 1] = p + "/" + pages;
            pagerCaption = new HudCaptionGroup(ACTION_PAGER_WIDTH, ACTION_SLOT_HEIGHT,
                    HudSkin.COMPACT, 12, variants);
            pagerCaptionPages = pages;
        }
        UiPainter.button(ui.begin(queue), pressed ? .82f : 1f, pressed ? 1f : 0f,
                HudSkin.COMPACT, 1.0f, px, py, ACTION_PAGER_WIDTH, ACTION_SLOT_HEIGHT);
        String caption = page + "/" + pages;
        TextFont font = ChamomiloUiV1Fonts.caption(pagerCaption.fontPixels, false, pagerCaption.density);
        int offset = pressed ? 1 : 0;
        font.moveTo(px + pagerCaption.textX(caption, false, ACTION_PAGER_WIDTH) + offset,
                py + pagerCaption.baseline + offset);
        font.paint(queue, pagerCaption.caption(caption), UiColor.TEXT.red,
                UiColor.TEXT.green, UiColor.TEXT.blue, 1.0f);
    }

    private void normalizeActionPage(int actionCount) {
        if (actionCount <= ACTION_VISIBLE_SLOTS) {
            actionPageStart = 0;
            return;
        }
        if (actionPageStart < 0 || actionPageStart >= actionCount) {
            actionPageStart = 0;
        }
    }

    private void advanceActionPage(int direction) {
        int actionCount = state.actions().size();
        if (actionCount <= ACTION_VISIBLE_SLOTS) return;
        int pages = (actionCount + ACTION_VISIBLE_SLOTS - 1)
                / ACTION_VISIBLE_SLOTS;
        int page = actionPageStart / ACTION_VISIBLE_SLOTS;
        page = Math.floorMod(page + direction, pages);
        actionPageStart = page * ACTION_VISIBLE_SLOTS;
    }

    private boolean actionPagerAt(int mouseX, int mouseY) {
        return state.actions().size() > ACTION_VISIBLE_SLOTS
                && inside(mouseX, mouseY,
                x + ACTION_PAGER_X, y + ACTION_Y,
                ACTION_PAGER_WIDTH, ACTION_SLOT_HEIGHT);
    }

    private int actionIndexAt(int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY,
                x + ACTION_X, y + ACTION_Y,
                ACTION_VISIBLE_SLOTS * ACTION_STRIDE,
                ACTION_SLOT_HEIGHT)) return -1;
        int slot = (mouseX - (x + ACTION_X)) / ACTION_STRIDE;
        int localX = (mouseX - (x + ACTION_X)) % ACTION_STRIDE;
        int index = actionPageStart + slot;
        return localX >= 0 && localX < ACTION_SLOT_WIDTH
                && index < state.actions().size() ? index : -1;
    }

    private void renderGauge(Queue queue, int gx, int gy, int gw, int gh,
                             float value, float red, float green, float blue) {
        UiRect well = new UiRect(gx, gy, gw, gh);
        HudSkin.gauge(ui.begin(queue), well, value, new UiColor(red, green, blue));
        HudSkin.glass(ui, well);
        int filled = Math.round(clamp(value) * gw);
        if (!HighResFocusBarSettings.animateGaugeShine || filled < 5) return;
        long offset = (gx * 31L + gy * 17L) * 1_000_000L;
        double angle = Math.floorMod(System.nanoTime() + offset, SHINE_PERIOD)
                / (double) SHINE_PERIOD * Math.PI * 2.0;
        float breathe = (float) ((Math.sin(angle) + 1.0) * 0.5);
        fillRect(queue, 1f, 0.98f, 0.90f,
                0.045f + breathe * 0.035f,
                gx + 1, gy + 1, Math.max(0, filled - 2),
                Math.max(2, gh / 3));
    }

    @Override
    protected void leftPressed(int mouseX, int mouseY, int buttons) {
        notePointerPosition(mouseX, mouseY);
        if (pinButtonAt(mouseX, mouseY)) {
            autoClose.setPinned(!autoClose.isPinned(), System.nanoTime());
            return;
        }
        if (closeButtonAt(mouseX, mouseY)) {
            clearSelection();
            return;
        }
        int waypointButton = waypointButtonAt(mouseX, mouseY);
        if (waypointButton >= 0) {
            PickableUnit subject = selectedSubject();
            if (subject != null) {
                pressedSelectedWaypointUntil = System.nanoTime()
                        + ACTION_PRESS_NANOS;
                boolean creature = subject instanceof CreatureCellRenderable
                        && !((CreatureCellRenderable) subject).isItem();
                WaypointerBridge.toggle(subject.getId(), creature);
            }
            return;
        }
        if (inspectButtonAt(mouseX, mouseY)) {
            PickableUnit subject = selectedSubject();
            PlayerAction inspect = inspectAction();
            if (subject != null && inspect != null) {
                pressedInspectUntil = System.nanoTime() + ACTION_PRESS_NANOS;
                HighResHudApi.runAs(ActionOrigin.HUD,
                        () -> owner.sendAction(inspect, subject.getId()));
            }
            return;
        }
        if (inside(mouseX, mouseY, x + PORTRAIT_X, y + PORTRAIT_Y,
                PORTRAIT_WIDTH, PORTRAIT_HEIGHT)) {
            draggedPortrait = selectedPortrait;
            portraitDragX = mouseX;
            portraitDragY = mouseY;
            return;
        }
        if (actionPagerAt(mouseX, mouseY)) {
            pressedPagerUntil = System.nanoTime() + ACTION_PRESS_NANOS;
            advanceActionPage(1);
            return;
        }
        int actionIndex = actionIndexAt(mouseX, mouseY);
        if (actionIndex >= 0) {
            pressedActionIndex = actionIndex;
            pressedActionUntil = System.nanoTime() + ACTION_PRESS_NANOS;
            HighResHudApi.runAs(ActionOrigin.HUD,
                    () -> selectBar.pressButton(actionIndex));
            return;
        }
        dragger.leftPressed(mouseX, mouseY, buttons);
    }

    @Override
    protected void mouseDragged(int mouseX, int mouseY) {
        notePointerPosition(mouseX, mouseY);
        if (draggedPortrait != null) {
            draggedPortrait.rotate(mouseX - portraitDragX,
                    mouseY - portraitDragY);
            portraitDragX = mouseX;
            portraitDragY = mouseY;
            return;
        }
        dragger.mouseDragged(mouseX, mouseY);
    }

    @Override
    protected void leftReleased(int mouseX, int mouseY) {
        notePointerPosition(mouseX, mouseY);
        draggedPortrait = null;
        dragger.leftReleased(mouseX, mouseY);
    }

    @Override
    protected void mouseMoved(int mouseX, int mouseY) {
        notePointerPosition(mouseX, mouseY);
    }

    @Override
    protected void mouseExited() {
        autoClose.pointerAbsent(System.nanoTime());
    }

    /**
     * Repairs Wurm's missing component mouseExited callback when the pointer
     * moves from this bar directly into the empty game world. This is called
     * by the existing HUD mouse-move event, never from the render/game tick.
     */
    public void hudPointerMoved(HeadsUpDisplay currentHud,
                                int mouseX, int mouseY) {
        if (currentHud != owner || !autoClose.isPointerInside()) return;
        if (!contentVisible
                || !inside(mouseX, mouseY, x, y, width, height)) {
            autoClose.pointerAbsent(System.nanoTime());
        }
    }

    public boolean mouseWheeledAt(HeadsUpDisplay ownerHud, int mouseX,
                                  int mouseY, int wheelDelta) {
        if (ownerHud != owner || wheelDelta == 0) return false;
        notePointerPosition(mouseX, mouseY);
        if (selectedPortrait != null
                && inside(mouseX, mouseY, x + PORTRAIT_X, y + PORTRAIT_Y,
                PORTRAIT_WIDTH, PORTRAIT_HEIGHT)) {
            return selectedPortrait.zoom(wheelDelta);
        }
        if (inside(mouseX, mouseY,
                x + ACTION_SHELF_X, y + ACTION_SHELF_Y,
                ACTION_SHELF_WIDTH, ACTION_SHELF_HEIGHT)
                && state.actions().size() > ACTION_VISIBLE_SLOTS) {
            advanceActionPage(wheelDelta > 0 ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    protected void rightPressed(int mouseX, int mouseY, int buttons) {
        notePointerPosition(mouseX, mouseY);
        if (inside(mouseX, mouseY, x + PORTRAIT_X, y + PORTRAIT_Y,
                PORTRAIT_WIDTH, PORTRAIT_HEIGHT)) {
            requestSubjectPopup(selectedSubject(), mouseX, mouseY);
            return;
        }
        final WurmPopup popup = new WurmPopup(
                "highResSelectBarMenu", "High res select bar", mouseX, mouseY);
        popup.addSeparator();
        dragger.addContextMenuEntry(popup);
        popup.addButton(popup.new WPopupLiveButton(
                showHotkeys ? "Hide action hotkeys" : "Show action hotkeys") {
            @Override
            protected void handleLeftClick() {
                showHotkeys = !showHotkeys;
            }
        });
        popup.addButton(popup.new WPopupLiveButton(
                showDistance ? "Distance: ON" : "Distance: OFF") {
            @Override
            protected void handleLeftClick() {
                showDistance = !showDistance;
                updatePresentation(selectedSubject());
            }
        });
        if (selectedPortrait != null) {
            popup.addButton(popup.new WPopupLiveButton(
                    "Resume portrait motion") {
                @Override
                protected void handleLeftClick() {
                    selectedPortrait.resumeMotion();
                }
            });
        }
        owner.showPopupComponent(popup);
    }

    private void requestSubjectPopup(PickableUnit subject,
                                     int mouseX, int mouseY) {
        if (subject != null) {
            owner.popupRequested(mouseX, mouseY,
                    subjectName(subject), subject.getId());
        }
    }

    @Override
    public void pick(PickData pickData, int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, x, y, width, height)) return;
        autoClose.pointerPresent();
        if (pinButtonAt(mouseX, mouseY)) {
            pickData.addText(autoClose.isPinned()
                    ? "Pin: ON — Select Bar stays open"
                    : "Pin: OFF — closes 5 seconds after pointer leaves");
            return;
        }
        if (closeButtonAt(mouseX, mouseY)) {
            pickData.addText("Deselect and close");
            return;
        }
        int waypointButton = waypointButtonAt(mouseX, mouseY);
        if (waypointButton >= 0) {
            PickableUnit markedSubject = selectedSubject();
            boolean marked = markedSubject != null
                    && WaypointerBridge.isMarked(markedSubject.getId());
            pickData.addText((marked ? "Remove" : "Add 15-minute")
                    + " Waypointer mark for selected object");
            return;
        }
        if (inspectButtonAt(mouseX, mouseY)) {
            pickData.addText("Inspect selected object");
            return;
        }
        if (inside(mouseX, mouseY, x + PORTRAIT_X, y + PORTRAIT_Y,
                PORTRAIT_WIDTH, PORTRAIT_HEIGHT)) {
            pickData.addText("Selected portrait: drag to rotate, wheel to zoom");
            pickData.addText("Right-click for the native object menu");
            return;
        }
        PickableUnit subject = selectedSubject();
        if (subject != null && inside(mouseX, mouseY,
                x + NAME_TEXT_X, y + HEADER_TEXT_Y,
                NAME_TEXT_WIDTH, HEADER_TEXT_HEIGHT)) {
            pickData.addText(subjectName(subject));
            return;
        }
        if (actionPagerAt(mouseX, mouseY)) {
            pickData.addText("More Select actions: click or use the mouse wheel");
            return;
        }
        int actionIndex = actionIndexAt(mouseX, mouseY);
        List<PlayerAction> actions = state.actions();
        if (actionIndex >= 0) {
            PlayerAction action = actions.get(actionIndex);
            SelectBarButtonProperty property = actionProperties.get(
                    action.getId());
            pickData.addText(ActionTooltip.resolve(
                    property == null ? null : property.getName(),
                    action.getName(), action.getId()));
            String key = binding(actionIndex);
            if (!key.isEmpty()) pickData.addText("Hotkey: " + key);
            return;
        }
        if (showDistance && inside(mouseX, mouseY,
                x, y + PANEL_HEIGHT, PANEL_WIDTH,
                DISTANCE_FOOTER_HEIGHT)) {
            int metres = distance(subject);
            pickData.addText(metres < 0
                    ? "Distance unavailable" : "Distance: " + metres + " m");
            return;
        }
        pickData.addText(subject == null ? "Nothing selected" : subjectName(subject));
    }

    @Override
    public void restorePositionHints(WindowPosition position) {
        setPosition(position.x, position.y);
        dragger.setDisabled((position.flags & 1) != 0);
        showHotkeys = (position.flags & 8) == 0;
        showDistance = (position.flags & 16) == 0;
        autoClose.setPinned((position.flags & 32) == 0, System.nanoTime());
        updatePresentation(selectedSubject());
        owner.toggleComponent(this, (position.flags & 2) == 0);
    }

    @Override
    public WindowPosition createPositionHints() {
        int flags = 0;
        if (dragger.isDisabled()) flags |= 1;
        if (!owner.isComponentEnabled(this)) flags |= 2;
        if (!showHotkeys) flags |= 8;
        if (!showDistance) flags |= 16;
        if (!autoClose.isPinned()) flags |= 32;
        return new WindowPosition(x, y, flags);
    }

    private PickableUnit selectedSubject() {
        PickableUnit current = selectBar == null ? null : selectBar.selectedUnit;
        long now = System.nanoTime();
        if (current != null) {
            actionsClearedForNoSelection = false;
            latchedSelectedSubject = current;
            selectedLastSeen = now;
            return current;
        }
        if (latchedSelectedSubject != null
                && now - selectedLastSeen <= SELECTION_GRACE_NANOS) {
            return latchedSelectedSubject;
        }
        latchedSelectedSubject = null;
        if (!actionsClearedForNoSelection) {
            state.clearActions();
            actionsClearedForNoSelection = true;
        }
        return null;
    }

    /** Mirrors the stock SelectBar progress callback. */
    public void progressChanged(float progress, String label,
                                boolean changeColor) {
        state.mirrorProgress(progress, label, changeColor);
    }

    /** Associates the next exact Examine response with its server target ID. */
    public void actionTargeted(PlayerAction action, long targetId) {
        PickableUnit selected = selectedSubject();
        if (action != null && action.getId() == PlayerAction.EXAMINE.getId()
                && selected != null
                && selected.getId() == targetId) {
            selectedExamine.expect(targetId);
        }
    }

    /**
     * Captures QL/Dam and type-specific state. Returns true only for lines
     * belonging to our automatic, deliberately silent Examine probe.
     */
    public boolean serverText(String title, String message) {
        PickableUnit selected = selectedSubject();
        long selectedId = selected == null
                ? Long.MIN_VALUE : selected.getId();
        SelectedExamineState.Observation observation =
                selectedExamine.observeResultFor(selectedId, title, message);
        long now = System.nanoTime();
        boolean silent = silentExamineId != Long.MIN_VALUE
                && now <= silentExamineUntil;

        if (observation == SelectedExamineState.Observation.EXAMINE) {
            SelectedExamineState.Snapshot snapshot =
                    selectedExamine.get(selectedId);
            // Creature descriptions legitimately have no item QL/Dam. Ground
            // items and creature-backed items (vehicles and corpses) are not
            // complete until both values actually arrived; otherwise a
            // generic first Examine line would permanently stop retries.
            if (!isItemSubject(selected)
                    || snapshot != null
                    && snapshot.hasQualityAndDamage()) {
                examineRetry.completed(selectedId);
            }
            if (silent) {
                silentExamineResponseStarted = true;
                silentExamineLastLineAt = now;
            } else {
                clearSilentExamine();
            }
            return silent;
        }
        if (observation
                == SelectedExamineState.Observation.RETRYABLE_FAILURE) {
            if (!silent) clearSilentExamine();
            return silent;
        }
        if (silent && silentExamineResponseStarted
                && SelectedExamineState.isEventTitle(title)
                && now - silentExamineLastLineAt
                <= SILENT_EXAMINE_TAIL_NANOS) {
            // Traits, runes, and other Examine continuation lines do not carry
            // a request id. They arrive immediately after the primary line.
            silentExamineLastLineAt = now;
            return true;
        }
        if (!silent || silentExamineResponseStarted) clearSilentExamine();
        return false;
    }

    /**
     * Sends a quiet Examine for each newly selected world subject. A failed or
     * missing response remains pending and is retried, with backoff, after the
     * subject enters normal interaction range.
     */
    private void serviceSelectedExamine(PickableUnit subject) {
        if (!isAutoExamineSubject(subject)) {
            resetAutoExamine();
            return;
        }
        long id = subject.getId();
        if (owner.getWorld() == null
                || owner.getWorld().getServerConnection() == null) return;

        long now = System.nanoTime();
        int metres = distance(subject);
        boolean inRange = metres >= 0
                && metres <= AUTO_EXAMINE_RETRY_RANGE_METRES;
        if (!examineRetry.shouldAttempt(id, inRange, now)) return;

        HiddenExamineCoordinator.Claim claim =
                HighResHudRuntime.claimHiddenExamine("selectbar", id);
        if (claim == HiddenExamineCoordinator.Claim.WAIT) return;

        silentExamineId = id;
        silentExamineUntil = now + SILENT_EXAMINE_NANOS;
        silentExamineResponseStarted = false;
        silentExamineLastLineAt = 0L;
        selectedExamine.expect(id);
        // Use the same source-item and action packet path as a native Examine.
        // Some item behaviours do not answer the narrower direct packet path
        // consistently, especially creature-backed corpses on the ground.
        if (claim == HiddenExamineCoordinator.Claim.SEND) {
            HighResHudApi.runAs(ActionOrigin.INTERNAL,
                    () -> owner.sendAction(PlayerAction.EXAMINE, id));
        }
        examineRetry.attempted(id, now);
    }

    private static boolean isAutoExamineSubject(PickableUnit subject) {
        CellRenderable cell = worldCell(subject);
        return cell != null && !(cell instanceof PlayerCellRenderable);
    }

    private static boolean isItemSubject(PickableUnit subject) {
        CellRenderable cell = worldCell(subject);
        return cell instanceof GroundItemCellRenderable
                || cell instanceof CreatureCellRenderable
                && ((CreatureCellRenderable) cell).isItem();
    }

    private static CellRenderable worldCell(PickableUnit subject) {
        PickableUnit current = subject;
        if (current instanceof SubPickableUnit) {
            current = ((SubPickableUnit) current).getParent();
        }
        return current instanceof CellRenderable
                ? (CellRenderable) current : null;
    }

    private void resetAutoExamine() {
        examineRetry.clear();
        clearSilentExamine();
    }

    private void clearSilentExamine() {
        silentExamineId = Long.MIN_VALUE;
        silentExamineUntil = 0L;
        silentExamineResponseStarted = false;
        silentExamineLastLineAt = 0L;
    }

    private void updatePresentation(PickableUnit selected) {
        contentVisible = selected != null;
        autoClose.setOpen(contentVisible, System.nanoTime());
        if (!contentVisible) {
            setSize(0, 0);
            return;
        }
        setSize(BASE_WIDTH, PANEL_HEIGHT
                + (showDistance ? DISTANCE_FOOTER_HEIGHT : 0));
    }

    private void notePointerPosition(int mouseX, int mouseY) {
        if (contentVisible && inside(mouseX, mouseY, x, y, width, height)) {
            autoClose.pointerPresent();
        } else {
            autoClose.pointerAbsent(System.nanoTime());
        }
    }

    private String subjectName(PickableUnit subject) {
        if (subject == null) return "";
        String name;
        if (selectBar != null && subject == selectBar.selectedUnit
                && !clean(selectBar.selectedUnitName).isEmpty()) {
            name = clean(selectBar.selectedUnitName);
        } else if (subject instanceof CreatureCellRenderable) {
            String creatureName = ((CreatureCellRenderable) subject)
                    .getCreatureData().getName();
            name = !clean(creatureName).isEmpty() ? clean(creatureName)
                    : clean(subject.getHoverName());
        } else {
            name = clean(subject.getHoverName());
        }
        if (name.isEmpty()) name = "Unknown object";
        return name;
    }

    /**
     * Butchering removes and re-adds the corpse renderable with the same id.
     * SelectBar already has a native hand-off path; arm it before the action
     * reaches the server so the replacement becomes selected automatically.
     */
    public void keepSelectedForReplacement() {
        PickableUnit selected = selectBar == null ? null : selectBar.selectedUnit;
        if (selected != null) selectBar.keepSelectedItem(selected.getId());
    }

    /** Records probe work without mutating HUD component lists during gameTick. */
    private void observeInspectSubject(PickableUnit subject) {
        long id = subject == null ? Long.MIN_VALUE : subject.getId();
        if (id == inspectSubjectId) return;
        inspectSubjectId = id;
        inspectProbeRequestId = -1;
        resolvedInspectAction = null;
        pendingInspectSubject = subject;
    }

    /** Runs before HeadsUpDisplay.beginRender, outside component iteration. */
    public void serviceInspectProbe() {
        PickableUnit subject = pendingInspectSubject;
        pendingInspectSubject = null;
        if (subject == null || subject.getId() != inspectSubjectId) return;
        owner.popupRequested(-10000, -10000,
                subjectName(subject), subject.getId());
        inspectProbeRequestId = HighResFocusBarMod.popupRequestId(owner);
        owner.clearAllPopups();
    }

    public boolean inspectProbeReceived(byte responseId, List actions) {
        if (inspectProbeRequestId < 0
                || inspectProbeRequestId
                != FocusMath.unsignedRequestId(responseId)) return false;
        // clearAllPopups deliberately advances the HUD request id after the
        // hidden probe was sent. Match the recorded wire id, not the HUD's
        // newer value, so the response remains usable without showing a menu.
        inspectProbeRequestId = -1;
        if (actions != null) {
            for (Object value : actions) {
                if (!(value instanceof PlayerAction)) continue;
                PlayerAction action = (PlayerAction) value;
                if (isInspectAction(action)) {
                    resolvedInspectAction = action;
                    break;
                }
            }
        }
        return true;
    }

    private int distance(PickableUnit subject) {
        CellRenderable cell = worldCell(subject);
        if (cell == null || owner.getWorld() == null) return -1;
        return FocusMath.horizontalDistance(owner.getWorld().getPlayerPosX(),
                owner.getWorld().getPlayerPosY(), cell.getXPos(), cell.getYPos());
    }

    private String binding(int index) {
        if (index < 0 || index >= SELECT_KEYS.length || owner.console == null) return "";
        try {
            String raw = clean(owner.console.getFirstBinding(SELECT_KEYS[index]));
            raw = raw.replace("CONTROL", "C").replace("CTRL", "C")
                    .replace("SHIFT", "S").replace("ALT", "A")
                    .replace("+", "");
            return raw.length() <= 4 ? raw : raw.substring(0, 4);
        } catch (Throwable ignored) {
            return "";
        }
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
        // Include the one-pixel shadow in the optical width. Without this,
        // every centred caption appears one pixel to the right of its cell.
        int opticalWidth = font.getWidth(value) + 1;
        int left = px + Math.max(0, (available - opticalWidth) / 2);
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

    private static String percent(float value) {
        return Math.round(clamp(value) * 100f) + "%";
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f,
                Float.isFinite(value) ? value : 0f));
    }

    private static boolean inside(int mouseX, int mouseY,
                                  int rx, int ry, int rw, int rh) {
        return mouseX >= rx && mouseY >= ry
                && mouseX < rx + rw && mouseY < ry + rh;
    }
}
