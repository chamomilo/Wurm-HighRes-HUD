package org.highresfightinghud.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Serialized quiet Examine probes for equipped items and the combat target. */
public final class CombatExamineQueue {
    private static final long RESPONSE_NANOS = 8_000_000_000L;
    private static final long GAP_NANOS = 500_000_000L;
    private static final Pattern QUALITY = Pattern.compile(
            "(?i)\\bql\\s*[:=]?\\s*([0-9]+(?:[\\.,][0-9]+)?)");
    private static final Pattern POWER = Pattern.compile(
            "(?i)\\[([0-9]+(?:[\\.,][0-9]+)?)\\]");

    public enum Kind { WEAPON, SHIELD, TARGET }

    public interface Sender {
        boolean examine(long targetId);
    }

    private final Deque<Probe> queue = new ArrayDeque<>();
    private final Map<Kind, Snapshot> snapshots = new EnumMap<>(Kind.class);
    private Probe active;
    private long activeUntil;
    private long nextSendAt;

    public synchronized void schedule(Kind kind, long targetId,
                                      String displayName) {
        if (kind == null || targetId < 0L) return;
        remove(kind);
        queue.addLast(new Probe(kind, targetId, clean(displayName)));
    }

    public synchronized void clearTarget() {
        remove(Kind.TARGET);
        snapshots.remove(Kind.TARGET);
    }

    public synchronized void tick(long nowNanos, Sender sender) {
        if (active != null && nowNanos > activeUntil) active = null;
        if (active != null || queue.isEmpty() || nowNanos < nextSendAt
                || sender == null) return;
        active = queue.removeFirst();
        activeUntil = nowNanos + RESPONSE_NANOS;
        nextSendAt = nowNanos + GAP_NANOS;
        if (!sender.examine(active.targetId)) {
            queue.addFirst(active);
            active = null;
        }
    }

    /** Returns true only for a response owned by an active quiet probe. */
    public synchronized boolean observe(String title, String message) {
        if (active == null || !isEventTitle(title) || message == null) {
            return false;
        }
        String value = clean(message);
        String lower = value.toLowerCase(Locale.ROOT);
        if (retryableFailure(lower)) {
            queue.addLast(active);
            active = null;
            return true;
        }
        if (!examineRelated(lower)) return false;

        Float quality = number(value, QUALITY);
        List<String> effects = effects(value, lower);
        snapshots.put(active.kind, new Snapshot(active.targetId,
                active.displayName, quality, effects, value));
        active = null;
        return true;
    }

    public synchronized Snapshot snapshot(Kind kind) {
        return snapshots.get(kind);
    }

    public synchronized boolean pending() {
        return active != null || !queue.isEmpty();
    }

    private void remove(Kind kind) {
        if (active != null && active.kind == kind) active = null;
        Deque<Probe> retained = new ArrayDeque<>();
        for (Probe probe : queue) {
            if (probe.kind != kind) retained.addLast(probe);
        }
        queue.clear();
        queue.addAll(retained);
    }

    private static boolean isEventTitle(String title) {
        if (title == null) return false;
        String value = title.trim().toLowerCase(Locale.ROOT);
        return value.equals("event") || value.equals(":event");
    }

    private static boolean retryableFailure(String value) {
        return value.contains("too far away")
                || value.startsWith("you can't reach ")
                || value.startsWith("you need to get closer")
                || value.endsWith(" is in the way.");
    }

    private static boolean examineRelated(String value) {
        return value.startsWith("you see ")
                || value.startsWith("you are looking at ")
                || value.contains("quality level")
                || value.matches(".*\\bql\\s*[:=].*")
                || value.contains("has been cast on it")
                || value.contains("rune")
                || value.contains("it is made from");
    }

    private static List<String> effects(String original, String lower) {
        List<String> result = new ArrayList<>();
        for (String sentence : original.split("(?<=[.!?])\\s+")) {
            String normalized = sentence.toLowerCase(Locale.ROOT);
            if (normalized.contains("has been cast on it")
                    || normalized.contains("rune")
                    || normalized.contains("imbue")
                    || normalized.contains("resist")) {
                String compact = clean(sentence).replaceAll("\\s+", " ");
                Matcher power = POWER.matcher(compact);
                if (power.find()) compact += " (" + power.group(1) + ")";
                result.add(limit(compact, 72));
            }
        }
        return result;
    }

    private static Float number(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) return null;
        try {
            return Float.valueOf(Float.parseFloat(
                    matcher.group(1).replace(',', '.')));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String limit(String value, int maximum) {
        return value.length() <= maximum ? value
                : value.substring(0, Math.max(0, maximum - 3)) + "...";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class Probe {
        final Kind kind;
        final long targetId;
        final String displayName;

        Probe(Kind kind, long targetId, String displayName) {
            this.kind = kind;
            this.targetId = targetId;
            this.displayName = displayName;
        }
    }

    public static final class Snapshot {
        public final long targetId;
        public final String displayName;
        public final float examinedQuality;
        public final List<String> effects;
        public final String rawDescription;

        Snapshot(long targetId, String displayName, Float quality,
                 List<String> effects, String rawDescription) {
            this.targetId = targetId;
            this.displayName = displayName;
            this.examinedQuality = quality == null ? Float.NaN : quality;
            this.effects = Collections.unmodifiableList(
                    new ArrayList<>(effects));
            this.rawDescription = rawDescription;
        }
    }
}
