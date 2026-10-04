package org.highresfocusbar.client;

/** Resolves the native Select Bar tooltip even for unnamed server actions. */
public final class ActionTooltip {
    private ActionTooltip() {
    }

    public static String resolve(String atlasName, String serverName,
                                 short actionId) {
        String atlas = clean(atlasName);
        if (!atlas.isEmpty()) return atlas;
        String server = clean(serverName);
        if (!server.isEmpty()) return server;
        return "Action " + actionId;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
