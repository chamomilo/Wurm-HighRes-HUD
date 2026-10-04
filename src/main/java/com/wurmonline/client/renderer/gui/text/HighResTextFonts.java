package com.wurmonline.client.renderer.gui.text;

import java.awt.Font;

public final class HighResTextFonts {
    private HighResTextFonts() {
    }

    public static TextFont regular() {
        try {
            return new SimpleTextFont(new Font("SansSerif", Font.PLAIN, 14), true);
        } catch (Throwable ignored) {
            return TextFont.getText();
        }
    }

    public static TextFont bold() {
        try {
            return new SimpleTextFont(new Font("SansSerif", Font.BOLD, 14), true);
        } catch (Throwable ignored) {
            return TextFont.getBoldText();
        }
    }

    public static TextFont title() {
        try {
            return new SimpleTextFont(new Font("SansSerif", Font.BOLD, 13), true);
        } catch (Throwable ignored) {
            return TextFont.getBoldText();
        }
    }

    public static TextFont small() {
        try {
            return new SimpleTextFont(new Font("SansSerif", Font.BOLD, 11), true);
        } catch (Throwable ignored) {
            return TextFont.getFixedSizeText();
        }
    }
}
