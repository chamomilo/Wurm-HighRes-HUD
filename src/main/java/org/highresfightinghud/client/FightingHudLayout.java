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

    public static final int PANEL_WIDTH = 676;
    public static final int PANEL_HEIGHT = 282;
    public static final int OUTER_INSET = 4;

    public static final int NO_TARGET_X = 5;
    public static final int NO_TARGET_Y = 4;
    public static final int NO_TARGET_WIDTH = 96;
    public static final int NO_TARGET_HEIGHT = 19;
    public static final int PORTRAIT_X = 11;
    public static final int PORTRAIT_Y = 31;
    public static final int PORTRAIT_SIZE = 84;
    public static final int INFO_X = 107;
    public static final int INFO_Y = 1;
    public static final int INFO_WIDTH = 314;
    public static final int INFO_HEIGHT = 85;
    public static final int FOCUS_X = 107, FOCUS_Y = 90, FOCUS_WIDTH = 314, FOCUS_HEIGHT = 36;
    public static final int FOCUS_BUTTON_X = 113, FOCUS_BUTTON_Y = 96;
    public static final int FOCUS_BUTTON_WIDTH = 126, FOCUS_BUTTON_HEIGHT = 24;
    public static final int FOCUS_TEXT_X = FOCUS_BUTTON_X + FOCUS_BUTTON_WIDTH + 9, FOCUS_TEXT_GAP = 8;

    public static final int ANALYSIS_X = 8;
    public static final int ANALYSIS_Y = 132;
    public static final int ANALYSIS_WIDTH = 414;
    public static final int ANALYSIS_HEIGHT = 142;
    public static final int ANALYSIS_TEXT_INSET = 11, ANALYSIS_ROW_GAP = 4;

    public static final int COMBAT_X = 430;
    public static final int COMBAT_Y = 4;
    public static final int COMBAT_SIZE = 160;
    public static final int COMBAT_HEIGHT = 152, SECTION_TITLE_Y = 20;
    public static final int MODE_X = 598;
    public static final int MODE_Y = 4;
    public static final int MODE_WIDTH = 68;
    public static final int MODE_HEIGHT = COMBAT_HEIGHT;

    public static final int POSITION_X = 430, POSITION_Y = 164, POSITION_WIDTH = 236, POSITION_HEIGHT = 46;
    public static final int POSITION_TEXT_Y = POSITION_Y + 38;
    public static final int DISTANCE_LABEL_X = POSITION_X + 7;
    public static final int DISTANCE_VALUE_X = POSITION_X + 65, DISTANCE_VALUE_WIDTH = 36;
    public static final int RANGE_ICON_X = POSITION_X + 108;
    public static final int FOOTING_LABEL_X = POSITION_X + 151, FOOTING_ICON_X = POSITION_X + 211;
    public static final int POSITION_ICON_Y = POSITION_Y + 18, POSITION_ICON_SIZE = 24;
    public static final int SPECIAL_X = 430, SPECIAL_Y = 218, SPECIAL_WIDTH = 236, SPECIAL_HEIGHT = 56;
    public static final int SPECIAL_BUTTON_SIZE = 34, SPECIAL_GAP = 4, SPECIAL_ICON_INSET = 5;

    public static int specialCellX(int index) { return SPECIAL_X + 6 + index * (SPECIAL_BUTTON_SIZE + SPECIAL_GAP); }
    public static int specialCellY() { return SPECIAL_Y + 18; }

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
                (COMBAT_HEIGHT - 20 - iconSize * 3) / 3));
        int content = iconSize * 3 + spacing * 2;
        return COMBAT_Y + 18 + (COMBAT_HEIGHT - 18 - content) / 2
                + (gridIndex / 3) * (iconSize + spacing);
    }
}
