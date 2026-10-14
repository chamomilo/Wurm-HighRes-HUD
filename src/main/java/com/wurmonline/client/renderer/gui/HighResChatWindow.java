package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.HudMessages;

/** Existing chat managers and the original HUD input field stay attached to this window. */
public class HighResChatWindow extends HighResNativeWindow {
    private final HighResChannelStrip channels;
    final HighResInventoryOpacity opacity;
    final ChamomiloUiV1CompactButton minimize;
    private static final int CONTROL_WIDTH = HighResInventoryOpacity.TAB_WIDTH + HighResChannelStrip.HEIGHT + 12;
    private int expandedX, expandedY, expandedWidth, expandedHeight;
    private boolean restoreBounds;
    private UiIcon minimizeIcon;
    private AbstractTab selectedTab;
    private boolean ready;
    private int channelCount;
    HighResChatWindow(WurmTabbedWindow nativeWindow, HeadsUpDisplay hud) {
        this("highres.chat", "Chat", nativeWindow, hud);
    }
    HighResChatWindow(String id, String title, WurmTabbedWindow nativeWindow, HeadsUpDisplay hud) {
        super(id, title, nativeWindow, hud);
        channels = new HighResChannelStrip(id + ".channels", nativeWindow);
        channels.parent = header;
        opacity = new HighResInventoryOpacity(this);
        opacity.parent = header;
        minimize = new ChamomiloUiV1CompactButton(UiIcon.MINIMIZE, HighResChannelStrip.HEIGHT,
                HighResWindowTypography.COMPACT_SCALE, () -> collapse());
        minimize.parent = header;
    }
    @Override void install() {
        if (window.minimized) window.toggleSize();
        super.install(); tick(); ready=true;
    }
    @Override boolean hasTitle() { return false; }
    @Override boolean keepsTabsWhenCollapsed() { return true; }
    @Override int tabControlsWidth() { return CONTROL_WIDTH; }
    @Override int channelHeight() { return channels == null ? HighResChannelStrip.HEIGHT : channels.height; }
    @Override FlexComponent tabs() { return channels; }
    @Override void tick() {
        super.tick();
        if (channels == null) return;
        channels.synchronize(Math.max(64, header.width-CONTROL_WIDTH));
        AbstractTab next = ((WurmTabbedWindow) window).getSelectedTab();
        if (next != selectedTab) {
            selectedTab = next;
            HighResWindowIntegration.sortContent(window.getComponent());
        }
        if (header.height != channelHeight()) {
            int growth=channelHeight()-header.height;
            boolean newChannels=ready&&channelCount!=channels.channelCount()&&!window.minimized;
            header.updateHeight(); window.layout();
            if(newChannels) window.setLocation(window.x,Math.max(0,window.y-growth),window.width,Math.max(140,window.height+growth));
        }
        channelCount=channels.channelCount();
        if(window.minimized) dockCollapsed();
        refreshMinimize();
        int minimum = HighResWindowIntegration.minimumHeight(window, window.height);
        if (!window.minimized && window.height < minimum) window.setSize(window.width, minimum);
    }
    HighResChannelStrip channels() { return channels; }
    @Override boolean usesOpacity() { return true; }
    private void layoutControls() {
        int top=header.y+channels.height-HighResChannelStrip.HEIGHT;
        minimize.setLocation(header.x+header.width-6-HighResChannelStrip.HEIGHT,top,HighResChannelStrip.HEIGHT,HighResChannelStrip.HEIGHT);
        opacity.setLocation(minimize.x-3-HighResInventoryOpacity.TAB_WIDTH,top+2,HighResInventoryOpacity.TAB_WIDTH,Math.max(10,HighResChannelStrip.HEIGHT-4));
    }
    private void refreshMinimize() {
        UiIcon next=window.minimized?UiIcon.CHEVRON_DOWN:UiIcon.MINIMIZE;
        if(minimizeIcon!=next){minimizeIcon=next;minimize.setIcon(next);}
        UiLabels labels=UiLabels.forLanguage(HudMessages.language());
        minimize.setHoverString(window.minimized?labels.restore:labels.minimize);
        layoutControls();
    }
    @Override void collapse() {
        if(!window.minimized){expandedX=window.x;expandedY=window.y;expandedWidth=window.width;expandedHeight=window.height;restoreBounds=true;}
        window.toggleSize();
        if(!window.minimized&&restoreBounds){window.setLocation(expandedX,expandedY,expandedWidth,expandedHeight);restoreBounds=false;}
        collapsedChanged();
    }
    @Override void collapsedChanged() {
        FlexComponent bottom=(FlexComponent)HighResWindowAccess.get(window,"southBar");
        bottom.sizeFlags=0;bottom.setSize(bottom.width,window.minimized?0:3);bottom.sizeFlags=FlexComponent.FIXED_HEIGHT;
        header.updateHeight();
        window.layout();
        if(window.minimized)dockCollapsed();
        refreshMinimize();
    }
    private void dockCollapsed() {
        int height=channelHeight(),top=Math.max(0,owner.getHeight()-height);
        if(window.y!=top||window.height!=height)window.setLocation(window.x,top,window.width,height);
    }
    @Override void paintHeaderControls(Queue queue) { layoutControls();minimize.render(queue,1f); }
    @Override void paintOpacity(Queue queue) { layoutControls();opacity.render(queue,1f); }
    @Override FlexComponent opacityAt(int x,int y) { layoutControls();return opacity.contains(x,y)?opacity:null; }
    @Override FlexComponent headerControlAt(int x,int y) { layoutControls();return minimize.contains(x,y)?minimize:opacity.contains(x,y)?opacity:null; }
}
