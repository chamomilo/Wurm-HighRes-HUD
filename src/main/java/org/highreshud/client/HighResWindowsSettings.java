package org.highreshud.client;

import java.util.Properties;

/** Read before HUD initialization without resolving any native GUI classes. */
public final class HighResWindowsSettings {
    public static boolean inventory = true, events = true, chat = true, character = true;
    public static boolean build = true;
    /** 0 follows the native preference; positive values control body text only. */
    public static volatile int fontSize;
    public static volatile int defaultInventoryOpacity = 100;
    public static String catalogExport="";
    public static int clampOpacity(int value) { return Math.max(10, Math.min(100, value)); }
    public static void configureOpacity(Properties source) {
        try { defaultInventoryOpacity = clampOpacity(Integer.parseInt(source.getProperty("inventory.defaultOpacity", "100"))); }
        catch (NumberFormatException invalid) { defaultInventoryOpacity = 100; }
    }
    private HighResWindowsSettings() { }
    public static void configure(Properties source) {
        try { fontSize = Integer.parseInt(source.getProperty("fontSize","0")); }
        catch (NumberFormatException invalid) { fontSize = 0; }
        configureOpacity(source);
        catalogExport=source.getProperty("craft.catalogExport","").trim();
        inventory = Boolean.parseBoolean(source.getProperty("inventory.enabledByDefault", "true"));
        events = Boolean.parseBoolean(source.getProperty("events.enabledByDefault", "true"));
        chat = Boolean.parseBoolean(source.getProperty("chat.enabledByDefault", "true"));
        character = Boolean.parseBoolean(source.getProperty("character.enabledByDefault", "true"));
        build = Boolean.parseBoolean(source.getProperty("build.enabledByDefault", "true"));
    }
}
