package org.highreshealthbar.client;

import com.wurmonline.client.game.World;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.game.inventory.InventoryMetaWindowControl;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.shared.constants.PlayerAction;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.highreshud.core.ActionOrigin;
import org.highreshud.core.HighResHudApi;
import org.highreshud.client.HiddenExamineCoordinator;
import org.highreshud.client.HighResHudRuntime;

/**
 * Owns the complete lifecycle of item-vehicle metadata. The HUD panel only
 * consumes formatted values; hidden inventory windows, exact Examine replies,
 * retries and stale-state cleanup stay isolated here.
 */
final class VehicleMetadataController {
    private static final long NO_SOURCE_ID = -10L;
    private static final long NONE = Long.MIN_VALUE;
    private static final long OPEN_RETRY_NANOS = 5_000_000_000L;
    private static final int MAX_OPEN_ATTEMPTS = 3;
    private static final long EXAMINE_TIMEOUT_NANOS = 10_000_000_000L;
    private static final long EXAMINE_RETRY_NANOS = 10_000_000_000L;
    private static final int MAX_EXAMINE_ATTEMPTS = 3;

    private final Logger log;
    private final VehicleStatusCache status = new VehicleStatusCache();

    private long openRequestId = NONE;
    private long openRequestSentAt;
    private int openRequestAttempts;
    private String openRequestName = "";
    private long metadataVehicleId = NONE;
    private InventoryMetaWindowView metadataWindow;

    private long automaticExamineId = NONE;
    private long automaticExamineSentAt;
    private int automaticExamineAttempts;
    private boolean automaticExamineSending;
    private long pendingExamineId = NONE;
    private long pendingExamineSentAt;
    private boolean suppressPendingExamine;

    VehicleMetadataController(Logger log) {
        this.log = log;
    }

    synchronized void tick(HeadsUpDisplay hud) {
        World world = hud == null ? null : hud.getWorld();
        CreatureCellRenderable vehicle = currentVehicle(world);
        if (vehicle == null) {
            reset(world);
            return;
        }

        long vehicleId = vehicle.getId();
        requestExactStats(hud, world, vehicleId);
        if (metadataVehicleId != NONE && metadataVehicleId != vehicleId) {
            closeMetadataWindow(world);
        }
        if (inventoryItem(world, vehicleId) != null) {
            clearOpenRequest();
            return;
        }
        if (world.getServerConnection() == null) return;

        long now = System.nanoTime();
        if (openRequestId != vehicleId) {
            openRequestId = vehicleId;
            openRequestAttempts = 0;
            openRequestName = vehicle.getCreatureData() == null ? ""
                    : vehicle.getCreatureData().getName();
        } else if (now - openRequestSentAt < OPEN_RETRY_NANOS) {
            return;
        }
        if (openRequestAttempts >= MAX_OPEN_ATTEMPTS) return;

        openRequestAttempts++;
        openRequestSentAt = now;
        HighResHudApi.runAs(ActionOrigin.INTERNAL,
                () -> world.getServerConnection().sendSingleAction(
                        NO_SOURCE_ID, vehicleId, PlayerAction.OPEN));
    }

    synchronized String[] stats(World world, long vehicleId,
                                String fallbackDetails) {
        String[] result = status.observeFallback(vehicleId, fallbackDetails);
        InventoryMetaItem item = inventoryItem(world, vehicleId);
        if (item != null) {
            result = status.observeMetadata(vehicleId,
                    item.getQuality(), item.getDamage());
        }
        return result;
    }

    synchronized boolean captureWindow(World world,
                                       InventoryMetaWindowView view) {
        long requestedId = openRequestId;
        if (view == null || requestedId == NONE) return false;
        InventoryMetaItem root = view.getRootItem();
        boolean idMatches = root != null && root.getId() == requestedId;
        boolean nameMatches = sameLooseName(view.getWindowName(),
                openRequestName);
        if (!idMatches && !nameMatches) return false;

        closeMetadataWindow(world);
        metadataVehicleId = requestedId;
        metadataWindow = view;
        clearOpenRequest();
        return true;
    }

    synchronized void observeAction(World world, long[] targets,
                                    PlayerAction action) {
        if (!isExamine(action)) return;
        CreatureCellRenderable vehicle = currentVehicle(world);
        if (vehicle == null || targets == null || targets.length != 1
                || targets[0] != vehicle.getId()) {
            clearPendingExamine();
            return;
        }
        pendingExamineId = targets[0];
        pendingExamineSentAt = System.nanoTime();
        suppressPendingExamine = automaticExamineSending;
    }

    synchronized boolean observeEvent(String context, String message) {
        long vehicleId = pendingExamineId;
        if (vehicleId == NONE) return false;
        if (System.nanoTime() - pendingExamineSentAt > EXAMINE_TIMEOUT_NANOS) {
            clearPendingExamine();
            return false;
        }
        if (context == null || !":event".equalsIgnoreCase(context.trim())) {
            return false;
        }
        if ("--".equals(MountStatusText.vehicleQuality(message))
                || "--".equals(MountStatusText.vehicleDamage(message))) {
            return false;
        }

        status.observeExamine(vehicleId, message);
        boolean consume = suppressPendingExamine;
        clearPendingExamine();
        return consume;
    }

    synchronized void reset(World world) {
        closeMetadataWindow(world);
        status.clear();
        clearOpenRequest();
        automaticExamineId = NONE;
        automaticExamineSentAt = 0L;
        automaticExamineAttempts = 0;
        automaticExamineSending = false;
        clearPendingExamine();
    }

    private void requestExactStats(HeadsUpDisplay hud, World world,
                                   long vehicleId) {
        if (status.isExact(vehicleId) || hud == null || world == null
                || world.getServerConnection() == null) {
            return;
        }
        long now = System.nanoTime();
        if (automaticExamineId != vehicleId) {
            automaticExamineId = vehicleId;
            automaticExamineSentAt = 0L;
            automaticExamineAttempts = 0;
        }
        if (automaticExamineAttempts >= MAX_EXAMINE_ATTEMPTS
                || automaticExamineSentAt != 0L
                && now - automaticExamineSentAt < EXAMINE_RETRY_NANOS) {
            return;
        }

        HiddenExamineCoordinator.Claim claim =
                HighResHudRuntime.claimHiddenExamine(
                        "healthbar.vehicle", vehicleId);
        if (claim == HiddenExamineCoordinator.Claim.WAIT) return;

        automaticExamineSentAt = now;
        automaticExamineAttempts++;
        if (claim == HiddenExamineCoordinator.Claim.JOIN) {
            pendingExamineId = vehicleId;
            pendingExamineSentAt = now;
            suppressPendingExamine = true;
            return;
        }
        automaticExamineSending = true;
        try {
            HighResHudApi.runAs(ActionOrigin.INTERNAL,
                    () -> hud.sendAction(PlayerAction.EXAMINE, vehicleId));
        } catch (Throwable error) {
            log.log(Level.FINE, "Unable to request exact vehicle QL/Dam", error);
        } finally {
            automaticExamineSending = false;
        }
    }

    private InventoryMetaItem inventoryItem(World world, long vehicleId) {
        InventoryMetaWindowView captured = metadataVehicleId == vehicleId
                ? metadataWindow : null;
        InventoryMetaItem item = itemFromView(captured, vehicleId);
        if (item != null || world == null
                || world.getInventoryManager() == null) return item;

        InventoryMetaWindowControl control = world.getInventoryManager()
                .getWindow(vehicleId);
        return control instanceof InventoryMetaWindowView
                ? itemFromView((InventoryMetaWindowView) control, vehicleId)
                : null;
    }

    private static InventoryMetaItem itemFromView(InventoryMetaWindowView view,
                                                   long vehicleId) {
        if (view == null) return null;
        InventoryMetaItem item = view.getItem(vehicleId);
        return item == null ? view.getRootItem() : item;
    }

    private void closeMetadataWindow(World world) {
        InventoryMetaWindowView captured = metadataWindow;
        metadataWindow = null;
        metadataVehicleId = NONE;
        if (captured != null && world != null
                && world.getServerConnection() != null) {
            world.getServerConnection().sendInventoryWindowClosed(
                    captured.getWindowId());
        }
    }

    private void clearOpenRequest() {
        openRequestId = NONE;
        openRequestSentAt = 0L;
        openRequestAttempts = 0;
        openRequestName = "";
    }

    private void clearPendingExamine() {
        pendingExamineId = NONE;
        pendingExamineSentAt = 0L;
        suppressPendingExamine = false;
    }

    private static CreatureCellRenderable currentVehicle(World world) {
        CreatureCellRenderable vehicle = world == null
                || world.getPlayer() == null ? null
                : world.getPlayer().getCarrierCreature();
        return vehicle != null && vehicle.isItem() ? vehicle : null;
    }

    private static boolean isExamine(PlayerAction action) {
        if (action == null) return false;
        short id = action.getId();
        return id == PlayerAction.EXAMINE.getId()
                || id == PlayerAction.DEFAULT_ACTION.getId();
    }

    private static boolean sameLooseName(String left, String right) {
        String a = clean(left).toLowerCase(Locale.ROOT);
        String b = clean(right).toLowerCase(Locale.ROOT);
        return !a.isEmpty() && !b.isEmpty()
                && (a.equals(b) || a.contains(b) || b.contains(a));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
