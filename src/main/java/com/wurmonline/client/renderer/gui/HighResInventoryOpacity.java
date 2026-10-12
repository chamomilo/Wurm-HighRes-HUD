package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.backend.Primitive;
import com.wurmonline.client.renderer.backend.Queue;
import org.chamomilo.wurm.ui.v1.UiColor;
import org.highreshud.client.HighResWindowsSettings;
import org.highreshud.client.i18n.HudMessages;

/** Per-window opacity with a recoverable, always-visible header slider. */
final class HighResInventoryOpacity extends FlexComponent {
    private static final int THUMB = 3, INSET = 2;
    private final HighResNativeWindow skin;
    private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
    private boolean dragging;
    private int grabOffset;

    HighResInventoryOpacity(HighResNativeWindow skin) {
        super("highres.inventory.opacity", 0, 0, 1, 22);
        this.skin = skin;
    }
    int value() { return skin.opacityPercent(); }
    static int widthFor(int filterWidth) { return Math.round((filterWidth / 3) * 1.3f); }
    private int travel() { return Math.max(1, width - 2 * INSET - THUMB); }
    int thumbX() { return x + INSET + Math.round((value() - 10) * travel() / 90f); }
    private void dragTo(int px) {
        int offset = Math.max(0, Math.min(travel(), px - grabOffset - x - INSET));
        skin.opacityPercent(10 + Math.round(offset * 90f / travel()));
    }
    @Override public FlexComponent getComponentAt(int px, int py) { return contains(px, py) ? this : null; }
    @Override protected void leftPressed(int px, int py, int count) {
        dragging = true;
        grabOffset = px >= thumbX() - 3 && px < thumbX() + THUMB + 3 ? px - thumbX() : THUMB / 2;
        dragTo(px);
    }
    @Override protected void mouseDragged(int px, int py) { if (dragging) dragTo(px); }
    @Override protected void leftReleased(int px, int py) { if (dragging) dragTo(px); dragging = false; }
    @Override public void pick(PickData pick, int px, int py) { pick.addText(HudMessages.tr("Inventory window opacity: {0}%", value())); }
    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        canvas.begin(queue);
        canvas.fill(UiColor.rgb(0x241d14), 1f, x, y, width, height);
        int thumb = thumbX();
        canvas.fill(UiColor.rgb(0x387943), 1f, x + INSET, y + INSET, thumb - x - INSET, height - 2 * INSET);
        canvas.fill(UiColor.rgb(0xebdfbb), 1f, thumb, y + INSET, THUMB, height - 2 * INSET);
    }
    static void apply(Queue queue, int first, int last, int percent) {
        if (queue == null || percent == 100) return;
        Primitive[] primitives = queue.getQueue();
        float opacity = HighResWindowsSettings.clampOpacity(percent) / 100f;
        for (int n = first; n < last; n++) {
            Primitive primitive=primitives[n]; primitive.a *= opacity;
            if(primitive.blendmode!=Primitive.BlendMode.ALPHABLEND&&primitive.blendmode!=Primitive.BlendMode.ALPHAADD) {
                primitive.blendmode=Primitive.BlendMode.ALPHABLEND; primitive.calcState();
            }
        }
    }
}
