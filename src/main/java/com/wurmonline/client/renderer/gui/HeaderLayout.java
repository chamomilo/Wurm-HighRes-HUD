package com.wurmonline.client.renderer.gui;

import static com.wurmonline.client.renderer.gui.HighResHealthBarLayout.*;

/** Immutable title line wrapping and frame-height decision. */
final class HeaderLayout {
    interface TextWidth {
        int measure(String value);
    }

    private static final HeaderLayout HIDDEN = new HeaderLayout(0,
            new String[0]);

    private final int height;
    private final String[] lines;

    private HeaderLayout(int height, String[] lines) {
        this.height = height;
        this.lines = lines;
    }

    static HeaderLayout hidden() {
        return HIDDEN;
    }

    static HeaderLayout create(boolean showName, boolean showTitles,
                               String playerName, String titles,
                               TextWidth width) {
        if (!showName) return HIDDEN;
        String name = fit(width, clean(playerName), HEADER_TEXT_MAX_WIDTH);
        String titleText = clean(titles);
        if (!showTitles || titleText.isEmpty()) {
            return new HeaderLayout(NAME_ONLY_TITLE_HEIGHT,
                    new String[]{name});
        }

        String[] wrapped = wrapTwoLines(width, titleText,
                HEADER_TEXT_MAX_WIDTH);
        if (width.measure(titleText) <= HEADER_TEXT_MAX_WIDTH) {
            return new HeaderLayout(TWO_LINE_TITLE_HEIGHT,
                    new String[]{name, wrapped[0]});
        }
        return new HeaderLayout(THREE_LINE_TITLE_HEIGHT,
                new String[]{name, wrapped[0], wrapped[1]});
    }

    int height() {
        return height;
    }

    String[] lines() {
        return lines;
    }

    private static String[] wrapTwoLines(TextWidth width, String value,
                                         int maxWidth) {
        if (value.isEmpty() || width.measure(value) <= maxWidth) {
            return new String[]{value, ""};
        }

        int fittingEnd = value.length();
        while (fittingEnd > 0
                && width.measure(value.substring(0, fittingEnd)) > maxWidth) {
            fittingEnd--;
        }
        if (fittingEnd <= 0) {
            return new String[]{"", fit(width, value, maxWidth)};
        }

        int wordBreak = value.lastIndexOf(' ', fittingEnd - 1);
        int split = wordBreak > 0 ? wordBreak : fittingEnd;
        return new String[]{
                value.substring(0, split).trim(),
                fit(width, value.substring(split).trim(), maxWidth)
        };
    }

    private static String fit(TextWidth width, String value, int maxWidth) {
        if (width.measure(value) <= maxWidth) return value;
        String ellipsis = "...";
        int end = value.length();
        while (end > 0
                && width.measure(value.substring(0, end) + ellipsis)
                > maxWidth) {
            end--;
        }
        return value.substring(0, end) + ellipsis;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
