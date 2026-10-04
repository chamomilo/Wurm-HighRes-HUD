package org.highreshud.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExamineBrokerTest {
    @Test
    public void coalescesSameTargetAndFansOutReply() {
        ExamineBroker broker = new ExamineBroker();
        List<String> calls = new ArrayList<>();
        ExamineBroker.Listener first = listener("select", calls);
        ExamineBroker.Listener second = listener("fight", calls);
        broker.request(42L, 10, true, 1_000L, first);
        broker.request(42L, 20, false, 1_000L, second);

        List<Long> sent = new ArrayList<>();
        broker.tick(100L, sent::add);
        assertEquals(Arrays.asList(42L), sent);
        assertEquals(1, broker.pendingCount());
        assertTrue(broker.observe("Event", "complete"));
        assertEquals(Arrays.asList("select:line", "fight:line",
                "select:COMPLETE", "fight:COMPLETE"), calls);
        assertEquals(0, broker.pendingCount());
    }

    @Test
    public void sendsOneProbeAtATimeByPriority() {
        ExamineBroker broker = new ExamineBroker();
        List<String> calls = new ArrayList<>();
        broker.request(1L, 1, true, 100L, listener("low", calls));
        broker.request(2L, 50, true, 100L, listener("high", calls));
        List<Long> sent = new ArrayList<>();

        broker.tick(10L, sent::add);
        broker.tick(50L, sent::add);
        broker.observe("Event", "complete");
        broker.tick(60L, sent::add);

        assertEquals(Arrays.asList(2L, 1L), sent);
    }

    private static ExamineBroker.Listener listener(String name,
                                                   List<String> calls) {
        return new ExamineBroker.Listener() {
            @Override
            public ExamineBroker.LineResult onLine(long targetId,
                                                   String title,
                                                   String message) {
                calls.add(name + ":line");
                return ExamineBroker.LineResult.COMPLETE;
            }

            @Override
            public void onFinished(long targetId,
                                   ExamineBroker.Completion completion) {
                calls.add(name + ':' + completion.name());
            }
        };
    }
}
