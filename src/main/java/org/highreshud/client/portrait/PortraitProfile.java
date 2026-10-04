package org.highreshud.client.portrait;

public final class PortraitProfile {
    public enum MatchType { EXACT, PREFIX }
    public enum Anchor { AUTO, HEAD, JAW, NECK, BOUNDS, CUSTOM }
    public enum Mode { FACE, BUST, FULL_MODEL }

    public String id = "";
    public String match = "";
    public String resourcePath = "";
    public MatchType matchType = MatchType.EXACT;

    public Anchor anchor = Anchor.AUTO;
    public String joint = "";
    public float offsetX;
    public float offsetY;
    public float offsetZ;

    // Camera X/Y are normalized to model height. Distance is a multiplier of
    // the automatic bounds-derived distance, keeping it useful for every scale.
    public float cameraX;
    public float cameraY;
    public float cameraDistance = 1f;
    public float fov = 35f;

    public float zoom = 1f;
    public float zoomMin = 0.65f;
    public float zoomMax = 1.65f;
    public float yaw;
    public float yawMin = -35f;
    public float yawMax = 35f;
    public float pitch;
    public float pitchMin = -15f;
    public float pitchMax = 15f;

    public Mode mode = Mode.FACE;
    public float cropX;
    public float cropY;
    public float cropWidth = 1f;
    public float cropHeight = 1f;

    public float autoTurnSpeed = 6f;
    public boolean mouseRotation = true;
    public String fallbackIcon = "";

    public static PortraitProfile defaults(String mapping, String resourcePath) {
        PortraitProfile profile = new PortraitProfile();
        profile.match = clean(mapping);
        profile.resourcePath = clean(resourcePath);
        if (!profile.match.startsWith("model.creature.")) {
            profile.mode = Mode.FULL_MODEL;
            profile.anchor = Anchor.BOUNDS;
            profile.yawMin = -180f;
            profile.yawMax = 180f;
        }
        return profile;
    }

    public PortraitProfile copy() {
        PortraitProfile result = new PortraitProfile();
        result.id = id;
        result.match = match;
        result.resourcePath = resourcePath;
        result.matchType = matchType;
        result.anchor = anchor;
        result.joint = joint;
        result.offsetX = offsetX;
        result.offsetY = offsetY;
        result.offsetZ = offsetZ;
        result.cameraX = cameraX;
        result.cameraY = cameraY;
        result.cameraDistance = cameraDistance;
        result.fov = fov;
        result.zoom = zoom;
        result.zoomMin = zoomMin;
        result.zoomMax = zoomMax;
        result.yaw = yaw;
        result.yawMin = yawMin;
        result.yawMax = yawMax;
        result.pitch = pitch;
        result.pitchMin = pitchMin;
        result.pitchMax = pitchMax;
        result.mode = mode;
        result.cropX = cropX;
        result.cropY = cropY;
        result.cropWidth = cropWidth;
        result.cropHeight = cropHeight;
        result.autoTurnSpeed = autoTurnSpeed;
        result.mouseRotation = mouseRotation;
        result.fallbackIcon = fallbackIcon;
        return result;
    }

    /** Fields that affect the off-screen render, excluding the later UI crop. */
    public String renderKey() {
        return anchor + "|" + joint + '|'
                + bits(offsetX) + '|' + bits(offsetY) + '|' + bits(offsetZ) + '|'
                + bits(cameraX) + '|' + bits(cameraY) + '|'
                + bits(cameraDistance) + '|' + bits(fov) + '|'
                + bits(zoom) + '|' + bits(yaw) + '|' + bits(pitch) + '|'
                + mode;
    }

    public void normalize() {
        fov = clamp(finite(fov, 35f), 1f, 160f);
        cameraDistance = clamp(finite(cameraDistance, 1f), 0.01f, 50f);
        zoomMin = clamp(finite(zoomMin, 0.65f), 0.01f, 50f);
        zoomMax = clamp(finite(zoomMax, 1.65f), zoomMin, 50f);
        // The automatic ranges describe motion only. Manual authoring must be
        // able to leave them without silently snapping the chosen composition.
        zoom = clamp(finite(zoom, 1f), 0.01f, 50f);
        yawMin = clamp(finite(yawMin, -35f), -3600f, 3600f);
        yawMax = clamp(finite(yawMax, 35f), yawMin, 3600f);
        yaw = finite(yaw, 0f);
        pitchMin = clamp(finite(pitchMin, -15f), -3600f, 3600f);
        pitchMax = clamp(finite(pitchMax, 15f), pitchMin, 3600f);
        pitch = finite(pitch, 0f);
        cropWidth = clamp(cropWidth, 0.05f, 1f);
        cropHeight = clamp(cropHeight, 0.05f, 1f);
        cropX = clamp(cropX, 0f, 1f - cropWidth);
        cropY = clamp(cropY, 0f, 1f - cropHeight);
        autoTurnSpeed = clamp(autoTurnSpeed, 0f, 180f);
        match = clean(match);
        resourcePath = clean(resourcePath);
        joint = clean(joint);
        fallbackIcon = clean(fallbackIcon);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float finite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static int bits(float value) {
        return Float.floatToIntBits(value);
    }
}
