package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.options.Options;
import com.wurmonline.client.renderer.gui.text.HudTextFonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import java.util.*;
import org.chamomilo.wurm.ui.v1.UiScale;
import org.chamomilo.wurm.ui.v1.UiTypography;

/** Applies the shared body family through native data metrics, without global font changes. */
final class HighResDataTypography {
    private HighResDataTypography() { }
    static int bodyPixels(int normal) {
        return org.highreshud.client.HighResWindowsSettings.fontSize > 0 ? pixels(normal) : normal;
    }
    static int pixels(int startingSize) {
        int override = org.highreshud.client.HighResWindowsSettings.fontSize;
        if (override > 0) {
            for (int size : UiTypography.sizes()) if (size >= override) return size;
            return 128;
        }
        int preference = Options.fontSizeDefault == null ? WurmComponent.fontSize : Math.round(Options.fontSizeDefault.value());
        int requested = Math.max(UiScale.BASE.pixels(startingSize), preference);
        for (int size : UiTypography.sizes()) if (size >= requested) return size;
        return 128;
    }
    static void apply(FlexComponent component) {
        if (component instanceof WurmTreeList) table((WurmTreeList<?>) component);
        else if (component instanceof WurmTextPanel) text((WurmTextPanel) component);
        // Input fields retain native caret, selection and clipboard metrics.
    }
    private static void table(WurmTreeList<?> table) {
        int pixels = pixels(14);
        TextFont regular = HudTextFonts.body(pixels, false), bold = HudTextFonts.body(pixels, true);
        if (table.text == regular && table.textBold == bold) return;
        FlexComponent content = (FlexComponent) HighResWindowAccess.get(table, "treeList");
        ScrollAnchor anchor = new ScrollAnchor(content, ((Number) HighResWindowAccess.get(table, "lineHeight")).intValue(), table.lines);
        table.changeText(regular); table.textBold = bold; content.textBold = bold;
        table.recalcLines(); table.layout();
        anchor.restore(((Number) HighResWindowAccess.get(table, "lineHeight")).intValue(), table.lines);
    }
    @SuppressWarnings("unchecked")
    private static void text(WurmTextPanel panel) {
        boolean paragraphs = (Boolean) HighResWindowAccess.get(panel, "useWrap");
        int pixels = pixels(paragraphs ? 16 : 14);
        TextFont regular = HudTextFonts.body(pixels, false), bold = HudTextFonts.body(pixels, true);
        if (panel.text == regular && panel.textBold == bold) return;
        List<Object> visible = (List<Object>) HighResWindowAccess.get(panel, "lines");
        List<Object> originals = (List<Object>) HighResWindowAccess.get(panel, "orgLines");
        ScrollAnchor anchor = new ScrollAnchor(panel, panel.text.getHeight() + 1, visible);
        Set<Integer> selected = new HashSet<>();
        for (Object line : visible) if ((Boolean) HighResWindowAccess.get(line, "selected"))
            selected.add(((Number) HighResWindowAccess.get(line, "id")).intValue());
        panel.text = regular; panel.textBold = bold;
        for (Object line : originals) HighResWindowAccess.set(line, "width", regular.getWidth((String) HighResWindowAccess.get(line, "text")));
        // Reuse native wrapping and multicolor segmentation; retain original messages and IDs.
        HighResWindowAccess.set(panel, "rebuild", true); panel.gameTick();
        for (Object line : visible) if (selected.contains(((Number) HighResWindowAccess.get(line, "id")).intValue()))
            HighResWindowAccess.set(line, "selected", true);
        anchor.restore(regular.getHeight() + 1, visible);
    }
    private static final class ScrollAnchor {
        final WurmScrollPanel scroll;
        final FlexComponent content;
        final Object first;
        final double rowOffset;
        final boolean atBottom;
        ScrollAnchor(FlexComponent content, int lineHeight, List<?> lines) {
            this.content = content;
            WurmScrollPanel found = null;
            for (WurmComponent parent = content.parent; parent != null; parent = parent.parent)
                if (parent instanceof WurmScrollPanel) { found = (WurmScrollPanel) parent; break; }
            scroll = found; atBottom = found != null && found.isAtBottom;
            rowOffset = found == null ? 0 : (double) found.yo / Math.max(1, lineHeight);
            int index = Math.max(0, (int) rowOffset);
            first = index < lines.size() ? key(lines.get(index)) : null;
        }
        private static Object key(Object line) {
            return line instanceof WTreeListNode ? line : HighResWindowAccess.get(line, "id");
        }
        void restore(int lineHeight, List<?> lines) {
            if (scroll == null) return;
            double position = rowOffset;
            if (first != null) for (int i = 0; i < lines.size(); i++) if (first.equals(key(lines.get(i)))) {
                position = i + rowOffset - Math.floor(rowOffset); break;
            }
            int maximum = Math.max(0, content.height - ((FlexComponent) scroll.offs).height);
            scroll.yo = atBottom ? maximum : Math.max(0, Math.min(maximum, (int) Math.round(position * lineHeight)));
            scroll.isAtBottom = atBottom; scroll.layout();
        }
    }
}
