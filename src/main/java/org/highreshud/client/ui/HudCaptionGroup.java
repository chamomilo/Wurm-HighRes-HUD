package org.highreshud.client.ui;

import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import org.chamomilo.wurm.ui.v1.UiButtonLayout;
import org.chamomilo.wurm.ui.v1.UiDensity;
import org.chamomilo.wurm.ui.v1.UiScale;
import org.chamomilo.wurm.ui.v1.UiTypography;

/** One size and ink baseline across every known state/peer, including hover. */
public final class HudCaptionGroup {
    public final int fontPixels, baseline;
    public final UiScale scale;
    public final UiDensity density = UiDensity.HIGH;
    private final Map<String, String> captions = new LinkedHashMap<>();

    public HudCaptionGroup(int width, int height, UiScale scale, String... variants) {
        this(width, height, scale, 128, variants);
    }

    public HudCaptionGroup(int width, int height, UiScale scale, int ceiling, String... variants) {
        if (variants.length == 0) throw new IllegalArgumentException("Empty typography group");
        this.scale = scale;
        int limit = ceiling;
        // Use the SDK's density, outer-edge and readable-floor policy first.
        for (String value : variants) {
            UiButtonLayout fit = UiButtonLayout.fit(new String[]{value}, width, height, density, scale, true);
            limit = Math.min(limit, fit.fontPixels);
            captions.put(value, fit.rows()[0]);
        }
        int chosen = 8, top = 0, bottom = 0;
        int insetY = scale.pixels(2) + scale.pixels(1);
        int insetX = scale.pixels(3) + scale.pixels(1);
        int[] sizes = UiTypography.sizes();
        for (int i = sizes.length - 1; i >= 0; i--) {
            int size = sizes[i];
            if (size > limit) continue;
            int t = 0, b = 0;
            boolean fits = true;
            for (String value : captions.values()) for (boolean bold : new boolean[]{false, true}) {
                Rectangle ink = UiTypography.ink(value, size, bold, density);
                t = Math.min(t, ink.y); b = Math.max(b, ink.y + ink.height);
                fits &= extent(value, size, bold) <= width - 2 * insetX;
            }
            if (fits && b - t <= height - 2 * insetY) {
                chosen = size; top = t; bottom = b; break;
            }
        }
        fontPixels = chosen;
        baseline = (height - (bottom - top)) / 2 - top;
    }

    private int extent(String value, int size, boolean bold) {
        Rectangle ink = UiTypography.ink(value, size, bold, density);
        return Math.max(UiTypography.width(value, size, bold, density), ink.x + ink.width) - Math.min(0, ink.x);
    }

    public String caption(String original) {
        String value = captions.get(original);
        if (value == null) throw new IllegalArgumentException("Unmeasured caption: " + original);
        return value;
    }

    public int textX(String original, boolean bold, int width) {
        String value = caption(original);
        Rectangle ink = UiTypography.ink(value, fontPixels, bold, density);
        return (width - extent(value, fontPixels, bold)) / 2 - Math.min(0, ink.x);
    }
}
