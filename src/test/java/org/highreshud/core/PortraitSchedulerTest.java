package org.highreshud.core;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class PortraitSchedulerTest {
    @Test
    public void identicalHudPortraitsShareOnePassAndTexture() {
        PortraitScheduler<Object> scheduler = new PortraitScheduler<>(3);
        PortraitScheduler.RequestKey same = key(7L, "bust", 1L);
        AtomicInteger renders = new AtomicInteger();

        scheduler.beginFrame();
        scheduler.submit("selectbar", same, 20);
        scheduler.submit("fighting", same, 10);
        scheduler.flush(key -> { renders.incrementAndGet(); return new Object(); });

        assertEquals(1, renders.get());
        assertEquals(1, scheduler.lastUniquePasses());
        assertEquals(1, scheduler.lastSavedPasses());
        assertSame(scheduler.value("selectbar"), scheduler.value("fighting"));
    }

    @Test
    public void sameSubjectWithDifferentViewNeedsSeparatePasses() {
        PortraitScheduler<Object> scheduler = new PortraitScheduler<>(3);
        scheduler.beginFrame();
        scheduler.submit("healthbar", key(7L, "face", 1L), 30);
        scheduler.submit("selectbar", key(7L, "bust", 1L), 20);
        scheduler.submit("fighting", key(7L, "bust", 1L), 10);
        scheduler.flush(key -> new Object());

        assertEquals(2, scheduler.lastUniquePasses());
        assertEquals(1, scheduler.lastSavedPasses());
        assertNotSame(scheduler.value("healthbar"),
                scheduler.value("selectbar"));
        assertSame(scheduler.value("selectbar"), scheduler.value("fighting"));
    }

    @Test
    public void threeDifferentHudSubjectsRemainThreeCorrectPasses() {
        PortraitScheduler<Object> scheduler = new PortraitScheduler<>(3);
        scheduler.beginFrame();
        scheduler.submit("healthbar", key(1L, "face", 1L), 30);
        scheduler.submit("selectbar", key(2L, "bust", 1L), 20);
        scheduler.submit("fighting", key(3L, "bust", 1L), 10);
        scheduler.flush(key -> new Object());

        assertEquals(3, scheduler.lastUniquePasses());
        assertEquals(0, scheduler.lastSavedPasses());
    }

    private static PortraitScheduler.RequestKey key(long subject,
                                                     String profile,
                                                     long animation) {
        return new PortraitScheduler.RequestKey(subject, 4L, profile,
                512, 512, animation);
    }
}
