package org.highresfocusbar.client;

import com.wurmonline.shared.constants.PlayerAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Thread-safe mirror of the two stock callbacks that contain transient data. */
public final class FocusBarState {
    public static final long NO_SELECTION = Long.MIN_VALUE;

    public static final class Progress {
        public final float value;
        public final String title;
        public final boolean changeColor;

        private Progress(float value, String title, boolean changeColor) {
            this.value = value;
            this.title = title;
            this.changeColor = changeColor;
        }
    }

    private List<PlayerAction> actions = Collections.emptyList();
    private Progress progress = new Progress(0f, "", false);
    private long selectionId = NO_SELECTION;

    /**
     * Keeps Select Bar actions stable across the stock client's transient
     * clear/reselect callbacks. A genuinely new selection gets Examine
     * immediately, while reselecting the same id preserves the full list.
     */
    public synchronized void selectionChanged(long nextSelectionId) {
        if (nextSelectionId == NO_SELECTION) return;
        if (nextSelectionId == selectionId) return;
        selectionId = nextSelectionId;
        actions = Collections.singletonList(PlayerAction.EXAMINE);
        progress = new Progress(0f, "", false);
    }

    public synchronized void mirrorActions(byte responseId, byte expectedId,
                                           List<PlayerAction> serverActions) {
        if (responseId != expectedId) return;
        List<PlayerAction> next = new ArrayList<>();
        Set<Short> ids = new LinkedHashSet<>();
        add(next, ids, PlayerAction.EXAMINE);
        if (serverActions != null) {
            for (PlayerAction action : serverActions) {
                add(next, ids, action);
            }
        }
        actions = Collections.unmodifiableList(next);
    }

    public synchronized void clearActions() {
        actions = Collections.emptyList();
        selectionId = NO_SELECTION;
    }

    public synchronized void clearProgress() {
        progress = new Progress(0f, "", false);
    }

    public synchronized long selectionId() {
        return selectionId;
    }

    public synchronized List<PlayerAction> actions() {
        return actions;
    }

    public synchronized void mirrorProgress(float value, String title,
                                            boolean changeColor) {
        float clamped = Math.max(0f, Math.min(1f,
                Float.isFinite(value) ? value : 0f));
        progress = new Progress(clamped, title == null ? "" : title.trim(),
                changeColor);
    }

    public synchronized Progress progress() {
        return progress;
    }

    private static void add(List<PlayerAction> target, Set<Short> ids,
                            PlayerAction action) {
        if (action == null) return;
        if (ids.add(action.getId())) target.add(action);
    }
}
