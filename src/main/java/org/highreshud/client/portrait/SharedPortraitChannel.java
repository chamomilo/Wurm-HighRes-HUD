package org.highreshud.client.portrait;

import com.wurmonline.client.resources.textures.Texture;

/** Adapter implemented by each logical portrait slot. */
public interface SharedPortraitChannel {
    PortraitRenderKey renderKey();
    void scheduleOwnRender();
    void finishOwnRender();
    Texture ownTexture();
    void shareFrom(SharedPortraitChannel leader);
}
