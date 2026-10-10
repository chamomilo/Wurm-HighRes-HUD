package org.highreshealthbar.client;

import com.wurmonline.client.renderer.PlayerBodyRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.game.World;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import com.wurmonline.client.renderer.gui.HealthBar;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.HighResHealthBar;
import com.wurmonline.client.renderer.gui.PaperDollRenderer;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.client.resources.textures.Texture;
import com.wurmonline.client.settings.SavePosManager;
import com.wurmonline.shared.constants.PlayerAction;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.highreshud.core.ActionOrigin;
import org.highreshud.core.HighResHudApi;
import org.highreshud.client.portrait.PortraitRenderKey;
import org.highreshud.client.portrait.SharedPortraitChannel;
import org.highreshud.client.portrait.SharedPortraitCoordinator;

/** Healthbar component adapter owned by the unified High-res HUD entry point. */
public final class HighResHealthBarMod {
    private static final Logger LOG = Logger.getLogger("HighResHealthBar");
    private static volatile HighResHealthBarMod instance;

    private volatile FatigueTracker fatigue;
    private volatile HeadsUpDisplay hud;
    private volatile HighResHealthBar panel;
    private volatile PaperDollRenderer portraitRenderer;
    private volatile PortraitBodySnapshot portraitBodySnapshot;
    private volatile boolean portraitRequested;
    private volatile boolean portraitInFlight;
    private volatile int portraitSubjectIdentity;
    private volatile float requestedPortraitRotation;
    private volatile SharedPortraitChannel sharedPortraitFrom;
    private volatile SorceryTitleTracker titles = new SorceryTitleTracker(15);
    private final SleepBonusToggleTracker sleepBonus =
            new SleepBonusToggleTracker();
    private final VehicleMetadataController vehicles =
            new VehicleMetadataController(LOG);
    private final SharedPortraitChannel portraitChannel =
            new SharedPortraitChannel() {
                @Override
                public PortraitRenderKey renderKey() {
                    return new PortraitRenderKey(portraitSubjectIdentity,
                            HighResHealthBarSettings.portraitRenderSize,
                            "paperdoll-player",
                            "healthbar|" + Float.floatToIntBits(
                                    requestedPortraitRotation) + '|'
                                    + Float.floatToIntBits(
                                    HighResHealthBarSettings.portraitFov));
                }

                @Override
                public void scheduleOwnRender() {
                    PaperDollRenderer renderer = portraitRenderer;
                    if (renderer != null && portraitRequested) {
                        portraitRequested = false;
                        portraitInFlight = true;
                        renderer.beginRender();
                    }
                }

                @Override
                public void finishOwnRender() {
                    PaperDollRenderer renderer = portraitRenderer;
                    if (renderer != null && portraitInFlight) {
                        renderer.endRender();
                        portraitInFlight = false;
                    }
                }

                @Override
                public Texture ownTexture() {
                    PaperDollRenderer renderer = portraitRenderer;
                    return renderer == null ? null : renderer.getTexture();
                }

                @Override
                public void shareFrom(SharedPortraitChannel leader) {
                    sharedPortraitFrom = leader;
                }
            };

    public HighResHealthBarMod() {
        instance = this;
        rebuildFatigueTracker();
    }

    public void configure(Properties properties) {
        HighResHealthBarSettings.configure(properties);
        rebuildFatigueTracker();
    }

    public void init() {
        LOG.info("High-res healthbar component initialized");
    }

    public static void hudReady(HeadsUpDisplay hud) {
        HighResHealthBarMod mod = instance;
        if (mod == null) return;
        try {
            mod.attach(hud);
        } catch (Throwable error) {
            LOG.log(Level.SEVERE, "Unable to attach high-res healthbar", error);
        }
    }

    private synchronized void attach(HeadsUpDisplay newHud) throws Exception {
        if (hud == newHud && panel != null) return;
        HighResHealthBarResources.ensurePackLoaded();
        vehicles.reset(world());
        hud = newHud;
        titles = new SorceryTitleTracker(15);
        sleepBonus.reset(newHud.getWorld() != null
                && newHud.getWorld().getPlayer() != null
                && newHud.getWorld().getPlayer().isSleepBonusActive());

        portraitRenderer = new PaperDollRenderer(newHud);
        portraitRenderer.initialize();
        portraitRequested = false;
        portraitInFlight = false;
        sharedPortraitFrom = null;
        installFixedPortraitLighting(portraitRenderer);
        // Creating this helper resolves PlayerBodyRenderable and PlayerObj via
        // reflection. Keep that work out of the mod-loader construction phase,
        // while other mods still need mutable Javassist classes in preInit().
        portraitBodySnapshot = new PortraitBodySnapshot(LOG);

        HealthBar original = newHud.getHealthBar();
        panel = new HighResHealthBar(original, this);

        Method add = ReflectionUtil.getMethod(HeadsUpDisplay.class, "addComponent",
                new Class[]{WurmComponent.class});
        ReflectionUtil.callPrivateMethod(newHud, add, panel);

        Field settingsField = HeadsUpDisplay.class.getDeclaredField("hudSettings");
        settingsField.setAccessible(true);
        com.wurmonline.client.renderer.gui.HudSettings settings =
                (com.wurmonline.client.renderer.gui.HudSettings) settingsField.get(newHud);
        settings.registerComponent("High-res healthbar", panel);
        settings.setAvailable(panel, true);
        settings.setEnabled(panel, HighResHealthBarSettings.enabledByDefault);

        Field saveField = HeadsUpDisplay.class.getDeclaredField("savePosManager");
        saveField.setAccessible(true);
        SavePosManager savePositions = (SavePosManager) saveField.get(newHud);
        boolean hasSavedPosition = savePositions.hasComponentRawPositions(
                "highreshealthbar");
        savePositions.registerAndRefresh(panel, "highreshealthbar");

        // restorePositionHints has already applied a saved enabled/disabled
        // state. Only apply the configured default for a new player profile.
        if (!hasSavedPosition && !HighResHealthBarSettings.enabledByDefault) {
            hideComponentIfVisible(newHud, panel);
        }

        // HUD.init has restored the stock component's saved state by this
        // point. Hide it only when it is actually present, so the check never
        // toggles an already-hidden stock healthbar back on.
        if (hideComponentIfVisible(newHud, original)) {
            LOG.info("Disabled the visible stock healthbar");
        }
        LOG.info("Registered independent HUD Settings component: High-res healthbar");
    }

    private static boolean hideComponentIfVisible(HeadsUpDisplay currentHud,
                                                   WurmComponent component)
            throws Exception {
        Method isEnabled = ReflectionUtil.getMethod(HeadsUpDisplay.class,
                "isComponentEnabled", new Class[]{WurmComponent.class});
        Boolean enabled = ReflectionUtil.callPrivateMethod(
                currentHud, isEnabled, component);
        if (!Boolean.TRUE.equals(enabled)) return false;

        Method hide = ReflectionUtil.getMethod(HeadsUpDisplay.class,
                "hideComponent", new Class[]{WurmComponent.class});
        ReflectionUtil.callPrivateMethod(currentHud, hide, component);
        return true;
    }

    private static void installFixedPortraitLighting(PaperDollRenderer renderer)
            throws Exception {
        Field lightField = PaperDollRenderer.class.getDeclaredField("lightManager");
        lightField.setAccessible(true);
        lightField.set(renderer, new FixedPortraitLightManager());
        if (!(lightField.get(renderer) instanceof FixedPortraitLightManager)) {
            throw new IllegalStateException("Unable to install fixed portrait lighting");
        }
    }

    public void gameTick() {
        observeSleepBonusState();
        FatigueTracker tracker = fatigue;
        if (tracker != null) tracker.tick(hud);
        SorceryTitleTracker titleTracker = titles;
        if (titleTracker != null) titleTracker.tick(hud);
        vehicles.tick(hud);
    }

    public String fatigueShortText() {
        FatigueTracker tracker = fatigue;
        return tracker == null ? "--:--" : tracker.shortText();
    }

    public String fatigueHoverText() {
        FatigueTracker tracker = fatigue;
        return tracker == null ? "Remaining fatigue: waiting for server"
                : tracker.hoverText();
    }

    public void requestPortrait(PlayerBodyRenderable body, float rotation) {
        PaperDollRenderer renderer = portraitRenderer;
        PortraitBodySnapshot snapshots = portraitBodySnapshot;
        if (renderer == null || snapshots == null || body == null) return;
        PlayerBodyRenderable portraitBody = snapshots.snapshot(body);
        if (portraitBody == null) return;
        portraitSubjectIdentity = System.identityHashCode(body);
        requestedPortraitRotation = rotation;
        portraitRequested = true;
        renderer.requestRender(portraitBody, rotation,
                HighResHealthBarSettings.portraitRenderSize,
                HighResHealthBarSettings.portraitRenderSize,
                false, HighResHealthBarSettings.portraitFov);
    }

    public Texture portraitTexture() {
        SharedPortraitChannel leader = sharedPortraitFrom;
        if (leader != null) return leader.ownTexture();
        PaperDollRenderer renderer = portraitRenderer;
        return renderer == null ? null : renderer.getTexture();
    }

    public boolean animateRisingGauges() {
        return HighResHealthBarSettings.animateRisingGauges;
    }

    public boolean toggleSleepBonus() {
        HeadsUpDisplay currentHud = hud;
        World currentWorld = world();
        if (currentHud == null || currentWorld == null
                || currentWorld.getPlayer() == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        boolean active = currentWorld.getPlayer().isSleepBonusActive();
        sleepBonus.observe(active, now);
        if (!active && (currentWorld.getPlayer().getSleepBonusSecondsLeft() <= 0
                || !sleepBonus.canActivate(now))) {
            return false;
        }
        if (!active) sleepBonus.activationRequested(now);
        HighResHudApi.runAs(ActionOrigin.HUD,
                () -> currentHud.handleInput("/fsleep"));
        return true;
    }

    public boolean canToggleSleepBonus() {
        World currentWorld = world();
        if (currentWorld == null || currentWorld.getPlayer() == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        boolean active = currentWorld.getPlayer().isSleepBonusActive();
        sleepBonus.observe(active, now);
        return active || (currentWorld.getPlayer().getSleepBonusSecondsLeft() > 0
                && sleepBonus.canActivate(now));
    }

    public int sleepBonusActivationCooldownSeconds() {
        observeSleepBonusState();
        return sleepBonus.secondsUntilActivation(System.currentTimeMillis());
    }

    public String sorceryTitle() {
        SorceryTitleTracker tracker = titles;
        return tracker == null ? "" : tracker.title();
    }

    public World world() {
        HeadsUpDisplay currentHud = hud;
        return currentHud == null ? null : currentHud.getWorld();
    }

    public void disembark() {
        HeadsUpDisplay currentHud = hud;
        World currentWorld = currentHud == null ? null : currentHud.getWorld();
        if (currentWorld == null || currentWorld.getPlayer() == null) return;
        CreatureCellRenderable carrier = currentWorld.getPlayer().getCarrierCreature();
        if (carrier == null) return;
        // sendLocalAction targets a surface tile at level zero. That target
        // can be blocked by a floor when the player is indoors or underground.
        long carrierId = carrier.getId();
        HighResHudApi.runAs(ActionOrigin.HUD,
                () -> currentHud.sendAction(PlayerAction.DISEMBARK, carrierId));
    }

    public String[] vehicleStats(long vehicleId, String fallbackDetails) {
        return vehicles.stats(world(), vehicleId, fallbackDetails);
    }

    public static void beginPortraitFrame(HeadsUpDisplay currentHud) {
        HighResHealthBarMod mod = instance;
        if (mod == null || mod.hud != currentHud) return;
        PaperDollRenderer renderer = mod.portraitRenderer;
        if (renderer != null && mod.portraitRequested) {
            SharedPortraitCoordinator.request(mod.portraitChannel);
        }
    }

    public static void endPortraitFrame(HeadsUpDisplay currentHud) {
        HighResHealthBarMod mod = instance;
        if (mod == null || mod.hud != currentHud) return;
        // SharedPortraitCoordinator completes this channel with the other HUDs.
    }

    public static boolean consumeFatigueMessage(String message) {
        HighResHealthBarMod mod = instance;
        if (mod != null) {
            mod.sleepBonus.observeServerMessage(
                    message, System.currentTimeMillis());
        }
        FatigueTracker tracker = mod == null ? null : mod.fatigue;
        return tracker != null && tracker.consumeAutomaticReply(message);
    }

    public static boolean captureTitleBml(String bml) {
        HighResHealthBarMod mod = instance;
        HeadsUpDisplay currentHud = mod == null ? null : mod.hud;
        if (currentHud == null || currentHud.getWorld() == null
                || currentHud.getWorld().getPlayer() == null) {
            return false;
        }
        SorceryTitleTracker tracker = mod.titles;
        if (tracker == null) return false;
        String before = tracker.title();
        boolean consume = tracker.capture(bml,
                currentHud.getWorld().getPlayer().getPlayerName());
        String after = tracker.title();
        if (!after.equals(before) || consume) {
            LOG.info(after.isEmpty()
                    ? "Title BML reports no sorcery title"
                    : "Captured sorcery title from title BML: " + after);
        }
        return consume;
    }

    public static boolean portraitMouseWheeled(HeadsUpDisplay currentHud,
                                                int mouseX, int mouseY,
                                                int wheelDelta) {
        HighResHealthBarMod mod = instance;
        HighResHealthBar currentPanel = mod == null ? null : mod.panel;
        return mod != null && mod.hud == currentHud && currentPanel != null
                && currentPanel.mouseWheeledAt(
                currentHud, mouseX, mouseY, wheelDelta);
    }

    public static boolean inventoryWindowReceived(HeadsUpDisplay currentHud,
                                                   InventoryMetaWindowView view) {
        HighResHealthBarMod mod = instance;
        return mod != null && mod.hud == currentHud
                && mod.vehicles.captureWindow(mod.world(), view);
    }

    public static void vehicleActionSent(long[] targets, PlayerAction action) {
        HighResHealthBarMod mod = instance;
        if (mod != null) mod.vehicles.observeAction(
                mod.world(), targets, action);
    }

    public static void vehicleSingleActionSent(long target,
                                               PlayerAction action) {
        vehicleActionSent(new long[]{target}, action);
    }

    public static boolean vehicleEventReceived(String context,
                                               String message) {
        HighResHealthBarMod mod = instance;
        return mod != null && mod.vehicles.observeEvent(context, message);
    }

    private synchronized void rebuildFatigueTracker() {
        fatigue = new FatigueTracker(
                HighResHealthBarSettings.fatigueRefreshSeconds,
                HighResHealthBarSettings.fatigueInitialDelaySeconds);
    }

    private void observeSleepBonusState() {
        World currentWorld = world();
        if (currentWorld == null || currentWorld.getPlayer() == null) return;
        sleepBonus.observe(currentWorld.getPlayer().isSleepBonusActive(),
                System.currentTimeMillis());
    }
}
