package org.highresfightinghud.client;

import java.util.Properties;

public final class HighResFightingHudSettings {
    public static boolean enabledByDefault = true;
    public static String resourcePack =
            "mods/highres-fighting-hud/highres-fighting-hud-resources-1.1.4.jar";
    public static int portraitRenderSize = 512;
    public static String portraitProfilesOverride =
            "mods/highres-fighting-hud/portrait-profiles.properties";
    public static boolean animateGaugeShine = true;
    public static boolean collectCombatKnowledge = true;
    public static String knowledgeDirectory =
            "mods/highres-fighting-hud/knowledge";

    private HighResFightingHudSettings() {
    }

    public static void configure(Properties properties) {
        if (properties == null) return;
        enabledByDefault = bool(properties, "enabledByDefault",
                enabledByDefault);
        resourcePack = text(properties, "resourcePack", resourcePack);
        portraitRenderSize = boundedInt(properties, "portraitRenderSize",
                portraitRenderSize, 256, 1024);
        portraitProfilesOverride = text(properties,
                "portraitProfilesOverride", portraitProfilesOverride);
        animateGaugeShine = bool(properties, "animateGaugeShine",
                animateGaugeShine);
        collectCombatKnowledge = bool(properties, "collectCombatKnowledge",
                collectCombatKnowledge);
        knowledgeDirectory = text(properties, "knowledgeDirectory",
                knowledgeDirectory);
    }

    private static String text(Properties properties, String key,
                               String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty()
                ? fallback : value.trim();
    }

    private static boolean bool(Properties properties, String key,
                                boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int boundedInt(Properties properties, String key,
                                  int fallback, int minimum, int maximum) {
        try {
            int parsed = Integer.parseInt(
                    properties.getProperty(key, "").trim());
            return Math.max(minimum, Math.min(maximum, parsed));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
