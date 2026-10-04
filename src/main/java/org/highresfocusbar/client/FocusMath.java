package org.highresfocusbar.client;

import java.util.Locale;

public final class FocusMath {
    public enum Bow { NONE, LONG, MEDIUM, SHORT }
    public enum RangeBand { NEUTRAL, GREEN, AMBER, RED }

    private FocusMath() {
    }

    public static int horizontalDistance(float playerX, float playerY,
                                         float subjectX, float subjectY) {
        double dx = playerX - subjectX;
        double dy = playerY - subjectY;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    public static RangeBand rangeBand(Bow bow, int metres) {
        switch (bow) {
            case LONG:
                if (metres == 80) return RangeBand.GREEN;
                return metres >= 40 && metres <= 180
                        ? RangeBand.AMBER : RangeBand.RED;
            case MEDIUM:
                if (metres == 40) return RangeBand.GREEN;
                return metres >= 20 && metres <= 180
                        ? RangeBand.AMBER : RangeBand.RED;
            case SHORT:
                if (metres == 20) return RangeBand.GREEN;
                return metres >= 4 && metres <= 180
                        ? RangeBand.AMBER : RangeBand.RED;
            default:
                return RangeBand.NEUTRAL;
        }
    }

    public static float smoothHealth(float previous, float current) {
        current = Math.max(0f, Math.min(1f, current));
        if (!Float.isFinite(previous) || previous < 0f) return current;
        return previous * 0.8f + current * 0.2f;
    }

    public static String corpseStatus(String name, String modelMapping) {
        String lowerName = name == null ? ""
                : name.trim().toLowerCase(java.util.Locale.ROOT);
        String lowerMapping = modelMapping == null ? ""
                : modelMapping.toLowerCase(java.util.Locale.ROOT);
        if (!lowerName.startsWith("corpse of ")
                || !lowerMapping.contains("corpse")) {
            return "";
        }
        return lowerMapping.contains(".butchered") ? "Butchered" : "Whole";
    }

    public static String qualityDamage(float quality, float damage) {
        return String.format(Locale.ROOT, "QL %.2f · Dam %.2f",
                quality, damage);
    }

    /** Wurm wire request ids are bytes, but -1 is our local no-request flag. */
    public static int unsignedRequestId(byte requestId) {
        return requestId & 0xff;
    }
}
