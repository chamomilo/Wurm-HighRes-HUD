package org.highreshud.client.portrait;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the previous frame's portrait requests, schedules one job per
 * unique key and lets compatible HUD slots read the leader's texture.
 */
public final class SharedPortraitCoordinator {
    private static final List<SharedPortraitChannel> requested =
            new ArrayList<>();
    private static final List<SharedPortraitChannel> leaders =
            new ArrayList<>();
    private static boolean collecting;
    private static int lastRequested;
    private static int lastPasses;

    private SharedPortraitCoordinator() {
    }

    public static synchronized void beginFrame() {
        requested.clear();
        leaders.clear();
        collecting = true;
    }

    public static synchronized void request(SharedPortraitChannel channel) {
        if (channel == null) return;
        if (!collecting) {
            // Standalone safety: render correctly even if a caller bypasses
            // the unified runtime, but no cross-channel dedup is possible.
            channel.shareFrom(null);
            channel.scheduleOwnRender();
            return;
        }
        requested.add(channel);
    }

    public static synchronized void dispatch() {
        if (!collecting) return;
        collecting = false;
        Map<PortraitRenderKey, SharedPortraitChannel> byKey =
                new LinkedHashMap<>();
        for (SharedPortraitChannel channel : requested) {
            PortraitRenderKey key = channel.renderKey();
            SharedPortraitChannel leader = byKey.get(key);
            if (leader == null) {
                channel.shareFrom(null);
                byKey.put(key, channel);
                leaders.add(channel);
            } else {
                channel.shareFrom(leader);
            }
        }
        lastRequested = requested.size();
        lastPasses = leaders.size();
        for (SharedPortraitChannel leader : leaders) {
            leader.scheduleOwnRender();
        }
    }

    public static synchronized void finishFrame() {
        for (SharedPortraitChannel leader : new ArrayList<>(leaders)) {
            leader.finishOwnRender();
        }
        requested.clear();
        leaders.clear();
    }

    public static synchronized int lastRequested() {
        return lastRequested;
    }

    public static synchronized int lastPasses() {
        return lastPasses;
    }

    public static synchronized int lastSavedPasses() {
        return Math.max(0, lastRequested - lastPasses);
    }
}
