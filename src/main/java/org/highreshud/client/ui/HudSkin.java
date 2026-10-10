package org.highreshud.client.ui;

import org.chamomilo.wurm.ui.v1.*;
import static org.highresfightinghud.client.FightingHudLayout.*;

/** Production painters shared with the offline geometry/visual probe. */
public final class HudSkin {
    public static final UiScale SCALE = UiScale.BASE;
    public static final UiScale COMPACT = new UiScale(.5f);
    public static final int PORTRAIT_BORDER = 6;
    public static final int HEALTH_OUTER_BORDER = 5;
    private static final UiFrameGrid HEALTH = new UiFrameGrid(3,
            new int[]{24,19,19,19,24}, new int[][]{{1},{107,109},{54,53,54,55},{1},{1}});
    private static final UiFrameGrid SELECT = new UiFrameGrid(3,
            new int[]{24,19,19,33,10}, new int[][]{{1},{1},{1},{1},{1}});
    private static final UiFrameGrid TARGET_IDLE = new UiFrameGrid(3,
            new int[]{26,28,28}, new int[][]{{1},{1},{1}});
    private static final UiFrameGrid TARGET_PROGRESS = new UiFrameGrid(3,
            new int[]{26,19,19,18}, new int[][]{{1},{1},{1},{1}});

    private HudSkin() { }

    public static void healthBack(UiCanvas ui, int x, int y) {
        UiPainter.background(ui, UiBackground.LEATHER, SCALE, 1.0f, x, y, 323, 108);
    }
    public static void healthFront(UiCanvas ui, int x, int y) {
        HEALTH.foreground(withoutLeftRail(ui, x + 104), 1.0f, x + 104, y, 219, 108);
        UiPainter.frame(ui, HEALTH_OUTER_BORDER, 1.0f, x, y, 323, 108);
        portraitFront(ui, x, y, 108);
    }
    public static void selectBack(UiCanvas ui, int x, int y) {
        UiPainter.background(ui, UiBackground.LEATHER, SCALE, 1.0f, x, y, 438, 108);
        UiPainter.background(ui, UiBackground.WALNUT, SCALE, 1.0f, x + 107, y + 3, 328, 21);
    }
    public static void selectFront(UiCanvas ui, int x, int y) {
        SELECT.foreground(withoutLeftRail(ui, x + 104), 1.0f, x + 104, y, 334, 108);
        portraitFront(ui, x, y, 108);
    }
    // The portrait now owns the shared rail. Omit the grid's former 3 px
    // left contour so it cannot create a second border inside the 6 px frame.
    private static UiCanvas withoutLeftRail(final UiCanvas ui, final int left) {
        return new UiCanvas() {
            public void fill(UiColor color, float alpha, int x, int y, int w, int h) {
                if (x != left || w > 3) ui.fill(color, alpha, x, y, w, h);
            }
            public boolean texture(UiAsset asset, float tint, float alpha,
                    int x, int y, int w, int h, float u0, float v0, float u1, float v1) {
                return x == left && w <= 3 || ui.texture(asset, tint, alpha,
                        x, y, w, h, u0, v0, u1, v1);
            }
        };
    }
    private static void portraitFront(UiCanvas ui, int x, int y, int h) {
        UiPainter.frame(ui, PORTRAIT_BORDER, 1.0f, x, y, 107, h);
    }

    public static UiRect healthGauge(int x, int y, int index) {
        if (index < 0 || index > 8) throw new IllegalArgumentException("Unknown health gauge");
        int row = index == 0 ? 0 : index <= 2 ? 1 : index <= 6 ? 2 : index - 4;
        int column = index <= 2 ? Math.max(0, index - 1) : index <= 6 ? index - 3 : 0;
        UiRect cell = HEALTH.cell(x + 104, y, 219, 108, row, column);
        // Widen only the exterior contour inward; shared dividers stay 3 px.
        int top = Math.max(cell.y, y + HEALTH_OUTER_BORDER);
        int right = Math.min(cell.x + cell.width, x + 323 - HEALTH_OUTER_BORDER);
        int bottom = Math.min(cell.y + cell.height, y + 108 - HEALTH_OUTER_BORDER);
        return new UiRect(cell.x, top, right - cell.x, bottom - top);
    }
    public static UiRect selectHealth(int x, int y) {
        return SELECT.cell(x + 104, y, 334, 108, 1, 0);
    }
    public static UiRect selectProgress(int x, int y) {
        return SELECT.cell(x + 104, y, 334, 108, 4, 0);
    }
    public static void gauge(UiCanvas ui, UiRect well, float value, UiColor color) {
        UiHudPainter.back(ui, UiBackground.LEATHER, 1.0f, well);
        UiHudPainter.value(ui, UiAxis.HORIZONTAL, value, color, 1.0f, well);
    }
    public static void glass(UiCanvas ui, UiRect well) {
        UiHudPainter.glass(ui, 1.0f, well);
    }

    public static void well(UiCanvas ui, int x, int y, int w, int h) {
        UiHudPainter.back(ui, UiBackground.LEATHER, 1.0f, new UiRect(x,y,w,h));
    }
    public static void panel(UiCanvas ui, int x, int y, int w, int h) {
        UiPainter.background(ui, UiBackground.LEATHER, SCALE, 1.0f, x,y,w,h);
        UiPainter.frame(ui,3,1.0f,x,y,w,h);
    }
    public static void fightingBack(UiCanvas ui, int x, int y) {
        UiPainter.background(ui,UiBackground.WALNUT,SCALE,1.0f,x,y,PANEL_WIDTH,PANEL_HEIGHT);
        UiPainter.frame(ui,3,1.0f,x,y,PANEL_WIDTH,PANEL_HEIGHT);
    }
    public static UiRect targetCell(int x, int y, boolean progress, int row) {
        return (progress ? TARGET_PROGRESS : TARGET_IDLE).cell(
                x + INFO_X, y + INFO_Y, INFO_WIDTH, INFO_HEIGHT, row, 0);
    }
    public static void targetFront(UiCanvas ui, int x, int y, boolean progress) {
        UiPainter.frame(ui,PORTRAIT_BORDER,1.0f,x+5,y+25,96,96);
        (progress ? TARGET_PROGRESS : TARGET_IDLE).foreground(ui,1.0f,
                x+INFO_X,y+INFO_Y,INFO_WIDTH,INFO_HEIGHT);
    }
}
