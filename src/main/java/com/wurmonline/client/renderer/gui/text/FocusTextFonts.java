package com.wurmonline.client.renderer.gui.text;

import java.awt.Font;

/** Creates exact-size fonts from inside the client font package. */
public final class FocusTextFonts {
    private FocusTextFonts() {
    }

    public static TextFont regular() {
        return create(Font.PLAIN, 14, TextFont.getText());
    }

    public static TextFont bold() {
        return create(Font.BOLD, 14, TextFont.getBoldText());
    }

    public static TextFont title() {
        return create(Font.BOLD, 13, TextFont.getBoldText());
    }

    public static TextFont small() {
        return create(Font.BOLD, 11, TextFont.getFixedSizeText());
    }

    private static TextFont create(int style, int size, TextFont fallback) {
        try {
            return new SimpleTextFont(new Font("SansSerif", style, size), true);
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
