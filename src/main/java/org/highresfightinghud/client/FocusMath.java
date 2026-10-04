package org.highresfightinghud.client;

public final class FocusMath {
    private FocusMath() {
    }

    public static float smoothHealth(float previous, float current) {
        current = Math.max(0f, Math.min(1f, current));
        if (!Float.isFinite(previous) || previous < 0f) return current;
        return previous * 0.8f + current * 0.2f;
    }

    public static boolean shouldSelectCorpse(long killedId, long selectedId,
                                             long targetId,
                                             long combatTargetId,
                                             long lastTargetId) {
        return killedId != Long.MIN_VALUE && (killedId == selectedId
                || killedId == targetId || killedId == combatTargetId
                || killedId == lastTargetId);
    }

}
