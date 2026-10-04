package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.cell.CreatureCellRenderable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One immutable carrier/hitch graph used by size, render and hover for a tick. */
final class RideStatusSnapshot {
    private static final RideStatusSnapshot EMPTY = new RideStatusSnapshot(
            null, Collections.<CreatureCellRenderable>emptyList());

    private final CreatureCellRenderable mount;
    private final List<CreatureCellRenderable> hitched;

    private RideStatusSnapshot(CreatureCellRenderable mount,
                               List<CreatureCellRenderable> hitched) {
        this.mount = mount;
        this.hitched = hitched;
    }

    static RideStatusSnapshot empty() {
        return EMPTY;
    }

    static RideStatusSnapshot of(CreatureCellRenderable mount,
                                 List<CreatureCellRenderable> hitched) {
        if (mount == null) return EMPTY;
        List<CreatureCellRenderable> animals = hitched == null
                || hitched.isEmpty()
                ? Collections.<CreatureCellRenderable>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(hitched));
        return new RideStatusSnapshot(mount, animals);
    }

    CreatureCellRenderable mount() {
        return mount;
    }

    List<CreatureCellRenderable> visibleHitched(boolean showHitched) {
        return showHitched ? hitched
                : Collections.<CreatureCellRenderable>emptyList();
    }

    int rowCount(boolean showRide, boolean showHitched) {
        if (mount == null) return 0;
        if (!mount.isItem()) return showRide ? 2 : 0;
        return (showRide ? 1 : 0) + (showHitched ? hitched.size() : 0);
    }

    int hitchedIndexForRow(int row, boolean showRide,
                           boolean showHitched) {
        if (!showHitched || mount == null || !mount.isItem()) return -1;
        int index = row - (showRide ? 1 : 0);
        return index >= 0 && index < hitched.size() ? index : -1;
    }
}
