package org.highreshud.client;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Runtime gate for the legacy HUD parsers while they migrate to ExamineBroker.
 * It guarantees that unrelated hidden targets are never in flight together
 * and lets multiple panels join one request for the same target.
 */
public final class HiddenExamineCoordinator {
    public enum Claim { SEND, JOIN, WAIT }

    private static final long REQUEST_TIMEOUT_NANOS = 8_000_000_000L;
    private static final long RESPONSE_TAIL_NANOS = 500_000_000L;
    private static final Set<String> owners = new LinkedHashSet<>();
    private static long targetId = Long.MIN_VALUE;
    private static long expiresAt;
    private static long responseTailUntil;
    private static boolean responseStarted;

    private HiddenExamineCoordinator() {
    }

    public static synchronized Claim claim(String owner, long target,
                                           long nowNanos) {
        expire(nowNanos);
        String safeOwner = owner == null ? "unknown" : owner;
        if (targetId == Long.MIN_VALUE) {
            targetId = target;
            expiresAt = nowNanos + REQUEST_TIMEOUT_NANOS;
            responseStarted = false;
            responseTailUntil = 0L;
            owners.add(safeOwner);
            return Claim.SEND;
        }
        if (responseStarted) return Claim.WAIT;
        if (targetId == target && !owners.contains(safeOwner)) {
            owners.add(safeOwner);
            return Claim.JOIN;
        }
        return Claim.WAIT;
    }

    public static synchronized boolean mayConsume(long nowNanos) {
        expire(nowNanos);
        return targetId != Long.MIN_VALUE;
    }

    public static synchronized void responseObserved(long nowNanos) {
        if (targetId == Long.MIN_VALUE) return;
        responseStarted = true;
        responseTailUntil = nowNanos + RESPONSE_TAIL_NANOS;
    }

    /** A visible Examine takes ownership; hidden replies must not swallow it. */
    public static synchronized void visibleExamineStarted() {
        clear();
    }

    public static synchronized long activeTarget() {
        return targetId;
    }

    public static synchronized int joinedOwners() {
        return owners.size();
    }

    public static synchronized void reset() {
        clear();
    }

    private static void expire(long nowNanos) {
        if (targetId == Long.MIN_VALUE) return;
        if (responseStarted ? nowNanos > responseTailUntil
                : nowNanos > expiresAt) {
            clear();
        }
    }

    private static void clear() {
        targetId = Long.MIN_VALUE;
        expiresAt = 0L;
        responseTailUntil = 0L;
        responseStarted = false;
        owners.clear();
    }
}
