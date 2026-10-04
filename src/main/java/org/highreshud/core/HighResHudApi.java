package org.highreshud.core;

/** Public access point for Keybinder, Waypointer, Third Person and future mods. */
public final class HighResHudApi {
    public static final int API_VERSION = 1;
    private static final HighResHudCore CORE = new HighResHudCore();

    private HighResHudApi() {
    }

    public static HighResHudCore core() {
        return CORE;
    }

    public static void runAs(ActionOrigin origin, Runnable action) {
        CORE.actions().runAs(origin, action);
    }
}
