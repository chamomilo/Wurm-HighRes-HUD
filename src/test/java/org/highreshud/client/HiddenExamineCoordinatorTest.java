package org.highreshud.client;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HiddenExamineCoordinatorTest {
    @After
    public void reset() {
        HiddenExamineCoordinator.reset();
    }

    @Test
    public void sameTargetJoinsButDifferentTargetWaits() {
        assertEquals(HiddenExamineCoordinator.Claim.SEND,
                HiddenExamineCoordinator.claim("selectbar", 42L, 100L));
        assertEquals(HiddenExamineCoordinator.Claim.JOIN,
                HiddenExamineCoordinator.claim("fightingHud", 42L, 110L));
        assertEquals(HiddenExamineCoordinator.Claim.WAIT,
                HiddenExamineCoordinator.claim("healthbar", 99L, 120L));
        assertEquals(2, HiddenExamineCoordinator.joinedOwners());
    }

    @Test
    public void visibleExamineDisablesSilentConsumption() {
        HiddenExamineCoordinator.claim("selectbar", 42L, 100L);
        assertTrue(HiddenExamineCoordinator.mayConsume(110L));
        HiddenExamineCoordinator.visibleExamineStarted();
        assertFalse(HiddenExamineCoordinator.mayConsume(120L));
    }

    @Test
    public void responseTailBlocksNextProbeThenExpires() {
        HiddenExamineCoordinator.claim("selectbar", 42L, 100L);
        HiddenExamineCoordinator.responseObserved(200L);
        assertEquals(HiddenExamineCoordinator.Claim.WAIT,
                HiddenExamineCoordinator.claim("fightingHud", 42L, 300L));
        assertEquals(HiddenExamineCoordinator.Claim.SEND,
                HiddenExamineCoordinator.claim("fightingHud", 42L,
                        500_000_201L));
    }
}
