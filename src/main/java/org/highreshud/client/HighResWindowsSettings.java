package org.highreshud.client;

import java.util.Properties;

/** Read before HUD initialization without resolving any native GUI classes. */
public final class HighResWindowsSettings {
    public static boolean inventory = true, events = true, chat = true, character = true;
    private HighResWindowsSettings() { }
    public static void configure(Properties source) {
        inventory = Boolean.parseBoolean(source.getProperty("inventory.enabledByDefault", "true"));
        events = Boolean.parseBoolean(source.getProperty("events.enabledByDefault", "true"));
        chat = Boolean.parseBoolean(source.getProperty("chat.enabledByDefault", "true"));
        character = Boolean.parseBoolean(source.getProperty("character.enabledByDefault", "true"));
    }
}
