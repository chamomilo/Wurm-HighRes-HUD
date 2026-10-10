package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.backend.Queue;
import org.chamomilo.wurm.ui.v1.UiButtonMotion;
import org.chamomilo.wurm.ui.v1.UiPainter;
import org.highreshud.client.ui.HudSkin;
import static org.highresfightinghud.client.FightingHudLayout.SPECIAL_ICON_INSET;
import org.highreshud.core.ActionOrigin;
import org.highreshud.core.HighResHudApi;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Native command adapter with Chamomilo icon-button feedback and state hints. */
final class HighResCombatHint extends StaticComponent {
    private final StaticComponent nativeControl;
    private final BooleanSupplier enabled;
    private final Supplier<String[]> hints;
    private final boolean iconButton;
    private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
    private final UiButtonMotion motion = new UiButtonMotion();
    private boolean armed, hovered, held;

    HighResCombatHint(WurmComponent parent, StaticComponent nativeControl,
                      BooleanSupplier enabled, Supplier<String[]> hints) {
        this(parent, nativeControl, enabled, hints, false);
    }
    HighResCombatHint(WurmComponent parent, StaticComponent nativeControl,
                      BooleanSupplier enabled, Supplier<String[]> hints, boolean iconButton) {
        super("highres-combat-hint", 0, 0, 0, 0);
        this.parent = parent;
        this.nativeControl = nativeControl;
        this.enabled = enabled;
        this.hints = hints;
        this.iconButton = iconButton;
    }
    private void syncBounds() {
        setLocation(nativeControl.x, nativeControl.y, nativeControl.width, nativeControl.height);
    }
    private boolean inside(int mx, int my) {
        return mx >= x && my >= y && mx < x + width && my < y + height;
    }
    StaticComponent at(int mx, int my) {
        syncBounds();
        return inside(mx, my) ? this : null;
    }
    void renderButton(Queue queue) {
        syncBounds();
        render(queue, 1.0f);
    }
    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        if (!iconButton) return;
        syncBounds();
        boolean available = enabled.getAsBoolean();
        if (!available) { armed = false; held = false; }
        motion.update(available, hovered, held, System.nanoTime());
        UiPainter.button(canvas.begin(queue), motion.brightness(), motion.depth(), HudSkin.COMPACT,
                1.0f, x, y, width, height);
        AttackButtonComponent icon = (AttackButtonComponent) nativeControl;
        boolean savedHidden = icon.hidden;
        int savedColor = icon.colorMode;
        int offset = motion.depth() >= .5f ? 1 : 0;
        try {
            icon.hidden = !available;
            if (!available) icon.colorMode = 0xffffff;
            else if (hovered) icon.colorMode = 0xfff09a;
            nativeControl.setLocation(x + SPECIAL_ICON_INSET + offset, y + SPECIAL_ICON_INSET + offset,
                    width - 2 * SPECIAL_ICON_INSET, height - 2 * SPECIAL_ICON_INSET);
            nativeControl.render(queue, 1.0f);
        } finally {
            icon.hidden = savedHidden; icon.colorMode = savedColor;
            nativeControl.setLocation(x, y, width, height);
        }
    }
    @Override protected void leftPressed(int mx, int my, int buttons) {
        armed = enabled.getAsBoolean() && inside(mx, my) && buttons != 2;
        held = armed;
        if (armed) motion.pointerPressed(System.nanoTime());
    }
    @Override protected void mouseDragged(int mx, int my) {
        hovered = inside(mx, my);
        if (!hovered) armed = false;
        held = armed && hovered;
    }
    @Override protected void mouseMoved(int mx, int my) { hovered = inside(mx, my); }
    @Override protected void mouseExited() { hovered = false; armed = false; held = false; }
    @Override protected void leftReleased(int mx, int my) {
        boolean fire = armed && inside(mx, my) && enabled.getAsBoolean();
        armed = false; held = false;
        if (fire) HighResHudApi.runAs(ActionOrigin.HUD, () -> nativeControl.leftReleased(mx, my));
    }
    @Override protected void rightPressed(int mx, int my, int buttons) {
        parent.rightPressed(mx, my, buttons);
    }
    @Override protected int getMouseCursor(int mx, int my) {
        return enabled.getAsBoolean() ? MOUSE_CURSOR_HAND : MOUSE_CURSOR_NORMAL;
    }
    @Override public void pick(PickData pick, int mx, int my) {
        nativeControl.pick(pick, mx, my);
        for (String hint : hints.get()) if (hint != null && !hint.isEmpty()) pick.addText(hint);
    }
}
