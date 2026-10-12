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
    public static final int TARGET_WIDTH = 332;
    public static final int TARGET_HEIGHT = 84;

    public static final int PANEL_WIDTH = 520;
    public static final int PANEL_HEIGHT = 186;
    public static final int OUTER_INSET = 4;

    public static final int NO_TARGET_X = 4;
    public static final int NO_TARGET_Y = 4;
    public static final int NO_TARGET_WIDTH = 78;
    public static final int NO_TARGET_HEIGHT = 20;
    public static final int PORTRAIT_X = 15;
    public static final int PORTRAIT_Y = 28;
    public static final int PORTRAIT_SIZE = 52;
    public static final int INFO_X = 84;
    public static final int INFO_Y = 4;
    public static final int INFO_WIDTH = 248;
    public static final int INFO_HEIGHT = 80;
    public static final int FOCUS_X = 4, FOCUS_Y = 86, FOCUS_WIDTH = 328, FOCUS_HEIGHT = 24;
    public static final int FOCUS_BUTTON_X = 7, FOCUS_BUTTON_Y = 88;
    public static final int FOCUS_BUTTON_WIDTH = 96, FOCUS_BUTTON_HEIGHT = 20;
    public static final int FOCUS_TEXT_X = FOCUS_BUTTON_X + FOCUS_BUTTON_WIDTH + 6, FOCUS_TEXT_GAP = 6;

    public static final int ANALYSIS_X = 4;
    public static final int ANALYSIS_Y = 112;
    public static final int ANALYSIS_WIDTH = 328;
    public static final int ANALYSIS_HEIGHT = 70;
    public static final int ANALYSIS_TEXT_INSET = 5, ANALYSIS_ROW_HEIGHT = 15;

    public static final int COMBAT_X = 336;
    public static final int COMBAT_Y = 4;
    public static final int STANCE_BUTTON_SIZE = 32, MODE_BUTTON_SIZE = 24;
    public static final int COMBAT_SIZE = 124;
    public static final int COMBAT_HEIGHT = 120, SECTION_TITLE_Y = 14, CONTROL_HEADER_HEIGHT = 15;
    public static final int MODE_X = 462;
    public static final int MODE_Y = 4;
    public static final int MODE_WIDTH = 54;
    public static final int MODE_HEIGHT = COMBAT_HEIGHT;

    public static final int POSITION_X = 336, POSITION_Y = 126, POSITION_WIDTH = 180, POSITION_HEIGHT = 24;
    public static final int POSITION_TEXT_Y = POSITION_Y + 19;
    public static final int DISTANCE_LABEL_X = POSITION_X + 5;
    public static final int DISTANCE_VALUE_X = POSITION_X + 57, DISTANCE_VALUE_WIDTH = 26;
    public static final int RANGE_ICON_X = POSITION_X + 87;
    public static final int FOOTING_LABEL_X = POSITION_X + 108, FOOTING_ICON_X = POSITION_X + 159;
    public static final int POSITION_ICON_Y = POSITION_Y + 4, POSITION_ICON_SIZE = 16;
    public static final int SPECIAL_X = 336, SPECIAL_Y = 152, SPECIAL_WIDTH = 180, SPECIAL_HEIGHT = 30;
    public static final int SPECIAL_BUTTON_SIZE = 28, SPECIAL_GAP = 1, SPECIAL_ICON_INSET = 2;

    public static int specialCellX(int index) { return SPECIAL_X + 3 + index * (SPECIAL_BUTTON_SIZE + SPECIAL_GAP); }
    public static int specialCellY() { return SPECIAL_Y + 1; }
    public static int modeCellX() { return MODE_X + (MODE_WIDTH - MODE_BUTTON_SIZE) / 2; }
    public static int modeCellY(int index) {
        int content = MODE_BUTTON_SIZE * 4 + 2 * 3;
        return MODE_Y + CONTROL_HEADER_HEIGHT + (MODE_HEIGHT - CONTROL_HEADER_HEIGHT - content) / 2
                + index * (MODE_BUTTON_SIZE + 2);
    }

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
        int spacing = 2;
        int content = iconSize * 3 + spacing * 2;
        return COMBAT_X + (COMBAT_SIZE - content) / 2
                + (gridIndex % 3) * (iconSize + spacing);
    }

    public static int stanceCellY(int gridIndex, int iconSize) {
        int spacing = 2;
        int content = iconSize * 3 + spacing * 2;
        return COMBAT_Y + CONTROL_HEADER_HEIGHT + (COMBAT_HEIGHT - CONTROL_HEADER_HEIGHT - content) / 2
                + (gridIndex / 3) * (iconSize + spacing);
    }
}
