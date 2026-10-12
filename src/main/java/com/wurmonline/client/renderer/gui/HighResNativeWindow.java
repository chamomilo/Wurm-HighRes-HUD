package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.HudMessages;

/** High-res presentation adapter around the exact window registered by Wurm. */
abstract class HighResNativeWindow {
    static final int FRAME = 5, TITLE_HEIGHT = 28, CHANNELS_HEIGHT = HighResChannelStrip.HEIGHT + 14;
    final WWindow window;
    final HeadsUpDisplay owner;
    final String id, title;
    final ChamomiloUiV1Canvas canvas;
    final Header header;
    final FlexComponent drawing = new FlexComponent("highres.window.draw", 0, 0, 1, 1) {
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) { HighResNativeWindow.this.render(queue); }
    };
    private boolean maximized;
    private int normalX, normalY, normalWidth, normalHeight;

    HighResNativeWindow(String id, String title, WWindow window, HeadsUpDisplay owner) {
        this.id = id; this.title = title; this.window = window; this.owner = owner;
        canvas = new ChamomiloUiV1Canvas(window);
        header = new Header();
    }
    void install() {
        // Keep native side/bottom resize handlers, save keys, body, input and listeners.
        window.setComponent(window.getComponent(), WurmBorderPanel.CENTER);
        window.setComponent(header, WurmBorderPanel.NORTH);
        FlexComponent bottom = (FlexComponent) HighResWindowAccess.get(window, "southBar");
        bottom.sizeFlags = 0; bottom.setSize(bottom.width, hasTitle() ? FRAME : 3); bottom.sizeFlags = FlexComponent.FIXED_HEIGHT;
        header.updateHeight();
        if (window.minimized) window.setSize(window.width, TITLE_HEIGHT + FRAME);
        window.layout();
    }
    void tick() { header.refresh(); HighResWindowIntegration.skinContent(window.getComponent()); }
    void render(Queue queue) {
        tick();
        int bodyY = hasTitle() ? window.y : window.y + header.height;
        UiPainter.background(canvas.begin(queue), UiBackground.LEATHER, UiScale.BASE,
                1.0f, window.x, bodyY, window.width, window.y + window.height - bodyY);
        if (!window.minimized) window.getComponent().render(queue, 1.0f);
        if (!window.minimized) HighResWindowIntegration.paintInventoryDrag(this, queue);
        // The continuous top rail sits under the lower edge of the joined tabs.
        if (!hasTitle()) UiPainter.frame(canvas, 3, 1.0f, window.x, bodyY - 3, window.width, window.y + window.height - bodyY + 3);
        header.render(queue, 1.0f);
        if (hasTitle()) UiPainter.frame(canvas, FRAME, 1.0f, window.x, window.y, window.width, window.height);
        if (!window.minimized && window.resizable) UiIcon.RESIZE.paint(canvas, UiColor.MUTED, 1.0f,
                window.x + window.width - 19, window.y + window.height - 19, 12);
    }
    void close() { owner.hideComponent(window); }
    void collapse() { window.toggleSize(); collapsedChanged(); }
    void collapsedChanged() {
        header.updateHeight();
        if (window.minimized) window.setSize(window.width, TITLE_HEIGHT + FRAME);
        window.layout();
    }
    void maximize() {
        if (window.minimized) collapse();
        if (!maximized) {
            normalX = window.x; normalY = window.y; normalWidth = window.width; normalHeight = window.height;
            window.setLocation(10, 10, Math.max(260, owner.getWidth() - 20), Math.max(140, owner.getHeight() - 20));
        } else window.setLocation(normalX, normalY, normalWidth, normalHeight);
        maximized = !maximized; header.refresh();
    }
    FlexComponent tabs() { return null; }
    boolean hasTitle() { return true; }
    int channelHeight() { return CHANNELS_HEIGHT; }
    String caption() { return HudMessages.tr(title); }
    WurmTreeList<?> itemTable() { return null; }

    final class Header extends FlexComponent {
        final ChamomiloUiV1Canvas ui = new ChamomiloUiV1Canvas(this);
        final ChamomiloUiV1CompactButton close, collapse, maximize;
        private UiIcon collapseIcon, maximizeIcon;
        Header() {
            super(id + ".header", 0, 0, 1, TITLE_HEIGHT);
            sizeFlags = FIXED_HEIGHT;
            if (!hasTitle()) { close = collapse = maximize = null; return; }
            close = new ChamomiloUiV1CompactButton(UiIcon.CLOSE, 24, UiScale.BASE, () -> HighResNativeWindow.this.close());
            collapse = new ChamomiloUiV1CompactButton(UiIcon.MINIMIZE, 24, UiScale.BASE, () -> HighResNativeWindow.this.collapse());
            maximize = new ChamomiloUiV1CompactButton(UiIcon.MAXIMIZE, 24, UiScale.BASE, () -> HighResNativeWindow.this.maximize());
            close.setIcon(UiIcon.CLOSE);
            close.parent = this; collapse.parent = this; maximize.parent = this;
            refresh();
        }
        void updateHeight() {
            int desired = (hasTitle() ? TITLE_HEIGHT : 0) + (!window.minimized && tabs() != null ? channelHeight() : 0);
            if (height == desired) return;
            sizeFlags = 0; setSize(width, desired); sizeFlags = FIXED_HEIGHT;
        }
        void refresh() {
            if (close == null) return;
            UiLabels labels = UiLabels.forLanguage(HudMessages.language());
            close.setHoverString(labels.close);
            UiIcon nextCollapse = window.minimized ? UiIcon.CHEVRON_DOWN : UiIcon.MINIMIZE;
            if (collapseIcon != nextCollapse) { collapseIcon = nextCollapse; collapse.setIcon(nextCollapse); }
            collapse.setHoverString(window.minimized ? labels.restore : labels.minimize);
            UiIcon nextMaximize = maximized ? UiIcon.RESTORE : UiIcon.MAXIMIZE;
            if (maximizeIcon != nextMaximize) { maximizeIcon = nextMaximize; maximize.setIcon(nextMaximize); }
            maximize.setHoverString(maximized ? labels.restore : labels.maximize);
        }
        @Override void componentResized() {
            if (close != null) {
                close.setLocation(x + width - FRAME - 24, y + 2, 24, 24);
                maximize.setLocation(close.x - 26, y + 2, 24, 24);
                collapse.setLocation(maximize.x - 26, y + 2, 24, 24);
            }
            FlexComponent strip = tabs();
            int inset = hasTitle() ? FRAME : 0;
            if (strip != null) strip.setLocation(x + inset, y + (hasTitle() ? TITLE_HEIGHT : 0), Math.max(1, width - 2 * inset), channelHeight());
        }
        @Override public FlexComponent getComponentAt(int px, int py) {
            if (!contains(px, py)) return null;
            if (close != null) {
                if (close.contains(px, py)) return close;
                if (collapse.contains(px, py)) return collapse;
                if (maximize.contains(px, py)) return maximize;
            }
            if (!window.minimized && tabs() != null && py >= y + (hasTitle() ? TITLE_HEIGHT : 0)) return tabs().getComponentAt(px, py);
            return this;
        }
        @Override protected void leftPressed(int px, int py, int count) {
            if (hasTitle() && count > 0 && count % 2 == 0) HighResNativeWindow.this.collapse();
            else window.dragger.leftPressed(px, py, count);
        }
        @Override protected void mouseDragged(int px, int py) { window.dragger.mouseDragged(px, py); }
        @Override protected void leftReleased(int px, int py) { window.dragger.leftReleased(px, py); }
        @Override public void pick(com.wurmonline.client.renderer.PickData pick, int px, int py) {
            if (hasTitle() && contains(px, py) && px < collapse.x) pick.addText(HighResNativeWindow.this.caption());
        }
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            componentResized();
            if (hasTitle()) {
                UiPainter.background(ui.begin(queue), UiBackground.WALNUT, UiScale.BASE, 1.0f, x, y, width, TITLE_HEIGHT);
                TextFont font = HighResWindowTypography.heading();
                String caption = HighResWindowIntegration.ellipsis(HighResNativeWindow.this.caption(), font, Math.max(0, collapse.x - x - 18));
                font.moveTo(x + FRAME + 6, y + 19);
                font.paint(queue, caption, UiColor.TEXT.red, UiColor.TEXT.green, UiColor.TEXT.blue, 1.0f);
                close.render(queue, 1.0f); collapse.render(queue, 1.0f); maximize.render(queue, 1.0f);
            }
            if (!window.minimized && tabs() != null) tabs().render(queue, 1.0f);
        }
    }
}
