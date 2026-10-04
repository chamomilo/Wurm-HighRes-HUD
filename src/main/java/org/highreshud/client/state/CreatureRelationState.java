package org.highreshud.client.state;

import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.shared.constants.AttitudeConstants;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Captures the exact server-provided attitude for visible creatures. */
public final class CreatureRelationState implements AttitudeConstants {
    private static final Map<CreatureCellRenderable, Integer> ATTITUDES =
            Collections.synchronizedMap(new WeakHashMap<CreatureCellRenderable, Integer>());

    private CreatureRelationState() {
    }

    public static void observe(CreatureCellRenderable creature, int attitude) {
        if (creature != null) ATTITUDES.put(creature, attitude);
    }

    public static int attitude(CreatureCellRenderable creature) {
        if (creature == null) return ATTITUDE_NEUTRAL;
        Integer exact = ATTITUDES.get(creature);
        if (exact != null) return exact;
        return creature.isFriend() ? ATTITUDE_FRIEND : ATTITUDE_HOSTILE;
    }

    public static String label(CreatureCellRenderable creature) {
        return labelForAttitude(attitude(creature));
    }

    public static String labelForAttitude(int attitude) {
        switch (attitude) {
            case ATTITUDE_ALLY: return "Ally";
            case ATTITUDE_HOSTILE: return "Hostile";
            case ATTITUDE_GM: return "GM";
            case ATTITUDE_EVIL: return "Evil";
            case ATTITUDE_GOOD: return "Good";
            case ATTITUDE_DEV: return "Developer";
            case ATTITUDE_FRIEND: return "Friend";
            case ATTITUDE_NEUTRAL:
            default: return "Neutral";
        }
    }

    public static boolean isHostile(CreatureCellRenderable creature) {
        int attitude = attitude(creature);
        return attitude == ATTITUDE_HOSTILE || attitude == ATTITUDE_EVIL;
    }
}
