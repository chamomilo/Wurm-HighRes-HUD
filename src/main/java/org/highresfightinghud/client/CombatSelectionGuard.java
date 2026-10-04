package org.highresfightinghud.client;

/**
 * Edge-triggered guard which keeps the stock Select Bar empty while Wurm's
 * combat-start callbacks settle. Target state is intentionally independent.
 */
public final class CombatSelectionGuard {
    public static final long DEFAULT_SUPPRESSION_NANOS = 1_500_000_000L;

    private boolean fighting;
    private long suppressUntil;

    /** Returns true exactly once when combat is entered. */
    public synchronized boolean update(boolean nextFighting, long now) {
        boolean entered = nextFighting && !fighting;
        fighting = nextFighting;
        if (entered) {
            suppressUntil = now + DEFAULT_SUPPRESSION_NANOS;
        } else if (!nextFighting) {
            suppressUntil = 0L;
        }
        return entered;
    }

    public synchronized boolean shouldSuppress(long now) {
        return fighting && now < suppressUntil;
    }
}
