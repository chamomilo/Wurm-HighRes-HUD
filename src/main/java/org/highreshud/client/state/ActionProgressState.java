package org.highreshud.client.state;

import com.wurmonline.shared.constants.PlayerAction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/** Correlates outgoing actions with the generic timer text sent by Wurm. */
public final class ActionProgressState {
    private static final int MAX_PENDING = 32;
    private static final long MAX_AGE_NANOS = 120_000_000_000L;

    private static final class Pending {
        final String name;
        final long sentAt;

        Pending(String name, long sentAt) {
            this.name = name;
            this.sentAt = sentAt;
        }
    }

    private final Deque<Pending> pending = new ArrayDeque<>();
    private String currentAction = "";

    public synchronized void actionSent(PlayerAction action, int copies) {
        if (action == null) return;
        if (action == PlayerAction.STOP
                || action.getId() == PlayerAction.STOP.getId()) {
            pending.clear();
            currentAction = "";
            return;
        }
        if (action.isInstant() || hasNoProgressTimer(action)) return;
        String name = clean(action.getName());
        if (name.isEmpty()) return;

        int count = action.isAtomic() ? 1 : Math.max(1, Math.min(10, copies));
        long now = System.nanoTime();
        prune(now);
        for (int index = 0; index < count; index++) {
            while (pending.size() >= MAX_PENDING) pending.removeFirst();
            pending.addLast(new Pending(name, now));
        }
    }

    public synchronized void actionStarted(String genericName,
                                           float durationSeconds) {
        if (clean(genericName).isEmpty() || durationSeconds <= 0f) {
            currentAction = "";
            return;
        }
        prune(System.nanoTime());
        Pending next = pending.pollFirst();
        currentAction = next == null ? "" : next.name;
    }

    public synchronized String enhance(String timerTitle) {
        String title = clean(timerTitle);
        String specific = currentAction;
        if (title.isEmpty() || specific.isEmpty()) return title;

        int colon = title.lastIndexOf(':');
        String generic = clean(colon < 0 ? title : title.substring(0, colon));
        String time = colon < 0 ? "" : title.substring(colon).trim();
        String genericLower = generic.toLowerCase(Locale.ROOT);
        String specificLower = specific.toLowerCase(Locale.ROOT);
        if (genericLower.contains(specificLower)
                || sameActionStem(genericLower, specificLower)) {
            return capitalize(title);
        }
        String combined = capitalize(generic) + " " + specific;
        return time.isEmpty() ? combined : combined + time;
    }

    synchronized int pendingCount() {
        return pending.size();
    }

    /**
     * Wurm marks several immediate/server-routing actions as non-instant even
     * though they never call HeadsUpDisplay.setAction. Keeping one of them in
     * the correlation queue would rename the next real timer (for example,
     * Examine followed by Genesis became "Casting Examine").
     */
    private static boolean hasNoProgressTimer(PlayerAction action) {
        short id = action.getId();
        return id == PlayerAction.EXAMINE.getId()
                || id == 2 // Immediate Look Equipment query.
                || id == PlayerAction.TARGET.getId()
                || id == PlayerAction.TARGET_HOSTILE.getId()
                || id == PlayerAction.NO_TARGET.getId()
                || id == 114; // Immediate Attack command.
    }

    private void prune(long now) {
        while (!pending.isEmpty()
                && now - pending.peekFirst().sentAt > MAX_AGE_NANOS) {
            pending.removeFirst();
        }
    }

    private static boolean sameActionStem(String generic, String specific) {
        if (specific.length() < 4) return false;
        return generic.contains(specific.substring(0, 4));
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value
                : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
