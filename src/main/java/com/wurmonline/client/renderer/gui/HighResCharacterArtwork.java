package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.client.resources.textures.Texture;
import org.chamomilo.wurm.ui.v1.UiColor;

/** Destination-sized raster artwork, retaining the native equipment subjects. */
final class HighResCharacterArtwork {
    static final int SLOTS=0,ANATOMY=1,WEIGHT=2,BODY=3,HAND=4;
    private static final String[] KEYS={"slots","anatomy","weight","body","hand"};
    private static final int[] CELLS={18,17,12,5,2,3,4,8,7,16,15,13,-1,-1,19,6,9,9,21,20,14,10,0,1,-1,11};
    private HighResCharacterArtwork() { }
    static void slot(Queue queue,byte id,boolean available,int x,int y) {
        if(id<0 || id>=CELLS.length || CELLS[id]<0)return;
        int cell=CELLS[id],column=cell%16,row=cell/16;
        // Empty-slot art is a subdued backdrop; equipped items retain full color.
        float brightness=available?.62f:.38f;
        draw(queue,SLOTS,new UiColor(brightness,brightness,brightness),x,y,44,44,
                column/16f,row/2f,(column+1)/16f,(row+1)/2f);
    }
    static void icon(Queue queue,int asset,UiColor tint,int x,int y,int width,int height) {
        draw(queue,asset,tint,x,y,width,height,0f,0f,1f,1f);
    }
    static void draw(Queue queue,int asset,UiColor tint,int x,int y,int width,int height,float u0,float v0,float u1,float v1) {
        Texture texture=ResourceTextureLoader.getNowrapLinearTexture("img.highrescharacter."+KEYS[asset]);
        if(texture!=null)Renderer.texturedQuadAlphaBlend(queue,texture,tint.red,tint.green,tint.blue,1f,
                x,y,width,height,u0,v0,u1-u0,v1-v0);
    }
}
