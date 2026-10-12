package com.wurmonline.client.renderer.gui;

/** Event, combat and skill logs share the channel adapter without introducing a second message store. */
public final class HighResEventsWindow extends HighResChatWindow {
    HighResEventsWindow(WurmTabbedWindow nativeWindow, HeadsUpDisplay hud) {
        super("highres.events", "Events", nativeWindow, hud);
    }
}
