package org.highreshealthbar.client;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tracks the server's activation-only /fsleep cooldown. */
public final class SleepBonusToggleTracker {
    static final long ACTIVATION_COOLDOWN_MILLIS = 300_000L;
    private static final Pattern DURATION_PART = Pattern.compile(
            "(\\d+)\\s*(day|hour|minute|second)s?",
            Pattern.CASE_INSENSITIVE);

    private boolean stateObserved;
    private boolean active;
    private long activationAvailableAtMillis;

    public synchronized void reset(boolean currentlyActive) {
        stateObserved = true;
        active = currentlyActive;
        activationAvailableAtMillis = 0L;
    }

    public synchronized void observe(boolean currentlyActive, long nowMillis) {
        if (!stateObserved) {
            reset(currentlyActive);
            return;
        }
        if (currentlyActive && !active) {
            activated(nowMillis);
        }
        active = currentlyActive;
    }

    public synchronized void activationRequested(long nowMillis) {
        activationAvailableAtMillis = nowMillis + ACTIVATION_COOLDOWN_MILLIS;
    }

    public synchronized void observeServerMessage(String message,
                                                   long nowMillis) {
        if (message == null) return;
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("you start using your sleep bonus")) {
            active = true;
            stateObserved = true;
            activated(nowMillis);
            return;
        }
        if (normalized.contains("you refrain from using your sleep bonus")) {
            active = false;
            stateObserved = true;
            return;
        }
        if (normalized.contains("you need to wait")
                && normalized.contains(
                "until you can toggle sleep bonus again")) {
            long duration = parseDurationMillis(normalized);
            if (duration > 0L) {
                activationAvailableAtMillis = nowMillis + duration;
            }
            active = false;
            stateObserved = true;
            return;
        }
        if (normalized.contains("you do not have any sleep bonus")) {
            activationAvailableAtMillis = 0L;
        }
    }

    public synchronized boolean canActivate(long nowMillis) {
        return secondsUntilActivation(nowMillis) == 0;
    }

    public synchronized int secondsUntilActivation(long nowMillis) {
        long remaining = activationAvailableAtMillis - nowMillis;
        if (remaining <= 0L) return 0;
        long seconds = (remaining + 999L) / 1_000L;
        return seconds > Integer.MAX_VALUE
                ? Integer.MAX_VALUE : (int) seconds;
    }

    public static String formatCountdown(int seconds) {
        int safe = Math.max(0, seconds);
        int minutes = safe / 60;
        int remainder = safe % 60;
        return minutes + ":" + (remainder < 10 ? "0" : "") + remainder;
    }

    static long parseDurationMillis(String message) {
        if (message == null) return 0L;
        Matcher matcher = DURATION_PART.matcher(message);
        long seconds = 0L;
        while (matcher.find()) {
            long value;
            try {
                value = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
                continue;
            }
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            if ("day".equals(unit)) seconds += value * 86_400L;
            else if ("hour".equals(unit)) seconds += value * 3_600L;
            else if ("minute".equals(unit)) seconds += value * 60L;
            else seconds += value;
        }
        return seconds * 1_000L;
    }

    private void activated(long nowMillis) {
        activationAvailableAtMillis = nowMillis + ACTIVATION_COOLDOWN_MILLIS;
    }
}
