package org.highresfightinghud.client;

/** Geometry-only estimate of whether a held shield can face the current mob. */
public final class ShieldArc {
    private ShieldArc() {
    }

    public static boolean eligible(boolean hasShield, double distance,
                                   float playerRotation,
                                   float playerX, float playerY,
                                   float targetX, float targetY) {
        if (!hasShield) return false;
        if (distance <= 1.5) return true;
        double targetBearing = Math.toDegrees(Math.atan2(
                targetX - playerX, targetY - playerY));
        double relative = normalize(targetBearing - playerRotation);
        return relative <= 22.5 || relative >= 247.5;
    }

    static double normalize(double degrees) {
        double result = degrees % 360.0;
        return result < 0.0 ? result + 360.0 : result;
    }
}
