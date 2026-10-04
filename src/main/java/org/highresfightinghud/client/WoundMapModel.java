package org.highresfightinghud.client;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import com.wurmonline.shared.util.ItemTypeUtilites;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only projection of a creature equipment window. Vanilla sends the
 * body as a tree; the focus HUD keeps only body parts which actually contain
 * wounds and places those wounds beside the body-part name.
 */
public final class WoundMapModel {
    private WoundMapModel() {
    }

    public static Snapshot snapshot(InventoryMetaWindowView view) {
        if (view == null || view.getRootItem() == null) return Snapshot.EMPTY;
        List<BodyPart> parts = new ArrayList<>();
        collect(view.getRootItem(), null, parts);
        return parts.isEmpty() ? Snapshot.EMPTY : new Snapshot(parts);
    }

    /** True for the body inventory used by vanilla Look Equipment. */
    public static boolean isEquipmentWindow(InventoryMetaWindowView view) {
        if (view == null || view.getRootItem() == null) return false;
        return containsBodyPart(view.getRootItem());
    }

    private static boolean containsBodyPart(InventoryMetaItem item) {
        if (ItemTypeUtilites.isBodypart(item.getTypeBits())) return true;
        for (InventoryMetaItem child : safeChildren(item)) {
            if (containsBodyPart(child)) return true;
        }
        return false;
    }

    private static void collect(InventoryMetaItem item, BodyPart current,
                                List<BodyPart> parts) {
        if (ItemTypeUtilites.isBodypart(item.getTypeBits())) {
            current = new BodyPart(displayName(item));
        }
        if (ItemTypeUtilites.isWound(item.getTypeBits())) {
            if (current == null) current = new BodyPart("Body");
            if (!parts.contains(current)) parts.add(current);
            current.wounds.add(new Wound(item));
            return;
        }
        for (InventoryMetaItem child : safeChildren(item)) {
            collect(child, current, parts);
        }
    }

    private static List<InventoryMetaItem> safeChildren(InventoryMetaItem item) {
        List<InventoryMetaItem> children = item.getChildren();
        return children == null ? Collections.<InventoryMetaItem>emptyList()
                : children;
    }

    private static String displayName(InventoryMetaItem item) {
        String name = clean(item.getDisplayName());
        if (name.isEmpty()) name = clean(item.getBaseName());
        return name.isEmpty() ? "Body" : name;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Snapshot {
        static final Snapshot EMPTY = new Snapshot(
                Collections.<BodyPart>emptyList());
        private final List<BodyPart> bodyParts;

        private Snapshot(List<BodyPart> bodyParts) {
            this.bodyParts = Collections.unmodifiableList(bodyParts);
        }

        public List<BodyPart> bodyParts() {
            return bodyParts;
        }

        public int woundCount() {
            int count = 0;
            for (BodyPart part : bodyParts) count += part.wounds.size();
            return count;
        }
    }

    public static final class BodyPart {
        private final String name;
        private final List<Wound> wounds = new ArrayList<>();

        private BodyPart(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public List<Wound> wounds() {
            return Collections.unmodifiableList(wounds);
        }
    }

    public static final class Wound {
        private final InventoryMetaItem item;

        private Wound(InventoryMetaItem item) {
            this.item = item;
        }

        public long id() {
            return item.getId();
        }

        public short iconId() {
            return item.getType();
        }

        public String name() {
            return displayName(item);
        }

        public String hoverText() {
            return clean(item.getHoverText());
        }

        public float quality() {
            return item.getQuality();
        }

        public float damage() {
            return item.getDamage();
        }
    }
}
