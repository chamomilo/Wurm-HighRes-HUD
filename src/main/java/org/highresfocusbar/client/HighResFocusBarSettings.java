package org.highresfocusbar.client;

import java.util.Properties;

public final class HighResFocusBarSettings {
    public static boolean enabledByDefault = true;
    public static String resourcePack =
            "mods/highres-selectbar/highres-selectbar-resources-1.0.6.jar";
    public static int portraitRenderSize = 512;
    public static String portraitProfilesOverride =
            "mods/highres-selectbar/portrait-profiles.properties";
    public static boolean animateGaugeShine = true;

    private HighResFocusBarSettings() {
    }

    public static void configure(Properties properties) {
        if (properties == null) return;
        enabledByDefault = bool(properties, "enabledByDefault", enabledByDefault);
        resourcePack = text(properties, "resourcePack", resourcePack);
        portraitRenderSize = boundedInt(properties, "portraitRenderSize",
                portraitRenderSize, 256, 1024);
        portraitProfilesOverride = text(properties, "portraitProfilesOverride",
                portraitProfilesOverride);
        animateGaugeShine = bool(properties, "animateGaugeShine",
                animateGaugeShine);
    }

    private static String text(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static boolean bool(Properties properties, String key,
                                boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int boundedInt(Properties properties, String key, int fallback,
                                  int minimum, int maximum) {
        try {
            int parsed = Integer.parseInt(properties.getProperty(key, "").trim());
            return Math.max(minimum, Math.min(maximum, parsed));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
