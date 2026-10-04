package org.highreshud.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ActionBusTest {
    @Test
    public void nestedOriginsAreScopedAndRestored() {
        ActionBus bus = new ActionBus();
        List<ActionOrigin> seen = new ArrayList<>();
        bus.subscribe(event -> seen.add(event.origin()));

        bus.runAs(ActionOrigin.HUD, () -> {
            publish(bus);
            bus.runAs(ActionOrigin.INTERNAL, () -> publish(bus));
            publish(bus);
        });
        publish(bus);

        assertEquals(ActionOrigin.HUD, seen.get(0));
        assertEquals(ActionOrigin.INTERNAL, seen.get(1));
        assertEquals(ActionOrigin.HUD, seen.get(2));
        assertEquals(ActionOrigin.USER, seen.get(3));
    }

    private static void publish(ActionBus bus) {
        bus.publish(new ActionEvent(-10L, new long[]{7L}, (short) 1,
                "test", bus.currentOrigin()));
    }
}
