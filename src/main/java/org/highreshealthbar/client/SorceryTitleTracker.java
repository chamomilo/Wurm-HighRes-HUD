package org.highreshealthbar.client;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

import java.util.Locale;

/** Extracts the ability/sorcery prefix from the stock title-selection BML. */
final class SorceryTitleTracker {
    private static final String KNOWN_AS = "you are currently known as:";
    private static final long RESPONSE_TIMEOUT_NANOS = 12_000_000_000L;
    private static final long RETRY_DELAY_NANOS = 20_000_000_000L;
    private static final int MAX_ATTEMPTS = 3;

    private long nextQueryNanos;
    private long pendingDeadlineNanos;
    private boolean pending;
    private boolean resolved;
    private int attempts;
    private String title = "";

    SorceryTitleTracker(int initialDelaySeconds) {
        nextQueryNanos = System.nanoTime()
                + Math.max(0L, initialDelaySeconds) * 1_000_000_000L;
    }

    void tick(HeadsUpDisplay hud) {
        if (hud == null) return;
        long now = System.nanoTime();
        synchronized (this) {
            if (resolved || attempts >= MAX_ATTEMPTS) return;
            if (pending) {
                if (now <= pendingDeadlineNanos) return;
                pending = false;
            }
            if (now < nextQueryNanos) return;
            pending = true;
            attempts++;
            pendingDeadlineNanos = now + RESPONSE_TIMEOUT_NANOS;
            nextQueryNanos = now + RETRY_DELAY_NANOS;
        }
        hud.handleInput("/titles");
    }

    synchronized boolean capture(String bml, String playerName) {
        String parsed = parse(bml, playerName);
        if (parsed == null) return false;
        boolean consume = pending;
        pending = false;
        resolved = true;
        title = parsed;
        return consume;
    }

    synchronized String title() {
        return title;
    }

    /**
     * @return the current prefix, an empty string when the title dialog proves
     * there is no prefix, or {@code null} when this is unrelated BML.
     */
    static String parse(String bml, String playerName) {
        if (bml == null || playerName == null || playerName.trim().isEmpty()) {
            return null;
        }

        String lower = bml.toLowerCase(Locale.ROOT);
        int knownAs = lower.indexOf(KNOWN_AS);
        if (knownAs < 0) return null;

        int valueStart = knownAs + KNOWN_AS.length();
        String lowerPlayerName = playerName.toLowerCase(Locale.ROOT);
        int playerStart = lower.indexOf(lowerPlayerName, valueStart);
        while (playerStart >= valueStart
                && playerStart > 0
                && !Character.isWhitespace(bml.charAt(playerStart - 1))) {
            playerStart = lower.indexOf(
                    lowerPlayerName, playerStart + lowerPlayerName.length());
        }
        if (playerStart < valueStart) return null;

        return bml.substring(valueStart, playerStart).trim();
    }
}
