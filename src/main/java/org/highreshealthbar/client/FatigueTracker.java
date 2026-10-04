package org.highreshealthbar.client;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FatigueTracker {
    private static final Pattern MESSAGE = Pattern.compile(
            "^\\s*You have\\s+(.+?)\\s+left\\.?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PART = Pattern.compile(
            "(\\d+)\\s*(day|days|hour|hours|minute|minutes|second|seconds)",
            Pattern.CASE_INSENSITIVE);

    private final long refreshNanos;
    private long nextQueryNanos;
    private long pendingDeadlineNanos;
    private boolean pending;
    private long fatigueSeconds = -1L;

    public FatigueTracker(int refreshSeconds, int initialDelaySeconds) {
        refreshNanos = Math.max(30L, refreshSeconds) * 1_000_000_000L;
        nextQueryNanos = System.nanoTime()
                + Math.max(0L, initialDelaySeconds) * 1_000_000_000L;
    }

    public void tick(HeadsUpDisplay hud) {
        if (hud == null) return;
        long now = System.nanoTime();
        synchronized (this) {
            if (pending && now > pendingDeadlineNanos) pending = false;
            if (pending || now < nextQueryNanos) return;
            pending = true;
            pendingDeadlineNanos = now + 12_000_000_000L;
            nextQueryNanos = now + refreshNanos;
        }
        hud.handleInput("/fatigue");
    }

    public synchronized boolean consumeAutomaticReply(String message) {
        if (!pending) return false;
        long parsed = parseSeconds(message);
        if (parsed < 0L) return false;
        pending = false;
        fatigueSeconds = parsed;
        nextQueryNanos = System.nanoTime() + refreshNanos;
        return true;
    }

    public synchronized String shortText() {
        return formatHoursMinutes(fatigueSeconds);
    }

    public synchronized String hoverText() {
        return fatigueSeconds < 0L
                ? "Remaining fatigue: waiting for server"
                : "Remaining fatigue: " + formatHoursMinutes(fatigueSeconds);
    }

    public static long parseSeconds(String message) {
        if (message == null) return -1L;
        Matcher whole = MESSAGE.matcher(message);
        if (!whole.matches()) return -1L;

        String duration = whole.group(1).toLowerCase(Locale.ROOT);
        Matcher parts = PART.matcher(duration);
        long total = 0L;
        boolean found = false;
        while (parts.find()) {
            found = true;
            long value;
            try {
                value = Long.parseLong(parts.group(1));
            } catch (NumberFormatException error) {
                return -1L;
            }
            String unit = parts.group(2);
            if (unit.startsWith("day")) total += value * 86_400L;
            else if (unit.startsWith("hour")) total += value * 3_600L;
            else if (unit.startsWith("minute")) total += value * 60L;
            else total += value;
        }
        return found ? total : -1L;
    }

    public static String formatHoursMinutes(long seconds) {
        if (seconds < 0L) return "--:--";
        long totalMinutes = seconds / 60L;
        return String.format(Locale.ROOT, "%d:%02d", totalMinutes / 60L,
                totalMinutes % 60L);
    }
}
