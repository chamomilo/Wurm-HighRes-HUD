package org.highresfightinghud.client;

import com.wurmonline.client.renderer.gui.text.FightingTextFonts;
import com.wurmonline.client.renderer.gui.text.TextFont;

public final class FightingFonts {
    private FightingFonts() {
    }

    public static TextFont regular() {
        return FightingTextFonts.regular();
    }

    public static TextFont bold() {
        return FightingTextFonts.bold();
    }

    public static TextFont title() {
        return FightingTextFonts.title();
    }

    public static TextFont small() {
        return FightingTextFonts.small();
    }
}
