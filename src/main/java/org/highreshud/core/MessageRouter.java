package org.highreshud.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Delivers every line to every observer before making one consume decision. */
public final class MessageRouter {
    public interface Listener {
        boolean onMessage(Message message);
    }

    public static final class Message {
        private final String title;
        private final String text;
        private final boolean multicolor;

        public Message(String title, String text, boolean multicolor) {
            this.title = title == null ? "" : title;
            this.text = text == null ? "" : text;
            this.multicolor = multicolor;
        }

        public String title() { return title; }
        public String text() { return text; }
        public boolean multicolor() { return multicolor; }
    }

    private static final Logger LOG = Logger.getLogger("HighResHud.Core");
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

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

    public boolean route(Message message) {
        boolean consume = false;
        for (Listener listener : listeners) {
            try {
                consume |= listener.onMessage(message);
            } catch (Throwable error) {
                LOG.log(Level.WARNING,
                        "Message subscriber failed; dispatch continues", error);
            }
        }
        return consume;
    }
}
