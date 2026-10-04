package org.highreshud.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LifecycleCoordinatorTest {
    @Test
    public void reconnectStartsNewGeneration() {
        LifecycleCoordinator lifecycle = new LifecycleCoordinator();
        lifecycle.hudReady();
        lifecycle.sessionReady();
        lifecycle.disconnecting();
        lifecycle.stopped();
        lifecycle.reset();

        assertEquals(LifecycleCoordinator.Stage.CREATED, lifecycle.stage());
        assertEquals(1L, lifecycle.generation());
    }

    @Test(expected = IllegalStateException.class)
    public void sessionCannotStartBeforeHud() {
        new LifecycleCoordinator().sessionReady();
    }
}
