package com.wurmonline.client.renderer.gui;

import java.util.*;
import com.wurmonline.client.game.inventory.*;
import org.highreshud.client.craft.CraftCatalog;

/** Exercises real ground callbacks, native dispatch and store gestures. */
final class HighResCraftWorldProbe {
    static long popupTarget;static int popupCount;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static InventoryMetaItem item(long id,String name,HighResCraftRecipe.Requirement r,float quality){
        return HighResCraftMetadata.world(id,(short)r.icon,name,(byte)r.materialId,quality,0,Math.max(1,r.tool?r.totalKg():r.unitKg), (byte)0);
    }
    private static void click(HighResBuildWindow.Store store,InventoryMetaItem item,int clicks){
        store.resizeRows();int line=store.lines.indexOf(item);check(line>=0,"Reserved item has a native row");
        int x=store.x+44,y=store.y+line*store.rowHeight+8;store.leftPressed(x,y,clicks);store.leftReleased(x,y);
    }
    private static void action(HighResBuildWindow build,int chance){
        build.tree(false).clear();build.tree(false).addTreeListItem(new CreationItemTreeLisItem(build.nativeWindow,build.recipe.name,(short)chance,(short)148,(short)build.recipe.result.icon,false),null);build.findAction();
    }
    @SuppressWarnings("unchecked") static void verify(HighResBuildWindow build,CreationListWindow window,HeadsUpDisplay hud,Map<Long,InventoryMetaItem> inventory,InventoryMetaItem root)throws Exception{
        Object oldBuild=HighResWindowAccess.get(hud,"creationWindow"),oldBrowser=HighResWindowAccess.get(hud,"creationListWindow"),oldHud;
        // Static fields use their declaring class directly rather than the instance access helper.
        java.lang.reflect.Field skinsField=HighResWindowIntegration.class.getDeclaredField("WINDOWS");skinsField.setAccessible(true);
        Map<WurmComponent,HighResNativeWindow> skins=(Map<WurmComponent,HighResNativeWindow>)skinsField.get(null);
        HighResNativeWindow oldBuildSkin=skins.get(build.nativeWindow),oldBrowserSkin=skins.get(window);
        List<InventoryMetaItem> previous=new ArrayList<>(root.getChildren());Map<Long,InventoryMetaItem> previousInventory=new HashMap<>(inventory);
        Object oldMemory=HighResWindowAccess.get(build,"memoryFile");HighResWindowAccess.set(build,"memoryFile",java.nio.file.Paths.get("build","craft-world-tool-memory.properties").toAbsolutePath());
        java.lang.reflect.Field hudField=HighResWindowIntegration.class.getDeclaredField("currentHud");hudField.setAccessible(true);oldHud=hudField.get(null);
        HighResRecipeWindow browser=new HighResRecipeWindow(window,hud);browser.install();
        HighResWindowAccess.set(hud,"creationWindow",build.nativeWindow);HighResWindowAccess.set(hud,"creationListWindow",window);hudField.set(null,hud);skins.put(build.nativeWindow,build);skins.put(window,browser);
        try{
            build.clearStores();build.waiting=false;build.refreshAfterAction=false;build.recipe=null;
            HighResWindowAccess.set(build.nativeWindow,"actionInUse",0);root.getChildren().clear();inventory.clear();inventory.put(root.getId(),root);
            build.nativeWindow.addCreationGroundItem("large anvil, iron",900,65,0,100,(short)791,CreationFrame.ItemType.ITEM);
            check(build.worldStock.containsKey(900L)&&build.panel.toolRows.rows.stream().anyMatch(i->i.getId()==900),"World anvil is stored before any recipe is chosen");
            HighResCraftRecipe metal=browser.browser.catalog.stream().filter(r->r.result!=null&&r.result.metal()&&r.entry!=null&&r.name.endsWith(", iron")&&r.requirements.stream().anyMatch(q->q.start&&q.tool&&CraftCatalog.clean(q.name).equals("large anvil"))).findFirst().get();
            build.select(metal);build.desiredMaterial=11;build.chooseVariant();build.panel.materialOptions();
            check(build.worldStock.containsKey(900L),"Choosing a recipe retains an explicitly reserved world tool");
            HighResCraftRecipe.Requirement tool=build.recipe.requirements.stream().filter(r->r.start&&r.tool).findFirst().get(),material=build.recipe.requirements.stream().filter(r->r.start&&!r.tool).findFirst().get();
            InventoryMetaItem first=item(501,"large anvil",tool,90),second=item(502,"large anvil",tool,80),lump=item(503,material.name,material,70);lump.setTemperatureState((byte)5);
            for(InventoryMetaItem item:Arrays.asList(first,second,lump)){root.getChildren().add(item);inventory.put(item.getId(),item);}
            build.autoPick();check(build.panel.toolRows.rows.size()==3&&build.nextItems.contains(900L)&&!build.nextItems.contains(501L),"Auto-pick retains all tool alternatives and selects the first reserved tool");
            InventoryMetaItem last=null;for(int n=0;n<12;n++){last=item(530+n,"large anvil",tool,60-n);root.getChildren().add(last);inventory.put(last.getId(),last);build.stock.add(last);}build.tick();build.panel.tools.scrollTo(0,10000);build.panel.tools.layout();
            HighResBuildWindow.Store toolRows=build.panel.toolRows;int lastLine=toolRows.lines.indexOf(last),scrollX=toolRows.x+44,scrollY=toolRows.y+lastLine*toolRows.rowHeight+8;
            check(scrollY>=build.panel.tools.y&&scrollY<build.panel.tools.y+build.panel.tools.height&&hud.getComponentAt(scrollX,scrollY)==toolRows,"Scrolled tool alternatives retain native HUD hit targets");
            hud.mousePressed(scrollX,scrollY,0,1);hud.mouseReleased(scrollX,scrollY,0);check(build.nextItems.contains(last.getId()),"Scrolling and native left-click select another exact tool ID");build.panel.tools.scrollTo(0,0);build.panel.tools.layout();
            click(build.panel.toolRows,second,1);check(build.nextItems.contains(502L)&&!build.nextItems.contains(900L),"Clicking another tool moves green selection by exact ID");
            int before=popupCount,line=build.panel.toolRows.lines.indexOf(second);build.panel.toolRows.rightPressed(build.panel.toolRows.x+44,build.panel.toolRows.y+line*build.panel.toolRows.rowHeight+8,1);
            check(popupCount==before+1&&popupTarget==502&&build.stock.contains(second),"Right-click requests the native item menu without removing its reservation");
            action(build,78);check(build.ready(),"The selected usable tool enables the server-approved action");
            HighResCraftMetadata.update(second,80,100,second.getWeight());int sent=HighResCraftProbe.sentActions;build.build();check(!build.ready()&&HighResCraftProbe.sentActions==sent,"Build rechecks damage immediately before sending");
            HighResCraftMetadata.update(second,80,0,second.getWeight());build.tick();action(build,78);build.build();
            check(HighResCraftProbe.sentActions==sent+1&&(HighResCraftProbe.sentSource==502||HighResCraftProbe.sentTarget==502)&&HighResCraftProbe.sentSource!=501&&HighResCraftProbe.sentTarget!=501,"Native dispatch uses the finally chosen tool, not another stored alternative");
            build.waiting=false;HighResWindowAccess.set(build.nativeWindow,"actionInUse",0);click(build.panel.toolRows,second,2);
            check(!build.stock.contains(second)&&!build.preferred.containsValue(502L),"Double-click removes the selected tool and its preference");
            build.selectNext(build.worldStock.get(900L).item);action(build,0);check(!build.ready(),"A zero server chance blocks Build even with enough mass and tools");
            action(build,78);sent=HighResCraftProbe.sentActions;build.build();check(HighResCraftProbe.sentActions==sent+1&&(HighResCraftProbe.sentSource==900||HighResCraftProbe.sentTarget==900),"A world anvil is dispatched through the native ground slot");
            build.waiting=false;HighResWindowAccess.set(build.nativeWindow,"actionInUse",0);
            build.nativeWindow.updateCreationGroundItem(900,22,5,99);check(build.worldStock.get(900L).item.getQuality()==22&&build.worldStock.get(900L).item.getDamage()==5,"World callbacks refresh stored QL, damage and mass");
            build.nativeWindow.removeCreationGroundItem(900);check(!build.worldStock.containsKey(900L)&&build.stock.stream().noneMatch(i->i.getId()==900),"World removal releases the tool and selected pair");
            build.nativeWindow.addCreationGroundItem("unfinished rare birchwood knarr",901,32,2,190,(short)590,CreationFrame.ItemType.ITEM);
            check(build.continuing()&&build.recipe!=null&&build.recipe.name.equals("knarr")&&build.panel.materialRows.rows.stream().anyMatch(i->i.getId()==901),"A long unfinished world name selects knarr and appears in Material store");
            HighResCraftRecipe.Requirement plank=new HighResCraftRecipe.Requirement("plank",626,CraftCatalog.get(22),1,2,false,false);
            InventoryMetaItem board=item(510,"plank",plank,75);root.getChildren().add(board);inventory.put(510L,board);
            build.tree(true).addTreeListItem(new CreationItemTreeLisItem(build.nativeWindow,"plank",(short)1,(short)193,(short)626,false),null);build.readRemaining();build.autoPick();
            check(build.ready()&&build.nextItems.contains(901L)&&build.nextItems.contains(510L),"One valid component and the selected world target enable Continue");
            build.worldStock.get(901L).available=false;check(!build.ready(),"An unavailable world target blocks Continue");build.worldStock.get(901L).available=true;
            sent=HighResCraftProbe.sentActions;build.build();check(HighResCraftProbe.sentActions==sent+1&&HighResCraftProbe.sentSource==510&&HighResCraftProbe.sentTarget==901,"Native Continue uses the selected plank and world ship ID");
            build.waiting=false;HighResWindowAccess.set(build.nativeWindow,"actionInUse",0);
            build.nativeWindow.replaceCreationGroundItem(901,"knarr, birchwood",901,35,0,200,(short)590,CreationFrame.ItemType.ITEM);check(!build.continuing()&&build.recipe==null&&build.stock.isEmpty()&&build.panel.requirements.rows.isEmpty()&&build.preview.texture()==null,"Completion clears the recipe, requirements, stores and preview");
            build.nativeWindow.addCreationGroundItem("unfinished knarr",902,20,0,80,(short)590,CreationFrame.ItemType.ITEM);InventoryMetaItem target=build.worldStock.get(902L).item;click(build.panel.materialRows,target,1);click(build.panel.materialRows,target,2);build.tick();check(!build.continuing()&&!build.worldStock.containsKey(902L),"Double-click removes an unfinished world target without rediscovering it");
            InventoryMetaItem unfinished=HighResCraftMetadata.world(520,(short)60,"unfinished wagon",(byte)14,30,0,31,(byte)0);root.getChildren().add(unfinished);inventory.put(520L,unfinished);build.add(unfinished,false,0);check(build.unfinished==unfinished&&build.stock.contains(unfinished)&&build.recipe.name.equals("wagon"),"Inventory unfinished targets use the same visible Material store");
            build.nativeWindow.removeInventoryItem(unfinished);build.tick();check(build.recipe==null&&build.stock.isEmpty()&&build.panel.requirements.rows.isEmpty(),"Native inventory completion/removal clears the full Build state");
            HighResCraftRecipe shaft=browser.browser.catalog.stream().filter(r->r.name.equals("shaft")).findFirst().get();HighResCraftRecipe.Requirement log=shaft.requirements.stream().filter(r->CraftCatalog.clean(r.name).equals("log")).findFirst().get();
            check(log.unitKg==2f,"Prepared server recipe includes the shaft's 100% waste: 2 kg of log");
            build.select(shaft);InventoryMetaItem shortLog=HighResCraftMetadata.world(521,(short)log.icon,"log",(byte)14,50,0,1.9f,(byte)0);check(build.invalid(log,shortLog).contains("Insufficient mass"),"A log below 2 kg is invalid even though the finished shaft weighs 1 kg");
            HighResCraftRecipe palisade=browser.browser.catalog.stream().filter(r->r.name.equalsIgnoreCase("Palisade")).findFirst().get();
            build.select(palisade);HighResCraftRecipe.Requirement hammer=palisade.requirements.get(0),border=palisade.requirements.get(1);check(hammer.tool&&border.location(),"Server-only palisade preserves hammer and Tile Border roles");
            InventoryMetaItem mallet=item(522,"hammer",hammer,80);root.getChildren().add(mallet);inventory.put(522L,mallet);build.autoPick();check(build.panel.toolRows.rows.contains(mallet)&&!build.panel.materialRows.rows.contains(mallet),"Palisade Auto-pick puts the hammer in Tool store");
            build.nativeWindow.addCreationGroundItem("Tile Border",903,-1,-1,-1,(short)0,CreationFrame.ItemType.NONE);check(build.nextTarget!=null&&build.nextTarget.getId()==903&&build.invalid(border,build.nextTarget).isEmpty(),"Add to crafting window supplies a usable world border without a mass or QL requirement");
            build.tree(false).addTreeListItem(new CreationItemTreeLisItem(build.nativeWindow,"Palisade",(short)80,(short)555,(short)0,false),null);build.findAction();check(build.ready(),"Hammer and Tile Border enable the server-approved palisade action");
            check(HighResBuildWindow.mass(-1).equals("—")&&HighResBuildWindow.ql(-1).equals("—"),"World location placeholders do not display negative mass or quality");
            java.awt.image.BufferedImage picture=new java.awt.image.BufferedImage(980,810,java.awt.image.BufferedImage.TYPE_INT_ARGB);HudButtonProbe.previewGraphics(picture);
            try{com.wurmonline.client.renderer.backend.Queue queue=InventoryOpacityPreview.begin();build.render(queue);InventoryOpacityPreview.finish();javax.imageio.ImageIO.write(picture,"png",new java.io.File("build/hud-palisade-preview.png"));}finally{HudButtonProbe.finishPreview();}
            build.clearStores();check(build.stock.isEmpty()&&build.worldStock.isEmpty()&&!build.continuing(),"Clear stores releases all local reservations without changing inventory");
        }finally{
            build.clearStores();hud.getComponents().remove(build.nativeWindow);hud.getComponents().remove(window);root.getChildren().clear();root.getChildren().addAll(previous);inventory.clear();inventory.putAll(previousInventory);
            if(oldBuildSkin==null)skins.remove(build.nativeWindow);else skins.put(build.nativeWindow,oldBuildSkin);if(oldBrowserSkin==null)skins.remove(window);else skins.put(window,oldBrowserSkin);
            HighResWindowAccess.set(hud,"creationWindow",oldBuild);HighResWindowAccess.set(hud,"creationListWindow",oldBrowser);hudField.set(null,oldHud);
            HighResWindowAccess.set(build,"memoryFile",oldMemory);
        }
        System.out.println("CRAFT_WORLD_OK: native world add/update/remove/complete, long ship name, visible unfinished store, tool alternatives and final dispatch, damage/zero-chance/unavailable-target gates, double-click removal and native context menus");
    }
}
