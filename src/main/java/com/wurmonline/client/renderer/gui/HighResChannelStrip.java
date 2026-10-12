package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.HudMessages;
import org.highreshud.client.i18n.HudHoverText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Sloped SDK artwork/metrics with native channel selection, context menus and unread state. */
final class HighResChannelStrip extends FlexComponent {
    static final UiScale CAPTION_SCALE = HighResWindowTypography.COMPACT_SCALE, ART_SCALE = new UiScale(.75f);
    static final int HEIGHT = CAPTION_SCALE.pixels(24);
    private final WurmTabbedWindow window;
    private final List<Channel> channels = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private String[] labels = new String[0];
    private int[] naturalWidths = new int[0];
    private int layoutWidth = -1;
    private AbstractTab selected;
    private long languageRevision = -1;
    HighResChannelStrip(String id, WurmTabbedWindow window) {
        super(id, 0, 0, 1, HEIGHT); this.window = window; sizeFlags = FIXED_HEIGHT;
    }
    @SuppressWarnings("unchecked")
    boolean synchronize(int availableWidth) {
        List<TabButton> snapshot = new ArrayList<>();
        WurmArrayPanel<TabbedTopBar> bars = (WurmArrayPanel<TabbedTopBar>) HighResWindowAccess.get(window, "tabPanels");
        for (TabbedTopBar bar : bars.components) {
            List<TabButton> tabs = (List<TabButton>) HighResWindowAccess.get(bar.getTabPanel(), "tabs");
            for (TabButton tab : tabs) if (!tab.tab.isHidden() && tab.tab.isOpened()) snapshot.add(tab);
        }
        boolean changed = languageRevision != HudMessages.revision() || snapshot.size() != channels.size();
        for (int i = 0; !changed && i < snapshot.size(); i++)
            changed = channels.get(i).nativeButton != snapshot.get(i)
                    || !channels.get(i).source.equals(snapshot.get(i).getLabel());
        if (changed) {
            for (Channel channel : channels) channel.parent = null;
            channels.clear();
            labels = new String[snapshot.size()]; naturalWidths = new int[snapshot.size()];
            for (int i = 0; i < labels.length; i++) {
                labels[i] = caption(snapshot.get(i).getLabel());
                // Keep the compact nominal role, rather than growing short event captions.
                int size = UiCompactLayout.fontPixels(CAPTION_SCALE);
                int measured = Math.max(UiTypography.width(labels[i], size, false, UiDensity.HIGH),
                        UiTypography.width(labels[i], size, true, UiDensity.HIGH));
                naturalWidths[i] = Math.max(48, Math.min(220, snapshot.get(i).width + 6));
                int leading = i == 0 ? 0 : Math.round(naturalWidths[i - 1] * .15f);
                while (naturalWidths[i] < 220 && naturalWidths[i] - Math.min(leading, naturalWidths[i] / 3)
                        - Math.round(naturalWidths[i] * .15f) < measured + 2 * CAPTION_SCALE.pixels(4) + 2) naturalWidths[i]++;
            }
            for (int i = 0; i < snapshot.size(); i++) channels.add(new Channel(snapshot.get(i), i, labels[i]));
            languageRevision = HudMessages.revision();
        }
        if (changed || layoutWidth != availableWidth) reflow(availableWidth);
        AbstractTab current = window.getSelectedTab();
        boolean selectionChanged = current != selected; selected = current;
        return changed || selectionChanged;
    }
    static String caption(String source) {
        String key = source.startsWith(":") ? source.substring(1) : source;
        // Built-in recruitment/information names also arrive in upper case.
        for (String standard : new String[]{"Recruit", "Info", "Infos", "Friends"})
            if (standard.equalsIgnoreCase(key)) return HudMessages.tr(standard);
        return HudMessages.tr(key);
    }
    private static final class Row {
        final int first;
        final UiTabLayout geometry;
        Row(int first, UiTabLayout geometry) { this.first = first; this.geometry = geometry; }
    }
    private void reflow(int availableWidth) {
        layoutWidth = Math.max(64, availableWidth); rows.clear();
        int[] widths = naturalWidths.clone();
        for (int i = 0; i < widths.length; i++) widths[i] = Math.min(widths[i], layoutWidth);
        int first = 0, used = 0;
        for (int i = 0; i < widths.length; i++) {
            int overlap = i == first ? 0 : Math.min(Math.round(widths[i - 1] * .15f), widths[i] / 3);
            if (i > first && used + widths[i] - overlap > layoutWidth) {
                addRow(first, i, widths); first = i; used = 0; overlap = 0;
            }
            used += widths[i] - overlap;
        }
        if (first < widths.length) addRow(first, widths.length, widths);
        int[] captionWidths = new int[widths.length];
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = rows.get(rowIndex);
            for (int local = 0; local < row.geometry.size(); local++) {
                Channel channel = channels.get(row.first + local);
                channel.row = row; channel.rowMember = local; channel.rowIndex = rowIndex;
                captionWidths[channel.member] = row.geometry.tabWidth(local) - channel.leading() - channel.trailing();
            }
        }
        UiCompactLayout[] captions = labels.length == 0 ? new UiCompactLayout[0] : UiCompactLayout.group(labels, captionWidths, CAPTION_SCALE, true);
        for (int i = 0; i < channels.size(); i++) channels.get(i).captionLayout = captions[i];
        sizeFlags = 0; setSize(layoutWidth, Math.max(1, rows.size()) * HEIGHT); sizeFlags = FIXED_HEIGHT;
        componentResized();
    }
    private void addRow(int first, int end, int[] widths) {
        rows.add(new Row(first, UiTabLayout.fitStacked(Arrays.copyOfRange(labels, first, end),
                Arrays.copyOfRange(widths, first, end), HEIGHT, UiDensity.HIGH, ART_SCALE, .15f)));
    }
    int rowCount() { return Math.max(1, rows.size()); }
    int channelCount() { return channels.size(); }
    @Override void componentResized() {
        for (Channel channel : channels) if (channel.row != null) channel.setLocation(x + channel.row.geometry.tabX(channel.rowMember), y + (rows.size() - 1 - channel.rowIndex) * HEIGHT,
                channel.row.geometry.tabWidth(channel.rowMember), HEIGHT);
    }
    @Override public FlexComponent getComponentAt(int px, int py) {
        if (!contains(px, py)) return null;
        int visualRow = (py - y) / HEIGHT;
        int rowIndex = rows.size() - 1 - visualRow;
        if (rowIndex < 0 || rowIndex >= rows.size()) return this;
        Row row = rows.get(rowIndex);
        int member = row.geometry.hitTest(px - x, py - y - visualRow * HEIGHT);
        return member < 0 ? this : channels.get(row.first + member);
    }
    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        for (Channel channel : channels) channel.render(queue, 1.0f);
    }
    @Override public void gameTick() { for (Channel channel : channels) channel.gameTick(); }
    // Empty space beside the channel buttons remains a native window drag area.
    @Override protected void leftPressed(int x, int y, int count) { window.dragger.leftPressed(x,y,count); }
    @Override protected void mouseDragged(int x, int y) { window.dragger.mouseDragged(x,y); }
    @Override protected void leftReleased(int x, int y) { window.dragger.leftReleased(x,y); }
    final class Channel extends WButton implements ButtonListener {
        final TabButton nativeButton;
        final int member;
        final String source;
        UiCompactLayout captionLayout;
        Row row;
        int rowMember, rowIndex;
        final ChamomiloUiV1Canvas ui = new ChamomiloUiV1Canvas(this);
        final UiButtonMotion motion = new UiButtonMotion();
        boolean held, armed;
        Channel(TabButton nativeButton, int member, String caption) {
            super(caption); this.nativeButton = nativeButton; this.member = member;
            source = nativeButton.getLabel(); parent = HighResChannelStrip.this;
            setButtonListener(this); setHoverString(HudHoverText.channel(source));
            sizeFlags = 0; setSize(naturalWidths[member], HEIGHT);
            sizeFlags = FIXED_WIDTH | FIXED_HEIGHT;
        }
        private int leading() { return row.geometry.captionLeadingInset(rowMember) - row.geometry.insetX; }
        private int trailing() { return row.geometry.captionTrailingInset(rowMember) - row.geometry.insetX; }
        boolean ownsPointer(int px, int py) {
            if (HighResChannelStrip.this.getComponentAt(px, py) != this) return false;
            for (WurmComponent cursor = HighResChannelStrip.this.parent; cursor != null; cursor = cursor.parent)
                if (!cursor.contains(px, py)) return false;
            return true;
        }
        @Override public void pick(com.wurmonline.client.renderer.PickData pick, int px, int py) {
            if (ownsPointer(px, py)) pick.addText(HudHoverText.channel(source));
        }
        @Override protected void leftPressed(int px, int py, int count) {
            if (!ownsPointer(px, py)) return;
            super.leftPressed(px, py, count); held = armed = nativeButton.isEnabled(); motion.pointerPressed(System.nanoTime());
        }
        @Override protected void mouseDragged(int px, int py) {
            super.mouseDragged(px, py); isCloseHovered = ownsPointer(px, py);
            if (!isCloseHovered) held = armed = false;
        }
        @Override protected void mouseMoved(int px, int py) { super.mouseMoved(px, py); hovered = ownsPointer(px, py); }
        @Override protected void mouseExited() { super.mouseExited(); held = armed = false; }
        @Override protected void rightPressed(int px, int py, int count) {
            if (!ownsPointer(px, py)) return;
            nativeButton.rightPressed(px, py, count);
            HighResSkillDetailDialog.extendMenu(hud, nativeButton);
        }
        @Override public void buttonPressed(WButton button) { }
        @Override public void buttonClicked(WButton button) {
            boolean fire = armed && parent == HighResChannelStrip.this && nativeButton.isEnabled()
                    && nativeButton.tab.isOpened() && !nativeButton.tab.isHidden(); held = armed = false;
            if (fire) nativeButton.tab.select();
        }
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            boolean enabled = nativeButton.isEnabled();
            setEnabled(enabled);
            if (!enabled) held = armed = false;
            motion.update(enabled, hovered, held && isCloseHovered, System.nanoTime());
            boolean unread = nativeButton.tab instanceof DefaultTab
                    ? (Boolean) HighResWindowAccess.get(nativeButton.tab, "hasNewMessages")
                    : (Boolean) HighResWindowAccess.get(nativeButton, "highlight");
            UiTabPainter.paintStacked(ui.begin(queue), ART_SCALE, 1.0f, x, y,
                    motion.brightness(), motion.depth(), window.getSelectedTab() == nativeButton.tab, unread, row.geometry, rowMember);
            boolean bold = enabled && hovered;
            TextFont font = HighResWindowTypography.caption(bold);
            UiColor color = UiPainter.buttonCaptionColor(enabled, motion.hover());
            int pressed = motion.depth() >= .5f ? 1 : 0;
            font.moveTo(x + leading() + captionLayout.textX(0, bold) + pressed, y + captionLayout.baseline(0) + pressed);
            font.paint(queue, captionLayout.rows()[0], color.red, color.green, color.blue, 1.0f);
        }
    }
}
