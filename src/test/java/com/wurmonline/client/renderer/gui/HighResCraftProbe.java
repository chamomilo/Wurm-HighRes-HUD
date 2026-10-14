package com.wurmonline.client.renderer.gui;
import java.util.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.wurmonline.client.game.inventory.*;
import com.wurmonline.client.game.SkillLogic;
import com.wurmonline.client.game.SkillLogicSet;
import org.highreshud.client.craft.*;
import org.highreshud.client.i18n.HudMessages;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.backend.Queue;
import org.highreshud.client.portrait.*;

/** Native controls and server models with only the transport/GPU boundaries substituted. */
public final class HighResCraftProbe {
    public static int requests;
    public static int sentActions;public static long sentSource,sentTarget;
    private static BufferedImage previewPattern;
    private static float[] previewQuad;
    public static void capturePreview(float x,float y,float w,float h,float u,float v,float uw,float vh){
        if(previewPattern==null)return;
        previewQuad=new float[]{x,y,w,h};
        HudButtonProbe.graphics.drawImage(previewPattern,Math.round(x),Math.round(y),Math.round(x+w),Math.round(y+h),
                Math.round(u*previewPattern.getWidth()),Math.round(v*previewPattern.getHeight()),
                Math.round((u+uw)*previewPattern.getWidth()),Math.round((v+vh)*previewPattern.getHeight()),null);
    }
    private static void verifyPreviewTexture() throws Exception {
        previewPattern=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<8;y++)for(int x=0;x<8;x++)previewPattern.setRGB(x,y,y<4?0xffff0000:0xff0000ff);
        BufferedImage image=new BufferedImage(350,290,BufferedImage.TYPE_INT_ARGB);HudButtonProbe.previewGraphics(image);
        try {
            FlexComponent box=new FlexComponent("offscreen orientation fixture",20,20,300,242){};
            HighResCraftPreview.paintTexture(null,null,box);
            int cx=Math.round(previewQuad[0]+previewQuad[2]/2),top=Math.round(previewQuad[1]+previewQuad[3]/4),bottom=Math.round(previewQuad[1]+previewQuad[3]*3/4);
            check(image.getRGB(cx,top)==0xff0000ff&&image.getRGB(cx,bottom)==0xffff0000,"Offscreen bottom-origin texture is displayed upright");
            check(previewQuad[0]>=box.x+4&&previewQuad[1]>=box.y+4&&previewQuad[0]+previewQuad[2]<=box.x+box.width-4
                    &&previewQuad[1]+previewQuad[3]<=box.y+box.height-4,"Preview quad stays within its frame");
        } finally {previewPattern=null;HudButtonProbe.finishPreview();}
        System.out.println("CRAFT_PREVIEW_TEXTURE_OK: asymmetric offscreen texture displayed upright and contained by its frame");
    }
    private static sun.misc.Unsafe unsafe()throws Exception{java.lang.reflect.Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);return(sun.misc.Unsafe)f.get(null);}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static InventoryMetaItem item(long id,String name,int icon,float kg,float quality,int mat)throws Exception {
        InventoryMetaItem i=(InventoryMetaItem)unsafe().allocateInstance(InventoryMetaItem.class);
        HighResWindowAccess.set(i,"id",id);HighResWindowAccess.set(i,"parentId",1L);HighResWindowAccess.set(i,"baseName",name);HighResWindowAccess.set(i,"displayName",name);
        HighResWindowAccess.set(i,"children",new ArrayList<InventoryMetaItem>());HighResWindowAccess.set(i,"type",(short)icon);HighResWindowAccess.set(i,"weight",kg);HighResWindowAccess.set(i,"quality",quality);HighResWindowAccess.set(i,"materialId",(byte)mat);return i;
    }
    private static CreationListItem component(String name,int count,int icon){return new CreationListItem(name,"Fine carpentry",count,(short)icon,(short)1,false);}
    public static void verify(HeadsUpDisplay hud,InventoryWindow inventory,File output)throws Exception {
        verifyPreviewTexture();
        new HighResCraftPreviewFramingTest().allCornersFitThroughoutRotationIncludingModelDepth();
        System.out.println("CRAFT_PREVIEW_FRAMING_OK: cube, wide, tall, deep, tiny and large models; all eight corners fit throughout a complete rotation");
        verifyPreviewPalette();
        com.wurmonline.client.game.PlayerObj previousPlayer=hud.getWorld().getPlayer();
        if(previousPlayer==null)HighResWindowAccess.set(hud.getWorld(),"player",unsafe().allocateInstance(com.wurmonline.client.game.PlayerObj.class));
        SkillLogicSet previousSkills=hud.getWorld().getPlayer().getSkillSet();
        java.lang.reflect.Field globalsField=SkillLogicSet.class.getDeclaredField("global_names_skills");globalsField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String,SkillLogic> globalSkills=(Map<String,SkillLogic>)globalsField.get(null);Map<String,SkillLogic> previousGlobals=new HashMap<>(globalSkills);
        SkillLogicSet skills=(SkillLogicSet)unsafe().allocateInstance(SkillLogicSet.class);HighResWindowAccess.set(skills,"skills",new HashMap<Long,SkillLogic>());HighResWindowAccess.set(skills,"skillListeners",new ArrayList<>());
        skills.addSkill(new SkillLogic(hud.getWorld(),1005,-1,"Carpentry",40,100,(byte)0));skills.addSkill(new SkillLogic(hud.getWorld(),10044,1005,"Fine carpentry",60,100,(byte)0));HighResWindowAccess.set(hud.getWorld().getPlayer(),"skillSet",skills);
        final InventoryMetaItem root=item(1,"Inventory",0,0,0,0);final Map<Long,InventoryMetaItem> map=new HashMap<>();map.put(1L,root);
        InventoryMetaWindowView view=new InventoryMetaWindowView(){public long getWindowId(){return -1;}public String getWindowName(){return "Inventory";}public InventoryMetaItem getRootItem(){return root;}public InventoryMetaItem getItem(long id){return map.get(id);}public boolean contains(long id){return map.containsKey(id);}public void addItemListener(InventoryMetaListener l){}public void removeItemListener(InventoryMetaListener l){}};
        check(com.wurmonline.client.renderer.gui.text.TextFont.getStaticSizeText()!=null,"Offline static font is initialized");
        java.lang.reflect.Constructor<?> base=WWindow.class.getDeclaredConstructor(String.class);base.setAccessible(true);
        java.lang.reflect.Constructor<?> fixture=sun.reflect.ReflectionFactory.getReflectionFactory().newConstructorForSerialization(CreationWindow.class,base);fixture.setAccessible(true);
        CreationWindow nativeBuild=(CreationWindow)fixture.newInstance("craft fixture");
        nativeBuild.setInitialSize(940,740,false);
        HighResWindowAccess.set(nativeBuild,"playerInventory",view);HighResWindowAccess.set(nativeBuild,"source",new CreationFrame(64,64,"source"));HighResWindowAccess.set(nativeBuild,"target",new CreationFrame(64,64,"target"));
        HighResWindowAccess.set(nativeBuild,"createItemList",new WurmTreeList<CreationItemTreeLisItem>("create fixture",new int[]{80},new String[]{"Chance"}));
        HighResWindowAccess.set(nativeBuild,"unfinishedItemList",new WurmTreeList<CreationItemTreeLisItem>("continue fixture",new int[]{80},new String[]{"Quantity"}));
        HighResWindowAccess.set(nativeBuild,"removedUsedItems",new ArrayList<Long>());
        HighResBuildWindow build=new HighResBuildWindow(nativeBuild,hud);build.install();
        CreationListItem wagon=component("wagon",0,60);wagon.addChild(component("plank",1,626));wagon.addChild(component("small wheel axle",1,267));wagon.addChild(component("plank",20,626));wagon.addChild(component("shaft",4,627));wagon.addChild(component("small nails",10,781));
        HighResCraftRecipe recipe=new HighResCraftRecipe(wagon);check(recipe.result.id==850&&recipe.entry!=null,"Wagon result and starters resolved");build.recipe=recipe;build.material=14;
        check(HighResCraftPreview.mappingFor(recipe.result,(byte)14).equals("model.transports.medium.wagon.birchwood"),"Result preview preserves the native wood mapping suffix");
        InventoryMetaItem oak=item(11,"plank",626,2,80,38),birch=item(12,"plank",626,2,25,14),shortPlank=item(13,"plank",626,.2f,35,14),shaft=item(14,"shaft",627,1,60,14),unfinished=item(20,"unfinished wagon",60,31,30,14);
        for(InventoryMetaItem i:Arrays.asList(oak,birch,shortPlank,shaft,unfinished)){map.put(i.getId(),i);root.getChildren().add(i);}
        InventoryMetaItem combinedPlanks=item(15,"plank",626,42,99,14);map.put(15L,combinedPlanks);root.getChildren().add(combinedPlanks);
        build.stock.add(shortPlank);build.autoPick();check(build.stock.contains(combinedPlanks)&&build.count(recipe.requirements.get(0))==2,"Auto-pick covers starter plus continuation mass from one usable combined pile despite an existing underweight reservation");
        build.stock.clear();map.remove(15L);root.getChildren().remove(combinedPlanks);
        build.stock.addAll(Arrays.asList(oak,birch,shortPlank,shaft));build.unfinished=unfinished;build.unfinishedId=20;build.target.addItem(unfinished);
        HighResCraftRecipe.Requirement wood=new HighResCraftRecipe.Requirement("plank",626,CraftCatalog.get(22),2,2,false,false);
        HighResCraftRecipe.Requirement shaftReq=new HighResCraftRecipe.Requirement("shaft",627,CraftCatalog.get(23),1,1,false,false);
        build.remaining.add(wood);build.remaining.add(shaftReq);build.desiredMaterial=14;
        build.plan();check(build.nextSource==oak,"First usable stack selected");check(build.nextItems.contains(11L),"Next material is green");
        Integer goodChance=build.continuationChance();check(goodChance!=null,"Continue estimates read live native crafting and parent skills");
        build.nextSource=birch;int poorChance=build.continuationChance();check(poorChance<goodChance,"Selected lower-QL component immediately lowers the Continue estimate");
        HighResWindowAccess.set(birch,"damage",50f);check(build.continuationChance()<poorChance,"Selected component damage is included");HighResWindowAccess.set(birch,"damage",0f);
        HighResWindowAccess.set(hud.getWorld(),"isEpic",true);check(build.continuationChance()>poorChance,"Epic uses Keybinder's skill curve");HighResWindowAccess.set(hud.getWorld(),"isEpic",false);build.plan();
        InventoryMetaItem freshBirch=item(12,"plank",626,2,55,14);map.put(12L,freshBirch);build.prune();check(build.stock.contains(freshBirch)&&!build.stock.contains(birch),"Server replacements refresh reserved QL/mass metadata without changing IDs or order");map.put(12L,birch);build.prune();
        HighResWindowAccess.set(birch,"rarity",(byte)1);String ordinaryRarity=build.rarityChance(1);
        check(build.storedRarityBoost(1)&&!build.selectedRarityBoost(1)&&build.rarityColor(1)!=org.chamomilo.wurm.ui.v1.UiColor.MUTED,"Rare stock highlights the available opportunity without changing the current normal selection");
        build.preferred.put(HighResBuildWindow.key(wood),12L);build.plan();check(build.selectedRarityBoost(1)&&build.rarityColor(1).red==1f&&!build.rarityChance(1).equals(ordinaryRarity),"Green rare component immediately adds its independent transfer chance and bright gold highlighting");
        check(Math.abs(build.materialUpgrade()-100.0/41)<.00001,"Continue material transfer uses all 41 recipe components, even when only three wooden steps remain");
        build.preferred.clear();HighResWindowAccess.set(birch,"rarity",(byte)0);build.plan();
        HighResBuildWindow.Store orderRows=build.panel.materialRows;orderRows.resizeRows();int shaftLine=orderRows.lines.indexOf(shaft);build.preferredRequirement=HighResBuildWindow.key(wood);orderRows.leftPressed(orderRows.x+8,orderRows.y+shaftLine*orderRows.rowHeight+8,1);orderRows.mouseDragged(orderRows.x+8,orderRows.y+8);orderRows.leftReleased(orderRows.x+8,orderRows.y+8);check(build.nextSource==shaft,"Dragging another material type before the selected group updates the next-use order");build.stock.clear();build.stock.addAll(Arrays.asList(oak,birch,shortPlank,shaft));build.plan();
        check(build.materialMatches(wood,oak),"Earlier wood can differ from desired final wood");wood.count=1;build.remaining.remove(shaftReq);
        check(!build.materialMatches(wood,oak)&&build.materialMatches(wood,birch),"Last remaining wood enforces final species");
        build.desiredMaterial=null;check(build.materialMatches(wood,oak),"Any material clears final wood restriction");
        build.preferred.put(HighResBuildWindow.key(wood),12L);build.plan();check(build.nextSource==birch,"Click override selects another stack");
        check(build.invalid(wood,shortPlank).contains("Insufficient"),"Underweight stack explains mass deficit");
        HighResCraftRecipe.Requirement lump=new HighResCraftRecipe.Requirement("lump",633,CraftCatalog.get(46),1,.4f,true,false);
        CreationListItem head=component("hammer head, iron",0,722);head.addChild(component("small anvil",1,791));head.addChild(component("lump, iron",1,633));HighResCraftRecipe metal=new HighResCraftRecipe(head);
        CraftCatalog.Template hammerTemplate=CraftCatalog.find("hammer",0),headTemplate=CraftCatalog.find("hammer head",0),shaftTemplate=CraftCatalog.get(23);
        CreationListItem assembledHammer=component("hammer, copper",0,hammerTemplate.icon);assembledHammer.addChild(component("hammer head, copper",1,headTemplate.icon));assembledHammer.addChild(component("shaft",1,shaftTemplate.icon));
        check(new HighResCraftRecipe(assembledHammer).requirements.get(0).materialId==CraftCatalog.get(47).material,"Metal variants carried by a weapon/tool part preserve their named metal even with a shared template");
        CreationListItem copperHead=component("hammer head, copper",0,headTemplate.icon);copperHead.addChild(component("small anvil",1,791));copperHead.addChild(component("lump, copper",1,CraftCatalog.get(47).icon));HighResCraftRecipe copperRecipe=new HighResCraftRecipe(copperHead);check(copperRecipe.entry!=null&&copperRecipe.requirements.get(0).tool,"Actual copper-lump icon resolves the shared metal recipe and retains its starting anvil tool");
        build.recipe=metal;build.unfinished=null;build.target.clearItemList();build.desiredMaterial=null;
        InventoryMetaItem iron=item(30,"lump",633,1,70,11);check(!build.materialMatches(lump,iron),"Metal choice is mandatory");build.desiredMaterial=11;check(build.materialMatches(lump,iron),"Starting lump uses selected metal");
        check(!build.invalid(lump,iron).isEmpty(),"Cold lump is red");iron.setTemperatureState((byte)5);check(build.invalid(lump,iron).isEmpty(),"Glowing lump passes heat/mass/material checks");
        build.recipe=recipe;build.unfinished=unfinished;build.target.addItem(unfinished);build.desiredMaterial=14;build.material=14;build.waitingForRemaining=false;
        build.tree(true).clear();CreationItemTreeLisItem category=new CreationItemTreeLisItem(nativeBuild,"Materials",(short)-1,(short)-1,(short)60,false);
        build.tree(true).addTreeListItem(category,null);build.tree(true).addTreeListItem(new CreationItemTreeLisItem(nativeBuild,"plank",(short)1,(short)193,(short)626,false),category);build.remainingSignature=0;build.readRemaining();check(build.remaining.size()==1&&build.remaining.get(0).count==1,"Authoritative remaining count applied");
        int count=requests;build.tick();build.tick();check(requests<=count+1,"Pair synchronization does not send repeated requests for an unchanged pair");
        nativeBuild.isUnfinishedView=true;check(build.ready(),"A single usable next step enables Build without filling the whole recipe");build.build();check(sentActions==1&&sentSource==12L&&sentTarget==20L,"Native dispatch uses exactly the green source and unfinished target");build.actionResult();build.tick();check(!build.ready(),"Action result waits for fresh remaining quantities");
        build.tree(true).addTreeListItem(category,null);build.tree(true).addTreeListItem(new CreationItemTreeLisItem(nativeBuild,"plank",(short)1,(short)193,(short)626,false),category);build.readRemaining();
        build.panel.materialRows.resizeRows();HighResBuildWindow.Store rows=build.panel.materialRows;rows.leftPressed(rows.x+5,rows.y+5,1);rows.mouseDragged(rows.x+5,rows.y+16);rows.leftReleased(rows.x+5,rows.y+16);check(build.stock.contains(oak),"Dragging within the same row preserves the reservation");
        HighResWindowAccess.set(unfinished,"baseName","wagon");build.prune();check(!build.continuing()&&build.remaining.isEmpty(),"Completed inventory item leaves continuation mode");HighResWindowAccess.set(unfinished,"baseName","unfinished wagon");build.recipe=recipe;build.desiredMaterial=14;build.stock.addAll(Arrays.asList(oak,birch,shortPlank,shaft));build.preferred.put(HighResBuildWindow.key(wood),12L);build.unfinished=unfinished;build.unfinishedId=20;build.target.addItem(unfinished);
        build.tree(true).clear();build.tree(true).addTreeListItem(category,null);build.tree(true).addTreeListItem(new CreationItemTreeLisItem(nativeBuild,"plank",(short)1,(short)193,(short)626,false),category);build.remainingSignature=0;build.readRemaining();build.tick();
        java.lang.reflect.Constructor<?> recipeFixture=sun.reflect.ReflectionFactory.getReflectionFactory().newConstructorForSerialization(CreationListWindow.class,base);recipeFixture.setAccessible(true);
        CreationListWindow nativeRecipes=(CreationListWindow)recipeFixture.newInstance("recipes fixture");HighResWindowAccess.set(nativeRecipes,"inventoryWindow",inventory);HighResWindowAccess.set(nativeRecipes,"createItemList",new ArrayList<CreationListItem>());
        @SuppressWarnings("unchecked") List<CreationListItem> list=(List<CreationListItem>)HighResWindowAccess.get(nativeRecipes,"createItemList");
        list.add(wagon);list.add(head);CreationListItem copper=component("hammer head, copper",0,722);copper.addChild(component("small anvil",1,791));copper.addChild(component("lump, copper",1,633));list.add(copper);
        HighResRecipeWindow browser=new HighResRecipeWindow(nativeRecipes,hud,null);browser.install();browser.tick();check(browser.browser.cache.isEmpty(),"Hidden recipe browser does not resolve metadata on login");browser.browser.refresh();check(browser.browser.rows.list.size()==2,"Metal variants collapse to one recipe");
        check(browser.browser.scope==HighResRecipeWindow.SearchScope.NAME&&browser.browser.rows.visible.size()==1&&browser.browser.rows.visible.get(0).category,"Default recipe view is grouped and searches names");
        browser.browser.rows.leftPressed(browser.browser.rows.x+8,browser.browser.rows.y+12,1);browser.browser.rows.leftReleased(browser.browser.rows.x+8,browser.browser.rows.y+12);
        check(browser.browser.rows.height>=156&&browser.browser.rows.contains(browser.browser.rows.x+5,browser.browser.rows.y+60),"Single-click group opening preserves complete native row hit rectangles");
        browser.browser.filterChanged("agon");check(browser.browser.rows.list.size()==1&&browser.browser.rows.list.get(0).name.equals("wagon"),"Name mode matches a partial recipe name");
        browser.browser.filterChanged("small nails");check(browser.browser.rows.list.isEmpty(),"Name mode does not search materials");
        int modeX=browser.browser.scopeButtons[1].x+12,modeY=browser.browser.scopeButtons[1].y+12;
        browser.browser.leftPressed(modeX,modeY,0);browser.browser.mouseDragged(modeX,modeY+60);browser.browser.leftReleased(modeX,modeY+60);check(browser.browser.scope==HighResRecipeWindow.SearchScope.NAME,"Dragging out of a search selector cancels the mode change");
        browser.browser.leftPressed(modeX,modeY,0);browser.browser.leftReleased(modeX,modeY);
        check(browser.browser.scope==HighResRecipeWindow.SearchScope.MATERIAL&&browser.browser.rows.list.size()==1&&browser.browser.rows.list.get(0).name.equals("wagon"),"Material selector release searches component names only");
        browser.browser.filterChanged("wagon");check(browser.browser.rows.list.isEmpty(),"Material mode does not search result names");
        browser.browser.scopeChanged(HighResRecipeWindow.SearchScope.BOTH);check(browser.browser.rows.list.size()==1,"Both mode retains the existing whole-recipe search");
        browser.browser.scopeChanged(HighResRecipeWindow.SearchScope.NAME);browser.browser.filterChanged("");
        build.panel.componentResized();HighResCraftStoreHeader storeHeader=build.panel.materialHeader;
        HighResCraftStoreHeader.Heading qualityHeading=storeHeader.headings[1];int hx=qualityHeading.x+8,hy=qualityHeading.y+8;
        qualityHeading.leftPressed(hx,hy,1);qualityHeading.leftReleased(hx,hy);check(rows.sortColumn==1&&rows.sortOrder==1,"Native inventory-style header release sorts complete store groups by QL");
        qualityHeading.leftPressed(hx,hy,1);qualityHeading.mouseDragged(hx,hy+80);qualityHeading.leftReleased(hx,hy+80);check(rows.sortOrder==1,"Header drag/release outside does not toggle sort");
        qualityHeading.leftPressed(hx,hy,1);qualityHeading.leftReleased(hx,hy);check(rows.sortOrder==-1,"Second completed header click reverses typed sort");
        int qualityWidth=storeHeader.columns[1];HighResCraftStoreHeader.Resize handle=storeHeader.handles[1];int boundary=handle.x+3;
        check(storeHeader.getComponentAt(boundary,hy)==handle,"Invisible native-style column resize handle takes priority over sorting");handle.leftPressed(boundary,hy,1);handle.mouseDragged(boundary+6,hy);handle.leftReleased(boundary+6,hy);check(storeHeader.columns[1]==qualityWidth+6&&rows.sortOrder==-1,"Column resizing changes geometry without sorting");
        storeHeader.filter.handleInputChanged(storeHeader.filter.input,"shaft");check(rows.rows.size()==1&&rows.rows.get(0)==shaft&&build.nextSource==birch,"Store filter preserves the chosen next-step identity: rows="+rows.rows.size()+", next="+(build.nextSource==null?"null":build.nextSource.getId())+", preferred="+build.preferred+", remaining="+build.remaining.size());storeHeader.filter.handleInput("");rows.sortColumn=-1;rows.resizeRows();
        int countBefore=rows.lines.size();int groupY=rows.y+rows.rowHeight+5;Object groupBefore=rows.entry(groupY);rows.leftPressed(rows.x+7,groupY,1);rows.leftReleased(rows.x+7,groupY);check(rows.lines.size()<countBefore,"Store groups collapse like inventory: lines="+countBefore+" -> "+rows.lines.size()+", box="+rows.x+","+rows.y+","+rows.width+","+rows.height+", entry="+groupBefore);rows.leftPressed(rows.x+7,groupY,1);rows.leftReleased(rows.x+7,groupY);check(rows.lines.size()==countBefore,"Store groups reopen without changing reservations");
        for(String language:HudMessages.languageCodes()){HudMessages.useLanguage(language);build.panel.refresh();HighResWindowsProbe.verifyTitleOpacity(build);HighResWindowsProbe.verifyTitleOpacity(browser);check(build.panel.recipes.x==build.panel.previewBox.x&&build.panel.recipes.y>=build.header.y+build.header.height&&build.panel.recipes.y+build.panel.recipes.height<build.panel.previewBox.y,"Choose recipe stays under the title at the upper left");check(build.panel.materialChoice.x==build.panel.previewBox.x&&build.panel.materialChoice.y>build.panel.previewBox.y+build.panel.previewBox.height&&build.panel.materialChoice.y+build.panel.materialChoice.height<build.panel.requirementScroll.y,"Material choice stays below the model and above requirements");check(build.panel.wiki.y>=build.header.y+build.header.height,"Wiki stays below the opacity title square");storeHeader.layoutColumns();check(storeHeader.filter.x+storeHeader.filter.width<storeHeader.headings[0].x+storeHeader.headings[0].width-10,"Inventory-style inline filter stays inside Name in each language");}
        verifyFinalWoodHint(build);
        HudMessages.useLanguage("en");HighResWindowAccess.set(birch,"rarity",(byte)1);build.panel.refresh();
        BufferedImage image=new BufferedImage(1660,820,BufferedImage.TYPE_INT_ARGB);HudButtonProbe.graphics=image.createGraphics();HudButtonProbe.graphics.setColor(new java.awt.Color(0x37392d));HudButtonProbe.graphics.fillRect(0,0,1660,820);
        nativeBuild.setLocation(10,10,950,780);nativeRecipes.setLocation(980,10,660,610);
        Queue queue=InventoryOpacityPreview.begin();build.render(queue);browser.render(queue);InventoryOpacityPreview.finish();HudButtonProbe.graphics.dispose();HudButtonProbe.graphics=null;ImageIO.write(image,"png",new File(output.getParentFile(),"hud-build-menu-preview.png"));
        verifyFullCatalogue(browser,list,head);
        verifyBundledCatalogue(nativeRecipes,hud,list);
        verifyRecipeNavigation(build,nativeRecipes,hud);
        verifyMetalAssembly(build,map,root);
        verifyNestedRecipeNavigation(build,nativeRecipes,hud);
        HighResCraftWorldProbe.verify(build,nativeRecipes,hud,map,root);
        System.out.println("CRAFT_UI_OK: native recipes, deduplication, component filter, mass/glowing reasons, next stack selection, last wood protection/clear, mandatory starting metal, server remaining counts, stable requests and four languages");
        globalSkills.clear();globalSkills.putAll(previousGlobals);HighResWindowAccess.set(hud.getWorld().getPlayer(),"skillSet",previousSkills);HighResWindowAccess.set(hud.getWorld(),"player",previousPlayer);
    }
    private static void verifyPreviewPalette() throws Exception {
        java.lang.reflect.Field colors=com.wurmonline.client.renderer.ItemColorsXml.class.getDeclaredField("itemColors");colors.setAccessible(true);Object previous=colors.get(null);
        String bronze="model.tool.pickaxe.blade.bronze",silver="model.tool.pickaxe.blade.silver";
        com.wurmonline.client.renderer.Color bronzeColor=new com.wurmonline.client.renderer.Color(.7f,.5f,.3f,1f),silverColor=new com.wurmonline.client.renderer.Color(1.2f,1.2f,1.2f,1.6f);
        Map<String,com.wurmonline.client.renderer.Color> palette=new HashMap<>();palette.put("bronze",bronzeColor);palette.put("silver",silverColor);colors.set(null,palette);
        try {
            com.wurmonline.client.renderer.model.ModelResourceWrapper model=new com.wurmonline.client.renderer.model.ModelResourceWrapper(null);
            HighResWindowAccess.set(model,"modelData",unsafe().allocateInstance(com.wurmonline.client.renderer.model.collada.ColladaModelData.class));
            PortraitRenderService renderer=(PortraitRenderService)unsafe().allocateInstance(PortraitRenderService.class);
            PortraitProfile profile=new PortraitProfile();renderer.requestRender(model,profile,bronze);
            check(HighResWindowAccess.get(renderer,"requestedMaterialColor")==bronzeColor,"Preview uses the native material-color lookup by selected mapping");PortraitRenderKey first=renderer.renderKey();
            renderer.requestRender(model,profile,silver);
            check(HighResWindowAccess.get(renderer,"requestedMaterialColor")==silverColor&&!first.equals(renderer.renderKey()),"Material Mod's reflection marker is preserved and different metals never share the same render");
            renderer.requestRender(model,profile);
            check(HighResWindowAccess.get(renderer,"requestedMaterialColor")==null,"Existing creature portrait requests preserve their material behavior");
        } finally {colors.set(null,previous);}
        System.out.println("CRAFT_PREVIEW_PALETTE_OK: native item color lookup, masked-material tint/reflection markers and separate render keys by selected material");
    }
    private static void verifyFinalWoodHint(HighResBuildWindow build){
        for(String language:HudMessages.languageCodes()){
            HudMessages.useLanguage(language);build.panel.refresh();
            check(build.panel.requirements.rows.contains(HudMessages.tr("Last wooden item should be of {0} material","birchwood")),"Final wood requirement is visible and localized: "+language);
        }
        HudMessages.useLanguage("en");byte other=build.panel.materialIds.stream().filter(id->id!=0&&id!=14).findFirst().get();build.desiredMaterial=other;build.panel.refresh();
        check(build.panel.requirements.rows.contains("Last wooden item should be of "+com.wurmonline.shared.util.MaterialUtilities.getMaterialString(other)+" material"),"Selecting another species updates the last wood requirement");
        build.desiredMaterial=null;build.panel.refresh();check(build.panel.requirements.rows.stream().noneMatch(row->row instanceof String&&((String)row).startsWith("Last wooden item")),"Any material removes the last wood requirement");
        build.desiredMaterial=14;build.panel.materialOptions();build.panel.refresh();
        System.out.println("FINAL_WOOD_HINT_OK: four languages, selected species updates and Any material clears the restriction text");
    }
    @SuppressWarnings("unchecked")
    private static void verifyMetalAssembly(HighResBuildWindow build,Map<Long,InventoryMetaItem> inventory,InventoryMetaItem root) throws Exception {
        HighResCraftRecipe pickaxe=HighResBundledRecipes.instance().recipes.values().stream().filter(r->r.name.equals("pickaxe, iron")).findFirst().get();
        build.select(pickaxe);
        for(int id=1;id<256;id++)if(com.wurmonline.shared.util.MaterialUtilities.isMetal((byte)id))
            check(build.panel.materialIds.contains((byte)id),"Assembly's iron-only catalog entry must offer every native metal: "+id);
        InventoryMetaItem shaft=item(110,"shaft",627,1,50,14),iron=item(111,"pickaxe head",723,1,50,11),copper=item(112,"pickaxe head",723,1,50,10);
        List<InventoryMetaItem> children=(List<InventoryMetaItem>)HighResWindowAccess.get(root,"children");List<InventoryMetaItem> previous=new ArrayList<>(children);
        children.clear();children.addAll(Arrays.asList(shaft,iron,copper));for(InventoryMetaItem item:children)inventory.put(item.getId(),item);
        try {
            build.desiredMaterial=10;build.panel.materialOptions();build.autoPick();
            HighResCraftRecipe.Requirement head=pickaxe.requirements.get(1);
            check(build.nextTarget==copper&&build.stock.contains(copper)&&!build.stock.contains(iron),"Auto-pick reserves a copper head for the selected copper pickaxe");
            check(build.requirementName(head).equals("pickaxe head, copper")&&build.resultName().equals("pickaxe, copper"),"Selected metal updates the assembly requirement and result captions");
            check(!build.invalid(head,iron).isEmpty()&&build.invalid(head,copper).isEmpty(),"Wrong-metal head is red; selected-metal head is usable");
            build.stock.add(iron);build.selectNext(iron);check(build.nextTarget==iron&&!build.ready(),"Manually selecting an iron head blocks building the copper result");
            build.selectNext(copper);
            CreationItemTreeLisItem action=new CreationItemTreeLisItem(build.nativeWindow,"pickaxe, copper",(short)78,(short)148,(short)743,false);
            build.tree(false).addTreeListItem(action,null);build.nativeWindow.isUnfinishedView=false;build.findAction();
            check(build.ready(),"Matching head enables the generic assembly recipe with its native metal action");
            check(CraftCatalog.clean("electrum lump").equals("lump"),"Electrum inventory components match the generic lump family");
        } finally {children.clear();children.addAll(previous);inventory.remove(110L);inventory.remove(111L);inventory.remove(112L);}
        System.out.println("METAL_ASSEMBLY_OK: all native metals, copper head auto-pick, required-metal captions, wrong-head rejection and native action readiness");
    }
    private static void verifyRecipeNavigation(HighResBuildWindow build,CreationListWindow window,HeadsUpDisplay hud) {
        Object previous=HighResWindowAccess.get(hud,"creationListWindow");HighResWindowAccess.set(hud,"creationListWindow",window);
        try {
            hud.getComponents().remove(window);build.panel.componentResized();int x=build.panel.recipes.x+10,y=build.panel.recipes.y+12;
            build.panel.leftPressed(x,y,0);build.panel.mouseDragged(x,y+60);build.panel.leftReleased(x,y+60);check(!hud.getComponents().contains(window),"Dragging outside Choose recipe cancels opening");
            build.panel.leftPressed(x,y,0);build.panel.leftReleased(x,y);check(hud.getComponents().contains(window),"Completed Choose recipe click opens the native recipe window");
            window.toggleSize();check(window.minimized,"Recipe window fixture collapses");build.panel.leftPressed(x,y,0);build.panel.leftReleased(x,y);check(!window.minimized&&hud.getComponents().contains(window),"Choose recipe restores the same collapsed recipe window");
            System.out.println("RECIPE_NAVIGATION_OK: Choose recipe button, native opening, cancelled drag and collapsed restore");
        } finally {hud.getComponents().remove(window);HighResWindowAccess.set(hud,"creationListWindow",previous);}
    }
    private static void verifyFullCatalogue(HighResRecipeWindow browser,List<CreationListItem> nativeItems,CreationListItem head) throws Exception {
        List<CreationListItem> saved=new ArrayList<>(nativeItems);nativeItems.clear();
        try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(org.highreshud.client.HudPackagedResources.open("org/highreshud/client/craft/entries.tsv"),java.nio.charset.StandardCharsets.UTF_8))) {
            for(String line;(line=reader.readLine())!=null;) {
                String[] cells=line.split("\t",-1);CraftCatalog.Template result=CraftCatalog.get(Integer.parseInt(cells[0])),source=CraftCatalog.get(Integer.parseInt(cells[1])),target=CraftCatalog.get(Integer.parseInt(cells[2]));
                if(result==null||source==null||target==null)continue;
                CreationListItem recipe=component(result.name,0,result.icon);recipe.addChild(component(source.name,1,source.icon));recipe.addChild(component(target.name,1,target.icon));
                for(String extra:cells[11].split(";"))if(!extra.isEmpty()){String[] part=extra.split(",");CraftCatalog.Template material=CraftCatalog.get(Integer.parseInt(part[1]));if(material!=null)recipe.addChild(component(material.name,Integer.parseInt(part[2]),material.icon));}
                nativeItems.add(recipe);
            }
        }
        List<CreationListItem> standard=new ArrayList<>(nativeItems);
        while(nativeItems.size()<2200){CreationListItem source=standard.get(nativeItems.size()%standard.size());CreationListItem recipe=component(source.getName(),0,HighResCraftRecipe.icon(source));for(CreationListItem child:source.getChilds())recipe.addChild(component(child.getName(),((Number)HighResWindowAccess.get(child,"numberOfitems")).intValue(),HighResCraftRecipe.icon(child)));nativeItems.add(recipe);}
        long started=System.nanoTime();browser.tick();check(browser.browser.cache.size()==3,"A full incoming catalog remains unprocessed while the browser is hidden");
        browser.browser.refresh();check(browser.browser.loading()&&browser.browser.cache.size()<=HighResRecipeWindow.Browser.BATCH_SIZE,"Opening processes a bounded batch, rather than blocking on thousands of recipes");
        int batches=1;while(browser.browser.loading()&&batches<200){browser.browser.refresh();batches++;}
        check(!browser.browser.loading()&&browser.browser.cache.size()==nativeItems.size(),"Full 2200-recipe catalog resolves completely");
        StringBuilder export=new StringBuilder();HighResCatalogExport.append(export,nativeItems.get(0),0);
        String[] exported=export.toString().split("\n");check(exported[0].startsWith("I\t0\t")&&exported[1].startsWith("I\t1\t")
                &&new String(Base64.getDecoder().decode(exported[0].split("\t")[2]),java.nio.charset.StandardCharsets.UTF_8).equals(nativeItems.get(0).getName()),"Manual capture preserves actual server names and component hierarchy");
        Map<CreationListItem,HighResCraftRecipe> parsed=new IdentityHashMap<>(browser.browser.cache);
        for(int n=0;n<80;n++){browser.browser.filterChanged(n%2==0?"plank":"");browser.browser.refresh();}
        for(Map.Entry<CreationListItem,HighResCraftRecipe> cached:parsed.entrySet())check(browser.browser.cache.get(cached.getKey())==cached.getValue(),"Filter/repaint reuses the exact cached recipe descriptor");
        browser.browser.filterChanged("");nativeItems.add(head);browser.browser.refresh();while(browser.browser.loading())browser.browser.refresh();
        for(Map.Entry<CreationListItem,HighResCraftRecipe> cached:parsed.entrySet())check(browser.browser.cache.get(cached.getKey())==cached.getValue(),"Late server recipes preserve already processed metadata");
        long millis=(System.nanoTime()-started)/1_000_000;check(millis<10_000,"Full catalogue and repeated filters must remain interactive, elapsed="+millis+"ms");
        System.out.println("CRAFT_CATALOG_CACHE_OK: 2200 native recipes, hidden-window no work, batches="+batches+", exact metadata reuse across 80 filters and server append, elapsedMs="+millis);
        nativeItems.clear();nativeItems.addAll(saved);browser.browser.filterChanged("");while(browser.browser.loading())browser.browser.refresh();
    }
    private static void verifyBundledCatalogue(CreationListWindow window,HeadsUpDisplay hud,List<CreationListItem> server) {
        long start=System.nanoTime();HighResRecipeWindow packed=new HighResRecipeWindow(window,hud);packed.browser.refresh();
        check(packed.items().size()==2702&&packed.browser.cache.size()==2702&&!packed.browser.loading(),"Actual Sklotopolis Novus snapshot is ready without resolving server metadata");
        List<CreationListItem> saved=new ArrayList<>(server);server.clear();server.add(component("changed server recipe",0,60));
        packed.tick();packed.browser.refresh();check(packed.items().size()==2702&&packed.browser.catalog.stream().noneMatch(r->r.name.equals("changed server recipe")),"Server catalog changes are ignored: no automatic comparison or refresh");
        HighResCraftRecipe wagon=packed.browser.catalog.stream().filter(r->r.name.equals("wagon")).findFirst().get();check(wagon.entry!=null&&wagon.entry.totalSteps==41,"Preprocessed snapshot retains full wagon rarity denominator");
        packed.browser.scopeChanged(HighResRecipeWindow.SearchScope.MATERIAL);packed.browser.filterChanged("small nails");check(packed.browser.rows.list.stream().anyMatch(r->r==wagon),"Packaged component filter reuses the prepared wagon descriptor");
        for(CreationListItem item:packed.items())check(packed.browser.cache.get(item)==packed.bundled.recipes.get(item),"Packaged descriptors are reused without runtime name/template lookup");
        server.clear();server.addAll(saved);System.out.println("SKLOTOPOLIS_BUNDLE_OK: 2702 actual server recipes, cached startup, no comparison/automatic refresh, component filter, wagon rarity; elapsedMs="+(System.nanoTime()-start)/1_000_000);
    }
    @SuppressWarnings("unchecked")
    private static void verifyNestedRecipeNavigation(HighResBuildWindow build,CreationListWindow window,HeadsUpDisplay hud) throws Exception {
        Object previousBuild=HighResWindowAccess.get(hud,"creationWindow"),previousBrowser=HighResWindowAccess.get(hud,"creationListWindow"),previousCategories=HighResWindowAccess.get(window,"categoryMap");
        java.lang.reflect.Field field=HighResWindowIntegration.class.getDeclaredField("WINDOWS");field.setAccessible(true);
        Map<WurmComponent,HighResNativeWindow> skins=(Map<WurmComponent,HighResNativeWindow>)field.get(null);
        HighResRecipeWindow browser=new HighResRecipeWindow(window,hud);browser.install();
        HighResNativeWindow oldBuild=skins.put(build.nativeWindow,build),oldBrowser=skins.put(window,browser);
        HighResWindowAccess.set(hud,"creationWindow",build.nativeWindow);HighResWindowAccess.set(hud,"creationListWindow",window);
        Map<Short,CreationListItem> names=new HashMap<>();names.put((short)45,new CreationListItem("Ships","",0,(short)0,(short)45,true));
        HighResWindowAccess.set(window,"categoryMap",names);hud.getComponents().add(window);
        try {
            browser.browser.refresh();check(browser.browser.categoryNames.get((short)45).equals("Ships"),"Groups use native category labels once without importing the server recipes");
            browser.browser.filterChanged("knarr");HighResRecipeWindow.Rows rows=browser.browser.rows;
            int root=-1;for(int i=0;i<rows.visible.size();i++)if(rows.visible.get(i).recipe!=null&&rows.visible.get(i).recipe.name.equals("knarr")&&rows.visible.get(i).component==null)root=i;
            check(root>=0,"Name search exposes knarr inside its category");int x=rows.x+90,y=rows.y+root*52+15;
            check(hud.getComponentAt(x,y)==rows,"Native HUD routes tree-row input through the visible scroll viewport");
            hud.mousePressed(x,y,0,1);hud.mouseReleased(x,y,0);
            check(hud.getComponents().contains(window)&&rows.visible.stream().anyMatch(row->row.component!=null),"One recipe click reveals its component list without opening Build or closing the browser");
            int stern=-1;for(int i=0;i<rows.visible.size();i++)if(rows.visible.get(i).component!=null&&CraftCatalog.clean(rows.visible.get(i).component.name).equals("stern"))stern=i;
            check(stern>=0&&rows.visible.get(stern).recipe!=null&&rows.visible.get(stern).expandable,"Knarr's stern component resolves to its own cached recipe");
            HighResRecipeWindow.TreeRow nested=rows.visible.get(stern);HighResCraftRecipe target=nested.recipe;
            x=rows.x+110;y=rows.y+stern*52+15;
            hud.mousePressed(x,y,0,1);hud.mouseReleased(x,y,0);
            check(browser.browser.selectedKey.equals(nested.key)&&rows.visible.get(stern+1).depth==nested.depth+1&&hud.getComponents().contains(window),"One component click selects stern and opens its own material group");
            for(String language:HudMessages.languageCodes()){HudMessages.useLanguage(language);browser.browser.refresh();check(browser.browser.scope==HighResRecipeWindow.SearchScope.NAME&&rows.visible.stream().anyMatch(row->row.key.equals(nested.key)),"Language changes preserve filter mode and the expanded nested recipe path");}
            HudMessages.useLanguage("en");
            build.collapse();check(build.window.minimized,"Build fixture is collapsed before choosing the nested recipe");
            hud.mousePressed(x,y,0,2);hud.mouseReleased(x,y,0);
            check(build.recipe==target&&build.recipe.name.equals("stern")&&!build.window.minimized&&hud.getComponents().contains(build.nativeWindow)&&!hud.getComponents().contains(window),"Double-click restores Build for the nested stern recipe and closes the browser");
        } finally {
            HudMessages.useLanguage("en");hud.getComponents().remove(window);hud.getComponents().remove(build.nativeWindow);
            if(oldBuild==null)skins.remove(build.nativeWindow);else skins.put(build.nativeWindow,oldBuild);
            if(oldBrowser==null)skins.remove(window);else skins.put(window,oldBrowser);
            HighResWindowAccess.set(hud,"creationWindow",previousBuild);HighResWindowAccess.set(hud,"creationListWindow",previousBrowser);HighResWindowAccess.set(window,"categoryMap",previousCategories);
        }
        System.out.println("RECIPE_TREE_OK: cached category groups, name/material/both selectors, single-click component expansion and four-language state, native double-click knarr -> stern Build and browser close");
    }
}
