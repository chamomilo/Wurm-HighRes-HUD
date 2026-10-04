package org.highreshealthbar.client;

import java.util.Properties;

public final class HighResHealthBarSettings {
    public static boolean enabledByDefault = true;
    public static String resourcePack =
            "mods/highres-healthbar/highres-healthbar-resources-1.0.5.jar";
    public static int portraitRenderSize = 256;
    public static float portraitRotation = 45f;
    // CharacterWindow renders the complete paper doll at 45 degrees. A narrower
    // FOV clips the head before our top-anchored HUD crop is even sampled.
    public static float portraitFov = 45f;
    public static float portraitCropWidth = 0.40f;
    public static float portraitCropHeight = 0.42f;
    public static int fatigueRefreshSeconds = 60;
    public static int fatigueInitialDelaySeconds = 4;
    public static boolean animateRisingGauges = true;

    private HighResHealthBarSettings() {
    }

    public static void configure(Properties properties) {
        if (properties == null) return;
        enabledByDefault = bool(properties, "enabledByDefault", enabledByDefault);
        resourcePack = properties.getProperty("resourcePack", resourcePack).trim();
        portraitRenderSize = boundedInt(properties, "portraitRenderSize",
                portraitRenderSize, 128, 1024);
        portraitRotation = boundedFloat(properties, "portraitRotation",
                portraitRotation, -90f, 90f);
        // Values below CharacterWindow's 45-degree FOV crop the head out of the
        // off-screen source before HighResHealthBar can select its upper slice.
        portraitFov = boundedFloat(properties, "portraitFov", portraitFov, 45f, 60f);
        portraitCropWidth = boundedFloat(properties, "portraitCropWidth",
                portraitCropWidth, 0.25f, 1f);
        portraitCropHeight = boundedFloat(properties, "portraitCropHeight",
                portraitCropHeight, 0.25f, 1f);
        fatigueRefreshSeconds = boundedInt(properties, "fatigueRefreshSeconds",
                fatigueRefreshSeconds, 30, 600);
        fatigueInitialDelaySeconds = boundedInt(properties, "fatigueInitialDelaySeconds",
                fatigueInitialDelaySeconds, 0, 60);
        animateRisingGauges = bool(properties, "animateRisingGauges", animateRisingGauges);
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
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

    private static float boundedFloat(Properties properties, String key, float fallback,
                                      float minimum, float maximum) {
        try {
            float parsed = Float.parseFloat(properties.getProperty(key, "").trim());
            return Math.max(minimum, Math.min(maximum, parsed));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
