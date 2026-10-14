package com.wurmonline.client.game.inventory;

/** Display-only metadata for a world reservation; never inserted into inventory. */
public final class HighResCraftMetadata {
    private HighResCraftMetadata() { }
    public static InventoryMetaItem world(long id, short icon, String name, byte material,
                                          float quality, float damage, float weight, byte rarity) {
        return new InventoryMetaItem(id, -1, icon, name, "", "", material,
                quality, damage, weight, 1f, 1f, 1f, 0, (short)-1, (short)0,
                (byte)0, rarity, (byte)0);
    }
    public static void update(InventoryMetaItem item, float quality, float damage, float weight) {
        item.setData(quality, damage, weight, item.getImproveIconId());
    }
}
