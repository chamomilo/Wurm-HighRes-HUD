package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.gui.text.HighResTextFonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.resources.textures.ResourceTexture;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.client.resources.textures.Texture;
import com.wurmonline.client.settings.WindowPosition;
import com.wurmonline.client.stats.Stats;
import org.highreshealthbar.client.HealthBarVisibilityPreferences;
import org.highreshealthbar.client.HighResHealthBarMod;
import org.highreshealthbar.client.HighResHealthBarSettings;
import org.highreshealthbar.client.MountStatusText;
import org.highreshealthbar.client.SleepBonusToggleTracker;
import org.highreshud.client.ui.HudCaptionGroup;
import org.highreshud.client.ui.HudSkin;
import org.chamomilo.wurm.ui.v1.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.wurmonline.client.renderer.gui.HighResHealthBarLayout.*;
import static com.wurmonline.client.renderer.gui.HudEffectFormulas.*;

public final class HighResHealthBar extends HealthBar {
    private static final long BUBBLE_DURATION = 1_200_000_000L;
    private static final long HIT_FLASH_DURATION = 450_000_000L;
    private static final long SPENT_FLASH_DURATION = 750_000_000L;
    private static final long RIDE_SCAN_INTERVAL = 250_000_000L;
    private static final float PORTRAIT_FACE_CROP_WIDTH = 0.23f;
    private static final float PORTRAIT_FACE_CROP_HEIGHT = 0.24f;
    private static final Field BASE_DRAGGER = findBaseDragger();
    private static final Field SERVER_CREATURES = findServerCreatures();

    private final HighResHealthBarMod controller;
    private final TextFont highText;
    private final TextFont highBold;
    private final TextFont titleText;
    private final TextFont smallText;
    private final HeaderLayout.TextWidth headerTextWidth;
    private final PortraitPoseController portraitPose;
    private final float[] previous = filled(GROWTH_GAUGE_COUNT, -1f);
    private final long[] bubblesUntil = new long[GROWTH_GAUGE_COUNT];
    private final float[] previousGaugeValues = filled(GAUGE_COUNT, -1f);
    private final float[] spentFlashFrom = new float[GAUGE_COUNT];
    private final float[] spentFlashTo = new float[GAUGE_COUNT];
    private final long[] spentFlashUntil = new long[GAUGE_COUNT];
    private final ChamomiloUiV1Canvas ui = new ChamomiloUiV1Canvas(this);
    private final HighResHudActionButton sleepButton;
    private final HighResHudActionButton rideButton;
    private ResourceTexture bloodTexture;
    private float previousDamage = -1f;
    private long hitFlashUntil;
    private boolean deathBloodLatched;
    private boolean showNumberValues = true;
    private boolean showTitles = true;
    private boolean showRide = true;
    private boolean showHitchedAnimals = true;
    private RideStatusSnapshot rideStatus = RideStatusSnapshot.empty();
    private long nextRideScan;
    private HeaderLayout headerLayout = HeaderLayout.hidden();
    private UiFrameGrid mountGrid;
    private int mountGridRows;

    public HighResHealthBar(HealthBar original, HighResHealthBarMod controller) {
        super(original.player);
        this.controller = controller;
        this.showName = original.showName;
        this.showSpeed = original.showSpeed;
        this.showFps = original.showFps;
        this.showSB = true;
        this.normalTitle = original.normalTitle;
        this.meditationTitle = original.meditationTitle;
        this.highText = HighResTextFonts.regular();
        this.highBold = HighResTextFonts.bold();
        this.titleText = HighResTextFonts.title();
        this.smallText = HighResTextFonts.small();
        this.headerTextWidth = new HeaderLayout.TextWidth() {
            @Override
            public int measure(String value) {
                return titleText.getWidth(value);
            }
        };
        this.portraitPose = new PortraitPoseController(
                HighResHealthBarSettings.portraitRotation,
                new java.util.Random(), System.nanoTime());
        this.text = highText;
        this.textBold = highBold;
        this.sleepButton = new HighResHudActionButton(this, SLEEP_BUTTON_WIDTH,
                SLEEP_BUTTON_HEIGHT, new HudCaptionGroup(SLEEP_BUTTON_WIDTH,
                SLEEP_BUTTON_HEIGHT, HudSkin.COMPACT, "Activate", "Deactivate"),
                "Activate", () -> controller.toggleSleepBonus());
        this.rideButton = new HighResHudActionButton(this, DISEMBARK_BUTTON_WIDTH,
                DISEMBARK_BUTTON_HEIGHT, new HudCaptionGroup(DISEMBARK_BUTTON_WIDTH,
                DISEMBARK_BUTTON_HEIGHT, HudSkin.COMPACT, "Disembark"),
                "Disembark", () -> controller.disembark());
        this.bloodTexture = texture("img.highreshealthbar.blood");
        refreshRideStatus();
        refreshHeaderLayout();
        createRenderer();
        setPosition(Math.max(8, original.x), Math.max(8, original.y + original.height + 8));
    }

    private static ResourceTexture texture(String resource) {
        return ResourceTextureLoader.getNowrapLinearTexture(resource);
    }

    @Override
    public void createRenderer() {
        super.createRenderer();
        updateHighResSize();
    }

    @Override
    public void toggleName() {
        showName = !showName;
        refreshHeaderLayout();
        updateHighResSize();
    }

    @Override
    public WindowPosition createPositionHints() {
        WindowPosition base = super.createPositionHints();
        int flags = HealthBarVisibilityPreferences.store(base.flags,
                showTitles, showNumberValues, showRide, showHitchedAnimals);
        return new WindowPosition(base.x, base.y, base.width, base.height, flags);
    }

    @Override
    public void restorePositionHints(WindowPosition position) {
        if (HealthBarVisibilityPreferences.hasSavedSettings(position.flags)) {
            showTitles = HealthBarVisibilityPreferences.showTitles(position.flags);
            showNumberValues = HealthBarVisibilityPreferences
                    .showNumberValues(position.flags);
            showRide = HealthBarVisibilityPreferences.showRide(position.flags);
            showHitchedAnimals = HealthBarVisibilityPreferences
                    .showHitchedAnimals(position.flags);
        }
        super.restorePositionHints(position);
        refreshHeaderLayout();
        updateHighResSize();
    }

    @Override
    public void toggleSpeed() {
        // Replaced by the single Toggle number values command in this module.
    }

    @Override
    public void toggleFps() {
        // Replaced by the single Toggle number values command in this module.
    }

    @Override
    public void toggleSB() {
        // The high-resolution module always includes sleep bonus. The stock
        // component still handles its own Toggle SB setting independently.
        showSB = true;
    }

    @Override
    public void gameTick() {
        super.gameTick();
        showSB = true;
        controller.gameTick();
        refreshRideStatus();
        refreshHeaderLayout();
        updateHighResSize();
        portraitPose.tick(System.nanoTime());
    }

    private void updateHighResSize() {
        int nextHeight = frameHeight() + (showNumberValues ? FOOTER_HEIGHT : 0);
        if (width != PANEL_WIDTH || height != nextHeight) setSize(PANEL_WIDTH, nextHeight);
    }

    private int contentY() {
        return y + titleHeight();
    }

    private int mainFrameHeight() {
        return CONTENT_HEIGHT + titleHeight();
    }

    private int frameHeight() {
        return mainFrameHeight() + mountBlockHeight();
    }

    private int mountBlockHeight() {
        return MOUNT_ROW_HEIGHT
                * rideStatus.rowCount(showRide, showHitchedAnimals);
    }

    private int titleHeight() {
        return headerLayout.height();
    }

    @Override
    protected void renderComponent(Queue queue, float ignoredAlpha) {
        int contentY = contentY();
        float damageValue = clamp(damage.getValue(0f));
        float healthValue = clamp(1f - damageValue);

        // Back only the body. The title zone must remain clear so the separate
        // dynamic nameplates do not merge into a full-width dark strip.
        HudSkin.healthBack(ui.begin(queue), x, contentY);
        renderPortrait(queue, x, contentY);
        renderTitlePlates(queue);
        renderBloodVignette(queue, x, contentY, healthValue);

        int left = x + GAUGE_X;

        float staminaValue = clamp(stamina.getValue(0f));
        float waterValue = clamp(water.getValue(0f));
        float foodValue = clamp(food.getValue(0f));
        // Solid colours deliberately replace the stock atlas grain. Depth and
        // motion are rendered as lightweight overlays below.
        drawSolidGauge(queue, left, contentY + STAMINA_Y,
                GAUGE_WIDTH, STAMINA_HEIGHT,
                staminaValue, 0.10f, 0.68f, 0.22f,
                GAUGE_STAMINA, 0.13f, 0.72f, 0.27f);
        drawDamageOverlay(queue, left, contentY + STAMINA_Y,
                GAUGE_WIDTH, STAMINA_HEIGHT, damageValue);
        drawHitFlash(queue, left, contentY + STAMINA_Y,
                GAUGE_WIDTH, STAMINA_HEIGHT, damageValue);
        HudSkin.glass(ui, HudSkin.healthGauge(x, contentY, GAUGE_STAMINA));

        drawSolidGauge(queue, left, contentY + WATER_FOOD_Y,
                WATER_WIDTH, WATER_FOOD_HEIGHT,
                waterValue, 0.15f, 0.47f, 0.78f,
                GAUGE_WATER, 0.10f, 0.57f, 0.82f);
        drawSolidGauge(queue, left + WATER_WIDTH + WATER_FOOD_GAP,
                contentY + WATER_FOOD_Y, FOOD_WIDTH, WATER_FOOD_HEIGHT,
                foodValue, 0.08f, 0.62f, 0.22f,
                GAUGE_FOOD, 0.30f, 0.78f, 0.34f);

        if (showCCFP()) {
            drawSolidGauge(queue, left, contentY + CCFP_Y,
                    CCFP_WIDTHS[0], CCFP_HEIGHT,
                    clamp(calories.getValue(0f)), 0.58f, 0.24f, 0.66f,
                    GAUGE_CALORIES, 0f, 0f, 0f);
            drawSolidGauge(queue, left + CCFP_OFFSETS[1],
                    contentY + CCFP_Y, CCFP_WIDTHS[1], CCFP_HEIGHT,
                    clamp(carbs.getValue(0f)), 0.26f, 0.55f, 0.72f,
                    GAUGE_CARBS, 0f, 0f, 0f);
            drawSolidGauge(queue, left + CCFP_OFFSETS[2],
                    contentY + CCFP_Y, CCFP_WIDTHS[2], CCFP_HEIGHT,
                    clamp(fats.getValue(0f)), 0.72f, 0.59f, 0.26f,
                    GAUGE_FATS, 0f, 0f, 0f);
            drawSolidGauge(queue, left + CCFP_OFFSETS[3],
                    contentY + CCFP_Y, CCFP_WIDTHS[3], CCFP_HEIGHT,
                    clamp(proteins.getValue(0f)), 0.48f, 0.70f, 0.29f,
                    GAUGE_PROTEINS, 0f, 0f, 0f);
        }
        float sleep = Math.min(1f, player.getSleepBonusSecondsLeft() / 18000f);
        float shade = player.isSleepBonusActive() ? 1f : 0.5f;
        drawSolidGauge(queue, left, contentY + SLEEP_BONUS_Y,
                GAUGE_WIDTH, SLEEP_BONUS_HEIGHT,
                sleep, 0.47f * shade, 0.53f * shade, 0.58f * shade,
                GAUGE_SLEEP, 0f, 0f, 0f);
        if (isPriest) {
            drawSolidGauge(queue, left, contentY + FAVOR_Y,
                    GAUGE_WIDTH, FAVOR_HEIGHT,
                    clamp(favorValue / Math.max(1f, faithValue)),
                    0.74f, 0.13f, 0.78f,
                    GAUGE_FAVOR, 0f, 0f, 0f);
        }

        renderFrame(queue);
        drawGaugeLabels(queue, contentY, left);
        drawSleepBonusControl(queue, contentY, left);
        renderMountStatus(queue);
        if (showNumberValues) {
            drawFooter(queue);
        }
        // User requirement: this is deliberately the final paint operation.
        drawNameAndTitlesLast(queue);
    }

    private void renderFrame(Queue queue) {
        HudSkin.healthFront(ui.begin(queue), x, contentY());
    }

    private void renderTitlePlates(Queue queue) {
        if (!showName) return;

        String[] lines = headerLayout.lines();
        for (int index = 0; index < lines.length; index++) {
            int width = Math.max(NAMEPLATE_MIN_WIDTH,
                    Math.min(PANEL_WIDTH - NAMEPLATE_X - 2,
                            titleText.getWidth(lines[index])
                                    + NAMEPLATE_TEXT_PADDING));
            int py = y + index * (TITLE_LINE_HEIGHT + TITLE_LINE_GAP);
            UiHudPainter.nameplate(ui.begin(queue), false, HudSkin.SCALE, 1.0f, x + NAMEPLATE_X, py, width);
            UiHudPainter.nameplate(ui, true, HudSkin.SCALE, 1.0f, x + NAMEPLATE_X, py, width);
        }
    }

    private void renderMountStatus(Queue queue) {
        CreatureCellRenderable mount = currentMount();
        if (mount == null) return;
        if (!mount.isItem() && !showRide) return;

        int mountY = y + mainFrameHeight();
        List<CreatureCellRenderable> hitched =
                rideStatus.visibleHitched(showHitchedAnimals);
        int rowCount = rideStatus.rowCount(showRide, showHitchedAnimals);
        if (rowCount > 0) {
            HudSkin.well(ui.begin(queue), x, mountY, PANEL_WIDTH, rowCount * MOUNT_ROW_HEIGHT);
            if (mountGrid == null || mountGridRows != rowCount) {
                int[] weights = new int[rowCount];
                int[][] columns = new int[rowCount][];
                java.util.Arrays.fill(weights, 1);
                for (int i = 0; i < rowCount; i++) columns[i] = new int[]{1};
                mountGrid = new UiFrameGrid(3, weights, columns);
                mountGridRows = rowCount;
            }
            mountGrid.foreground(ui, 1.0f, x, mountY, PANEL_WIDTH, rowCount * MOUNT_ROW_HEIGHT);
        }

        if (mount.isItem()) {
            int animalRowOffset = 0;
            if (showRide) {
                paintRideRow(queue, vehicleStatusLine(mount), mountY);
                animalRowOffset = 1;
            }
            for (int index = 0; index < hitched.size(); index++) {
                renderCreatureHealthRow(queue, hitched.get(index),
                        mountY + (index + animalRowOffset) * MOUNT_ROW_HEIGHT);
            }
        } else {
            paintRideRow(queue, "Riding " + mountName(mount), mountY);
            renderCreatureHealthRow(queue, mount, mountY + MOUNT_ROW_HEIGHT);
        }
    }

    private void paintRideRow(Queue queue, String value, int rowY) {
        int buttonX = disembarkButtonX();
        int textX = x + MOUNT_GAUGE_X + 3;
        int maxTextWidth = Math.max(20, buttonX - textX - 4);
        paintMountRow(queue, value, rowY,
                0.97f, 0.92f, 0.78f, maxTextWidth);
        drawDisembarkButton(queue, rowY);
    }

    private void drawDisembarkButton(Queue queue, int rowY) {
        int buttonX = disembarkButtonX();
        int buttonY = rowY + DISEMBARK_BUTTON_Y;
        drawActionButton(queue, buttonX, buttonY,
                DISEMBARK_BUTTON_WIDTH, DISEMBARK_BUTTON_HEIGHT,
                "Disembark", true);
    }

    private void drawSleepBonusControl(Queue queue, int contentY, int left) {
        boolean active = player.isSleepBonusActive();
        int cooldown = controller.sleepBonusActivationCooldownSeconds();
        boolean enabled = controller.canToggleSleepBonus();
        int buttonX = sleepBonusButtonX(left);
        int buttonY = contentY + SLEEP_BONUS_Y + SLEEP_BUTTON_Y;
        drawActionButton(queue, buttonX, buttonY,
                SLEEP_BUTTON_WIDTH, SLEEP_BUTTON_HEIGHT,
                active ? "Deactivate" : "Activate", enabled);

        if (cooldown > 0) {
            String timer = SleepBonusToggleTracker.formatCountdown(cooldown);
            int timerX = buttonX - SLEEP_TIMER_GAP - smallText.getWidth(timer);
            int baseline = buttonY
                    + (SLEEP_BUTTON_HEIGHT + smallText.getHeight()) / 2;
            paintShadowed(smallText, queue, timer, timerX, baseline,
                    active ? 0.98f : 0.82f,
                    active ? 0.78f : 0.64f,
                    active ? 0.28f : 0.43f);
        }
    }

    private void drawActionButton(Queue queue, int buttonX, int buttonY,
                                  int buttonWidth, int buttonHeight,
                                  String label, boolean enabled) {
        HighResHudActionButton button = "Disembark".equals(label) ? rideButton : sleepButton;
        button.setLocation(buttonX, buttonY, buttonWidth, buttonHeight);
        button.caption(label, enabled);
        button.render(queue, 1.0f);
    }

    private int disembarkButtonX() {
        return x + PANEL_WIDTH - DISEMBARK_BUTTON_RIGHT
                - DISEMBARK_BUTTON_WIDTH;
    }

    private boolean overDisembarkButton(int mouseX, int mouseY) {
        if (!showRide || currentMount() == null) return false;
        int rowY = y + mainFrameHeight();
        return inside(mouseX, mouseY,
                disembarkButtonX(), rowY + DISEMBARK_BUTTON_Y,
                DISEMBARK_BUTTON_WIDTH, DISEMBARK_BUTTON_HEIGHT);
    }

    private static int sleepBonusButtonX(int left) {
        return left + GAUGE_WIDTH - SLEEP_BUTTON_RIGHT - SLEEP_BUTTON_WIDTH;
    }

    private boolean overSleepBonusButton(int mouseX, int mouseY) {
        int contentY = contentY();
        return inside(mouseX, mouseY,
                sleepBonusButtonX(x + GAUGE_X),
                contentY + SLEEP_BONUS_Y + SLEEP_BUTTON_Y,
                SLEEP_BUTTON_WIDTH, SLEEP_BUTTON_HEIGHT);
    }

    private void renderCreatureHealthRow(Queue queue,
                                         CreatureCellRenderable creature,
                                         int rowY) {
        float health = clamp(creature.getPercentHealth() / 100f);
        drawSolidGauge(queue, x + MOUNT_GAUGE_X,
                rowY + MOUNT_GAUGE_Y,
                MOUNT_GAUGE_WIDTH, MOUNT_GAUGE_HEIGHT,
                health, 0.10f, 0.62f, 0.22f,
                -1, 0f, 0f, 0f);
        paintMountRow(queue, mountName(creature) + "  Health: "
                        + percent(health), rowY,
                0.96f, 0.96f, 0.93f);
    }

    private void paintMountRow(Queue queue, String value, int rowY,
                               float red, float green, float blue) {
        paintMountRow(queue, value, rowY, red, green, blue,
                MOUNT_GAUGE_WIDTH - 6);
    }

    private void paintMountRow(Queue queue, String value, int rowY,
                               float red, float green, float blue,
                               int maxTextWidth) {
        int baseline = rowY
                + (MOUNT_ROW_HEIGHT + smallText.getHeight()) / 2;
        paintShadowed(smallText, queue,
                fit(smallText, value, maxTextWidth),
                x + MOUNT_GAUGE_X + 3, baseline, red, green, blue);
    }

    private CreatureCellRenderable currentMount() {
        return rideStatus.mount();
    }

    private static String mountName(CreatureCellRenderable mount) {
        if (mount == null || mount.getCreatureData() == null) return "mount";
        String name = mount.getCreatureData().getName();
        String clean = MountStatusText.cleanCreatureName(name);
        return clean.isEmpty() ? "mount" : clean;
    }

    private String vehicleStatusLine(CreatureCellRenderable vehicle) {
        String details = vehicleDetails(vehicle);
        String[] stats = controller.vehicleStats(vehicle.getId(), details);
        return "Riding " + mountName(vehicle)
                + "  QL " + stats[0] + ", Dam " + stats[1];
    }

    private static String vehicleDetails(CreatureCellRenderable vehicle) {
        if (vehicle == null || vehicle.getCreatureData() == null) return "";
        StringBuilder details = new StringBuilder();
        appendDetail(details, vehicle.getCreatureData().getHoverText());
        appendDetail(details, vehicle.getCreatureData().getDescription());
        appendDetail(details, vehicle.getCreatureData().getName());
        return details.toString();
    }

    private static void appendDetail(StringBuilder target, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (target.length() > 0) target.append(' ');
        target.append(value.trim());
    }

    private void refreshRideStatus() {
        CreatureCellRenderable mount = player == null
                ? null : player.getCarrierCreature();
        if (mount == null || !mount.isItem()) {
            rideStatus = RideStatusSnapshot.of(mount,
                    Collections.<CreatureCellRenderable>emptyList());
            nextRideScan = 0L;
            return;
        }

        long now = System.nanoTime();
        if (rideStatus.mount() == mount && now < nextRideScan) return;
        List<CreatureCellRenderable> hitched = scanHitchedAnimals(mount);
        rideStatus = RideStatusSnapshot.of(mount, hitched);
        nextRideScan = now + RIDE_SCAN_INTERVAL;
    }

    private List<CreatureCellRenderable> scanHitchedAnimals(
            CreatureCellRenderable vehicle) {
        if (vehicle == null || SERVER_CREATURES == null) {
            return Collections.emptyList();
        }
        World world = controller.world();
        if (world == null || world.getServerConnection() == null) {
            return Collections.emptyList();
        }
        try {
            ServerConnectionListenerClass listener = world.getServerConnection()
                    .getServerConnectionListener();
            Object rawCreatures = SERVER_CREATURES.get(listener);
            if (!(rawCreatures instanceof Map)) return Collections.emptyList();
            List<CreatureCellRenderable> result = new ArrayList<>();
            for (Object value : ((Map<?, ?>) rawCreatures).values()) {
                if (!(value instanceof CreatureCellRenderable)) continue;
                CreatureCellRenderable candidate =
                        (CreatureCellRenderable) value;
                if (candidate != null && candidate != vehicle
                        && !candidate.isItem()
                        && candidate.getCarrierCreature() == vehicle
                        && candidate.shouldHitch()) {
                    result.add(candidate);
                }
            }
            Collections.sort(result, new Comparator<CreatureCellRenderable>() {
                @Override
                public int compare(CreatureCellRenderable left,
                                   CreatureCellRenderable right) {
                    return Long.compare(left.getId(), right.getId());
                }
            });
            return result;
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    private void renderPortrait(Queue queue, int portraitX, int portraitY) {
        controller.requestPortrait(player.getPlayerBody(),
                portraitPose.rotation());
        Texture portrait = controller.portraitTexture();
        if (portrait == null) return;

        float cropWidth = portraitCropWidth();
        float cropHeight = portraitCropHeight();
        float cropLeft = 0.5f - cropWidth * 0.5f;

        // PaperDollRenderer's offscreen target is bottom-up. These V coordinates
        // copy CharacterWindow's 1 -> -1 convention. Anchoring every crop at
        // V=1 keeps the crown/helmet at the upper edge while zoom changes only
        // how much of the body remains visible below it. All samples stay in
        // the valid [0, 1] UV range.
        Renderer.texturedQuadAlphaBlend(queue, portrait,
                1f, 1f, 1f, 1f,
                portraitX + PORTRAIT_APERTURE_X,
                portraitY + PORTRAIT_APERTURE_Y,
                PORTRAIT_APERTURE_WIDTH, PORTRAIT_APERTURE_HEIGHT,
                cropLeft,
                1f,
                cropWidth, -cropHeight);
    }

    private float portraitCropWidth() {
        return lerp(HighResHealthBarSettings.portraitCropWidth,
                PORTRAIT_FACE_CROP_WIDTH, portraitPose.zoom());
    }

    private float portraitCropHeight() {
        return lerp(HighResHealthBarSettings.portraitCropHeight,
                PORTRAIT_FACE_CROP_HEIGHT, portraitPose.zoom());
    }

    private void renderBloodVignette(Queue queue, int portraitX, int portraitY,
                                     float healthValue) {
        if (player.isDead()) {
            deathBloodLatched = true;
        } else if (deathBloodLatched && healthValue > 0.01f) {
            deathBloodLatched = false;
        }

        float intensity = deathBloodLatched
                ? 1f : healthValue < 0.5f ? (0.5f - healthValue) * 2f : 0f;
        intensity = clamp(intensity);
        if (intensity <= 0f) return;

        int inset = PORTRAIT_BLOOD_INSET;
        float washAlpha = deathBloodLatched ? 0.58f : intensity * 0.18f;
        fillRect(queue, 0.92f, 0.015f, 0.01f, washAlpha,
                portraitX + inset, portraitY + inset,
                PORTRAIT_SIZE - inset * 2, PORTRAIT_SIZE - inset * 2);

        ResourceTexture active = bloodTexture;
        if (active == null) {
            active = texture("img.highreshealthbar.blood");
            bloodTexture = active;
        }
        if (active != null) {
            Renderer.texturedQuadAlphaBlend(queue, active,
                    1f, 1f, 1f, intensity,
                    portraitX, portraitY, PORTRAIT_SIZE, PORTRAIT_SIZE,
                    0f, 0f, 1f, 1f);
        }
    }

    private void drawSolidGauge(Queue queue, int gx, int gy, int gw, int gh,
                                float value, float red, float green, float blue,
                                int gaugeIndex, float bubbleRed,
                                float bubbleGreen, float bubbleBlue) {
        // Main gauges occupy the grid's exact cells, with no second inset.
        UiRect well = gaugeIndex >= 0
                ? HudSkin.healthGauge(x, contentY(), gaugeIndex)
                : new UiRect(gx + GAUGE_INSET_X, gy + GAUGE_INSET_Y,
                        Math.max(0, gw - GAUGE_INSET_X * 2),
                        Math.max(0, gh - GAUGE_INSET_Y * 2));
        int innerX = well.x;
        int innerY = well.y;
        int innerWidth = well.width;
        int innerHeight = well.height;
        HudSkin.gauge(ui.begin(queue), well, value, new UiColor(red, green, blue));
        int fill = Math.max(0, Math.min(innerWidth,
                Math.round(innerWidth * value)));
        if (gaugeIndex >= 0 && gaugeIndex < previous.length) {
            drawGrowthAnimation(queue, innerX, innerY,
                    fill, innerHeight, value,
                    bubbleRed, bubbleGreen, bubbleBlue, gaugeIndex);
        }
        if (gaugeIndex >= 0) {
            drawSpentGaugeFlash(queue, innerX, innerY,
                    innerWidth, innerHeight, value, gaugeIndex,
                    red, green, blue);
        }
        // Stamina receives damage/hit overlays before its common glass layer.
        if (gaugeIndex != GAUGE_STAMINA) HudSkin.glass(ui, well);
    }

    private void drawSpentGaugeFlash(Queue queue, int gx, int gy,
                                     int width, int height, float value,
                                     int index, float red, float green,
                                     float blue) {
        if (index < 0 || index >= previousGaugeValues.length
                || width <= 0 || height <= 0) return;
        long now = System.nanoTime();
        float oldValue = previousGaugeValues[index];
        if (oldValue >= 0f && value < oldValue - 0.00035f) {
            if (now < spentFlashUntil[index]) {
                spentFlashFrom[index] = Math.min(spentFlashFrom[index], value);
                spentFlashTo[index] = Math.max(spentFlashTo[index], oldValue);
            } else {
                spentFlashFrom[index] = value;
                spentFlashTo[index] = oldValue;
            }
            spentFlashUntil[index] = now + SPENT_FLASH_DURATION;
        }
        previousGaugeValues[index] = value;
        if (now >= spentFlashUntil[index]) return;

        int from = Math.max(0, Math.min(width,
                Math.round(width * spentFlashFrom[index])));
        int to = Math.max(from, Math.min(width,
                Math.round(width * spentFlashTo[index])));
        int flashWidth = Math.max(2, to - from);
        flashWidth = Math.min(flashWidth, width - from);
        if (flashWidth <= 0) return;

        float remaining = (spentFlashUntil[index] - now)
                / (float) SPENT_FLASH_DURATION;
        float elapsed = 1f - remaining;
        float flicker = 0.65f + 0.35f
                * Math.abs((float) Math.sin(elapsed * Math.PI * 3.0));
        float alpha = remaining * flicker;
        fillRect(queue, Math.min(1f, red + 0.55f),
                Math.min(1f, green + 0.50f),
                Math.min(1f, blue + 0.28f),
                0.42f + alpha * 0.48f,
                gx + from, gy, flashWidth, height);
        fillRect(queue, 1f, 1f, 0.82f, alpha * 0.90f,
                gx + from, gy, flashWidth, 1);
    }

    private void drawDamageOverlay(Queue queue, int gx, int gy, int gw, int gh,
                                   float damageValue) {
        UiRect well = HudSkin.healthGauge(x, contentY(), GAUGE_STAMINA);
        gx = well.x; gy = well.y; gw = well.width; gh = well.height;
        int damageWidth = Math.max(0,
                Math.min(gw, Math.round(gw * damageValue)));
        if (damageWidth <= 0) return;
        int dx = gx + gw - damageWidth;
        ui.fill(UiColor.rgb(0xc21712), 1f, dx, gy, damageWidth, gh);
    }

    private void drawGrowthAnimation(Queue queue, int gx, int gy, int fill, int gh,
                                     float value, float red, float green, float blue,
                                     int index) {
        long now = System.nanoTime();
        if (previous[index] >= 0f && value > previous[index] + 0.00035f) {
            bubblesUntil[index] = now + BUBBLE_DURATION;
        }
        previous[index] = value;
        if (!controller.animateRisingGauges() || fill < 5 || now >= bubblesUntil[index]) return;

        float remaining = (bubblesUntil[index] - now) / (float) BUBBLE_DURATION;
        float life = 1f - remaining;
        int shimmerX = gx + Math.max(0, fill - 2);
        fillRect(queue, 1f, 1f, 0.84f, 0.32f + remaining * 0.45f,
                shimmerX, gy, 2, gh);

        for (int bubble = 0; bubble < 5; bubble++) {
            float phase = (life + bubble * 0.19f) % 1f;
            int bx = gx + 2 + Math.abs((index * 31 + bubble * 23)
                    % Math.max(3, fill - 3));
            int by = gy + gh - 1 - Math.round(phase * Math.max(1, gh - 1));
            int size = bubble % 3 == 0 ? 3 : 2;
            float bubbleAlpha = (1f - phase) * remaining * 0.65f;
            fillRect(queue, Math.min(1f, red + 0.34f),
                    Math.min(1f, green + 0.34f), Math.min(1f, blue + 0.34f),
                    bubbleAlpha, bx, by, size, size);
            if (size == 3) {
                fillRect(queue, 1f, 1f, 0.92f, bubbleAlpha * 0.8f,
                        bx + 1, by, 1, 1);
            }
        }
    }

    private void drawHitFlash(Queue queue, int gx, int gy, int gw, int gh,
                              float damageValue) {
        long now = System.nanoTime();
        if (previousDamage >= 0f && damageValue > previousDamage + 0.00035f) {
            hitFlashUntil = now + HIT_FLASH_DURATION;
        }
        previousDamage = damageValue;
        if (now >= hitFlashUntil) return;

        UiRect well = HudSkin.healthGauge(x, contentY(), GAUGE_STAMINA);
        gx = well.x; gy = well.y; gw = well.width; gh = well.height;

        float remaining = (hitFlashUntil - now) / (float) HIT_FLASH_DURATION;
        float pulse = remaining * remaining;
        fillRect(queue, 0.95f, 0.035f, 0.02f, 0.20f + pulse * 0.68f,
                gx, gy, gw, gh);
        fillRect(queue, 1f, 0.55f, 0.40f, pulse * 0.72f,
                gx, gy, gw, 1);
    }

    private void drawFooter(Queue queue) {
        int footerTop = y + frameHeight();
        int baseline = footerTop
                + (FOOTER_HEIGHT + highText.getHeight()) / 2;

        fillRect(queue, 0.58f, 0.55f, 0.49f, 0.55f,
                x + FOOTER_FIRST_DIVIDER, footerTop + 3, 1, 12);
        fillRect(queue, 0.58f, 0.55f, 0.49f, 0.55f,
                x + FOOTER_SECOND_DIVIDER, footerTop + 3, 1, 12);

        paintFooterPair(queue, "FPS:", String.valueOf(Stats.fps.getCurrent()),
                highText, 0, FOOTER_FIRST_DIVIDER, baseline,
                0.94f, 0.94f, 0.91f);
        paintFooterPair(queue, "Fatigue:", controller.fatigueShortText(),
                highBold, FOOTER_FIRST_DIVIDER + 1,
                FOOTER_SECOND_DIVIDER, baseline,
                0.98f, 0.78f, 0.28f);
        paintFooterPair(queue, "Speed:", Stats.speed.getCurrent(),
                highText, FOOTER_SECOND_DIVIDER + 1, PANEL_WIDTH, baseline,
                0.94f, 0.94f, 0.91f);
    }

    private void paintFooterPair(Queue queue, String label, String value,
                                 TextFont valueFont, int fieldLeft,
                                 int fieldRight, int baseline,
                                 float valueRed, float valueGreen,
                                 float valueBlue) {
        int gap = 3;
        int totalWidth = highText.getWidth(label) + gap
                + valueFont.getWidth(value);
        int textX = x + fieldLeft
                + (fieldRight - fieldLeft - totalWidth) / 2;
        paintShadowed(highText, queue, label, textX, baseline,
                0.76f, 0.76f, 0.72f);
        paintShadowed(valueFont, queue, value,
                textX + highText.getWidth(label) + gap, baseline,
                valueRed, valueGreen, valueBlue);
    }

    private void drawGaugeLabels(Queue queue, int contentY, int left) {
        paintGaugeLabel(queue, "Stamina / HP", left,
                contentY + STAMINA_Y, STAMINA_HEIGHT);
        paintGaugeLabel(queue, "Water", left,
                contentY + WATER_FOOD_Y, WATER_FOOD_HEIGHT);
        paintGaugeLabel(queue, "Food", left + WATER_WIDTH + WATER_FOOD_GAP,
                contentY + WATER_FOOD_Y, WATER_FOOD_HEIGHT);
        if (showCCFP()) {
            String[] labels = new String[]{
                    "Calories", "Carbs", "Fats", "Proteins"
            };
            for (int index = 0; index < labels.length; index++) {
                paintGaugeLabel(queue, labels[index],
                        left + CCFP_OFFSETS[index],
                        contentY + CCFP_Y, CCFP_HEIGHT);
            }
        }
        paintGaugeLabel(queue, player.isSleepBonusActive()
                        ? "Sleep bonus: ON" : "Sleep bonus: OFF", left,
                contentY + SLEEP_BONUS_Y, SLEEP_BONUS_HEIGHT);
        if (isPriest) {
            paintGaugeLabel(queue, "Favor", left,
                    contentY + FAVOR_Y, FAVOR_HEIGHT);
        }
    }

    private void paintGaugeLabel(Queue queue, String value, int px, int py,
                                 int barHeight) {
        // The shared HUD font adapter uses the lower edge of the text box, not an AWT
        // baseline. Center the complete glyph box inside the coloured channel.
        int baseline = py + (barHeight + smallText.getHeight()) / 2;
        paintShadowed(smallText, queue, value, px + 3, baseline,
                0.96f, 0.96f, 0.93f);
    }

    private void drawNameAndTitlesLast(Queue queue) {
        if (!showName) return;
        String[] lines = headerLayout.lines();
        int lineStep = TITLE_LINE_HEIGHT + TITLE_LINE_GAP;
        for (int index = 0; index < lines.length; index++) {
            paint(titleText, queue, lines[index], x + HEADER_TEXT_X,
                    y + 2 + TITLE_LINE_HEIGHT + index * lineStep,
                    0.97f, 0.92f, 0.78f, 1f);
        }
    }

    private void refreshHeaderLayout() {
        headerLayout = HeaderLayout.create(showName, showTitles,
                player == null ? "" : player.getPlayerName(),
                allTitles(), headerTextWidth);
    }

    private String allTitles() {
        StringBuilder titles = new StringBuilder();
        if (controller != null) appendTitle(titles, controller.sorceryTitle());
        appendTitle(titles, meditationTitle);
        appendTitle(titles, normalTitle);
        return titles.toString();
    }

    private static void appendTitle(StringBuilder target, String title) {
        if (title == null || title.trim().isEmpty()) return;
        if (target.length() > 0) target.append(' ');
        target.append('[').append(title.trim()).append(']');
    }

    @Override
    protected void leftPressed(int mouseX, int mouseY, int buttons) {
        if (overDisembarkButton(mouseX, mouseY)) {
            rideButton.leftPressed(mouseX, mouseY, buttons);
            return;
        }
        if (buttons != 2 && overSleepBonusButton(mouseX, mouseY)) {
            sleepButton.leftPressed(mouseX, mouseY, buttons);
            return;
        }
        int contentY = contentY();
        if (inside(mouseX, mouseY,
                x + PORTRAIT_APERTURE_X, contentY + PORTRAIT_APERTURE_Y,
                PORTRAIT_APERTURE_WIDTH, PORTRAIT_APERTURE_HEIGHT)) {
            portraitPose.randomize(System.nanoTime());
        }
        // Preserve stock dragging and double-click paper-doll behaviour.
        super.leftPressed(mouseX, mouseY, buttons);
    }

    @Override public StaticComponent getComponentAt(int mx, int my) {
        // Synchronize hit geometry independently of render ordering/window dragging.
        sleepButton.setLocation(sleepBonusButtonX(x + GAUGE_X),
                contentY() + SLEEP_BONUS_Y + SLEEP_BUTTON_Y, SLEEP_BUTTON_WIDTH, SLEEP_BUTTON_HEIGHT);
        sleepButton.caption(player.isSleepBonusActive() ? "Deactivate" : "Activate", controller.canToggleSleepBonus());
        rideButton.setLocation(disembarkButtonX(), y + mainFrameHeight() + DISEMBARK_BUTTON_Y,
                DISEMBARK_BUTTON_WIDTH, DISEMBARK_BUTTON_HEIGHT);
        if (overSleepBonusButton(mx, my)) return sleepButton;
        if (overDisembarkButton(mx, my)) return rideButton;
        return super.getComponentAt(mx, my);
    }

    public boolean mouseWheeledAt(HeadsUpDisplay owner, int mouseX, int mouseY,
                                  int wheelDelta) {
        if (owner == null || wheelDelta == 0
                || !owns(owner.getComponentAt(mouseX, mouseY))) {
            return false;
        }
        int contentY = contentY();
        if (!inside(mouseX, mouseY,
                x + PORTRAIT_APERTURE_X, contentY + PORTRAIT_APERTURE_Y,
                PORTRAIT_APERTURE_WIDTH, PORTRAIT_APERTURE_HEIGHT)) {
            return false;
        }

        long now = System.nanoTime();
        portraitPose.adjustZoom(wheelDelta, now);
        return true;
    }

    private boolean owns(WurmComponent target) {
        for (WurmComponent component = target; component != null;
             component = component.parent) {
            if (component == this) return true;
        }
        return false;
    }

    private static String fit(TextFont font, String value, int maxWidth) {
        if (font.getWidth(value) <= maxWidth) return value;
        String ellipsis = "...";
        int end = value.length();
        while (end > 0
                && font.getWidth(value.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return value.substring(0, end) + ellipsis;
    }

    @Override
    protected void rightPressed(int mouseX, int mouseY, int buttons) {
        final WurmPopup popup = new WurmPopup(
                "highResHealthBarMenu", "Options", mouseX, mouseY);
        popup.addSeparator();
        addDragContextMenuEntry(popup);
        popup.addButton(popup.new WPopupLiveButton("Toggle name display") {
            @Override
            protected void handleLeftClick() {
                HighResHealthBar.this.toggleName();
            }
        });
        popup.addButton(popup.new WPopupLiveButton(
                showTitles ? "Titles: ON" : "Titles: OFF") {
            @Override
            protected void handleLeftClick() {
                showTitles = !showTitles;
                refreshHeaderLayout();
                updateHighResSize();
            }
        });
        popup.addButton(popup.new WPopupLiveButton("Toggle number values") {
            @Override
            protected void handleLeftClick() {
                showNumberValues = !showNumberValues;
                updateHighResSize();
            }
        });
        popup.addButton(popup.new WPopupLiveButton(showHitchedAnimals
                ? "Show hitched animals: ON"
                : "Show hitched animals: OFF") {
            @Override
            protected void handleLeftClick() {
                showHitchedAnimals = !showHitchedAnimals;
                updateHighResSize();
            }
        });
        popup.addButton(popup.new WPopupLiveButton(
                showRide ? "Ride: ON" : "Ride: OFF") {
            @Override
            protected void handleLeftClick() {
                showRide = !showRide;
                updateHighResSize();
            }
        });
        hud.showPopupComponent(popup);
    }

    private void addDragContextMenuEntry(WurmPopup popup) {
        if (BASE_DRAGGER == null) return;
        try {
            ((DragController) BASE_DRAGGER.get(this)).addContextMenuEntry(popup);
        } catch (IllegalAccessException ignored) {
            // Moving remains available even if this optional lock-menu entry
            // cannot be reached on a future client build.
        }
    }

    private static Field findBaseDragger() {
        try {
            Field field = HealthBar.class.getDeclaredField("dragger");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private static Field findServerCreatures() {
        try {
            Field field = ServerConnectionListenerClass.class
                    .getDeclaredField("creatures");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private static void paintShadowed(TextFont font, Queue queue, String value,
                                      int px, int baseline, float red, float green,
                                      float blue) {
        paint(font, queue, value, px + 1, baseline + 1,
                0.015f, 0.012f, 0.008f, 0.98f);
        paint(font, queue, value, px, baseline, red, green, blue, 1f);
    }

    private static void paint(TextFont font, Queue queue, String value,
                              int px, int baseline, float red, float green,
                              float blue, float alpha) {
        if (value == null || value.isEmpty()) return;
        font.moveTo(px, baseline);
        font.paint(queue, value, red, green, blue, alpha);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float[] filled(int size, float value) {
        float[] result = new float[size];
        java.util.Arrays.fill(result, value);
        return result;
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * clamp(progress);
    }

    @Override
    public void pick(PickData pickData, int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, x, y, width, height)) return;

        int contentY = contentY();
        if (inside(mouseX, mouseY,
                x + PORTRAIT_APERTURE_X, contentY + PORTRAIT_APERTURE_Y,
                PORTRAIT_APERTURE_WIDTH, PORTRAIT_APERTURE_HEIGHT)) {
            addPaperDollSummary(pickData);
            return;
        }

        int left = x + GAUGE_X;

        if (inside(mouseX, mouseY, left, contentY + STAMINA_Y,
                GAUGE_WIDTH, STAMINA_HEIGHT)) {
            addStaminaAndHealth(pickData);
            return;
        }
        if (inside(mouseX, mouseY, left, contentY + WATER_FOOD_Y,
                WATER_WIDTH, WATER_FOOD_HEIGHT)) {
            float waterValue = clamp(water.getValue(0f));
            float waterModifier = waterStaminaModifier(waterValue);
            pickData.addText("Water: " + percent(waterValue));
            if (showCCFP()) {
                float carbsValue = clamp(carbs.getValue(0f));
                float usageModifier = waterUsageModifier(carbsValue);
                pickData.addText("Water usage: " + modifierPercent(usageModifier)
                        + "% (-" + reductionPercent(usageModifier)
                        + "% from Carbs " + percent(carbsValue) + ")");
            }
            pickData.addText("Stamina regeneration modifier: x"
                    + twoDecimals(waterModifier) + " (-"
                    + reductionPercent(waterModifier) + "%)");
            pickData.addText("Server formula: hydration cubed");
            return;
        }
        if (inside(mouseX, mouseY,
                left + WATER_WIDTH + WATER_FOOD_GAP,
                contentY + WATER_FOOD_Y,
                FOOD_WIDTH, WATER_FOOD_HEIGHT)) {
            float foodValue = clamp(food.getValue(0f));
            float nutritionValue = currentNutritionLevel();
            float foodModifier = foodStaminaModifier(foodValue);
            pickData.addText("Food: " + percent(foodValue));
            if (showCCFP()) {
                float proteinsValue = clamp(proteins.getValue(0f));
                float usageModifier = foodUsageModifier(proteinsValue);
                pickData.addText("Food usage: " + modifierPercent(usageModifier)
                        + "% (-" + reductionPercent(usageModifier)
                        + "% from Proteins " + percent(proteinsValue) + ")");
            }
            pickData.addText("Stamina regeneration modifier: x"
                    + twoDecimals(foodModifier) + " (-"
                    + reductionPercent(foodModifier) + "%)");
            pickData.addText("Heavy work: "
                    + (heavyWorkAvailable(foodValue) ? "available" : "blocked"));
            pickData.addText("Nutrition: " + percent(nutritionValue));
            pickData.addText("Skill-gain bonus: +" + nutritionSkillBonus(
                    nutritionValue) + "%");
            pickData.addText(nutritionValue > 0.60f
                    ? "Natural healing: bonus active"
                    : "Natural healing: bonus starts above 60%");
            return;
        }
        if (showCCFP()) {
            String[] names = new String[]{"Calories", "Carbohydrates", "Fats", "Proteins"};
            float[] values = new float[]{calories.getValue(0f), carbs.getValue(0f),
                    fats.getValue(0f), proteins.getValue(0f)};
            for (int index = 0; index < 4; index++) {
                int miniX = left + CCFP_OFFSETS[index];
                if (inside(mouseX, mouseY, miniX, contentY + CCFP_Y,
                        CCFP_WIDTHS[index], CCFP_HEIGHT)) {
                    addCcfpTooltip(pickData, index, names[index], values[index]);
                    return;
                }
            }
        }
        if (inside(mouseX, mouseY, left, contentY + SLEEP_BONUS_Y,
                GAUGE_WIDTH, SLEEP_BONUS_HEIGHT)) {
            boolean active = player.isSleepBonusActive();
            int cooldown = controller.sleepBonusActivationCooldownSeconds();
            pickData.addText("Sleep bonus: "
                    + hoursMinutes(player.getSleepBonusSecondsLeft()));
            pickData.addText(active ? "Active (ON)" : "Inactive (OFF)");
            pickData.addText("Skill gain: " + (active ? "x2" : "x1 (bonus OFF)"));
            pickData.addText("Stamina regeneration: "
                    + (active ? "x3" : "x1 (bonus OFF)"));
            if (active && showCCFP()) {
                float fatsValue = clamp(fats.getValue(0f));
                float consumptionModifier = sleepBonusConsumptionModifier(fatsValue);
                pickData.addText("Sleep consumption: "
                        + modifierPercent(consumptionModifier) + "% (-"
                        + reductionPercent(consumptionModifier)
                        + "% from Fats " + percent(fatsValue) + ")");
            } else {
                pickData.addText(active
                        ? "Sleep consumption: 100%"
                        : "Sleep consumption: paused");
            }
            pickData.addText("Next activation: " + (cooldown > 0
                    ? SleepBonusToggleTracker.formatCountdown(cooldown)
                    : "available"));
            if (!active && player.getSleepBonusSecondsLeft() <= 0) {
                pickData.addText("Activate unavailable: no stored sleep bonus");
            } else if (!controller.canToggleSleepBonus()) {
                pickData.addText("Activate unavailable until cooldown expires");
            }
            pickData.addText("Left-click the "
                    + (active ? "Deactivate" : "Activate")
                    + " button to toggle (/fsleep)");
            return;
        }
        if (isPriest && inside(mouseX, mouseY, left, contentY + FAVOR_Y,
                GAUGE_WIDTH, FAVOR_HEIGHT)) {
            pickData.addText("Favor: " + oneDecimal(favorValue));
            pickData.addText("Faith: " + oneDecimal(faithValue));
            if (showCCFP()) {
                float fatsValue = clamp(fats.getValue(0f));
                float favorModifier = favorRegenerationModifier(fatsValue);
                pickData.addText("Favor regeneration from Fats: "
                        + modifierPercent(favorModifier) + "% (+"
                        + increasePercent(favorModifier) + "% at Fats "
                        + percent(fatsValue) + ")");
            }
            return;
        }

        CreatureCellRenderable mount = currentMount();
        int mountY = y + mainFrameHeight();
        int currentMountHeight = mountBlockHeight();
        if (mount != null && inside(mouseX, mouseY,
                x, mountY, PANEL_WIDTH, currentMountHeight)) {
            if (overDisembarkButton(mouseX, mouseY)) {
                pickData.addText("Disembark");
                return;
            }
            int row = Math.max(0, (mouseY - mountY) / MOUNT_ROW_HEIGHT);
            if (mount.isItem()) {
                if (showRide && row == 0) {
                    pickData.addText(vehicleStatusLine(mount));
                } else {
                    int animalIndex = rideStatus.hitchedIndexForRow(
                            row, showRide, showHitchedAnimals);
                    if (animalIndex < 0) return;
                    CreatureCellRenderable animal = rideStatus
                            .visibleHitched(true).get(animalIndex);
                    float health = clamp(animal.getPercentHealth() / 100f);
                    pickData.addText("Hitched creature: " + mountName(animal));
                    pickData.addText("Hitched creature health: "
                            + percent(health));
                }
            } else {
                pickData.addText("Riding " + mountName(mount));
                pickData.addText("Mount health: " + percent(clamp(
                        mount.getPercentHealth() / 100f)));
            }
            return;
        }

        int footerTop = y + frameHeight();
        if (showNumberValues && mouseY >= footerTop) {
            int localX = mouseX - x;
            if (localX < FOOTER_FIRST_DIVIDER) {
                pickData.addText("Frames per second: " + Stats.fps.getCurrent());
            } else if (localX < FOOTER_SECOND_DIVIDER) {
                pickData.addText(controller.fatigueHoverText());
            } else {
                pickData.addText("Speed: " + Stats.speed.getCurrent());
            }
        }
    }

    private void addPaperDollSummary(PickData pickData) {
        StringBuilder name = new StringBuilder(player.getPlayerName());
        if (normalTitle != null && !normalTitle.isEmpty()) {
            name.append(" [").append(normalTitle).append(']');
        }
        pickData.addText(name.toString());
        if (!controller.sorceryTitle().isEmpty()) {
            pickData.addText("Sorcery title: " + controller.sorceryTitle());
        }
        if (meditationTitle != null && !meditationTitle.isEmpty()) {
            pickData.addText("Meditation title: " + meditationTitle);
        }
        addStaminaAndHealth(pickData);
        if (showCCFP()) {
            float waterUsage = waterUsageModifier(carbs.getValue(0f));
            float foodUsage = foodUsageModifier(proteins.getValue(0f));
            pickData.addText("Water: " + percent(water.getValue(0f))
                    + " (usage -" + reductionPercent(waterUsage)
                    + "% from Carbs)");
            pickData.addText("Food: " + percent(food.getValue(0f))
                    + " (usage -" + reductionPercent(foodUsage)
                    + "% from Proteins), Nutrition: "
                    + percent(currentNutritionLevel()));
        } else {
            pickData.addText("Water: " + percent(water.getValue(0f)));
            pickData.addText("Food: " + percent(food.getValue(0f))
                    + ", Nutrition: " + percent(currentNutritionLevel()));
        }
        if (showCCFP()) {
            pickData.addText("Calories: " + percent(calories.getValue(0f))
                    + ", Carbs: " + percent(carbs.getValue(0f)));
            pickData.addText("Fats: " + percent(fats.getValue(0f))
                    + ", Proteins: " + percent(proteins.getValue(0f)));
        }
        boolean sleepActive = player.isSleepBonusActive();
        StringBuilder sleepSummary = new StringBuilder("Sleep bonus: ")
                .append(hoursMinutes(player.getSleepBonusSecondsLeft()))
                .append(sleepActive ? " ON" : " OFF");
        if (sleepActive && showCCFP()) {
            sleepSummary.append(" (consumption -")
                    .append(reductionPercent(sleepBonusConsumptionModifier(
                            fats.getValue(0f))))
                    .append("% from Fats)");
        }
        pickData.addText(sleepSummary.toString());
        if (isPriest) {
            StringBuilder favorSummary = new StringBuilder("Favor: ")
                    .append(oneDecimal(favorValue))
                    .append(", Faith: ").append(oneDecimal(faithValue));
            if (showCCFP()) {
                favorSummary.append(" (regeneration +")
                        .append(increasePercent(favorRegenerationModifier(
                                fats.getValue(0f))))
                        .append("% from Fats)");
            }
            pickData.addText(favorSummary.toString());
        }
        pickData.addText(controller.fatigueHoverText());
    }

    private void addStaminaAndHealth(PickData pickData) {
        float waterModifier = waterStaminaModifier(water.getValue(0f));
        float foodModifier = foodStaminaModifier(food.getValue(0f));
        boolean sleepBonusActive = player.isSleepBonusActive();
        float regenerationModifier = staminaRegenerationModifier(
                water.getValue(0f), food.getValue(0f), sleepBonusActive);

        pickData.addText("Stamina: " + percent(stamina.getValue(0f)));
        if (showCCFP()) {
            float drainModifier = staminaDrainModifier(calories.getValue(0f));
            pickData.addText("Stamina drain: -" + reductionPercent(drainModifier)
                    + "% (Calories)");
        }
        pickData.addText("Stamina regeneration: "
                + modifierPercent(regenerationModifier) + "% of base (Water -"
                + reductionPercent(waterModifier) + "%, Food -"
                + reductionPercent(foodModifier) + "%"
                + (sleepBonusActive ? ", Sleep x3" : "") + ")");
        pickData.addText("Health: " + percent(1f - damage.getValue(0f)));
    }

    private static void addCcfpTooltip(PickData pickData, int index,
                                       String name, float rawValue) {
        float value = clamp(rawValue);
        pickData.addText(name + ": " + percent(value));
        if (index == 0) {
            float multiplier = 1f / (1f + value / 3f);
            pickData.addText("Effect: reduces stamina drain");
            pickData.addText("Current modifier: x" + twoDecimals(multiplier)
                    + " (-" + Math.round((1f - multiplier) * 100f) + "%)");
        } else if (index == 1) {
            float multiplier = waterUsageModifier(value);
            pickData.addText("Effect: reduces water usage");
            pickData.addText("Current modifier: x" + twoDecimals(multiplier)
                    + " (-" + Math.round((1f - multiplier) * 100f) + "%)");
        } else if (index == 2) {
            float favorMultiplier = favorRegenerationModifier(value);
            float sleepMultiplier = sleepBonusConsumptionModifier(value);
            pickData.addText("Favor regeneration: x"
                    + twoDecimals(favorMultiplier) + " (+"
                    + increasePercent(favorMultiplier) + "%)");
            pickData.addText("Sleep consumption: x"
                    + twoDecimals(sleepMultiplier) + " (-"
                    + reductionPercent(sleepMultiplier) + "%)");
        } else {
            float multiplier = foodUsageModifier(value);
            pickData.addText("Effect: reduces food usage");
            pickData.addText("Current modifier: x" + twoDecimals(multiplier)
                    + " (-" + Math.round((1f - multiplier) * 100f) + "%)");
        }
    }

    private static boolean inside(int px, int py, int rx, int ry, int rw, int rh) {
        return px >= rx && px < rx + rw && py >= ry && py < ry + rh;
    }

    private static String percent(float value) {
        return Math.round(clamp(value) * 100f) + "%";
    }

    private static String oneDecimal(float value) {
        return String.valueOf(Math.round(value * 10f) / 10f);
    }

    private static String twoDecimals(float value) {
        int hundredths = Math.round(value * 100f);
        int fraction = Math.abs(hundredths % 100);
        return (hundredths / 100) + "." + (fraction < 10 ? "0" : "") + fraction;
    }

    private static int modifierPercent(float value) {
        return Math.round(Math.max(0f, value) * 100f);
    }

    private static int reductionPercent(float multiplier) {
        return Math.round(Math.max(0f, 1f - multiplier) * 100f);
    }

    private static int increasePercent(float multiplier) {
        return Math.round(Math.max(0f, multiplier - 1f) * 100f);
    }

    private static String nutritionSkillBonus(float value) {
        float bonus = Math.max(0f, (clamp(value) * 100f - 50f) / 10f);
        return oneDecimal(bonus);
    }

    private float currentNutritionLevel() {
        return clamp(player.getNutritionLevel());
    }

    private static boolean heavyWorkAvailable(float value) {
        return clamp(value) >= 5535f / 65535f;
    }

    private static String hoursMinutes(int seconds) {
        int totalMinutes = Math.max(0, seconds) / 60;
        int minutes = totalMinutes % 60;
        return (totalMinutes / 60) + ":" + (minutes < 10 ? "0" : "") + minutes;
    }
}
