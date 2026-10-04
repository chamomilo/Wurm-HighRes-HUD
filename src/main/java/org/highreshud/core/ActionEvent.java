package org.highreshud.core;

import java.util.Arrays;

/** Immutable action envelope shared with current and future client modules. */
public final class ActionEvent {
    private final long sourceId;
    private final long[] targetIds;
    private final short actionId;
    private final String actionName;
    private final ActionOrigin origin;

    public ActionEvent(long sourceId, long[] targetIds, short actionId,
                       String actionName, ActionOrigin origin) {
        this.sourceId = sourceId;
        this.targetIds = targetIds == null ? new long[0] : targetIds.clone();
        this.actionId = actionId;
        this.actionName = actionName == null ? "" : actionName;
        this.origin = origin == null ? ActionOrigin.UNKNOWN : origin;
    }

    public long sourceId() {
        return sourceId;
    }

    public long[] targetIds() {
        return targetIds.clone();
    }

    public short actionId() {
        return actionId;
    }

    public String actionName() {
        return actionName;
    }

    public ActionOrigin origin() {
        return origin;
    }

    @Override
    public String toString() {
        return "ActionEvent{" + actionId + ", origin=" + origin
                + ", targets=" + Arrays.toString(targetIds) + '}';
    }
}
