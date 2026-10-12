package com.wurmonline.client.renderer.gui;

/** Existing chat managers and the original HUD input field stay attached to this window. */
public class HighResChatWindow extends HighResNativeWindow {
    private final HighResChannelStrip channels;
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
    }
    @Override void install() {
        if (window.minimized) window.toggleSize();
        super.install(); tick(); ready=true;
    }
    @Override boolean hasTitle() { return false; }
    @Override int channelHeight() { return channels == null ? HighResChannelStrip.HEIGHT : channels.height; }
    @Override FlexComponent tabs() { return channels; }
    @Override void tick() {
        super.tick();
        if (channels == null) return;
        channels.synchronize(Math.max(64, header.width));
        AbstractTab next = ((WurmTabbedWindow) window).getSelectedTab();
        if (next != selectedTab) {
            selectedTab = next;
            HighResWindowIntegration.sortContent(window.getComponent());
        }
        if (header.height != (window.minimized ? 0 : channelHeight())) {
            int growth=(window.minimized?0:channelHeight())-header.height;
            boolean newChannels=ready&&channelCount!=channels.channelCount()&&!window.minimized;
            header.updateHeight(); window.layout();
            if(newChannels) window.setLocation(window.x,Math.max(0,window.y-growth),window.width,Math.max(140,window.height+growth));
        }
        channelCount=channels.channelCount();
        int minimum = HighResWindowIntegration.minimumHeight(window, window.height);
        if (!window.minimized && window.height < minimum) window.setSize(window.width, minimum);
    }
    HighResChannelStrip channels() { return channels; }
}
