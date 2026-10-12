package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.*;

/** Shared caption and original-case data roles; native editing keeps its own metrics. */
final class HighResWindowTypography {
    static final UiScale COMPACT_SCALE = new UiScale(.875f);
    static final int CAPTION_PIXELS = UiCompactLayout.fontPixels(COMPACT_SCALE);
    private HighResWindowTypography() { }
    static TextFont caption(boolean bold) { return ChamomiloUiV1Fonts.caption(CAPTION_PIXELS, bold, UiDensity.HIGH); }
    static TextFont heading() { return caption(true); }
    static TextFont label() { return ChamomiloUiV1Fonts.caption(CAPTION_PIXELS, false, UiDensity.LOW); }
}
