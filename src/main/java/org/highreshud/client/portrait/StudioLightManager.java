package org.highreshud.client.portrait;

import com.wurmonline.client.renderer.Color;
import com.wurmonline.client.renderer.light.PaperDollLightManager;

import java.nio.FloatBuffer;

/** Camera-locked neutral light: profiles do not depend on world time/weather. */
public final class StudioLightManager extends PaperDollLightManager {
    private static final float AMBIENT = 0.62f;
    private static final float DIFFUSE = 0.42f;

    public StudioLightManager() {
        neutral(getAmbientColor(), AMBIENT);
        neutral(getDiffuseColor(), DIFFUSE);
    }

    @Override
    public void addSunLight(FloatBuffer target) {
        target.clear();
        put4(target, 0f, 0f, 2f, 0f);
        put4(target, DIFFUSE, DIFFUSE, DIFFUSE, 1f);
        put4(target, AMBIENT, AMBIENT, AMBIENT, 1f);
        put4(target, 0f, 0f, 0f, 0f);
        put4(target, 0f, 0f, 0f, 0f);
        target.rewind();
    }

    private static void neutral(Color color, float value) {
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
