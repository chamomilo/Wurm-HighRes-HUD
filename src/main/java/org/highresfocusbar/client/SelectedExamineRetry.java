package org.highresfocusbar.client;

/** Retry/backoff state for one selected subject's automatic Examine. */
public final class SelectedExamineRetry {
    private static final long NONE = Long.MIN_VALUE;

    private long subjectId = NONE;
    private boolean complete;
    private int attempts;
    private long nextAttemptAt;

    public boolean shouldAttempt(long selectedId, boolean inRange, long now) {
        if (subjectId != selectedId) {
            subjectId = selectedId;
            complete = false;
            attempts = 0;
            nextAttemptAt = 0L;
        }
        if (complete) return false;
        // The first request is immediate even for a remote selection. Further
        // requests wait until the world renderable enters interaction range.
        return attempts == 0 || inRange && now >= nextAttemptAt;
    }

    public void attempted(long selectedId, long now) {
        if (subjectId != selectedId) return;
        attempts++;
        int shift = Math.max(0, Math.min(3, attempts - 1));
        nextAttemptAt = now + (3L << shift) * 1_000_000_000L;
    }

    public void completed(long selectedId) {
        if (subjectId == selectedId) complete = true;
    }

    public long subjectId() {
        return subjectId;
    }

    public void clear() {
        subjectId = NONE;
        complete = false;
        attempts = 0;
        nextAttemptAt = 0L;
    }
}
