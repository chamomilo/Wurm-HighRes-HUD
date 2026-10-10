package org.highresfightinghud.client;

import com.wurmonline.shared.constants.FightConstants;
import java.util.Locale;

/** Exact server focus level, with an explicitly estimated initial engagement gate. */
public final class CombatFocusState {
    private boolean fighting;
    private int level, stance, engagement;
    private long lastOptions = Long.MIN_VALUE, pendingUntil;
    private String levelMessage = "";

    public void combatChanged(boolean value) {
        if (fighting == value) return;
        fighting = value;
        engagement = 0;
        lastOptions = Long.MIN_VALUE;
        pendingUntil = 0L;
    }

    public void optionsReceived(long now) {
        if (!fighting) return;
        // The first packet initializes the fight. Burst updates from changing
        // stances must not masquerade as successive combat rounds.
        if (lastOptions == Long.MIN_VALUE) lastOptions = now;
        else if (now - lastOptions >= 1_000_000_000L) {
            engagement = Math.min(3, engagement + 1);
            lastOptions = now;
        }
    }

    public void positionReceived(byte value) { stance = value; }
    public void levelReceived(byte value, String message) {
        level = Math.max(0, Math.min(5, value));
        levelMessage = message == null ? "" : message.trim();
        pendingUntil = 0L;
    }
    public int level() { return level; }
    public String levelMessage() { return levelMessage; }
    public void attemptSent(long now) { pendingUntil = now + 2_000_000_000L; }

    public void observeMessage(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (text.contains("you need to get into the fight more first")) {
            engagement = 0;
            lastOptions = Long.MIN_VALUE;
            pendingUntil = 0L;
        } else if (text.contains("you fail to reach a higher degree of focus")
                || text.contains("already focused to the maximum")) pendingUntil = 0L;
    }

    public String readiness(boolean hasTarget, boolean stunned, boolean nativeEnabled,
                            boolean specialMovesAvailable, boolean focusing, long now) {
        if (!hasTarget) return "No target";
        if (!fighting) return "Not in combat";
        if (stunned) return "Stunned";
        if (stance == FightConstants.ATTACK_PRONE) return "On the ground";
        if (stance == FightConstants.ATTACK_OPEN) return "Imbalanced";
        if (level >= 5) return "Maximum focus";
        if (focusing) return "Focusing...";
        if (now < pendingUntil) return "Request sent";
        if (!nativeEnabled) return "Unavailable";
        if (level > 0 || specialMovesAvailable) return "Ready to try";
        if (engagement < 3) return "Engaging ~" + engagement + "/3";
        return "Ready to try ~";
    }

    public static boolean ready(String status) { return status.startsWith("Ready to try"); }
}
