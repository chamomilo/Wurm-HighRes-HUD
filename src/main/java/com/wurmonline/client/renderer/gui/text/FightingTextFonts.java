package com.wurmonline.client.renderer.gui.text;

/** Shared Chamomilo typography for all compact HUD text roles. */
public final class FightingTextFonts {
    private FightingTextFonts() { }
    public static TextFont regular() { return HudTextFonts.regular(); }
    public static TextFont bold() { return HudTextFonts.bold(); }
    public static TextFont title() { return HudTextFonts.title(); }
    public static TextFont small() { return HudTextFonts.small(); }
}