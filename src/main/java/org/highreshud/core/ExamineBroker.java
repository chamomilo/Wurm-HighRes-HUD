package org.highreshud.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serializes hidden Examine traffic. Wurm's text reply has no request id, so
 * allowing more than one probe in flight is intrinsically ambiguous.
 */
public final class ExamineBroker {
    private static final Logger LOG = Logger.getLogger("HighResHud.ExamineBroker");
    public enum LineResult { IGNORE, ACCEPT, COMPLETE, FAILED }
    public enum Completion { COMPLETE, FAILED, TIMEOUT }

    public interface Sender {
        void send(long targetId);
    }

    public interface Listener {
        LineResult onLine(long targetId, String title, String message);
        void onFinished(long targetId, Completion completion);
    }

    private final PriorityQueue<Request> pending = new PriorityQueue<>(11,
            new Comparator<Request>() {
                @Override
                public int compare(Request left, Request right) {
                    int byPriority = Integer.compare(right.priority, left.priority);
                    return byPriority != 0 ? byPriority
                            : Long.compare(left.sequence, right.sequence);
                }
            });
    private final Map<Long, Request> byTarget = new HashMap<>();
    private long sequence;
    private Request inFlight;

    public synchronized void request(long targetId, int priority,
                                     boolean silent, long timeoutNanos,
                                     Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener");
        if (timeoutNanos <= 0L) throw new IllegalArgumentException("timeoutNanos");
        Request existing = byTarget.get(targetId);
        if (existing != null) {
            existing.listeners.add(new Registration(silent, listener));
            if (existing != inFlight && priority > existing.priority) {
                pending.remove(existing);
                existing.priority = priority;
                pending.add(existing);
            }
            return;
        }
        Request request = new Request(targetId, priority, timeoutNanos,
                sequence++);
        request.listeners.add(new Registration(silent, listener));
        byTarget.put(targetId, request);
        pending.add(request);
    }

    public synchronized void tick(long nowNanos, Sender sender) {
        if (sender == null) throw new IllegalArgumentException("sender");
        if (inFlight != null && nowNanos - inFlight.sentAt >= inFlight.timeoutNanos) {
            finish(inFlight, Completion.TIMEOUT);
        }
        if (inFlight != null) return;
        Request next = pending.poll();
        if (next == null) return;
        inFlight = next;
        next.sentAt = nowNanos;
        try {
            sender.send(next.targetId);
        } catch (RuntimeException error) {
            finish(next, Completion.FAILED);
            throw error;
        }
    }

    /** Returns true only when a silent subscriber accepted the line. */
    public synchronized boolean observe(String title, String message) {
        Request active = inFlight;
        if (active == null) return false;
        boolean consume = false;
        boolean complete = false;
        boolean failed = false;
        for (Registration registration : new ArrayList<>(active.listeners)) {
            LineResult result;
            try {
                result = registration.listener.onLine(active.targetId,
                        title == null ? "" : title,
                        message == null ? "" : message);
            } catch (Throwable error) {
                LOG.log(Level.WARNING,
                        "Examine subscriber failed; dispatch continues", error);
                continue;
            }
            if (result != LineResult.IGNORE && registration.silent) consume = true;
            complete |= result == LineResult.COMPLETE;
            failed |= result == LineResult.FAILED;
        }
        if (failed) finish(active, Completion.FAILED);
        else if (complete) finish(active, Completion.COMPLETE);
        return consume;
    }

    public synchronized int pendingCount() {
        return pending.size() + (inFlight == null ? 0 : 1);
    }

    public synchronized long inFlightTarget() {
        return inFlight == null ? Long.MIN_VALUE : inFlight.targetId;
    }

    public synchronized void clear() {
        List<Request> requests = new ArrayList<>(pending);
        pending.clear();
        if (inFlight != null) requests.add(inFlight);
        inFlight = null;
        byTarget.clear();
        for (Request request : requests) notifyFinished(request, Completion.FAILED);
    }

    private void finish(Request request, Completion completion) {
        if (inFlight == request) inFlight = null;
        else pending.remove(request);
        byTarget.remove(request.targetId);
        notifyFinished(request, completion);
    }

    private static void notifyFinished(Request request, Completion completion) {
        for (Registration registration : new ArrayList<>(request.listeners)) {
            try {
                registration.listener.onFinished(request.targetId, completion);
            } catch (Throwable error) {
                LOG.log(Level.WARNING,
                        "Examine completion subscriber failed", error);
            }
        }
    }

    private static final class Request {
        private final long targetId;
        private int priority;
        private final long timeoutNanos;
        private final long sequence;
        private final List<Registration> listeners = new ArrayList<>();
        private long sentAt;

        private Request(long targetId, int priority, long timeoutNanos,
                        long sequence) {
            this.targetId = targetId;
            this.priority = priority;
            this.timeoutNanos = timeoutNanos;
            this.sequence = sequence;
        }
    }

    private static final class Registration {
        private final boolean silent;
        private final Listener listener;

        private Registration(boolean silent, Listener listener) {
            this.silent = silent;
            this.listener = listener;
        }
    }
}
