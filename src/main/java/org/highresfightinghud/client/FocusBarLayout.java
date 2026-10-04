package org.highresfightinghud.client;

/**
 * Exact pixel lanes shared by the focus-bar renderer and its contract tests.
 * Text coordinates passed to Wurm are lower box edges, not AWT baselines.
 */
public final class FocusBarLayout {
    public static final int HEADER_TEXT_Y = 4;
    public static final int HEADER_TEXT_HEIGHT = 20;

    // Three recessed information lanes in the Fighting frame.
    public static final int HEALTH_GAUGE_Y = 30;
    public static final int PROGRESS_GAUGE_Y = 49;
    public static final int GAUGE_HEIGHT = 14;
    public static final int STATUS_TEXT_Y = 66;
    public static final int STATUS_TEXT_HEIGHT = 18;

    private FocusBarLayout() {
    }

    /** Lower text-box edge which vertically centres a Wurm TextFont. */
    public static int centeredLowerEdge(int top, int height, int fontHeight) {
        return top + (height + fontHeight) / 2;
    }
}
