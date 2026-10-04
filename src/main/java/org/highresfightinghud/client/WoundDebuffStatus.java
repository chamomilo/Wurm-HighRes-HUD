package org.highresfightinghud.client;

/** Builds the conservative status shown above the live target wound map. */
public final class WoundDebuffStatus {
    private WoundDebuffStatus() {
    }

    /**
     * The stock wound packet exposes the live wound and its coarse body-item
     * parent, but not the exact server wound location which selects accuracy,
     * dodge, parry or skill modifiers. Report the guaranteed wound-penalty
     * state without inventing a more specific debuff category.
     */
    public static String line(int woundCount, boolean loaded) {
        if (!loaded) return "Debuff: reading...";
        return woundCount > 0
                ? "Debuff: wound penalties applied"
                : "Debuff: none detected";
    }
}
