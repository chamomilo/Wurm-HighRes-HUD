package org.highreshud.client.portrait;

import com.wurmonline.client.resources.textures.Texture;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class SharedPortraitCoordinatorTest {
    @After
    public void finish() {
        SharedPortraitCoordinator.finishFrame();
    }

    @Test
    public void compatibleChannelsScheduleOnlyOneGpuJob() {
        FakeChannel select = new FakeChannel(key(7L, "bust"));
        FakeChannel fighting = new FakeChannel(key(7L, "bust"));

        SharedPortraitCoordinator.beginFrame();
        SharedPortraitCoordinator.request(select);
        SharedPortraitCoordinator.request(fighting);
        SharedPortraitCoordinator.dispatch();

        assertEquals(2, SharedPortraitCoordinator.lastRequested());
        assertEquals(1, SharedPortraitCoordinator.lastPasses());
        assertEquals(1, SharedPortraitCoordinator.lastSavedPasses());
        assertEquals(1, select.scheduled);
        assertEquals(0, fighting.scheduled);
        assertSame(select, fighting.sharedFrom);
    }

    @Test
    public void distinctSubjectsAndProfilesRemainDistinct() {
        FakeChannel health = new FakeChannel(key(1L, "face"));
        FakeChannel select = new FakeChannel(key(2L, "bust"));
        FakeChannel fighting = new FakeChannel(key(2L, "manual-yaw"));

        SharedPortraitCoordinator.beginFrame();
        SharedPortraitCoordinator.request(health);
        SharedPortraitCoordinator.request(select);
        SharedPortraitCoordinator.request(fighting);
        SharedPortraitCoordinator.dispatch();

        assertEquals(3, SharedPortraitCoordinator.lastPasses());
        assertEquals(1, health.scheduled);
        assertEquals(1, select.scheduled);
        assertEquals(1, fighting.scheduled);
    }

    private static PortraitRenderKey key(long subject, String profile) {
        return new PortraitRenderKey(subject, 512, "model.creature.human",
                profile);
    }

    private static final class FakeChannel implements SharedPortraitChannel {
        private final PortraitRenderKey key;
        private int scheduled;
        private int finished;
        private SharedPortraitChannel sharedFrom;

        private FakeChannel(PortraitRenderKey key) {
            this.key = key;
        }

        @Override public PortraitRenderKey renderKey() { return key; }
        @Override public void scheduleOwnRender() { scheduled++; }
        @Override public void finishOwnRender() { finished++; }
        @Override public Texture ownTexture() { return null; }
        @Override public void shareFrom(SharedPortraitChannel leader) {
            sharedFrom = leader;
        }
    }
}
