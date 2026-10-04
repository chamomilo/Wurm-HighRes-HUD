package com.wurmonline.client.renderer.gui;

/** Exact-pixel geometry shared by rendering, hit-testing and tests. */
final class HighResHealthBarLayout {
    static final int PANEL_WIDTH = 323;
    static final int PORTRAIT_SIZE = 108;
    static final int CONTENT_HEIGHT = 108;

    // SansSerif Bold 13 rasterizes to an 18 px line box.
    static final int NAME_ONLY_TITLE_HEIGHT = 22;
    static final int TWO_LINE_TITLE_HEIGHT = 42;
    static final int THREE_LINE_TITLE_HEIGHT = 62;
    static final int TITLE_LINE_HEIGHT = 18;
    static final int TITLE_LINE_GAP = 2;
    static final int FOOTER_HEIGHT = 18;
    static final int FOOTER_FIRST_DIVIDER = 75;
    static final int FOOTER_SECOND_DIVIDER = 190;

    static final int MOUNT_ROW_HEIGHT = 20;
    static final int MOUNT_GAUGE_X = 3;
    static final int MOUNT_GAUGE_Y = 2;
    static final int MOUNT_GAUGE_WIDTH = 317;
    static final int MOUNT_GAUGE_HEIGHT = 16;
    static final int DISEMBARK_BUTTON_WIDTH = 76;
    static final int DISEMBARK_BUTTON_HEIGHT = 16;
    static final int DISEMBARK_BUTTON_RIGHT = 3;
    static final int DISEMBARK_BUTTON_Y = 2;

    static final int PORTRAIT_APERTURE_X = 13;
    static final int PORTRAIT_APERTURE_Y = 7;
    static final int PORTRAIT_APERTURE_WIDTH = 85;
    static final int PORTRAIT_APERTURE_HEIGHT = 90;
    static final int PORTRAIT_BLOOD_INSET = 14;

    static final int GAUGE_X = 107;
    static final int GAUGE_WIDTH = 212;
    static final int GAUGE_INSET_X = 2;
    static final int GAUGE_INSET_Y = 2;
    static final int STAMINA_Y = 2;
    static final int STAMINA_HEIGHT = 22;
    static final int WATER_FOOD_Y = 27;
    static final int WATER_WIDTH = 104;
    static final int FOOD_WIDTH = 105;
    static final int WATER_FOOD_GAP = 3;
    static final int WATER_FOOD_HEIGHT = 16;
    static final int CCFP_Y = 46;
    static final int[] CCFP_OFFSETS = new int[]{0, 54, 107, 161};
    static final int[] CCFP_WIDTHS = new int[]{51, 50, 51, 51};
    static final int CCFP_HEIGHT = 16;
    static final int SLEEP_BONUS_Y = 65;
    static final int SLEEP_BONUS_HEIGHT = 16;
    static final int SLEEP_BUTTON_WIDTH = 76;
    static final int SLEEP_BUTTON_HEIGHT = 14;
    static final int SLEEP_BUTTON_RIGHT = 1;
    static final int SLEEP_BUTTON_Y = 1;
    static final int SLEEP_TIMER_GAP = 4;
    static final int FAVOR_Y = 84;
    static final int FAVOR_HEIGHT = 16;

    static final int NAMEPLATE_SOURCE_WIDTH = 128;
    static final int NAMEPLATE_HEIGHT = 22;
    static final int NAMEPLATE_LEFT_CAP = 12;
    static final int NAMEPLATE_RIGHT_CAP = 24;
    static final int NAMEPLATE_MIN_WIDTH = 54;
    static final int NAMEPLATE_X = 3;
    static final int NAMEPLATE_TEXT_PADDING = 30;
    static final int HEADER_TEXT_X = 11;
    static final int HEADER_TEXT_MAX_WIDTH = PANEL_WIDTH
            - NAMEPLATE_X - 2 - NAMEPLATE_TEXT_PADDING;

    static final int GAUGE_COUNT = 9;
    static final int GROWTH_GAUGE_COUNT = 3;
    static final int GAUGE_STAMINA = 0;
    static final int GAUGE_WATER = 1;
    static final int GAUGE_FOOD = 2;
    static final int GAUGE_CALORIES = 3;
    static final int GAUGE_CARBS = 4;
    static final int GAUGE_FATS = 5;
    static final int GAUGE_PROTEINS = 6;
    static final int GAUGE_SLEEP = 7;
    static final int GAUGE_FAVOR = 8;

    private HighResHealthBarLayout() {
    }
}
