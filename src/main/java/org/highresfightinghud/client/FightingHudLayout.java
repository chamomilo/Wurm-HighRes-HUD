package org.highresfightinghud.client;

/**
 * Fixed geometry for the unified target, analysis, and native combat HUD.
 *
 * <p>The stock fight controls are still the real Wurm components.  The
 * constants below merely give those components stable places inside the same
 * panel as the target portrait and learned combat card.</p>
 */
public final class FightingHudLayout {
    public static final int ATLAS_WIDTH = 424;
    public static final int ATLAS_HEIGHT = 126;
    public static final int TARGET_ATLAS_X = 0;
    public static final int TARGET_WIDTH = ATLAS_WIDTH;
    public static final int TARGET_HEIGHT = ATLAS_HEIGHT;

    public static final int PANEL_WIDTH = 656;
    public static final int PANEL_HEIGHT = 258;
    public static final int OUTER_INSET = 4;

    public static final int PORTRAIT_X = 8;
    public static final int PORTRAIT_Y = 28;
    public static final int PORTRAIT_SIZE = 90;
    public static final int INFO_X = 106;
    public static final int INFO_WIDTH = 266;

    public static final int ANALYSIS_X = 8;
    public static final int ANALYSIS_Y = 132;
    public static final int ANALYSIS_WIDTH = 414;
    public static final int ANALYSIS_HEIGHT = 118;

    public static final int COMBAT_X = 430;
    public static final int COMBAT_Y = 28;
    public static final int COMBAT_SIZE = 150;
    public static final int MODE_X = 588;
    public static final int MODE_Y = 28;
    public static final int MODE_WIDTH = 58;
    public static final int MODE_HEIGHT = 150;

    public static final int AUX_X = 430;
    public static final int AUX_Y = 184;
    public static final int AUX_WIDTH = 216;
    public static final int AUX_HEIGHT = 66;

    /** Native attack-stance order in a visual 3 x 3 target grid. */
    public static final int[] ATTACK_STANCE_GRID = new int[]{
            6, 7, 1,
            5, 0, 2,
            4, 10, 3
    };

    private FightingHudLayout() {
    }

    public static int totalWidth(int nativeFightWidth) {
        return PANEL_WIDTH;
    }

    public static int totalHeight(int nativeFightHeight) {
        return PANEL_HEIGHT;
    }

    public static int stanceCellX(int gridIndex, int iconSize) {
        int spacing = Math.max(2, Math.min(6,
                (COMBAT_SIZE - iconSize * 3) / 4));
        int content = iconSize * 3 + spacing * 2;
        return COMBAT_X + (COMBAT_SIZE - content) / 2
                + (gridIndex % 3) * (iconSize + spacing);
    }

    public static int stanceCellY(int gridIndex, int iconSize) {
        int spacing = Math.max(2, Math.min(6,
                (COMBAT_SIZE - 20 - iconSize * 3) / 3));
        int content = iconSize * 3 + spacing * 2;
        return COMBAT_Y + 18 + (COMBAT_SIZE - 18 - content) / 2
                + (gridIndex / 3) * (iconSize + spacing);
    }
}
