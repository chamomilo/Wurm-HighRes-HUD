package org.highreshud.client.i18n;

import java.util.Locale;

/** Full descriptions are independent of compact captions and resolve in the active language. */
public final class HudHoverText {
    private HudHoverText() { }

    public static String column(String source, boolean friends, String nativeHint) {
        String key = source.trim().toUpperCase(Locale.ROOT);
        switch (key) {
            case "QL": return HudMessages.tr("Item quality");
            case "DMG": return HudMessages.tr("Item damage");
            case "WEIGHT": return HudMessages.tr("Item weight");
            case "VOLUME": return HudMessages.tr("Item volume");
            case "PRICE": return HudMessages.tr("Item price");
            case "I": return HudMessages.tr("Item material type");
            case "NAME": return HudMessages.tr(friends ? "Character name" : "Item name");
            case "STATUS": return HudMessages.tr("Player online status");
            default: return HudMessages.tr(nativeHint == null || nativeHint.trim().isEmpty() ? source : nativeHint);
        }
    }

    public static String channel(String source) {
        String key = source.startsWith(":") ? source.substring(1) : source;
        switch (key.toUpperCase(Locale.ROOT)) {
            case "LOCAL": return HudMessages.tr("Local chat");
            case "GLOBAL": return HudMessages.tr("Global chat");
            case "TRADE": return HudMessages.tr("Trade chat");
            case "VILLAGE": return HudMessages.tr("Village chat");
            case "ALLIANCE": return HudMessages.tr("Alliance chat");
            case "KINGDOM": return HudMessages.tr("Kingdom chat");
            case "CA HELP": return HudMessages.tr("Community assistant help chat");
            case "GM HELP": return HudMessages.tr("Game master help chat");
            case "RECRUIT": return HudMessages.tr("Recruitment chat");
            case "INFO": case "INFOS": return HudMessages.tr("Information channel");
            case "EVENT": case "EVENTS": return HudMessages.tr("Event log");
            case "COMBAT": return HudMessages.tr("Combat log");
            case "SKILLS": return HudMessages.tr("Skill changes");
            case "FRIENDS": return HudMessages.tr("Friends list");
            default: return source;
        }
    }
}
