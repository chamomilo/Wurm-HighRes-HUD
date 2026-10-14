package org.highreshud.client.craft;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CraftRarityRollTest {
    private final CraftRarityRoll estimator =
            new CraftRarityRoll();

    @Test public void audibleDrumrollSeparatesRareAndSupremeForCreation(){
        CraftRarityRoll.Range rare=estimator.afterDrumroll(1,(byte)0,(byte)0,false,(byte)0,false,true,41);
        CraftRarityRoll.Range supreme=estimator.afterDrumroll(2,(byte)0,(byte)0,false,(byte)0,false,true,41);
        assertEquals(100.0/41,rare.getMinimum(),.00001);
        assertTrue(supreme.getMaximum()<rare.getMinimum());
        assertTrue(supreme.getMaximum()>supreme.getMinimum());
        CraftRarityRoll.Range simple=estimator.afterDrumroll(1,(byte)0,(byte)0,false,(byte)0,false,false,1);
        assertEquals(100,simple.getMinimum(),.00001);
    }
    @Test public void consumedRareMaterialAndExistingRarityAreIncluded(){
        CraftRarityRoll.Range ordinary=estimator.afterDrumroll(2,(byte)0,(byte)0,true,(byte)0,false,true,41);
        CraftRarityRoll.Range source=estimator.afterDrumroll(2,(byte)0,(byte)2,true,(byte)0,false,true,41);
        assertTrue(source.getMinimum()>ordinary.getMaximum());
        assertEquals(100,estimator.afterDrumroll(1,(byte)1,(byte)0,true,(byte)0,false,true,41).getMinimum(),.00001);
    }

    @Test
    public void normalUnrunedForgeAtSixtyThreePercentMatchesServerRange() {
        CraftRarityRoll.Range range = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 0, true);

        assertEquals(10.10, range.getMinimum(), 0.005);
        assertEquals(10.40, range.getMaximum(), 0.005);
    }

    @Test
    public void targetRarityRuneMultipliesServerTransferChance() {
        CraftRarityRoll.Range baseline = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 0, false);
        CraftRarityRoll.Range runed = estimator.estimate(
                (byte) 0, 0.10, 0.10, (byte) 0, false);

        assertEquals(baseline.getMinimum() * 1.10,
                runed.getMinimum(), 0.0001);
        assertEquals(baseline.getMaximum() * 1.10,
                runed.getMaximum(), 0.0001);
    }

    @Test
    public void existingRarityRequiresAStillHigherActionRarity() {
        CraftRarityRoll.Range normal = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 0, false);
        CraftRarityRoll.Range rare = estimator.estimate(
                (byte) 1, 0.0, 0.0, (byte) 0, false);
        CraftRarityRoll.Range supreme = estimator.estimate(
                (byte) 2, 0.0, 0.0, (byte) 0, false);

        assertTrue(rare.getMaximum() < normal.getMinimum());
        assertTrue(supreme.getMaximum() < rare.getMinimum());
    }

    @Test
    public void rarerConsumableAddsOnlyAConservativeDeletionUpperBound() {
        CraftRarityRoll.Range ordinary = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 0, true);
        CraftRarityRoll.Range rareSource = estimator.estimate(
                (byte) 0, 0.0, 0.0, (byte) 1, true);

        assertEquals(ordinary.getMinimum(), rareSource.getMinimum(), 0.0);
        assertTrue(rareSource.getMaximum() > ordinary.getMaximum());
    }
}
