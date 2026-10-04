package org.highreshealthbar.client;

/**
 * Packs the high-resolution healthbar's additional visibility choices into
 * the unused bits of Wurm's per-profile {@code WindowPosition.flags} value.
 */
public final class HealthBarVisibilityPreferences {
    private static final int SETTINGS_PRESENT = 1 << 6;
    private static final int SHOW_TITLES = 1 << 7;
    private static final int SHOW_NUMBER_VALUES = 1 << 8;
    private static final int SHOW_RIDE = 1 << 9;
    private static final int SHOW_HITCHED_ANIMALS = 1 << 10;
    private static final int OWN_FLAGS = SETTINGS_PRESENT | SHOW_TITLES
            | SHOW_NUMBER_VALUES | SHOW_RIDE | SHOW_HITCHED_ANIMALS;

    private HealthBarVisibilityPreferences() {
    }

    public static int store(int baseFlags, boolean showTitles,
                            boolean showNumberValues, boolean showRide,
                            boolean showHitchedAnimals) {
        int flags = (baseFlags & ~OWN_FLAGS) | SETTINGS_PRESENT;
        if (showTitles) flags |= SHOW_TITLES;
        if (showNumberValues) flags |= SHOW_NUMBER_VALUES;
        if (showRide) flags |= SHOW_RIDE;
        if (showHitchedAnimals) flags |= SHOW_HITCHED_ANIMALS;
        return flags;
    }

    public static boolean hasSavedSettings(int flags) {
        return (flags & SETTINGS_PRESENT) != 0;
    }

    public static boolean showTitles(int flags) {
        return (flags & SHOW_TITLES) != 0;
    }

    public static boolean showNumberValues(int flags) {
        return (flags & SHOW_NUMBER_VALUES) != 0;
    }

    public static boolean showRide(int flags) {
        return (flags & SHOW_RIDE) != 0;
    }

    public static boolean showHitchedAnimals(int flags) {
        return (flags & SHOW_HITCHED_ANIMALS) != 0;
    }
}
