package com.wurmonline.client.renderer.gui;

import org.highreshud.client.i18n.HudMessages;

/** Backpack, quiver, vehicle holds and other server-opened native item windows. */
public final class HighResContainerWindow extends HighResNativeWindow {
    HighResContainerWindow(WWindow window, HeadsUpDisplay hud) {
        super("highres.container." + window.name, window.getTitle(), window, hud);
    }
    @Override void install() {
        super.install();
        HighResWindowIntegration.installInventoryHeadings(itemTable());
        HighResWindowIntegration.skinContent(window.getComponent());
    }
    // Translate standard container labels, retaining custom names and server suffixes verbatim.
    @Override String caption() { return containerCaption(window.getTitle()); }
    static String containerCaption(String source) {
        for (String name : new String[]{"backpack", "quiver", "vehicle hold"})
            if (source.equalsIgnoreCase(name)) return HudMessages.tr(name);
        return source;
    }
    @Override WurmTreeList<?> itemTable() {
        if (window instanceof InventoryContainerWindow) return (WurmTreeList<?>) HighResWindowAccess.get(window, "list");
        return ((InventoryListComponent) HighResWindowAccess.get(window, "component")).itemList;
    }
    // This sends the native server close notification and removes its inventory listeners.
    @Override void close() { window.closePressed(); }
}
