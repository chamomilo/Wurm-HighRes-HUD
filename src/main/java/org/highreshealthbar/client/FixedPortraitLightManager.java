package org.highreshealthbar.client;

import com.wurmonline.client.renderer.Color;
import com.wurmonline.client.renderer.light.PaperDollLightManager;

import java.nio.FloatBuffer;

/**
 * Camera-locked studio lighting for the independent HUD paper doll.
 *
 * <p>The stock paper-doll light has a strong directional component, so rotated
 * models can become almost black on one side. This version keeps most of the
 * illumination ambient and uses a smaller frontal component for shape.</p>
 */
public final class FixedPortraitLightManager extends PaperDollLightManager {
    static final float AMBIENT = 0.62f;
    static final float DIFFUSE = 0.42f;

    public FixedPortraitLightManager() {
        setNeutral(getAmbientColor(), AMBIENT);
        setNeutral(getDiffuseColor(), DIFFUSE);
    }

    @Override
    public void addSunLight(FloatBuffer target) {
        // StateUniformManager's fixed 20-float sun block:
        // direction, diffuse, ambient, specular and trailing parameters.
        target.clear();
        put4(target, 0f, 0f, 2f, 0f);                 // camera-forward direction
        put4(target, DIFFUSE, DIFFUSE, DIFFUSE, 1f); // soft key light
        put4(target, AMBIENT, AMBIENT, AMBIENT, 1f); // even fill light
        put4(target, 0f, 0f, 0f, 0f);                // no moving specular source
        put4(target, 0f, 0f, 0f, 0f);
        target.rewind();
    }

    private static void setNeutral(Color color, float value) {
        color.r = value;
        color.g = value;
        color.b = value;
        color.a = 1f;
    }

    private static void put4(FloatBuffer target, float x, float y,
                             float z, float w) {
        target.put(x).put(y).put(z).put(w);
    }
}
