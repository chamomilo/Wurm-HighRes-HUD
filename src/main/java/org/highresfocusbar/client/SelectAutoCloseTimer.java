package org.highresfocusbar.client;

/**
 * State machine for the Select Bar's optional pointer-aware auto close.
 *
 * <p>An unpinned bar closes only while the pointer is away from it. Pointer
 * presence cancels the deadline completely; leaving starts a fresh full
 * delay. A newly opened unpinned bar still gets a deadline until the HUD
 * reports that the pointer is inside. Selecting a different subject also
 * restarts the full delay while the pointer is outside.</p>
 * Time is supplied by the caller so the behaviour remains deterministic in
 * tests and uses the monotonic {@link System#nanoTime()} clock in game.
 */
public final class SelectAutoCloseTimer {
    public static final long DELAY_NANOS = 5_000_000_000L;
    private static final long NO_DEADLINE = Long.MIN_VALUE;

    private final long delayNanos;
    private boolean pinned = true;
    private boolean open;
    private boolean pointerInside;
    private long deadlineNanos = NO_DEADLINE;

    public SelectAutoCloseTimer() {
        this(DELAY_NANOS);
    }

    SelectAutoCloseTimer(long delayNanos) {
        if (delayNanos <= 0L) {
            throw new IllegalArgumentException("delayNanos must be positive");
        }
        this.delayNanos = delayNanos;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isPointerInside() {
        return pointerInside;
    }

    public void setPinned(boolean pinned, long now) {
        if (this.pinned == pinned) return;
        this.pinned = pinned;
        if (pinned) {
            cancelDeadline();
        } else if (open) {
            if (pointerInside) cancelDeadline();
            else arm(now);
        }
    }

    public void setOpen(boolean open, long now) {
        if (this.open == open) return;
        this.open = open;
        if (!open) {
            pointerInside = false;
            cancelDeadline();
        } else if (!pinned) {
            if (pointerInside) cancelDeadline();
            else arm(now);
        }
    }

    /** Pauses auto close for as long as the pointer remains over the bar. */
    public void pointerPresent() {
        if (!open) return;
        pointerInside = true;
        cancelDeadline();
    }

    /** Starts a fresh full countdown after a real inside-to-outside change. */
    public void pointerAbsent(long now) {
        if (!open || !pointerInside) return;
        pointerInside = false;
        if (!pinned) arm(now);
    }

    /** Gives a newly selected subject its own complete visible interval. */
    public void selectionChanged(long now) {
        if (!open || pinned) return;
        if (pointerInside) cancelDeadline();
        else arm(now);
    }

    /** Returns true once for an elapsed unpinned window. */
    public boolean hasExpired(long now) {
        if (!open || pinned || pointerInside
                || deadlineNanos == NO_DEADLINE
                || now - deadlineNanos < 0L) {
            return false;
        }
        cancelDeadline();
        return true;
    }

    boolean hasDeadline() {
        return deadlineNanos != NO_DEADLINE;
    }

    private void arm(long now) {
        deadlineNanos = now + delayNanos;
    }

    private void cancelDeadline() {
        deadlineNanos = NO_DEADLINE;
    }
}
