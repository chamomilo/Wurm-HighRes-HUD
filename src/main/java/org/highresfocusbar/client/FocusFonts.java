package org.highresfocusbar.client;

import com.wurmonline.client.renderer.gui.text.FocusTextFonts;
import com.wurmonline.client.renderer.gui.text.TextFont;

public final class FocusFonts {
    private FocusFonts() {
    }

    public static TextFont regular() {
        return FocusTextFonts.regular();
    }

    public static TextFont bold() {
        return FocusTextFonts.bold();
    }

    public static TextFont title() {
        return FocusTextFonts.title();
    }

    public static TextFont small() {
        return FocusTextFonts.small();
    }
}
