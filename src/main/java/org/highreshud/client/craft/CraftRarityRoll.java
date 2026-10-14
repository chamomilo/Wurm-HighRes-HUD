package org.highreshud.client.craft;

/** Keybinder 1.0.0 rarity distribution, adapted to creation/continuation transfers. */
public final class CraftRarityRoll {
    /** Probability of at least the requested rarity after an audible drumroll and success. */
    public Range afterDrumroll(int minimumRarity,byte currentRarity,
                              byte sourceRarity,boolean sourceTransfers,
                              byte targetRarity,boolean targetTransfers,
                              boolean advanced,int totalComponents){
        if(minimumRarity<1||minimumRarity>3)throw new IllegalArgumentException("Rarity tier");
        if(currentRarity>=minimumRarity)return new Range(100,100);
        double transfer=advanced?1.0/Math.max(1,totalComponents):1.0;
        return afterDrumroll(minimumRarity,currentRarity,sourceRarity,sourceTransfers?transfer:0,targetRarity,targetTransfers?transfer:0,transfer);
    }
    public Range afterDrumroll(int minimumRarity,byte currentRarity,byte sourceRarity,double sourceTransfer,
                              byte targetRarity,double targetTransfer,double actionTransfer){
        if(minimumRarity<1||minimumRarity>3)throw new IllegalArgumentException("Rarity tier");
        if(currentRarity>=minimumRarity)return new Range(100,100);
        double low=creationChance(minimumRarity,false,actionTransfer,sourceRarity,sourceTransfer,targetRarity,targetTransfer);
        double high=creationChance(minimumRarity,true,actionTransfer,sourceRarity,sourceTransfer,targetRarity,targetTransfer);
        return new Range(low*100,high*100);
    }
    public double materialUpgrade(byte currentRarity,byte sourceRarity,double sourceTransfer,byte targetRarity,double targetTransfer){
        double chance=sourceRarity>currentRarity?clamp(sourceTransfer,0,1):0;
        if(targetRarity>currentRarity)chance=1-(1-chance)*(1-clamp(targetTransfer,0,1));
        return chance*100;
    }
    private static double creationChance(int tier,boolean paying,double transfer,byte source,double sourceTransfer,byte target,double targetTransfer){
        // A drumroll means the action roll is nonzero. Normalize Keybinder's
        // active-window distribution before the creation-specific 1/N transfer.
        double chance=higherRarityProbability((byte)(tier-1),paying)/higherRarityProbability((byte)0,paying)*transfer;
        if(source>=tier)chance=1-(1-chance)*(1-clamp(sourceTransfer,0,1));
        if(target>=tier)chance=1-(1-chance)*(1-clamp(targetTransfer,0,1));
        return clamp(chance,0,1);
    }
    public Range estimate(byte targetRarity,
                   double minimumRuneModifier, double maximumRuneModifier,
                   byte sourceRarity, boolean sourceMayBeConsumed) {
        double lowAction = higherRarityProbability(targetRarity, false)
                * transferChance(minimumRuneModifier);
        double highAction = higherRarityProbability(targetRarity, true)
                * transferChance(maximumRuneModifier);

        double low = lowAction;
        double high = highAction;
        if (sourceMayBeConsumed && sourceRarity > targetRarity) {
            // Whether the last grams are deleted is not known before send. If
            // they are, the rarer source gets a separate 1% transfer roll.
            high = highAction + (1.0 - highAction) * 0.01;
        }
        return new Range(low * 100.0, high * 100.0);
    }

    private static double higherRarityProbability(byte targetRarity,
                                                   boolean paying) {
        double fantastic = paying ? 0.000103 : 0.000100;
        double supreme = paying ? 0.04 : 0.01;
        if (targetRarity <= 0)
            return fantastic + (1.0 - fantastic)
                    * (supreme + (1.0 - supreme) * 0.5);
        if (targetRarity == 1)
            return fantastic + (1.0 - fantastic) * supreme;
        if (targetRarity == 2) return fantastic;
        return 0.0;
    }

    private static double transferChance(double runeModifier) {
        return clamp(0.2 * (1.0 + Math.max(0.0, runeModifier)), 0.0, 1.0);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class Range {
        private final double minimum;
        private final double maximum;

        Range(double minimum, double maximum) {
            this.minimum = minimum;
            this.maximum = Math.max(minimum, maximum);
        }

        public double getMinimum() { return minimum; }
        public double getMaximum() { return maximum; }
    }
}
