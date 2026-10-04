package org.highreshud.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InputRouterTest {
    @Test
    public void routesByPriorityUntilOwned() {
        InputRouter router = new InputRouter();
        List<String> calls = new ArrayList<>();
        router.subscribe(10, event -> { calls.add("waypointer"); return false; });
        router.subscribe(100, event -> { calls.add("hud"); return false; });
        router.subscribe(50, event -> { calls.add("third-person"); return true; });
        router.subscribe(0, event -> { calls.add("keybinder"); return true; });

        assertTrue(router.route(new InputRouter.Event(
                InputRouter.Kind.MOUSE_WHEEL, 10, 20, 1)));
        assertEquals(Arrays.asList("hud", "third-person"), calls);
    }
}
