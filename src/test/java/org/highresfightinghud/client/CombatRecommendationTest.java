package org.highresfightinghud.client;

import org.junit.Test;
import static org.junit.Assert.*;

public class CombatRecommendationTest {
    private static int noHead() {
        return AttackStanceModel.allStancesMask() & ~((1 << 6) | (1 << 7) | (1 << 1));
    }

    @Test public void unarmedHeadRestrictionSelectsNextBestAvailableZone() {
        CombatKnowledge model = new CombatKnowledge();
        observe(model, 0, CombatObservation.Outcome.HIT, 1);
        observe(model, 0, CombatObservation.Outcome.MISS, 1);
        assertEquals(7, model.snapshot(0).bestStance);
        assertEquals(10, model.snapshot(0, noHead()).bestStance);
    }

    @Test public void learnedResultsRankRemainingZonesAndSurviveWeaponChanges() {
        CombatKnowledge model = new CombatKnowledge();
        observe(model, 7, CombatObservation.Outcome.HIT, 100);
        observe(model, 5, CombatObservation.Outcome.HIT, 20);
        observe(model, 10, CombatObservation.Outcome.MISS, 100);
        assertEquals(7, model.snapshot(0).bestStance);
        CombatKnowledge.Snapshot available = model.snapshot(0, noHead());
        assertEquals(5, available.bestStance);
        assertEquals(20, available.bestStanceSamples);
        assertEquals(220, available.outgoingAttempts);
        assertEquals(7, model.snapshot(0, AttackStanceModel.allStancesMask()).bestStance);
    }

    @Test public void unavailableCombatHasNoRecommendationOrProjectedGain() {
        CombatKnowledge model = new CombatKnowledge();
        observe(model, 7, CombatObservation.Outcome.HIT, 10);
        CombatKnowledge.Snapshot unavailable = model.snapshot(0, 0);
        assertEquals(-1, unavailable.bestStance);
        assertEquals(0, unavailable.bestScore, 0);
        assertEquals(0, unavailable.projectedGainPercent, 0);
        assertEquals(0, unavailable.bestStanceSamples);
        assertEquals(10, unavailable.outgoingAttempts);
    }

    @Test public void soleCenterAttackRemainsValidAndUnrelatedStancesDoNot() {
        CombatKnowledge model = new CombatKnowledge();
        assertEquals(0, model.snapshot(7, 1 << 0).bestStance);
        assertEquals(-1, model.snapshot(0, 1 << 14).bestStance);
    }

    private static void observe(CombatKnowledge model, int stance, CombatObservation.Outcome outcome, int count) {
        CombatObservation event = new CombatObservation(CombatObservation.Direction.OUTGOING, outcome,
                CombatObservation.DamageType.CRUSH, "body", .2);
        for (int i = 0; i < count; i++) model.observe(event, stance, 0, false);
    }
}
