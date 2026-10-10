package org.highresfocusbar.client;

/**
 * Exact pixel lanes shared by the focus-bar renderer and its contract tests.
 * Text coordinates passed to Wurm are lower box edges, not AWT baselines.
 */
public final class FocusBarLayout {
    public static final int HEADER_TEXT_Y = 2;
    public static final int HEADER_TEXT_HEIGHT = 22;

    public static final int NAME_TEXT_X = 117;
    public static final int NAME_TEXT_WIDTH = 265;
    public static final int DISTANCE_FOOTER_HEIGHT = 18;

    // The compact Healthbar portrait module defines the complete 108 px
    // height. Every right-hand lane is independent; progress never replaces
    // status text.
    public static final int FOCUS_HEALTH_GAUGE_Y = 27;
    public static final int FOCUS_HEALTH_GAUGE_HEIGHT = 16;
    public static final int FOCUS_STATUS_Y = 46;
    public static final int FOCUS_STATUS_HEIGHT = 16;
    public static final int FOCUS_PROGRESS_GAUGE_Y = 99;
    public static final int FOCUS_PROGRESS_GAUGE_HEIGHT = 3;
    public static final int FOCUS_GAUGE_LEFT_INSET = 7;
    public static final int FOCUS_GAUGE_RIGHT_INSET = 4;

    public static final int ACTION_SHELF_X = 107;
    public static final int ACTION_SHELF_Y = 65;
    public static final int ACTION_SHELF_WIDTH = 328;
    public static final int ACTION_SHELF_HEIGHT = 30;
    public static final int ACTION_X = ACTION_SHELF_X;
    public static final int ACTION_Y = 66;
    public static final int ACTION_SLOT_SIZE = 28;
    public static final int ACTION_SLOT_GAP = 0;
    public static final int ACTION_ICON_INSET = 3;
    public static final int ACTION_ICON_SIZE = ACTION_SLOT_SIZE - ACTION_ICON_INSET * 2;
    public static final int ACTION_VISIBLE_SLOTS = 10;
    public static final int ACTION_PAGER_X = 403;
    public static final int ACTION_PAGER_WIDTH = 29;

    public static final int PIN_ZONE_X = 388;
    public static final int PIN_ZONE_Y = 2;
    public static final int PIN_ZONE_SIZE = 22;
    public static final int PIN_GLYPH_SIZE = 18;

    public static final int CLOSE_ZONE_X = 412;
    public static final int CLOSE_ZONE_Y = 2;
    public static final int CLOSE_ZONE_SIZE = 22;
    public static final int CLOSE_GLYPH_SIZE = 12;

    private FocusBarLayout() {
    }

    /** Lower text-box edge which vertically centres a Wurm TextFont. */
    public static int centeredLowerEdge(int top, int height, int fontHeight) {
        return top + (height + fontHeight) / 2;
    }
}
