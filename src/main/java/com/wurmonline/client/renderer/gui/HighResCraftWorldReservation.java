package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.inventory.HighResCraftMetadata;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.game.World;
import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.renderer.ObjectData;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import org.highreshud.client.craft.CraftCatalog;

/** Retains Wurm's actual ground target alongside inventory-style display data. */
final class HighResCraftWorldReservation {
    final CreationFrame.GroundCreationItem ground;
    final InventoryMetaItem item;
    private boolean tracked;
    boolean available=true;
    HighResCraftWorldReservation(CreationFrame frame, String name, long id,
                                float quality, float damage, float weight, short icon,
                                CreationFrame.ItemType type) {
        ground=frame.new GroundCreationItem(name,id,quality,damage,weight,type);
        CraftCatalog.Template template=CraftCatalog.find(name,icon);
        String materialName=name.replaceFirst("(?i)^(unfinished |incomplete |planned |rare |supreme |fantastic )+", "");
        byte material=(byte)HighResCraftRecipe.Requirement.namedMaterial(materialName,template);
        // The native inventory predicate recognizes 'unfinished'; structures may use other prefixes.
        String display=(ground.isUnFinished()?"unfinished ":"")+CraftCatalog.clean(name);
        item=HighResCraftMetadata.world(id,icon,display,material,quality,damage,weight,
                HighResBuildWindow.rarityFromName(name.replaceFirst("(?i)^(unfinished |incomplete |planned )+", "")));
    }
    void refresh(World world) {
        if(world==null||world.getServerConnection()==null)return;
        ServerConnectionListenerClass listener=world.getServerConnection().getServerConnectionListener();
        if(listener==null)return;
        GroundItemCellRenderable rendered=listener.findGroundItem(item.getId());
        if(rendered==null){available=!tracked;return;}
        tracked=true;ObjectData data=(ObjectData)HighResWindowAccess.get(rendered,"item");
        available=data.getLayer()==world.getPlayerLayer();
        if(item.getMaterialId()!=data.getMaterialId())item.setMaterialId(data.getMaterialId());
        if(item.getRarity()!=data.getRarity())item.setRarity(data.getRarity());
    }
    void update(float quality,float damage,float weight) {
        ground.setItemProperties(quality,damage,weight);
        HighResCraftMetadata.update(item,quality,damage,weight);
    }
}
