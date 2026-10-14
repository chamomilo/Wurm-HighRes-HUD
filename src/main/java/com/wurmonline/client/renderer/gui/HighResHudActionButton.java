package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.UiButtonMotion;
import org.chamomilo.wurm.ui.v1.UiColor;
import org.chamomilo.wurm.ui.v1.UiPainter;
import org.highreshud.client.ui.HudCaptionGroup;
import org.highreshud.client.i18n.HudMessages;

/** Compact native HUD child: exact bounds, release activation and drag cancellation. */
final class HighResHudActionButton extends StaticComponent {
    private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
    private final HudCaptionGroup group;
    private final UiButtonMotion motion = new UiButtonMotion();
    private final Runnable action;
    private String caption;
    private boolean available = true, hovered, held, armed, active;
    private java.util.function.Supplier<com.wurmonline.client.resources.textures.Texture> leadingIcon;
    void leadingIcon(java.util.function.Supplier<com.wurmonline.client.resources.textures.Texture> icon) { leadingIcon=icon; }

    HighResHudActionButton(WurmComponent parent, int width, int height,
                           HudCaptionGroup group, String caption, Runnable action) {
        super("highres-hud.action." + caption, 0, 0, width, height);
        this.parent = parent;
        this.group = group;
        this.caption = caption;
        this.action = action;
    }

    void caption(String value, boolean enabled) {
        group.caption(value); // Caption changes never resize the native component.
        caption = value; available = enabled;
        if (!enabled) { armed = false; held = false; }
    }

    void active(boolean selected) { active = selected; }

    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        group.refreshLanguage();
        motion.update(available, hovered, held, System.nanoTime());
        UiPainter.button(canvas.begin(queue), motion.brightness(), motion.depth(), group.scale,
                1.0f, x, y, width, height);
        boolean bold = available && hovered;
        TextFont font = ChamomiloUiV1Fonts.caption(group.fontPixels, bold, group.density);
        UiColor color = UiPainter.buttonCaptionColor(available, motion.hover());
        if (active && available) {
            // Same persistent selection treatment as Chamomilo's ON/OFF actions.
            canvas.fill(UiColor.rgb(0x61bd70), .18f, x+5, y+5, width-10, height-10);
            canvas.fill(UiColor.rgb(0x84e397), 1f, x+5, y+height-4, width-10, 2);
            if (!hovered) color = UiColor.rgb(0x84e397);
        }
        int offset = motion.depth() >= .5f ? 1 : 0;
        int leading=leadingIcon==null?0:52;
        if(leadingIcon!=null)HighResBuildWindow.icon(queue,leadingIcon.get(),x+6+offset,y+(height-36)/2+offset,36,available?1f:.5f);
        font.moveTo(x + leading + group.textX(caption, bold, width-leading) + offset, y + group.baseline + offset);
        font.paint(queue, group.caption(caption), color.red, color.green, color.blue, 1.0f);
    }

    @Override protected void leftPressed(int mx, int my, int buttons) {
        armed = available && contains(mx, my) && buttons != 2;
        held = armed;
        if (armed) motion.pointerPressed(System.nanoTime());
    }
    @Override protected void mouseDragged(int mx, int my) {
        hovered = contains(mx, my);
        if (!hovered) armed = false;
        held = armed && hovered;
    }
    @Override protected void leftReleased(int mx, int my) {
        boolean fire = available && armed && contains(mx, my);
        armed = false; held = false;
        if (fire) action.run();
    }
    @Override protected void mouseMoved(int mx, int my) { hovered = contains(mx, my); }
    @Override protected void mouseExited() { hovered = false; armed = false; held = false; }
    @Override protected void rightPressed(int mx, int my, int buttons) {
        if (parent != null) parent.rightPressed(mx, my, buttons);
    }
    @Override protected int getMouseCursor(int mx, int my) {
        return available ? MOUSE_CURSOR_HAND : MOUSE_CURSOR_NORMAL;
    }
    @Override public void pick(PickData pick, int mx, int my) {
        pick.addText(HudMessages.tr(caption));
        parent.pick(pick, mx, my);
    }
}
