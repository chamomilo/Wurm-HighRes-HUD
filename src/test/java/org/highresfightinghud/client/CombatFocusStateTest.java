package org.highresfightinghud.client;

import com.wurmonline.shared.constants.FightConstants;
import org.junit.Test;
import static org.junit.Assert.*;

public class CombatFocusStateTest {
    private static String status(CombatFocusState state, long now) {
        return state.readiness(true, false, true, false, false, now);
    }
    @Test public void initialEngagementIsEstimatedAndBurstPacketsDoNotEnableFocus() {
        CombatFocusState state = new CombatFocusState();
        assertEquals("Not in combat", status(state, 0));
        state.combatChanged(true);
        state.optionsReceived(0);
        for (int i = 1; i < 100; i++) state.optionsReceived(i);
        assertEquals("Engaging ~0/3", status(state, 100));
        for (int i = 1; i <= 3; i++) state.optionsReceived(i * 1_000_000_000L);
        assertEquals("Ready to try ~", status(state, 3_000_000_000L));
        state.observeMessage("You need to get into the fight more first.");
        assertEquals("Engaging ~0/3", status(state, 4_000_000_000L));
        state.combatChanged(false); state.combatChanged(true);
        assertFalse(CombatFocusState.ready(status(state, 5_000_000_000L)));
    }
    @Test public void authoritativeLevelAndCombatRestrictionsTakePriority() {
        CombatFocusState state = new CombatFocusState(); state.combatChanged(true);
        state.levelReceived((byte)2, "Now focused");
        assertEquals(2, state.level()); assertEquals("Now focused", state.levelMessage());
        assertEquals("Ready to try", status(state, 0));
        assertEquals("No target", state.readiness(false,false,true,false,false,0));
        assertEquals("Stunned", state.readiness(true,true,true,false,false,0));
        state.positionReceived((byte)FightConstants.ATTACK_PRONE);
        assertEquals("On the ground", status(state,0));
        state.positionReceived((byte)FightConstants.ATTACK_OPEN);
        assertEquals("Imbalanced", status(state,0));
        state.positionReceived((byte)FightConstants.ATTACK_CENTER);
        assertEquals("Focusing...",state.readiness(true,false,true,false,true,0));
        state.attemptSent(0); assertEquals("Request sent",status(state,1));
        assertEquals("Ready to try",status(state,2_000_000_000L));
        state.levelReceived((byte)5,"Maximum"); assertEquals("Maximum focus",status(state,3_000_000_000L));
    }
    @Test public void serverGrantedSpecialMoveProvesInitialEngagement() {
        CombatFocusState state = new CombatFocusState();state.combatChanged(true);
        assertEquals("Ready to try",state.readiness(true,false,true,true,false,0));
    }
}
