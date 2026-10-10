package org.highresfocusbar.client;

/** UV conversion for the remastered stock-compatible 16 x 16 action atlas. */
public final class FocusAtlasUv {
    private static final float CELL = 32f;
    private static final float ATLAS = 512f;

    private FocusAtlasUv() {
    }

    public static float offset(int cell) {
        return cell * CELL / ATLAS;
    }

    public static float size() {
        return CELL / ATLAS;
    }

    /** Exclude the legacy 3 px atlas contour when the kit owns the button. */
    public static float iconOffset(int cell) {
        return (cell * CELL + 3f) / ATLAS;
    }

    public static float iconSize() {
        return (CELL - 6f) / ATLAS;
    }
}
