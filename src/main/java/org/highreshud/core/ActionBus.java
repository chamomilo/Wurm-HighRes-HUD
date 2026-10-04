package org.highreshud.core;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fan-out bus for the single sendAction/sendSingleAction hook. Origin is kept
 * on a per-thread stack so nested technical actions cannot be mistaken for a
 * user's input by Keybinder or another subscriber.
 */
public final class ActionBus {
    public interface Listener {
        void onAction(ActionEvent event);
    }

    private static final Logger LOG = Logger.getLogger("HighResHud.Core");
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final ThreadLocal<ArrayDeque<ActionOrigin>> origins =
            new ThreadLocal<ArrayDeque<ActionOrigin>>() {
                @Override
                protected ArrayDeque<ActionOrigin> initialValue() {
                    return new ArrayDeque<>();
                }
            };

    public Subscription subscribe(final Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener");
        listeners.add(listener);
        return new Subscription() {
            @Override
            public void unsubscribe() {
                listeners.remove(listener);
            }
        };
    }

    public ActionOrigin currentOrigin() {
        ActionOrigin current = origins.get().peek();
        return current == null ? ActionOrigin.USER : current;
    }

    public void runAs(ActionOrigin origin, Runnable action) {
        if (action == null) return;
        ArrayDeque<ActionOrigin> stack = origins.get();
        stack.push(origin == null ? ActionOrigin.UNKNOWN : origin);
        try {
            action.run();
        } finally {
            stack.pop();
            if (stack.isEmpty()) origins.remove();
        }
    }

    public void publish(ActionEvent event) {
        if (event == null) return;
        for (Listener listener : listeners) {
            try {
                listener.onAction(event);
            } catch (Throwable error) {
                LOG.log(Level.WARNING,
                        "Action subscriber failed; dispatch continues", error);
            }
        }
    }
}
