package com.wurmonline.client.renderer.gui.text;

import com.wurmonline.client.renderer.backend.Queue;
import org.chamomilo.wurm.ui.v1.UiDensity;
import java.util.HashMap;
import java.util.Map;

/** Shared Chamomilo fonts, preserving the HUD's native lower-edge text coordinates. */
public final class HudTextFonts {
    private static final Map<Integer, TextFont> CACHE = new HashMap<>();
    private HudTextFonts() { }
    public static TextFont regular() { return font(14, false, UiDensity.LOW); }
    public static TextFont bold() { return font(14, true, UiDensity.LOW); }
    public static TextFont title() { return font(14, true, UiDensity.HIGH); }
    public static TextFont small() { return font(12, true, UiDensity.LOW); }

    private static TextFont font(int size, boolean bold, UiDensity density) {
        int key = size + (bold ? 256 : 0) + (density == UiDensity.HIGH ? 512 : 0);
        TextFont result = CACHE.get(key);
        if (result == null) {
            result = new LowerEdgeFont(ChamomiloUiV1Fonts.caption(size, bold, density));
            CACHE.put(key, result);
        }
        return result;
    }

    private static final class LowerEdgeFont extends TextFont {
        private final TextFont delegate;
        LowerEdgeFont(TextFont delegate) { this.delegate = delegate; }
        public void moveTo(int x, int lowerEdge) {
            delegate.moveTo(x, lowerEdge - delegate.getHeight() + delegate.getAscent());
        }
        public int paint(Queue q, String s, float r, float g, float b, float a) {
            return delegate.paint(q, s, r, g, b, a);
        }
        public int getWidth(String s) { return delegate.getWidth(s); }
        public int getWidth(char[] s, int start, int count) { return delegate.getWidth(s, start, count); }
        public int getHeight() { return delegate.getHeight(); }
        public int getAscent() { return delegate.getAscent(); }
        public int getDescent() { return delegate.getDescent(); }
        public int getLeading() { return delegate.getLeading(); }
    }
}
