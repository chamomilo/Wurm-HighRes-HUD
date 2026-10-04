package org.highresfightinghud.client;

/** Stable mapping between Wurm's attack-stance ids and the 3 x 3 target grid. */
public final class AttackStanceModel {
    private static final int[] IDS = {6, 7, 1, 5, 0, 2, 4, 10, 3};
    private static final String[] LABELS = {
            "HIGH LEFT", "HIGH", "HIGH RIGHT",
            "LEFT", "CENTER", "RIGHT",
            "LOW LEFT", "LOW", "LOW RIGHT"
    };
    private static final String[] SHORT_LABELS = {
            "NW", "N", "NE", "W", "C", "E", "SW", "S", "SE"
    };

    /*
     * Relative damage priors.  Learned hit rates rapidly dominate these
     * modest target-zone preferences; they are not presented as server truth.
     */
    private static final double[] DAMAGE_PRIORS = {
            1.135, 1.187, 1.135,
            1.061, 1.016, 1.061,
            1.044, 1.080, 1.044
    };

    private AttackStanceModel() {
    }

    public static int count() {
        return IDS.length;
    }

    public static int idAt(int gridIndex) {
        return gridIndex >= 0 && gridIndex < IDS.length ? IDS[gridIndex] : 0;
    }

    public static int gridIndex(int stanceId) {
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i] == stanceId) return i;
        }
        return 4;
    }

    public static String label(int stanceId) {
        return LABELS[gridIndex(stanceId)];
    }

    public static String shortLabel(int stanceId) {
        return SHORT_LABELS[gridIndex(stanceId)];
    }

    public static double damagePrior(int stanceId) {
        return DAMAGE_PRIORS[gridIndex(stanceId)];
    }
}
