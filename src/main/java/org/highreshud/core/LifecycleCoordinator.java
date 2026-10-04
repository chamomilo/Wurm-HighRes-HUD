package org.highreshud.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Explicit client lifecycle shared by every component and future extension. */
public final class LifecycleCoordinator {
    public enum Stage {
        CREATED,
        HUD_READY,
        SESSION_READY,
        DISCONNECTING,
        STOPPED
    }

    public interface Listener {
        void onStageChanged(Stage previous, Stage current, long generation);
    }

    private static final Logger LOG = Logger.getLogger("HighResHud.Core");
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private Stage stage = Stage.CREATED;
    private long generation;

    public synchronized Stage stage() {
        return stage;
    }

    public synchronized long generation() {
        return generation;
    }

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

    public void hudReady() {
        transition(Stage.HUD_READY, false);
    }

    public void sessionReady() {
        transition(Stage.SESSION_READY, false);
    }

    public void disconnecting() {
        transition(Stage.DISCONNECTING, false);
    }

    public void stopped() {
        transition(Stage.STOPPED, false);
    }

    /** Starts a new login generation after disconnect/reconnect. */
    public synchronized void reset() {
        Stage previous = stage;
        generation++;
        stage = Stage.CREATED;
        notifyListeners(previous, stage, generation);
    }

    private synchronized void transition(Stage next, boolean unused) {
        if (stage == next) return;
        if (!allowed(stage, next)) {
            throw new IllegalStateException("Invalid lifecycle transition "
                    + stage + " -> " + next);
        }
        Stage previous = stage;
        stage = next;
        notifyListeners(previous, next, generation);
    }

    private static boolean allowed(Stage from, Stage to) {
        switch (from) {
            case CREATED: return to == Stage.HUD_READY || to == Stage.STOPPED;
            case HUD_READY: return to == Stage.SESSION_READY
                    || to == Stage.DISCONNECTING || to == Stage.STOPPED;
            case SESSION_READY: return to == Stage.DISCONNECTING
                    || to == Stage.STOPPED;
            case DISCONNECTING: return to == Stage.STOPPED;
            case STOPPED:
            default: return false;
        }
    }

    private void notifyListeners(Stage previous, Stage current,
                                 long currentGeneration) {
        for (Listener listener : listeners) {
            try {
                listener.onStageChanged(previous, current, currentGeneration);
            } catch (Throwable error) {
                LOG.log(Level.WARNING,
                        "Lifecycle subscriber failed; dispatch continues", error);
            }
        }
    }
}
