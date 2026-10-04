package org.highreshud.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Render-backend-neutral portrait scheduler. It deduplicates compatible HUD
 * portraits while preserving separate passes for distinct subjects or views.
 * The world avatar is intentionally outside this scheduler.
 */
public final class PortraitScheduler<T> {
    public interface Renderer<T> {
        T render(RequestKey key);
    }

    public static final class RequestKey {
        private final long subjectId;
        private final long modelRevision;
        private final String profile;
        private final int width;
        private final int height;
        private final long animationBucket;

        public RequestKey(long subjectId, long modelRevision, String profile,
                          int width, int height, long animationBucket) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("portrait dimensions");
            }
            this.subjectId = subjectId;
            this.modelRevision = modelRevision;
            this.profile = profile == null ? "" : profile;
            this.width = width;
            this.height = height;
            this.animationBucket = animationBucket;
        }

        public long subjectId() { return subjectId; }
        public long modelRevision() { return modelRevision; }
        public String profile() { return profile; }
        public int width() { return width; }
        public int height() { return height; }
        public long animationBucket() { return animationBucket; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RequestKey)) return false;
            RequestKey key = (RequestKey) other;
            return subjectId == key.subjectId
                    && modelRevision == key.modelRevision
                    && width == key.width && height == key.height
                    && animationBucket == key.animationBucket
                    && profile.equals(key.profile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(subjectId, modelRevision, profile, width,
                    height, animationBucket);
        }
    }

    private final int maxPassesPerFrame;
    private final Map<String, Submission> submissions = new LinkedHashMap<>();
    private final Map<String, T> slotValues = new HashMap<>();
    private final Map<RequestKey, CacheEntry<T>> cache = new HashMap<>();
    private long frame;
    private int lastUniquePasses;
    private int lastSavedPasses;

    public PortraitScheduler(int maxPassesPerFrame) {
        if (maxPassesPerFrame <= 0) {
            throw new IllegalArgumentException("maxPassesPerFrame");
        }
        this.maxPassesPerFrame = maxPassesPerFrame;
    }

    public synchronized void beginFrame() {
        frame++;
        submissions.clear();
        lastUniquePasses = 0;
        lastSavedPasses = 0;
    }

    public synchronized void submit(String slot, RequestKey key,
                                    int priority) {
        if (slot == null || slot.trim().isEmpty()) {
            throw new IllegalArgumentException("slot");
        }
        if (key == null) throw new IllegalArgumentException("key");
        submissions.put(slot, new Submission(slot, key, priority));
    }

    public synchronized void flush(Renderer<T> renderer) {
        if (renderer == null) throw new IllegalArgumentException("renderer");
        Map<RequestKey, List<Submission>> groups = new HashMap<>();
        for (Submission submission : submissions.values()) {
            List<Submission> group = groups.get(submission.key);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(submission.key, group);
            }
            group.add(submission);
        }
        lastSavedPasses = submissions.size() - groups.size();
        List<List<Submission>> ordered = new ArrayList<>(groups.values());
        Collections.sort(ordered, new Comparator<List<Submission>>() {
            @Override
            public int compare(List<Submission> left, List<Submission> right) {
                return Integer.compare(maxPriority(right), maxPriority(left));
            }
        });

        int budget = maxPassesPerFrame;
        for (List<Submission> group : ordered) {
            RequestKey key = group.get(0).key;
            CacheEntry<T> cached = cache.get(key);
            T value = cached == null ? null : cached.value;
            if (cached == null && budget > 0) {
                value = renderer.render(key);
                cache.put(key, new CacheEntry<>(value, frame));
                lastUniquePasses++;
                budget--;
            } else if (cached != null) {
                cached.lastUsedFrame = frame;
            }
            if (value != null) {
                for (Submission submission : group) {
                    slotValues.put(submission.slot, value);
                }
            }
        }
        evictUnused(120L);
    }

    public synchronized T value(String slot) {
        return slotValues.get(slot);
    }

    public synchronized int lastUniquePasses() {
        return lastUniquePasses;
    }

    public synchronized int lastSavedPasses() {
        return lastSavedPasses;
    }

    public synchronized int activeSlots() {
        return submissions.size();
    }

    private void evictUnused(long maximumAge) {
        List<RequestKey> expired = new ArrayList<>();
        for (Map.Entry<RequestKey, CacheEntry<T>> entry : cache.entrySet()) {
            if (frame - entry.getValue().lastUsedFrame > maximumAge) {
                expired.add(entry.getKey());
            }
        }
        for (RequestKey key : expired) cache.remove(key);
    }

    private static int maxPriority(List<Submission> group) {
        int result = Integer.MIN_VALUE;
        for (Submission submission : group) {
            result = Math.max(result, submission.priority);
        }
        return result;
    }

    private static final class Submission {
        private final String slot;
        private final RequestKey key;
        private final int priority;

        private Submission(String slot, RequestKey key, int priority) {
            this.slot = slot;
            this.key = key;
            this.priority = priority;
        }
    }

    private static final class CacheEntry<T> {
        private final T value;
        private long lastUsedFrame;

        private CacheEntry(T value, long lastUsedFrame) {
            this.value = value;
            this.lastUsedFrame = lastUsedFrame;
        }
    }
}
