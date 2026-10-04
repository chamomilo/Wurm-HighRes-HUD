package org.highresfightinghud.client;

/** Computes the incremental height of wound/debuff rows under Targeted. */
public final class WoundRowLayout {
    private WoundRowLayout() {
    }

    public static int rowCount(int woundCount, int debuffCount) {
        return Math.max(0, woundCount) + Math.max(0, debuffCount);
    }

    public static int height(int woundCount, int debuffCount, int rowHeight) {
        return rowCount(woundCount, debuffCount) * Math.max(0, rowHeight);
    }
}
