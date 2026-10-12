package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.options.Options;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.SkillDetailSettings;
import org.highreshud.client.i18n.HudMessages;

/** Keep native tab-menu commands and add one detail-level action. */
final class HighResSkillDetailDialog {
    private static HeadsUpDisplay owner;
    private static WWindow dialog;
    private static Body body;
    private HighResSkillDetailDialog() { }
    static void extendMenu(HeadsUpDisplay hud, TabButton tab) {
        String name = tab.tab.name;
        if (!name.equalsIgnoreCase(":Skills") && !name.equalsIgnoreCase("Skills")) return;
        // Tab menus are registered in popups; lastPopup belongs to server action menus.
        java.util.List<?> popups = (java.util.List<?>) HighResWindowAccess.get(hud, "popups");
        if (popups == null || popups.isEmpty()) return;
        final WurmPopup popup = (WurmPopup) popups.get(popups.size() - 1);
        if (!"tabMenu".equals(popup.id)) return;
        popup.addSeparator();
        popup.addButton(new MenuItem(popup, hud));
        popup.recalculateWidth(); popup.layout();
    }
    static final class MenuItem extends WurmPopup.WPopupAbstractButton implements ButtonListener {
        final WurmPopup popup;
        final HeadsUpDisplay hudOwner;
        final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
        final UiButtonMotion motion = new UiButtonMotion();
        final UiCompactLayout captionLayout;
        boolean armed;
        MenuItem(WurmPopup popup, HeadsUpDisplay hud) {
            popup.super(HudMessages.tr("Change detalization level"), null);
            this.popup = popup; hudOwner = hud; setButtonListener(this);
            setHoverString(HudMessages.tr("Choose the minimum skill change shown in the Skills log"));
            String label = getLabel();
            captionLayout = UiCompactLayout.group(new String[]{label}, new int[]{120}, HighResWindowTypography.COMPACT_SCALE, false)[0];
            sizeFlags = 0; setSize(captionLayout.width, captionLayout.height); sizeFlags = FIXED_HEIGHT;
        }
        @Override protected void leftPressed(int x, int y, int count) {
            armed = isEnabled() && contains(x, y); isCloseHovered = armed;
            HighResWindowAccess.set(this, "isClosing", armed); motion.pointerPressed(System.nanoTime());
        }
        @Override protected void mouseDragged(int x, int y) { if (!contains(x, y)) armed = isCloseHovered = false; }
        @Override protected void mouseExited() { super.mouseExited(); armed = false; }
        public void buttonPressed(WButton ignored) { }
        public void buttonClicked(WButton ignored) {
            if (armed && isCloseHovered) { armed = false; hudOwner.removePopup(popup); open(hudOwner); }
        }
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            motion.update(isEnabled(), hovered, armed && isCloseHovered, System.nanoTime());
            UiPainter.button(canvas.begin(queue), motion.brightness(), motion.depth(), motion.hover(), HighResChannelStrip.CAPTION_SCALE, 1.0f, x, y, width, height);
            TextFont font = HighResWindowTypography.caption(hovered && isEnabled());
            UiColor color = UiPainter.buttonCaptionColor(isEnabled(), motion.hover());
            int pressed = motion.depth() >= .5f ? 1 : 0;
            font.moveTo(x + (width - captionLayout.width) / 2 + captionLayout.textX(0, hovered) + pressed, y + captionLayout.baseline(0) + pressed);
            font.paint(queue, getLabel(), color.red, color.green, color.blue, 1.0f);
        }
    }
    static void restoreNativeMessages() {
        // Native settings otherwise aggregate/suppress ticks before the display filter sees them.
        if (SkillDetailSettings.configured()) Options.skillgainMinimum.set(5);
    }
    static void open(HeadsUpDisplay hud) {
        if (owner != hud) {
            owner = hud;
            dialog = new WWindow("High-res skill detail level", true);
            dialog.setTitle("Skill detail level"); dialog.resizable = false;
            body = new Body(); dialog.setComponent(body);
            dialog.setLocation(Math.max(0, (hud.getWidth() - 390) / 2), Math.max(0, (hud.getHeight() - 142) / 2), 390, 142);
            HighResWindowIntegration.attachDialog(dialog, hud);
        }
        body.refresh(); body.choice.selectIndex(SkillDetailSettings.selectedIndex());
        hud.showComponent(dialog);
    }
    private static final class Body extends FlexComponent {
        final ChamomiloUiV1DropDown choice;
        final ChamomiloUiV1CompactButton apply, cancel;
        private long revision = -1;
        Body() {
            super("highres.skills.detail.body", 0, 0, 384, 110);
            choice = new ChamomiloUiV1DropDown("highres.skills.detail.choice", 364, SkillDetailSettings.LEVELS.clone(), ignored -> { });
            choice.resize(364, 24); choice.setCaptionTypography(HighResWindowTypography.CAPTION_PIXELS, UiDensity.LOW, 17, UiScale.BASE);
            apply = new ChamomiloUiV1CompactButton(HudMessages.tr("Apply"), 120, HighResChannelStrip.CAPTION_SCALE, () -> {
                SkillDetailSettings.save(choice.selectedIndex()); restoreNativeMessages(); owner.hideComponent(dialog);
            });
            cancel = new ChamomiloUiV1CompactButton(HudMessages.tr("Cancel"), 120, HighResChannelStrip.CAPTION_SCALE, () -> owner.hideComponent(dialog));
            choice.parent = this; apply.parent = this; cancel.parent = this; refresh();
        }
        void refresh() {
            if (revision == HudMessages.revision()) return;
            String[] labels = SkillDetailSettings.LEVELS.clone(); labels[0] = HudMessages.tr("All"); choice.setOptions(labels);
            String[] captions = {HudMessages.tr("Apply"), HudMessages.tr("Cancel")};
            UiCompactLayout[] group = UiCompactLayout.group(captions, new int[]{120, 120}, HighResChannelStrip.CAPTION_SCALE, true);
            apply.applyLayout(captions[0], group[0]); cancel.applyLayout(captions[1], group[1]);
            choice.setHoverString(HudMessages.tr("Choose the minimum skill change shown in the Skills log"));
            apply.setHoverString(HudMessages.tr("Save the selected skill detail level"));
            cancel.setHoverString(HudMessages.tr("Close without saving the skill detail level"));
            revision = HudMessages.revision(); componentResized();
        }
        @Override void componentResized() {
            if (choice == null) return;
            choice.setLocation(x + 10, y + 31, Math.max(64, width - 20), 24);
            cancel.setLocation(x + width - 130, y + height - 31, 120, 21);
            apply.setLocation(cancel.x - 128, cancel.y, 120, 21);
        }
        @Override public FlexComponent getComponentAt(int px, int py) {
            if (!contains(px, py)) return null;
            if (choice.contains(px, py)) return choice;
            if (apply.contains(px, py)) return apply;
            if (cancel.contains(px, py)) return cancel;
            return this;
        }
        @Override public void gameTick() { choice.gameTick(); }
        @Override public void pick(com.wurmonline.client.renderer.PickData pick, int px, int py) {
            if (contains(px, py)) pick.addText(HudMessages.tr("Choose the minimum skill change shown in the Skills log"));
        }
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            refresh(); componentResized();
            TextFont font = HighResWindowTypography.label();
            font.moveTo(x + 10, y + 21);
            font.paint(queue, HudMessages.tr("Minimum skill tick"), UiColor.TEXT.red, UiColor.TEXT.green, UiColor.TEXT.blue, 1.0f);
            choice.render(queue, 1.0f); apply.render(queue, 1.0f); cancel.render(queue, 1.0f);
        }
    }
}
