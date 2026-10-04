package org.highreshud.core;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageRouterTest {
    @Test
    public void everyObserverSeesMessageBeforeConsumeDecision() {
        MessageRouter router = new MessageRouter();
        AtomicInteger calls = new AtomicInteger();
        router.subscribe(message -> {
            calls.incrementAndGet();
            return true;
        });
        router.subscribe(message -> {
            calls.incrementAndGet();
            return false;
        });

        assertTrue(router.route(new MessageRouter.Message(
                "Event", "You see a horse.", false)));
        assertEquals(2, calls.get());
    }
}
