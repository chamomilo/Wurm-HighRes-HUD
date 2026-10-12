package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import org.highreshud.client.i18n.HudMessages;

/** Keeps the native item tree, activation label, drag/drop, selection and item menus. */
public final class HighResInventoryWindow extends HighResNativeWindow {
    HighResInventoryWindow(InventoryWindow nativeWindow, HeadsUpDisplay hud) {
        super("highres.inventory", "Inventory", nativeWindow, hud);
    }
    @Override void install() {
        super.install();
        // The native label still receives activation updates; its value now lives in the title.
        WurmBorderPanel body = (WurmBorderPanel) window.getComponent();
        body.setComponent(null, WurmBorderPanel.SOUTH);
        HighResWindowIntegration.installInventoryHeadings(itemTable());
        HighResWindowIntegration.skinContent(window.getComponent());
        window.layout();
    }
    @Override String caption() {
        Object label = HighResWindowAccess.get(window, "itemLabel");
        InventoryMetaItem active = (InventoryMetaItem) HighResWindowAccess.get(label, "item");
        return HudMessages.tr("Inventory") + " · " + HudMessages.tr("Activated: {0}",
                active == null ? HudMessages.tr("None") : active.getDisplayName());
    }
    @Override void close() { window.closePressed(); }
    @Override WurmTreeList<?> itemTable() { return ((InventoryWindow) window).getInventoryListComponent().itemList; }
}
