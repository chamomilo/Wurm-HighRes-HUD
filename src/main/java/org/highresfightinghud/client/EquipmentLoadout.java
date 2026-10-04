package org.highresfightinghud.client;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lightweight weapon/shield fingerprint read from the live equipment tree. */
public final class EquipmentLoadout {
    public static final EquipmentLoadout EMPTY = new EquipmentLoadout(
            -1L, "", Float.NaN, 0, -1L, "", Float.NaN, 0);

    public final long weaponId;
    public final String weaponName;
    public final float weaponQuality;
    public final int weaponRarity;
    public final long shieldId;
    public final String shieldName;
    public final float shieldQuality;
    public final int shieldRarity;

    private EquipmentLoadout(long weaponId, String weaponName,
                             float weaponQuality, int weaponRarity,
                             long shieldId, String shieldName,
                             float shieldQuality, int shieldRarity) {
        this.weaponId = weaponId;
        this.weaponName = clean(weaponName);
        this.weaponQuality = weaponQuality;
        this.weaponRarity = weaponRarity;
        this.shieldId = shieldId;
        this.shieldName = clean(shieldName);
        this.shieldQuality = shieldQuality;
        this.shieldRarity = shieldRarity;
    }

    public static EquipmentLoadout inspect(InventoryMetaWindowView view) {
        if (view == null || view.getRootItem() == null) return EMPTY;
        List<Candidate> candidates = new ArrayList<>();
        collect(view.getRootItem(), "", candidates);
        Candidate shield = null;
        Candidate weapon = null;
        for (Candidate candidate : candidates) {
            if (candidate.shield && better(candidate, shield)) {
                shield = candidate;
            }
            if (candidate.weapon && better(candidate, weapon)) {
                weapon = candidate;
            }
        }
        return new EquipmentLoadout(
                weapon == null ? -1L : weapon.item.getId(),
                weapon == null ? "" : weapon.item.getDisplayName(),
                weapon == null ? Float.NaN : weapon.item.getQuality(),
                weapon == null ? 0 : weapon.item.getRarity(),
                shield == null ? -1L : shield.item.getId(),
                shield == null ? "" : shield.item.getDisplayName(),
                shield == null ? Float.NaN : shield.item.getQuality(),
                shield == null ? 0 : shield.item.getRarity());
    }

    public boolean hasShield() {
        return shieldId >= 0L;
    }

    public String signature() {
        return weaponId + ":" + weaponQuality + ":" + weaponRarity + '|'
                + shieldId + ":" + shieldQuality + ":" + shieldRarity;
    }

    /** Stable bucket used to keep estimates relevant to the current loadout. */
    public String modelKey() {
        return normalized(weaponName) + ":q" + qualityBand(weaponQuality)
                + ":r" + weaponRarity + "|" + normalized(shieldName)
                + ":q" + qualityBand(shieldQuality) + ":r" + shieldRarity;
    }

    private static void collect(InventoryMetaItem item, String parentPath,
                                List<Candidate> output) {
        if (item == null) return;
        String name = clean(item.getDisplayName()).toLowerCase(Locale.ROOT);
        String path = parentPath + '/' + name;
        boolean held = parentPath.contains("hand")
                || parentPath.contains("weapon")
                || parentPath.contains("shield");
        boolean shield = name.contains("shield");
        boolean weapon = !shield && weaponName(name);
        if (shield || weapon) {
            output.add(new Candidate(item, held, shield, weapon));
        }
        List<InventoryMetaItem> children = item.getChildren();
        if (children == null) return;
        for (InventoryMetaItem child : children) collect(child, path, output);
    }

    private static boolean weaponName(String name) {
        String value = ' ' + name + ' ';
        return value.contains(" sword ") || value.contains(" axe ")
                || value.contains(" maul ") || value.contains(" hammer ")
                || value.contains(" club ") || value.contains(" spear ")
                || value.contains(" staff ") || value.contains(" knife ")
                || value.contains(" dagger ") || value.contains(" scythe ")
                || value.contains(" halberd ") || value.contains(" bow ")
                || value.contains(" weapon ") || value.contains(" pickaxe ");
    }

    private static boolean better(Candidate candidate, Candidate current) {
        if (current == null) return true;
        if (candidate.held != current.held) return candidate.held;
        return candidate.item.getQuality() > current.item.getQuality();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalized(String value) {
        String result = clean(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return result.isEmpty() ? "none" : result;
    }

    private static int qualityBand(float quality) {
        if (!Float.isFinite(quality)) return -1;
        return Math.max(0, Math.min(20, (int) Math.floor(quality / 5f)));
    }

    private static final class Candidate {
        final InventoryMetaItem item;
        final boolean held;
        final boolean shield;
        final boolean weapon;

        Candidate(InventoryMetaItem item, boolean held, boolean shield,
                  boolean weapon) {
            this.item = item;
            this.held = held;
            this.shield = shield;
            this.weapon = weapon;
        }
    }
}
