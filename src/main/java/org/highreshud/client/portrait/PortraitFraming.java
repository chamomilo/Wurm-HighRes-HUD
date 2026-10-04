package org.highreshud.client.portrait;

import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.model.ModelDataBounds;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;

public final class PortraitFraming {
    private static final float REFERENCE_FOV = 35f;

    private PortraitFraming() {
    }

    public static Matrix modelMatrix(ModelResourceWrapper model,
                                     PortraitProfile profile, float aspect) {
        PortraitModelIntrospector.Snapshot snapshot =
                PortraitModelIntrospector.inspect(model, profile);
        ModelDataBounds bounds = snapshot.bounds;
        float height = Math.max(0.001f, bounds.getH1() - bounds.getH0());
        float distance = baseDistance(bounds, profile.mode, aspect)
                * profile.cameraDistance / Math.max(0.01f, profile.zoom);

        Matrix anchor = new Matrix().setTranslation(-snapshot.anchorX,
                -snapshot.anchorH, -snapshot.anchorY);
        Matrix rotation = new Matrix().fromRotation(
                radians(profile.pitch), radians(profile.yaw), 0f);
        Matrix camera = new Matrix().setTranslation(
                (profile.offsetX - profile.cameraX) * height,
                (profile.offsetY - profile.cameraY) * height,
                -distance + profile.offsetZ * height);
        Matrix rotated = new Matrix();
        Matrix result = new Matrix();
        Matrix.mult(camera, rotation, rotated);
        Matrix.mult(rotated, anchor, result);
        return result;
    }

    public static float baseDistance(ModelDataBounds bounds,
                                     PortraitProfile.Mode mode, float aspect) {
        float height = Math.max(0.001f, bounds.getH1() - bounds.getH0());
        float width = Math.max(bounds.getX1() - bounds.getX0(),
                bounds.getY1() - bounds.getY0());
        return baseDistance(height, width, mode, aspect);
    }

    static float baseDistance(float height, float width,
                              PortraitProfile.Mode mode, float aspect) {
        height = Math.max(0.001f, height);
        width = Math.max(0.001f, width);
        float visibleHeight;
        float visibleWidth;
        switch (mode) {
            case FACE:
                visibleHeight = height * 0.32f;
                visibleWidth = height * 0.42f;
                break;
            case BUST:
                visibleHeight = height * 0.58f;
                visibleWidth = Math.min(Math.max(width * 0.72f,
                        height * 0.48f), height * 0.9f);
                break;
            case FULL_MODEL:
            default:
                visibleHeight = height * 1.10f;
                visibleWidth = width * 1.10f;
                break;
        }
        float tangent = (float) Math.tan(Math.toRadians(REFERENCE_FOV * 0.5f));
        float vertical = visibleHeight * 0.5f / tangent;
        float horizontal = visibleWidth * 0.5f
                / (tangent * Math.max(0.1f, aspect));
        return Math.max(0.05f, Math.max(vertical, horizontal));
    }

    static float loopFraction(float elapsedSeconds, float lengthSeconds) {
        float normalized = lengthSeconds > 0.001f
                ? elapsedSeconds / lengthSeconds : elapsedSeconds;
        return normalized - (float) Math.floor(normalized);
    }

    /**
     * Most Wurm idle clips explicitly carry shouldLoop=false. Their final
     * bind/scale keys can visibly enlarge a portrait. A ping-pong workaround
     * traversed that bad tail twice at every turn, producing two consecutive
     * jerks on bear-like rigs. Loop only the authored motion range instead.
     */
    public static float idleFraction(float elapsedSeconds, float lengthSeconds,
                                     boolean authoredLoop) {
        if (authoredLoop) return loopFraction(elapsedSeconds, lengthSeconds);
        float phase = loopFraction(elapsedSeconds, lengthSeconds);
        return 0.0125f + phase * 0.925f;
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }
}
