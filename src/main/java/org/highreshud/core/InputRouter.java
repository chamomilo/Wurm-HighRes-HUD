package org.highreshud.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Priority router intended for HUD, Keybinder, Waypointer and camera input. */
public final class InputRouter {
    public enum Kind { MOUSE_MOVE, MOUSE_WHEEL, MOUSE_BUTTON, KEY }

    public interface Listener {
        boolean onInput(Event event);
    }

    public static final class Event {
        private final Kind kind;
        private final int x;
        private final int y;
        private final int value;

        public Event(Kind kind, int x, int y, int value) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.value = value;
        }

        public Kind kind() { return kind; }
        public int x() { return x; }
        public int y() { return y; }
        public int value() { return value; }
    }

    private static final Logger LOG = Logger.getLogger("HighResHud.Core");
    private final List<Entry> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    public Subscription subscribe(final int priority,
                                  final Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener");
        final Entry entry = new Entry(priority, sequence.getAndIncrement(), listener);
        listeners.add(entry);
        return new Subscription() {
            @Override
            public void unsubscribe() {
                listeners.remove(entry);
            }
        };
    }

    public boolean route(Event event) {
        List<Entry> snapshot = new ArrayList<>(listeners);
        Collections.sort(snapshot, Entry.ORDER);
        for (Entry entry : snapshot) {
            try {
                if (entry.listener.onInput(event)) return true;
            } catch (Throwable error) {
                LOG.log(Level.WARNING,
                        "Input subscriber failed; routing continues", error);
            }
        }
        return false;
    }

    private static final class Entry {
        private static final Comparator<Entry> ORDER = new Comparator<Entry>() {
            @Override
            public int compare(Entry left, Entry right) {
                int priorityOrder = Integer.compare(right.priority, left.priority);
                return priorityOrder != 0 ? priorityOrder
                        : Long.compare(left.sequence, right.sequence);
            }
        };
        private final int priority;
        private final long sequence;
        private final Listener listener;

        private Entry(int priority, long sequence, Listener listener) {
            this.priority = priority;
            this.sequence = sequence;
            this.listener = listener;
        }
    }
}
