package com.wurmonline.client.renderer.gui;

import java.util.Random;

/** Owns portrait rotation, zoom, interpolation and automatic-pose timing. */
final class PortraitPoseController {
    static final long TURN_DURATION = 3_000_000_000L;
    static final long MIN_TURN_DELAY = 60_000_000_000L;
    static final long TURN_DELAY_RANGE = 60_000_000_000L;
    static final float WHEEL_ZOOM_STEP = 0.10f;

    private final Random random;
    private float rotation;
    private float rotationFrom;
    private float rotationTarget;
    private float zoom;
    private float zoomFrom;
    private float zoomTarget;
    private long turnStart;
    private long turnEnd;
    private long nextTurn;

    PortraitPoseController(float initialRotation, Random random, long now) {
        this.random = random;
        rotation = initialRotation;
        rotationFrom = initialRotation;
        rotationTarget = initialRotation;
        nextTurn = now + randomDelay();
    }

    void tick(long now) {
        if (turnEnd > turnStart && now < turnEnd) {
            tickCurrentTransition(now);
            return;
        }
        tickCurrentTransition(now);
        if (now >= nextTurn) randomize(now);
    }

    void randomize(long now) {
        tickCurrentTransition(now);
        rotationFrom = rotation;
        // Triangular distribution: values around zero are more common while
        // the complete -90..+90 range remains reachable.
        rotationTarget = (random.nextFloat() + random.nextFloat() - 1f) * 90f;
        zoomFrom = zoom;
        zoomTarget = distinctRandom(zoom, 0f, 1f, 0.25f);
        turnStart = now;
        turnEnd = now + TURN_DURATION;
        nextTurn = now + randomDelay();
    }

    void adjustZoom(int wheelDelta, long now) {
        if (wheelDelta == 0) return;
        tickCurrentTransition(now);
        // Wurm reports wheel-up as a negative delta.
        zoom = clamp(zoom + (wheelDelta < 0
                ? WHEEL_ZOOM_STEP : -WHEEL_ZOOM_STEP));
        zoomFrom = zoom;
        zoomTarget = zoom;
        rotationFrom = rotation;
        rotationTarget = rotation;
        turnStart = 0L;
        turnEnd = 0L;
        nextTurn = now + randomDelay();
    }

    float rotation() {
        return rotation;
    }

    float zoom() {
        return zoom;
    }

    private void tickCurrentTransition(long now) {
        if (turnEnd <= turnStart) return;
        if (now >= turnEnd) {
            rotation = rotationTarget;
            zoom = zoomTarget;
            return;
        }
        float progress = clamp((now - turnStart)
                / (float) (turnEnd - turnStart));
        float smooth = progress * progress * (3f - 2f * progress);
        rotation = rotationFrom + (rotationTarget - rotationFrom) * smooth;
        zoom = zoomFrom + (zoomTarget - zoomFrom) * smooth;
    }

    private float distinctRandom(float current, float minimum,
                                 float maximum, float minimumChange) {
        float candidate = current;
        for (int attempt = 0; attempt < 8; attempt++) {
            candidate = minimum + random.nextFloat() * (maximum - minimum);
            if (Math.abs(candidate - current) >= minimumChange) break;
        }
        return candidate;
    }

    private long randomDelay() {
        return MIN_TURN_DELAY
                + (long) (random.nextDouble() * TURN_DELAY_RANGE);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
