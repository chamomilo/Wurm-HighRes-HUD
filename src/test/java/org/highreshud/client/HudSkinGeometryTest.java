package org.highreshud.client;

import java.util.ArrayList;
import java.util.List;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.ui.HudSkin;
import org.junit.Test;
import static org.junit.Assert.*;

public class HudSkinGeometryTest {
    @Test
    public void valuesFillTheirFrameCellsWithoutLegacyInsets() {
        rect(HudSkin.selectHealth(0,0),107,27,328,16);
        rect(HudSkin.selectProgress(0,0),107,98,328,7);
        rect(HudSkin.healthGauge(0,0,0),107,5,211,19);
        rect(HudSkin.healthGauge(0,0,1),107,27,104,16);
        rect(HudSkin.healthGauge(0,0,2),214,27,104,16);
        rect(HudSkin.healthGauge(0,0,3),107,46,51,16);
        rect(HudSkin.healthGauge(0,0,6),268,46,50,16);
        rect(HudSkin.healthGauge(0,0,7),107,65,211,16);
        rect(HudSkin.healthGauge(0,0,8),107,84,211,19);
        UiRect local=HudSkin.selectHealth(0,0),moved=HudSkin.selectHealth(31,43);
        rect(moved,local.x+31,local.y+43,local.width,local.height);
    }

    @Test
    public void doubledPortraitContourKeepsBothOuterPanelSizes() {
        for(boolean health:new boolean[]{true,false}) {
            RecordingCanvas canvas=new RecordingCanvas(health?323:438,108);
            if(health)HudSkin.healthFront(canvas,0,0);else HudSkin.selectFront(canvas,0,0);
            if(health)assertTrue(canvas.fills.contains("318,5,5,98"));
            assertTrue(canvas.fills.contains("0,0,107,6"));
            assertTrue(canvas.fills.contains("101,6,6,96"));
            assertFalse("No second 3 px left grid rail",canvas.rects.contains("104,3,3,21"));
        }
    }
    private static void rect(UiRect actual,int x,int y,int w,int h) {
        assertArrayEquals(new int[]{x,y,w,h},new int[]{actual.x,actual.y,actual.width,actual.height});
    }
    private static final class RecordingCanvas implements UiCanvas {
        final int width,height;final List<String> fills=new ArrayList<>(),rects=new ArrayList<>();
        RecordingCanvas(int w,int h){width=w;height=h;}
        void record(int x,int y,int w,int h) {
            assertTrue("Decoration stays inside the existing panel",x>=0&&y>=0&&x+w<=width&&y+h<=height);
            rects.add(x+","+y+","+w+","+h);
        }
        public void fill(UiColor c,float a,int x,int y,int w,int h) {
            record(x,y,w,h);fills.add(x+","+y+","+w+","+h);
        }
        public boolean texture(UiAsset asset,float tint,float a,int x,int y,int w,int h,float u0,float v0,float u1,float v1) {
            record(x,y,w,h);assertEquals(1f,a,0f);return true;
        }
    }
}
