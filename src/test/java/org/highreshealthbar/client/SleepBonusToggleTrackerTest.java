package org.highreshealthbar.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SleepBonusToggleTrackerTest {
    @Test
    public void activationStartsFiveMinuteCountdown() {
        SleepBonusToggleTracker tracker = new SleepBonusToggleTracker();
        tracker.reset(false);
        tracker.activationRequested(10_000L);

        assertFalse(tracker.canActivate(10_000L));
        assertEquals(300, tracker.secondsUntilActivation(10_000L));
        assertEquals(181, tracker.secondsUntilActivation(129_001L));
        assertEquals(0, tracker.secondsUntilActivation(310_000L));
        assertTrue(tracker.canActivate(310_000L));
    }

    @Test
    public void serverMessagesConfirmOrCorrectCooldown() {
        SleepBonusToggleTracker tracker = new SleepBonusToggleTracker();
        tracker.reset(false);
        tracker.activationRequested(1_000L);
        tracker.observeServerMessage(
                "You start using your sleep bonus.", 1_250L);
        assertEquals(300, tracker.secondsUntilActivation(1_250L));

        tracker.observeServerMessage(
                "You need to wait 2 minutes and 7 seconds until you can "
                        + "toggle sleep bonus again.", 5_000L);
        assertEquals(127, tracker.secondsUntilActivation(5_000L));
        assertEquals("2:07", SleepBonusToggleTracker.formatCountdown(127));
    }

    @Test
    public void noBonusFailureClearsOptimisticRequest() {
        SleepBonusToggleTracker tracker = new SleepBonusToggleTracker();
        tracker.reset(false);
        tracker.activationRequested(20_000L);
        tracker.observeServerMessage(
                "You do not have any sleep bonus. You can gain some by sleeping.",
                20_100L);
        assertTrue(tracker.canActivate(20_100L));
    }
}
