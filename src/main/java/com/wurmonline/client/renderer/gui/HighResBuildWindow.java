package com.wurmonline.client.renderer.gui;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.game.SkillLogic;
import com.wurmonline.client.game.SkillLogicSet;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.resources.textures.*;
import com.wurmonline.shared.util.MaterialUtilities;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.craft.*;
import org.highreshud.client.i18n.HudMessages;
import org.highreshud.client.ui.HudCaptionGroup;

/** Inventory reservations, one-step dispatch and authoritative unfinished requirements. */
final class HighResBuildWindow extends HighResNativeWindow {
    final CreationWindow nativeWindow;
    final CreationFrame source,target;
    final Panel panel;
    final HighResCraftPreview preview=new HighResCraftPreview();
    HighResCraftRecipe recipe;
    final List<InventoryMetaItem> stock=new ArrayList<>();
    final Map<Long,HighResCraftWorldReservation> worldStock=new LinkedHashMap<>();
    final Set<Long> toolReservations=new HashSet<>();
    final List<HighResCraftRecipe.Requirement> remaining=new ArrayList<>();
    final Map<String,Long> preferred=new HashMap<>();
    final Set<Long> nextItems=new LinkedHashSet<>();
    InventoryMetaItem unfinished;
    CreationFrame.GroundCreationItem ground;
    HighResCraftRecipe.Requirement nextRequirement;
    InventoryMetaItem nextSource,nextTarget;
    String preferredRequirement="";
    volatile String status="";
    byte material;
    Byte desiredMaterial;
    void chooseVariant(){if(recipe==null||desiredMaterial==null||recipe.variants.isEmpty())return;List<HighResCraftRecipe> variants=new ArrayList<>(recipe.variants);
        for(HighResCraftRecipe variant:variants)for(HighResCraftRecipe.Requirement r:variant.requirements)if(r.start&&!r.tool&&r.template!=null&&r.template.metal()&&r.materialId==(desiredMaterial&255)){if(variant!=recipe){variant.variants.clear();variant.variants.addAll(variants);recipe=variant;}return;}
    }
    long pairSource=Long.MIN_VALUE,pairTarget=Long.MIN_VALUE,unfinishedId=Long.MIN_VALUE;
    int remainingSignature;
    boolean waiting,refreshAfterAction;
    boolean waitingForRemaining;
    long waitingSince;
    long createdSince;
    final Set<Long> beforeCreate=new HashSet<>();
    volatile InventoryMetaItem pendingCreated;
    CreationItemTreeLisItem action;
    private final Properties toolIcons=new Properties();
    private final CraftSkillRoll skillRoll=new CraftSkillRoll();
    private final CraftRarityRoll rarityRoll=new CraftRarityRoll();
    private final Path memoryFile=Paths.get(System.getProperty("user.home"),".chamomilo","craft-tools.properties");

    HighResBuildWindow(CreationWindow window,HeadsUpDisplay hud) {
        super("highres.build","Build menu",window,hud);nativeWindow=window;
        source=(CreationFrame)HighResWindowAccess.get(window,"source");target=(CreationFrame)HighResWindowAccess.get(window,"target");
        panel=new Panel();
        if(Files.isRegularFile(memoryFile))try(InputStream in=Files.newInputStream(memoryFile)){toolIcons.load(in);}catch(IOException ignored){}
    }
    @Override void install(){window.resizable=true;window.setComponent(panel);super.install();window.setSize(Math.max(940,window.width),Math.max(740,window.height));}
    @Override String caption(){return HudMessages.tr("Build menu")+(recipe==null?"":" · "+recipe.displayName());}
    @Override void close(){window.closePressed();}
    private void openRecipes(){CreationListWindow recipes=owner.getCreationListWindow();if(recipes==null)return;if(recipes.minimized)recipes.toggleSize();owner.openCreationListWindowVisible();}
    @Override void tick(){
        if(pendingCreated!=null){InventoryMetaItem item=pendingCreated;pendingCreated=null;target.clearItemList();target.clearGroundItem();target.addItem(item);createdSince=0;}
        header.refresh();captureNativeReservations();discoverUnfinished();prune();readRemaining();plan();synchronizePair();findAction();
        if(waiting&&nativeWindow.getActionInUse()==0&&System.nanoTime()-waitingSince>500_000_000L){waiting=false;refreshAfterAction=true;}
        if(refreshAfterAction){refreshAfterAction=false;nativeWindow.clearItemLists();remainingSignature=0;waitingForRemaining=continuing();pairSource=Long.MIN_VALUE;synchronizePair();}
        updatePreview();
        panel.refresh();
    }
    void select(HighResCraftRecipe selected){
        recipe=selected;unfinished=null;ground=null;unfinishedId=Long.MIN_VALUE;remaining.clear();remainingSignature=0;
        material=(byte)(recipe.result==null?0:recipe.result.material);desiredMaterial=null;preferred.clear();preferredRequirement="";
        source.clearItemList();source.clearGroundItem();target.clearItemList();target.clearGroundItem();pairSource=pairTarget=Long.MIN_VALUE;
        stock.removeIf(item->!toolReservations.contains(item.getId())||recipe.requirements.stream().noneMatch(r->r.tool&&r.matches(item)));
        Set<Long> kept=new HashSet<>();for(InventoryMetaItem item:stock)kept.add(item.getId());worldStock.keySet().retainAll(kept);toolReservations.retainAll(kept);
        action=null;status="";waiting=false;waitingForRemaining=false;panel.materialOptions();preview.select(recipe.result,material);tick();
    }
    void captureNativeReservations(){
        // Native inventory/context-menu additions still arrive in the original two slots.
        for(CreationFrame frame:new CreationFrame[]{source,target})for(InventoryMetaItem item:new ArrayList<>(frame.itemList))
            if(!item.isUnfinished()&&stock.stream().noneMatch(stored->stored.getId()==item.getId())){
                stock.add(item);HighResCraftRecipe.Requirement r=requirement(item);
                if(r!=null&&r.tool)toolReservations.add(item.getId());
            }
    }
    void worldAdded(String name,long id,float quality,float damage,float weight,short icon,CreationFrame.ItemType type){
        HighResCraftWorldReservation reservation=new HighResCraftWorldReservation(source,name,id,quality,damage,weight,icon,type);
        worldStock.put(id,reservation);int position=stock.size();for(int i=0;i<stock.size();i++)if(stock.get(i).getId()==id){position=i;break;}
        stock.removeIf(item->item.getId()==id);stock.add(Math.min(position,stock.size()),reservation.item);
        if(reservation.ground.isUnFinished()){
            toolReservations.remove(id);
            source.clearItemList();source.clearGroundItem();target.clearItemList();target.groundCreationItem=reservation.ground;
            discoverUnfinished();
            createdSince=0;pendingCreated=null;
        }else{
            HighResCraftRecipe.Requirement r=requirement(reservation.item);
            CraftCatalog.Template template=CraftCatalog.find(name,icon);
            if(r!=null?r.tool:template!=null&&(template.flags.contains(38)||template.flags.contains(15)))toolReservations.add(id);else toolReservations.remove(id);
        }
        regroup();pairSource=pairTarget=Long.MIN_VALUE;status="";tick();
    }
    void worldUpdated(long id,float quality,float damage,float weight){
        HighResCraftWorldReservation reservation=worldStock.get(id);if(reservation==null)return;
        reservation.update(quality,damage,weight);pairSource=pairTarget=Long.MIN_VALUE;tick();
    }
    void worldReplaced(long oldId,String name,long id,float quality,float damage,float weight,short icon,CreationFrame.ItemType type){
        boolean wasTarget=oldId==unfinishedId;
        boolean incomplete=new HighResCraftWorldReservation(source,name,id,quality,damage,weight,icon,type).ground.isUnFinished();
        if(oldId==id&&(!wasTarget||incomplete)){worldAdded(name,id,quality,damage,weight,icon,type);return;}
        Map<String,Long> choices=new HashMap<>(preferred);removeReservation(oldId);
        if(wasTarget&&!incomplete){
            completed();tick();return;
        }
        worldAdded(name,id,quality,damage,weight,icon,type);
        if(!incomplete){for(Map.Entry<String,Long> choice:choices.entrySet())if(choice.getValue()==oldId)preferred.put(choice.getKey(),id);tick();}
    }
    void removeReservation(long id){
        stock.removeIf(item->item.getId()==id);worldStock.remove(id);toolReservations.remove(id);preferred.values().removeIf(value->value==id);
        if(id==unfinishedId){unfinished=null;ground=null;unfinishedId=Long.MIN_VALUE;remaining.clear();remainingSignature=0;waitingForRemaining=false;panel.materialOptions();}
        if(source.getFirstItemId()==id){source.clearItemList();source.clearGroundItem();}
        if(target.getFirstItemId()==id){target.clearItemList();target.clearGroundItem();}
        pairSource=pairTarget=Long.MIN_VALUE;action=null;
    }
    void clearStores(){for(InventoryMetaItem item:new ArrayList<>(stock))removeReservation(item.getId());preferred.clear();preferredRequirement="";tick();}
    void completed(){
        recipe=null;unfinished=null;ground=null;unfinishedId=Long.MIN_VALUE;remaining.clear();remainingSignature=0;
        stock.clear();worldStock.clear();toolReservations.clear();preferred.clear();preferredRequirement="";nextItems.clear();
        nextSource=nextTarget=null;nextRequirement=null;action=null;desiredMaterial=null;material=0;status="";
        waiting=refreshAfterAction=waitingForRemaining=false;createdSince=0;pendingCreated=null;beforeCreate.clear();
        pairSource=pairTarget=Long.MIN_VALUE;source.clearItemList();source.clearGroundItem();target.clearItemList();target.clearGroundItem();
        nativeWindow.clearItemLists();nativeWindow.isUnfinishedView=false;preview.select(null,(byte)0);panel.materialOptions();
    }
    void inventoryRemoved(InventoryMetaItem item){if(item.getId()==unfinishedId)completed();}
    void discoverUnfinished(){
        InventoryMetaItem item=null;CreationFrame.GroundCreationItem onGround=null;
        for(CreationFrame f:new CreationFrame[]{source,target}) {
            for(InventoryMetaItem meta:f.itemList)if(meta.isUnfinished()){item=meta;break;}
            if(f.groundCreationItem!=null&&f.groundCreationItem.isUnFinished())onGround=f.groundCreationItem;
        }
        if(item==null&&onGround==null)return;
        long id=item!=null?item.getId():onGround.getId();
        unfinished=item;ground=onGround;
        if(id==unfinishedId)return;
        long previous=unfinishedId;unfinishedId=id;
        if(previous!=Long.MIN_VALUE){stock.removeIf(meta->meta.getId()==previous);worldStock.remove(previous);}
        InventoryMetaItem reserved=item!=null?item:worldStock.containsKey(id)?worldStock.get(id).item:null;
        if(reserved!=null&&stock.stream().noneMatch(meta->meta.getId()==id))stock.add(reserved);
        String name=item!=null?item.getBaseName():onGround.getName();
        HighResCraftRecipe match=HighResWindowIntegration.recipeFor(name);
        if(match!=null||recipe==null||!CraftCatalog.clean(recipe.name).equals(CraftCatalog.clean(name)))recipe=match;
        desiredMaterial=null;preferred.clear();preferredRequirement="";
        if(recipe!=null){material=reserved!=null?reserved.getMaterialId():(byte)(recipe.result==null?0:recipe.result.material);preview.select(recipe.result,material);}
        pairSource=pairTarget=Long.MIN_VALUE;remainingSignature=0;remaining.clear();waitingForRemaining=true;panel.materialOptions();
        nativeWindow.clearItemLists();
    }
    void prune(){
        for(HighResCraftWorldReservation reservation:worldStock.values())reservation.refresh(owner.getWorld());
        if(ground!=null&&!ground.isUnFinished())completed();
        for(ListIterator<InventoryMetaItem> it=stock.listIterator();it.hasNext();) {
            InventoryMetaItem item=it.next();if(worldStock.containsKey(item.getId()))continue;
            InventoryMetaItem current=nativeWindow.playerInventory.getItem(item.getId());
            if(current==null||current.getWeight()<=0&&!current.isUnfinished())it.remove();else if(current!=item)it.set(current);
        }
        if(unfinished!=null)unfinished=nativeWindow.playerInventory.getItem(unfinished.getId());
        if(unfinishedId!=Long.MIN_VALUE&&ground==null&&(unfinished==null||!unfinished.isUnfinished()))completed();
    }
    @SuppressWarnings("unchecked") WurmTreeList<CreationItemTreeLisItem> tree(boolean unfinished){return (WurmTreeList<CreationItemTreeLisItem>)HighResWindowAccess.get(nativeWindow,unfinished?"unfinishedItemList":"createItemList");}
    @SuppressWarnings("unchecked") List<CreationItemTreeLisItem> allRows(boolean unfinished){
        WTreeListNode<CreationItemTreeLisItem> root=(WTreeListNode<CreationItemTreeLisItem>)HighResWindowAccess.get(tree(unfinished),"rootNode");
        List<CreationItemTreeLisItem> rows=new ArrayList<>();collect(root,rows);return rows;
    }
    private void collect(WTreeListNode<CreationItemTreeLisItem> node,List<CreationItemTreeLisItem> rows){for(WTreeListNode<CreationItemTreeLisItem> child:node.children){if(child.item!=null)rows.add(child.item);collect(child,rows);}}
    void readRemaining(){
        if(!continuing())return;
        List<CreationItemTreeLisItem> rows=allRows(true);if(rows.isEmpty())return;int signature=1;
        for(CreationItemTreeLisItem row:rows)signature=31*signature+row.getName().hashCode()+row.getChance().hashCode();
        waitingForRemaining=false;if(signature==remainingSignature)return;remainingSignature=signature;remaining.clear();
        for(CreationItemTreeLisItem row:rows){int count=((Number)HighResWindowAccess.get(row,"changeOrQuantity")).intValue();if(count<=0)continue;
            int icon=((Number)HighResWindowAccess.get(row,"iconId")).intValue();CraftCatalog.Template t=CraftCatalog.find(row.getName(),icon);
            remaining.add(new HighResCraftRecipe.Requirement(row.getName(),icon,t,count,t==null?Float.NaN:t.grams/1000f,false,false));
        }
    }
    boolean continuing(){return unfinished!=null||ground!=null;}
    List<HighResCraftRecipe.Requirement> activeRequirements(){return continuing()?remaining:recipe==null?Collections.emptyList():recipe.requirements;}
    HighResCraftRecipe.Requirement requirement(InventoryMetaItem item){for(HighResCraftRecipe.Requirement r:activeRequirements())if(r.matches(item))return r;
        if(continuing()&&recipe!=null)for(HighResCraftRecipe.Requirement r:recipe.requirements)if(r.tool&&r.matches(item))return r;return null;}
    boolean materialMatches(HighResCraftRecipe.Requirement r,InventoryMetaItem item){
        if(!continuing()&&recipe!=null&&recipe.result!=null&&recipe.result.metal()&&determinesFinalMaterial(r))return desiredMaterial!=null&&item.getMaterialId()==desiredMaterial;
        return desiredMaterial==null||!determinesFinalMaterial(r)||!lastFamilyStep(r)||item.getMaterialId()==desiredMaterial;
    }
    String requirementName(HighResCraftRecipe.Requirement r){
        return desiredMaterial!=null&&!continuing()&&recipe!=null&&recipe.result!=null&&recipe.result.metal()&&determinesFinalMaterial(r)
                ?CraftCatalog.clean(r.name)+", "+MaterialUtilities.getMaterialString(desiredMaterial):r.name;
    }
    String resultName(){return recipe==null?"":desiredMaterial!=null&&recipe.result!=null&&recipe.result.metal()
            ?recipe.displayName()+", "+MaterialUtilities.getMaterialString(desiredMaterial):recipe.name;}
    boolean determinesFinalMaterial(HighResCraftRecipe.Requirement r) {
        return !r.tool&&r.template!=null&&recipe!=null&&recipe.result!=null&&(recipe.result.wood()&&r.template.wood()||!continuing()&&recipe.result.metal()&&r.start&&r.template.metal());
    }
    boolean lastFamilyStep(HighResCraftRecipe.Requirement current) {
        if(!determinesFinalMaterial(current))return false;
        if(recipe.result.metal())return current==recipe.requirements.get(1)||!determinesFinalMaterial(recipe.requirements.get(1));
        int steps=0;
        for(HighResCraftRecipe.Requirement r:activeRequirements())if(determinesFinalMaterial(r)&&(!continuing()||!r.start))steps+=r.count;
        if(continuing())return steps==1;
        int later=0;for(HighResCraftRecipe.Requirement r:recipe.requirements)if(!r.start&&determinesFinalMaterial(r))later+=r.count;
        if(later>0)return false;
        // Both starters share one creation action; the target determines material when both match.
        return recipe.requirements.size()<2||current==recipe.requirements.get(1)||!determinesFinalMaterial(recipe.requirements.get(1));
    }
    String invalid(HighResCraftRecipe.Requirement r,InventoryMetaItem item){
        HighResCraftWorldReservation world=worldStock.get(item.getId());
        if(world!=null&&!world.available)return HudMessages.tr("This world item is no longer available here");
        if(r!=null&&r.location())return world==null?HudMessages.tr("Select this target with Add to crafting window in the world"):"";
        if(item.getDamage()>=100f)return HudMessages.tr("This item is too damaged to use");
        if(item.getId()==unfinishedId)return "";
        if(r==null)return HudMessages.tr("This material is not required by the recipe");
        StringBuilder why=new StringBuilder();
        if(!CraftReadiness.enough(item.getWeight(),r.unitKg))why.append(HudMessages.tr("Insufficient mass: {0}; need {1}",mass(item.getWeight()),mass(r.unitKg)));
        if(r.glowing(recipe)&&item.getTemperature()!=5){if(why.length()>0)why.append(" · ");why.append(HudMessages.tr("Must be glowing"));}
        if(!materialMatches(r,item)){if(why.length()>0)why.append(" · ");why.append(desiredMaterial==null?HudMessages.tr("Choose a metal before building"):HudMessages.tr(recipe!=null&&recipe.result!=null&&recipe.result.metal()?"Starting metal component must use {0}":"Last material step must use {0}",MaterialUtilities.getMaterialString(desiredMaterial)));}
        return why.toString();
    }
    InventoryMetaItem choose(HighResCraftRecipe.Requirement r,Set<Long> used){
        if(r==null)return null;Long preferredId=preferred.get(key(r));
        if(preferredId!=null)for(InventoryMetaItem item:stock)if(item.getId()==preferredId&&r.matches(item)&&!used.contains(item.getId()))return item;
        InventoryMetaItem invalid=null;
        for(InventoryMetaItem item:stock)if(!used.contains(item.getId())&&r.matches(item)) {
            if(invalid(r,item).isEmpty())return item;if(invalid==null)invalid=item;
        }return invalid;
    }
    void plan(){
        nextItems.clear();nextSource=nextTarget=null;nextRequirement=null;
        if(recipe==null&&!continuing())return;
        if(continuing()) {
            nextItems.add(unfinishedId);
            List<HighResCraftRecipe.Requirement> ordered=new ArrayList<>(remaining);
            ordered.sort(Comparator.comparingInt(r->{if(key(r).equals(preferredRequirement))return -1;for(int i=0;i<stock.size();i++)if(r.matches(stock.get(i)))return i;return Integer.MAX_VALUE;}));
            for(HighResCraftRecipe.Requirement r:ordered){InventoryMetaItem item=choose(r,nextItems);if(item!=null){nextRequirement=r;nextSource=item;nextItems.add(item.getId());if(invalid(r,item).isEmpty()||key(r).equals(preferredRequirement))break;nextItems.remove(item.getId());nextSource=null;}}
        }else if(recipe.requirements.size()>=2) {
            HighResCraftRecipe.Requirement a=recipe.requirements.get(0),b=recipe.requirements.get(1);
            nextSource=choose(a,nextItems);if(nextSource!=null)nextItems.add(nextSource.getId());
            nextTarget=choose(b,nextItems);if(nextTarget!=null)nextItems.add(nextTarget.getId());nextRequirement=b;
        }
    }
    static String key(HighResCraftRecipe.Requirement r){return CraftCatalog.clean(r.name)+":"+r.start;}
    void selectNext(InventoryMetaItem item){if(item.getId()==unfinishedId)return;HighResCraftRecipe.Requirement r=requirement(item);if(r==null){status=invalid(null,item);return;}preferred.put(key(r),item.getId());preferredRequirement=key(r);pairSource=pairTarget=Long.MIN_VALUE;tick();}
    void fillFrame(CreationFrame frame,InventoryMetaItem item){if(item==null)return;HighResCraftWorldReservation world=worldStock.get(item.getId());if(world!=null)frame.groundCreationItem=world.ground;else frame.addItem(item);}
    void synchronizePair(){
        long sid=nextSource==null?-1:nextSource.getId(),tid=continuing()?unfinished!=null?unfinished.getId():ground.getId():nextTarget==null?-1:nextTarget.getId();
        if(sid==pairSource&&tid==pairTarget)return;pairSource=sid;pairTarget=tid;action=null;
        source.clearItemList();source.clearGroundItem();target.clearItemList();target.clearGroundItem();
        fillFrame(source,nextSource);
        if(unfinished!=null)target.addItem(unfinished);else if(ground!=null)target.groundCreationItem=ground;else fillFrame(target,nextTarget);
        nativeWindow.isUnfinishedView=continuing();
        HighResWindowAccess.set(nativeWindow,"selectedActions",1);
        if(recipe!=null)HighResWindowAccess.set(nativeWindow,"itemNameToSelect",recipe.name);
        nativeWindow.clearItemLists();nativeWindow.requestCreationList();
    }
    void findAction(){
        action=null;if(continuing())return;
        for(CreationItemTreeLisItem row:allRows(false))if(row.getActionId()!=-1&&recipe!=null&&CraftCatalog.clean(row.getName()).equals(CraftCatalog.clean(recipe.name))){action=row;break;}
    }
    boolean ready(){
        if(waiting||nextSource==null||nextRequirement==null)return false;
        if(!continuing()&&recipe!=null&&recipe.result!=null&&recipe.result.metal()&&desiredMaterial==null)return false;
        if(continuing())return !waitingForRemaining&&remaining.size()>0&&invalid(nextRequirement,nextSource).isEmpty()
                &&(ground==null||worldStock.containsKey(ground.getId())&&invalid(null,worldStock.get(ground.getId()).item).isEmpty());
        return recipe!=null&&recipe.requirements.size()>=2&&nextTarget!=null&&invalid(recipe.requirements.get(0),nextSource).isEmpty()&&invalid(recipe.requirements.get(1),nextTarget).isEmpty()&&action!=null&&((Number)HighResWindowAccess.get(action,"changeOrQuantity")).intValue()>0;
    }
    void build(){
        tick();if(!ready())return;
        if(!continuing()){beforeCreate.clear();List<InventoryMetaItem> inventory=new ArrayList<>();flatten(nativeWindow.playerInventory.getRootItem(),inventory);for(InventoryMetaItem i:inventory)beforeCreate.add(i.getId());createdSince=System.nanoTime();}
        if(!continuing()){WurmTreeList<CreationItemTreeLisItem> list=tree(false);list.clearSelections();list.getNode(action).setSelected(true);}
        rememberTool();nativeWindow.sendCreateAction();waiting=true;waitingSince=System.nanoTime();status=HudMessages.tr("Waiting for action result");panel.refresh();
    }
    void actionResult(){waiting=false;waitingForRemaining=continuing();status="";refreshAfterAction=true;remainingSignature=0;}
    void inventoryAdded(InventoryMetaItem item){if(recipe==null||createdSince==0||System.nanoTime()-createdSince>30_000_000_000L||beforeCreate.contains(item.getId())||!item.isUnfinished())return;
        if(!CraftCatalog.clean(item.getBaseName()).equals(CraftCatalog.clean(recipe.name)))return;
        pendingCreated=item;
    }
    void updatePreview(){if(recipe==null||recipe.result==null)return;byte visual=desiredMaterial!=null?desiredMaterial:unfinished!=null?unfinished.getMaterialId():(byte)recipe.result.material;
        if(desiredMaterial==null&&nextRequirement!=null&&nextSource!=null&&determinesFinalMaterial(nextRequirement)&&lastFamilyStep(nextRequirement))visual=nextSource.getMaterialId();material=visual;preview.select(recipe.result,visual);
    }
    void rememberTool(){if(recipe==null)return;for(InventoryMetaItem item:stock){HighResCraftRecipe.Requirement r=requirement(item);if(nextItems.contains(item.getId())&&r!=null&&r.start&&r.tool){
        String value=Short.toString(item.getType());if(value.equals(toolIcons.getProperty(recipe.name)))return;toolIcons.setProperty(recipe.name,value);
        try{Files.createDirectories(memoryFile.getParent());try(OutputStream out=Files.newOutputStream(memoryFile)){toolIcons.store(out,"Starting tools by server recipe");}}catch(IOException ignored){}return;}}
    }
    Texture toolIcon(){
        if(recipe!=null){for(HighResCraftRecipe.Requirement r:recipe.requirements)if(r.start&&r.tool)return IconLoader.getIcon((short)r.icon);
            String saved=toolIcons.getProperty(recipe.name);if(saved!=null)try{return IconLoader.getIcon(Short.parseShort(saved));}catch(NumberFormatException ignored){}
        }return ResourceTextureLoader.getNowrapLinearTexture("img.highresbuild.build");
    }
    void autoPick(){
        if(!continuing()&&recipe!=null&&recipe.result!=null&&recipe.result.metal()&&desiredMaterial==null){status=HudMessages.tr("Choose a metal before building");return;}
        if(recipe==null&&!continuing())return;List<InventoryMetaItem> inventory=new ArrayList<>();flatten(nativeWindow.playerInventory.getRootItem(),inventory);
        // Usable first, then quality. Existing manual order and selections stay intact.
        Map<String,List<HighResCraftRecipe.Requirement>> demands=new LinkedHashMap<>();
        for(HighResCraftRecipe.Requirement r:activeRequirements())demands.computeIfAbsent(CraftCatalog.clean(r.name)+":"+r.tool,k->new ArrayList<>()).add(r);
        for(List<HighResCraftRecipe.Requirement> group:demands.values()) {
            HighResCraftRecipe.Requirement r=group.get(0);float wanted=0;int minimumItems=0;
            for(HighResCraftRecipe.Requirement demand:group){wanted+=demand.totalKg();minimumItems+=r.tool?0:demand.template!=null&&demand.template.combine()?demand.start?1:0:demand.count;}
            minimumItems=Math.max(1,minimumItems);
            HighResCraftRecipe.Requirement materialGuard=r;for(HighResCraftRecipe.Requirement demand:group)if(lastFamilyStep(demand))materialGuard=demand;
            List<InventoryMetaItem> matches=new ArrayList<>();for(InventoryMetaItem item:inventory)if(!item.isUnfinished()&&r.matches(item)&&invalid(r,item).isEmpty()&&materialMatches(materialGuard,item))matches.add(item);
            matches.sort(Comparator.comparingDouble(InventoryMetaItem::getQuality).reversed());
            float have=0;int count=0;for(InventoryMetaItem item:stock)if(matches.contains(item)){have+=item.getWeight();count++;}
            for(InventoryMetaItem item:matches){if(!r.tool&&CraftReadiness.enough(have,wanted)&&count>=minimumItems)break;
                if(!stock.contains(item)){stock.add(item);if(r.tool)toolReservations.add(item.getId());have+=item.getWeight();count++;}}
        }
        regroup();status="";tick();
    }
    void flatten(InventoryMetaItem item,List<InventoryMetaItem> out){if(item==null)return;for(InventoryMetaItem child:item.getChildren()){out.add(child);flatten(child,out);}}
    float available(HighResCraftRecipe.Requirement r){float total=0;for(InventoryMetaItem item:stock)if(r.matches(item)&&materialMatches(r,item))total+=item.getWeight();return total;}
    int count(HighResCraftRecipe.Requirement r){int n=0;for(InventoryMetaItem item:stock)if(r.matches(item)&&materialMatches(r,item))n++;return n;}
    boolean add(InventoryMetaItem item,boolean tools,int at){
        if(item==null)return false;
        if(item.isUnfinished()) {target.clearItemList();target.clearGroundItem();target.addItem(item);discoverUnfinished();tick();return true;}
        HighResCraftRecipe.Requirement r=requirement(item);if(r==null||r.tool!=tools){status=r==null?invalid(null,item):HudMessages.tr(r.tool?"Place tools in the tool store":"Place materials in the material store");return false;}
        stock.remove(item);stock.add(Math.max(0,Math.min(at,stock.size())),item);if(r.tool)toolReservations.add(item.getId());regroup();status="";tick();return true;
    }
    void regroup(){Map<String,List<InventoryMetaItem>> groups=new LinkedHashMap<>();for(InventoryMetaItem i:stock){String k=CraftCatalog.clean(i.getBaseName())+":"+i.getMaterialId();groups.computeIfAbsent(k,s->new ArrayList<>()).add(i);}stock.clear();for(List<InventoryMetaItem> group:groups.values())stock.addAll(group);}
    boolean drop(int px,int py,DraggableComponent dragged){
        boolean tools=panel.tools.contains(px,py)||panel.toolHeader.contains(px,py),materials=panel.materials.contains(px,py)||panel.materialHeader.contains(px,py);
        if(!tools&&!materials&&!panel.previewBox.contains(px,py))return false;
        long[] ids=new long[0];InventoryMetaItem direct=null;
        if(dragged instanceof InventoryListComponent.InventoryTreeListItem){InventoryListComponent.InventoryTreeListItem row=(InventoryListComponent.InventoryTreeListItem)dragged;ids=row.getOwner().getSelectedCommandTargets();direct=row.item;}
        else if(dragged instanceof InventoryContainerWindow.InventoryContainerItem){InventoryContainerWindow.InventoryContainerItem row=(InventoryContainerWindow.InventoryContainerItem)dragged;ids=row.getSelectedCommandTargets();direct=row.getItem();}
        else if(dragged instanceof PaperDollItem)ids=new long[]{((PaperDollItem)dragged).getId()};
        int at=stock.size();Store store=tools?panel.toolRows:panel.materialRows;InventoryMetaItem anchor=store.at(py);if(anchor!=null)at=stock.indexOf(anchor);
        if(ids.length==0&&direct!=null)add(direct,tools,at);
        for(long id:ids){InventoryMetaItem item=nativeWindow.playerInventory.getItem(id);if(item!=null)add(item,tools,at++);}
        owner.resetDrag();return true;
    }
    static String mass(float kg){return kg<0?"—":Float.isNaN(kg)?"? kg":String.format(Locale.ROOT,"%.3f kg",kg);}
    static String ql(float value){return value<0?"—":String.format(Locale.ROOT,"%.2f",value);}
    String qualityRange(HighResCraftRecipe.Requirement r){float min=Float.POSITIVE_INFINITY,max=0;for(InventoryMetaItem i:stock)if(r.matches(i)){min=Math.min(min,i.getQuality());max=Math.max(max,i.getQuality());}return Float.isInfinite(min)?"—":ql(min)+(max>min?"–"+ql(max):"");}
    Integer continuationChance(){
        if(!continuing()||recipe==null||recipe.result==null||nextSource==null||Float.isNaN(recipe.result.difficulty))return null;
        try{
            SkillLogic skill=SkillLogicSet.getSkill(recipe.nativeItem.getSkill());if(skill==null)return null;
            com.wurmonline.client.game.SkillLogicSet skills=owner.getWorld().getPlayer().getSkillSet();
            SkillLogic parent=skills==null?null:skills.getParentSkill(skill.getId());
            double parentValue=parent==null||parent.getId()==skill.getId()?0:parent.getValue();
            // Continue checks the recipe difficulty with the selected component;
            // Improve-only priest/repairer bonuses do not belong to this action.
            return skillRoll.estimate(skill.getValue(),parentValue,recipe.result.difficulty,
                    nextSource.getQuality(),nextSource.getDamage(),owner.getWorld().isServerEpic());
        }catch(RuntimeException|LinkageError unavailable){return null;}
    }
    String rarityChance(int tier){
        if(recipe==null||recipe.entry==null||nextSource==null||!continuing()&&nextTarget==null)return "—";
        double actionTransfer=recipe.entry.advanced?CraftRarityRules.continueTransfer(recipe.entry):1;
        CraftRarityRoll.Range range=rarityRoll.afterDrumroll(tier,currentRarity(),nextSource.getRarity(),selectedTransfer(true),
                nextTarget==null?0:nextTarget.getRarity(),selectedTransfer(false),actionTransfer);
        return percent(range.getMinimum())+(range.getMaximum()-range.getMinimum()<.00005?"":"–"+percent(range.getMaximum()))+"%";
    }
    byte currentRarity(){return unfinished!=null?unfinished.getRarity():ground==null?0:rarityFromName(ground.getName());}
    double selectedTransfer(boolean sourceItem){
        InventoryMetaItem item=sourceItem?nextSource:nextTarget;if(item==null||recipe==null||recipe.entry==null)return 0;
        HighResCraftRecipe.Requirement r=continuing()?nextRequirement:recipe.requirements.get(sourceItem?0:1);
        if(r==null||!invalid(r,item).isEmpty())return 0;
        return continuing()?sourceItem?CraftRarityRules.continueTransfer(recipe.entry):0:CraftRarityRules.startTransfer(recipe.entry,r.template,sourceItem,item.getWeight(),r.unitKg);
    }
    double storedTransfer(InventoryMetaItem item){
        HighResCraftRecipe.Requirement r=requirement(item);if(r==null||r.tool||recipe==null||recipe.entry==null||!invalid(r,item).isEmpty())return 0;
        if(continuing()||!r.start)return CraftRarityRules.continueTransfer(recipe.entry);
        return CraftRarityRules.startTransfer(recipe.entry,r.template,r==recipe.requirements.get(0),item.getWeight(),r.unitKg);
    }
    boolean selectedRarityBoost(int tier){return currentRarity()<tier&&(nextSource!=null&&nextSource.getRarity()>=tier&&selectedTransfer(true)>0||nextTarget!=null&&nextTarget.getRarity()>=tier&&selectedTransfer(false)>0);}
    boolean storedRarityBoost(int tier){if(currentRarity()>=tier)return false;for(InventoryMetaItem item:stock)if(item.getRarity()>=tier&&storedTransfer(item)>0)return true;return false;}
    double materialUpgrade(){return rarityRoll.materialUpgrade(currentRarity(),nextSource==null?0:nextSource.getRarity(),selectedTransfer(true),nextTarget==null?0:nextTarget.getRarity(),selectedTransfer(false));}
    static String rarityName(byte rarity){return HudMessages.tr(rarity>=3?"Fantastic":rarity==2?"Supreme":"Rare");}
    static String percent(double value){return String.format(Locale.ROOT,value>0&&value<.01?"%.4f":"%.2f",value);}
    static byte rarityFromName(String name){String s=name.toLowerCase(Locale.ROOT).replaceFirst("^unfinished ","");return (byte)(s.startsWith("fantastic ")?3:s.startsWith("supreme ")?2:s.startsWith("rare ")?1:0);}
    void wiki(){if(recipe==null)return;String topic=recipe.result==null?CraftCatalog.clean(recipe.name):recipe.result.name;
        String named=CraftCatalog.clean(recipe.name);if(named.startsWith("small ")||named.startsWith("large "))topic=named;
        final String page=topic.substring(0,1).toUpperCase(Locale.ROOT)+topic.substring(1);
        Thread browse=new Thread(()->{try{String encoded=java.net.URLEncoder.encode(page.replace(' ','_'),"UTF-8").replace("+","%20");java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://www.wurmpedia.com/index.php/"+encoded));}catch(Exception failure){status=HudMessages.tr("Could not open Wiki");}},"HighResHud-Wiki");browse.setDaemon(true);browse.start();
    }
    static void icon(Queue q,Texture texture,int x,int y,int size,float brightness){if(texture!=null)Renderer.texturedQuadAlphaBlend(q,texture,brightness,brightness,brightness,1f,x,y,size,size,0f,0f,1f,1f);}
    static void text(Queue q,String text,int x,int baseline,int width,UiColor color,boolean bold){HighResRecipeWindow.draw(q,text,x,baseline,width,color,bold);}

    final class Panel extends FlexComponent {
        final ChamomiloUiV1DropDown materialChoice;
        final List<Byte> materialIds=new ArrayList<>();
        final FlexComponent previewBox=new FlexComponent("highres.build.preview",0,0,300,280){};
        final RequirementRows requirements=new RequirementRows();
        final Store materialRows=new Store(false),toolRows=new Store(true);
        final HighResCraftStoreHeader materialHeader=new HighResCraftStoreHeader("build.material.column-headings",materialRows),toolHeader=new HighResCraftStoreHeader("build.tool.column-headings",toolRows);
        final ChamomiloUiV1ScrollPanel requirementScroll,materials,tools;
        final HighResHudActionButton auto,clear,stop,build;
        final HighResHudActionButton wiki,recipes;
        String locale="";boolean configuringMaterial;
        WurmComponent pressedControl;
        Panel(){super("highres.build.panel",0,0,930,700);
            materialHeader.parent=this;toolHeader.parent=this;
            materialChoice=new ChamomiloUiV1DropDown("highres.build.material",300,new String[]{HudMessages.tr("Any material")},i->{if(!configuringMaterial&&i>=0&&i<materialIds.size()){desiredMaterial=i==0?null:materialIds.get(i);if(desiredMaterial!=null)material=desiredMaterial;chooseVariant();preview.select(recipe==null?null:recipe.result,material);pairSource=pairTarget=Long.MIN_VALUE;tick();}});materialChoice.parent=this;
            requirementScroll=new ChamomiloUiV1ScrollPanel("highres.build.requirements",requirements,false,true);requirementScroll.parent=this;HighResRecipeWindow.autoHide(requirementScroll);
            materials=new ChamomiloUiV1ScrollPanel("highres.build.materials",materialRows,false,true);materials.parent=this;HighResRecipeWindow.autoHide(materials);
            tools=new ChamomiloUiV1ScrollPanel("highres.build.tools",toolRows,false,true);tools.parent=this;HighResRecipeWindow.autoHide(tools);
            HudCaptionGroup utilities=new HudCaptionGroup(new int[]{230,150,90},32,UiScale.BASE,18,"Auto-pick up from inventory","Clear stores","Stop");
            auto=new HighResHudActionButton(this,230,32,utilities,"Auto-pick up from inventory",()->autoPick());
            clear=new HighResHudActionButton(this,150,32,utilities,"Clear stores",()->clearStores());
            stop=new HighResHudActionButton(this,90,32,utilities,"Stop",()->owner.sendAction(com.wurmonline.shared.constants.PlayerAction.STOP,new long[0]));
            HudCaptionGroup primary=new HudCaptionGroup(144,48,UiScale.BASE,26,"Build","Continue");
            build=new HighResHudActionButton(this,196,48,primary,"Build",()->HighResBuildWindow.this.build());build.leadingIcon(()->toolIcon());previewBox.parent=this;
            HudCaptionGroup navigation=new HudCaptionGroup(new int[]{273,80},32,UiScale.BASE,18,"Choose recipe","Wiki");
            wiki=new HighResHudActionButton(this,80,32,navigation,"Wiki",()->HighResBuildWindow.this.wiki());
            recipes=new HighResHudActionButton(this,273,32,navigation,"Choose recipe",()->openRecipes());
        }
        void materialOptions(){
            configuringMaterial=true;materialIds.clear();List<String> labels=new ArrayList<>();materialIds.add((byte)0);labels.add(HudMessages.tr("Any material"));
            CraftCatalog.Template result=recipe==null?null:recipe.result;
            if(!continuing()&&result!=null&&result.metal())labels.set(0,HudMessages.tr("Choose metal (required)"));
            if(continuing()&&result!=null&&result.metal()){labels.set(0,HudMessages.tr("Fixed metal: {0}",MaterialUtilities.getMaterialString(material)));desiredMaterial=null;materialChoice.setOptions(labels.toArray(new String[0]));materialChoice.selectIndex(0);materialChoice.setAvailable(false);materialChoice.setHoverString(HudMessages.tr("Metal is fixed when the unfinished item is created"));configuringMaterial=false;return;}
            for(int i=1;i<256;i++)if(result!=null&&(result.wood()&&MaterialUtilities.isWood((byte)i)||result.metal()&&MaterialUtilities.isMetal((byte)i))){materialIds.add((byte)i);labels.add(MaterialUtilities.getMaterialString((byte)i));}
            materialChoice.setOptions(labels.toArray(new String[0]));materialChoice.selectIndex(desiredMaterial==null?0:Math.max(0,materialIds.indexOf(desiredMaterial)));materialChoice.setAvailable(labels.size()>1);materialChoice.setHoverString(HudMessages.tr(result!=null&&result.metal()?"Choose a metal before building":"Only the last component of the result's material family must match; Any material removes this restriction"));configuringMaterial=false;
        }
        void refresh(){
            if(!locale.equals(HudMessages.language())){locale=HudMessages.language();materialOptions();}
            build.caption(continuing()?"Continue":"Build",ready());auto.caption("Auto-pick up from inventory",recipe!=null||continuing());clear.caption("Clear stores",!stock.isEmpty());
            wiki.caption("Wiki",recipe!=null);
            recipes.caption("Choose recipe",true);
            requirements.resizeRows();materialRows.resizeRows();toolRows.resizeRows();componentResized();
        }
        @Override void componentResized(){
            int left=Math.max(285,Math.min(340,width/3)),right=x+left+18,rightWidth=Math.max(320,width-left-28);
            wiki.setLocation(x+width-94,y+1,80,32);
            recipes.setLocation(x+10,y+5,left-12,32);
            previewBox.setLocation(x+10,y+46,left-12,242);
            materialChoice.setLocation(previewBox.x,y+323,previewBox.width,32);
            requirementScroll.setLocation(x+8,y+367,left-8,Math.max(100,height-381));requirementScroll.layout();
            materialHeader.setLocation(right,y+62,rightWidth,28);materialHeader.layoutColumns();
            materials.setLocation(right,materialHeader.y+28,rightWidth,Math.max(100,(height-246)*2/3));materials.layout();
            int toolTop=materials.y+materials.height+32;
            toolHeader.setLocation(right,toolTop,rightWidth,28);toolHeader.layoutColumns();
            tools.setLocation(right,toolTop+28,rightWidth,Math.max(62,y+height-164-toolTop-28));tools.layout();
            auto.setLocation(right,y+height-43,230,32);clear.setLocation(right+236,y+height-43,150,32);stop.setLocation(right+392,y+height-43,90,32);
            build.setLocation(x+width-204,y+height-101,196,48);
        }
        @Override public FlexComponent getComponentAt(int px,int py){if(!contains(px,py))return null;for(FlexComponent c:new FlexComponent[]{materialChoice,requirementScroll,materialHeader,toolHeader,materials,tools})if(c.contains(px,py))return c.getComponentAt(px,py);
            return this;}
        @Override protected void leftPressed(int px,int py,int count){pressedControl=null;for(WurmComponent c:new WurmComponent[]{build,auto,clear,stop,wiki,recipes})if(c.contains(px,py)){pressedControl=c;c.leftPressed(px,py,count);break;}}
        @Override protected void mouseDragged(int px,int py){if(pressedControl!=null)pressedControl.mouseDragged(px,py);}
        @Override protected void leftReleased(int px,int py){WurmComponent c=pressedControl;pressedControl=null;if(c!=null)c.leftReleased(px,py);}
        @Override protected void mouseMoved(int px,int py){for(WurmComponent c:new WurmComponent[]{build,auto,clear,stop,wiki,recipes}){if(c.contains(px,py))c.mouseMoved(px,py);else c.mouseExited();}}
        @Override protected void mouseExited(){for(WurmComponent c:new WurmComponent[]{build,auto,clear,stop,wiki,recipes})c.mouseExited();pressedControl=null;}
        @Override public void pick(PickData pick,int px,int py){
            if(recipes.contains(px,py)){pick.addText(HudMessages.tr("Open recipe browser"));return;}
            if(wiki.contains(px,py)){pick.addText(HudMessages.tr("Wiki"));return;}
            if(px>=materials.x&&py>=y+height-160&&py<y+height-50&&px<build.x){pick.addText(HudMessages.tr("Keybinder Improve model: skill, parent skill, selected component QL/damage and Epic; hidden server modifiers may change estimates"));pick.addText(HudMessages.tr("Rarity is at least the named tier, after an audible drumroll and successful step; ranges cover account status and include consumed rare components"));pick.addText(HudMessages.tr("Material rarity upgrade on the next successful step: {0}% without drumroll",percent(materialUpgrade())));if(storedRarityBoost(1)&&!selectedRarityBoost(1))pick.addText(HudMessages.tr("Rare material is stored; the displayed next-step chance follows the green selection"));pick.addText(HudMessages.tr("Next QL is the successful-step upper bound; actual skill power and hidden modifiers determine the gain"));return;}
            if(build.contains(px,py)&&!ready())pick.addText(waiting?HudMessages.tr("Waiting for action result"):HudMessages.tr("Select usable materials for one step"));
            else pick.addText(HudMessages.tr("Drop materials and tools from inventory; click to select the next step; drag to reorder; double-click to remove; right-click for item actions"));
        }
        @Override protected void renderComponent(Queue q,float ignored){
            componentResized();ChamomiloUiV1Canvas ui=new ChamomiloUiV1Canvas(this);ui.begin(q);
            materialChoice.render(q,1f);wiki.render(q,1f);recipes.render(q,1f);UiPainter.background(ui,UiBackground.WALNUT,UiScale.BASE,1f,previewBox.x,previewBox.y,previewBox.width,previewBox.height);UiPainter.frame(ui,3,1f,previewBox.x,previewBox.y,previewBox.width,previewBox.height);
            Texture image=preview.texture();if(image!=null)HighResCraftPreview.paintTexture(q,image,previewBox);
            else text(q,HudMessages.tr(recipe==null?"Select a recipe or drop an unfinished item":"Loading result model"),previewBox.x+12,previewBox.y+125,previewBox.width-24,UiColor.MUTED,false);
            String subject=unfinished!=null?unfinished.getDisplayName()+" · QL "+ql(unfinished.getQuality())+" · "+mass(unfinished.getWeight()):ground!=null?ground.getName()+" · QL "+ql(ground.getQuality())+" · "+mass(ground.getWeight()):resultName();
            text(q,subject,x+12,y+310,previewBox.width,UiColor.TEXT,true);
            text(q,HudMessages.tr("Material store"),materials.x,y+47,materials.width-90,UiColor.TEXT,true);materialHeader.render(q,1f);materials.render(q,1f);
            text(q,HudMessages.tr("Tool store"),tools.x,toolHeader.y-11,tools.width,UiColor.TEXT,true);toolHeader.render(q,1f);tools.render(q,1f);requirementScroll.render(q,1f);
            Integer estimated=continuationChance();String chance=action!=null?action.getChance():estimated==null?HudMessages.tr("Estimate unavailable"):"≈ "+estimated+"%";
            int sx=materials.x,sy=y+height-142,sw=Math.max(140,width-(sx-x)-210);
            text(q,HudMessages.tr("Step success: {0}",chance),sx,sy,sw,UiColor.TEXT,false);
            int steps=recipe==null||recipe.entry==null||!recipe.entry.advanced?0:recipe.entry.totalSteps;
            text(q,HudMessages.tr("Rare after drumroll: {0}",rarityChance(1)),sx,sy+22,sw,rarityColor(1),selectedRarityBoost(1));
            text(q,HudMessages.tr("Supreme after drumroll: {0}",rarityChance(2)),sx,sy+44,sw,rarityColor(2),selectedRarityBoost(2));
            float current=unfinished!=null?unfinished.getQuality():ground!=null?ground.getQuality():0f;
            String next=nextSource==null||steps==0||!continuing()?"—":HudMessages.tr("up to {0} on success",ql(CraftReadiness.nextQualityUpperBound(current,CraftReadiness.effectiveQuality(nextSource.getQuality(),nextSource.getDamage()),steps,CraftCatalog.clean(nextSource.getBaseName()).equals("wool"))));
            text(q,HudMessages.tr("Next QL: {0}",next),sx,sy+66,sw,UiColor.TEXT,false);
            build.render(q,1f);auto.render(q,1f);clear.render(q,1f);stop.render(q,1f);
            if(!status.isEmpty())text(q,status,x+12,y+height-14,Math.max(160,materials.x-x-24),UiColor.MUTED,false);
        }
    }
    UiColor rarityColor(int tier){return selectedRarityBoost(tier)?UiColor.rgb(0xffd96b):storedRarityBoost(tier)?UiColor.rgb(0xc8b476):UiColor.MUTED;}
    final class RequirementRows extends FlexComponent {
        final List<Object> rows=new ArrayList<>();
        RequirementRows(){super("highres.build.requirement.rows",0,0,300,1);sizeFlags=FIXED_HEIGHT;}
        void resizeRows(){rows.clear();if(recipe!=null&&recipe.result!=null&&recipe.result.wood()&&desiredMaterial!=null)rows.add(HudMessages.tr("Last wooden item should be of {0} material",MaterialUtilities.getMaterialString(desiredMaterial)));if(continuing()){rows.add("Remaining to finish");rows.addAll(remaining);}else if(recipe!=null){rows.add("Start");for(HighResCraftRecipe.Requirement r:recipe.requirements)if(r.start)rows.add(r);rows.add("Continue");for(HighResCraftRecipe.Requirement r:recipe.requirements)if(!r.start)rows.add(r);}HighResRecipeWindow.resizeContent(this,Math.max(1,panel.requirementScroll.width-20),Math.max(1,rows.size()*56));}
        @Override public void pick(PickData pick,int px,int py){int i=(py-y)/56;if(i<0||i>=rows.size())return;Object row=rows.get(i);if(row instanceof String){pick.addText(HudMessages.tr((String)row));return;}HighResCraftRecipe.Requirement r=(HighResCraftRecipe.Requirement)row;if(r.location()){pick.addText(r.name+" · "+HudMessages.tr("World target"));pick.addText(HudMessages.tr("Select this target with Add to crafting window in the world"));return;}pick.addText(requirementName(r)+" ×"+r.count+" · "+mass(r.totalKg()));pick.addText(HudMessages.tr("Stock: {0}",mass(available(r))));if(!r.tool&&available(r)<r.totalKg())pick.addText(HudMessages.tr("Insufficient mass: {0}; need {1}",mass(available(r)),mass(r.totalKg())));if(r.glowing(recipe))pick.addText(HudMessages.tr("Must be glowing"));}
        private void heading(Queue q,String value,int top){
            String label=HudMessages.tr(value);com.wurmonline.client.renderer.gui.text.TextFont font=com.wurmonline.client.renderer.gui.text.HudTextFonts.body(14,true);
            int split=label.length();while(split>0&&font.getWidth(label.substring(0,split))>width-12)split--;
            if(split<label.length()){int space=label.lastIndexOf(' ',split);if(space>0)split=space;}
            text(q,label.substring(0,split),x+6,top+21,width-12,UiColor.TEXT,true);
            if(split<label.length())text(q,label.substring(split).trim(),x+6,top+43,width-12,UiColor.TEXT,true);
        }
        @Override protected void renderComponent(Queue q,float ignored){for(int i=0;i<rows.size();i++){int top=y+i*56;if(top+56<panel.requirementScroll.y||top>panel.requirementScroll.y+panel.requirementScroll.height)continue;Object row=rows.get(i);if(row instanceof String){heading(q,(String)row,top);continue;}HighResCraftRecipe.Requirement r=(HighResCraftRecipe.Requirement)row;if(r.location()){UiColor c=count(r)>0?UiColor.TEXT:UiColor.rgb(0xf08070);text(q,r.name+" · "+HudMessages.tr("World target"),x+6,top+21,width-12,c,true);text(q,HudMessages.tr(count(r)>0?"Selected":"Not selected"),x+6,top+43,width-12,c,false);continue;}UiColor c=r.tool?count(r)>0?UiColor.TEXT:UiColor.rgb(0xf08070):CraftReadiness.enough(available(r),r.totalKg())?UiColor.TEXT:UiColor.rgb(0xf08070);
            text(q,requirementName(r)+" ×"+r.count+" · QL "+qualityRange(r),x+6,top+21,width-12,c,true);text(q,HudMessages.tr("Stock: {0} / {1}",mass(available(r)),mass(r.totalKg())),x+6,top+43,width-12,c,false);}}
    }
    final class Store extends FlexComponent {
        final boolean isTools;final List<InventoryMetaItem> rows=new ArrayList<>();final List<Object> lines=new ArrayList<>();final Set<String> collapsed=new HashSet<>();
        String query="";int sortColumn=-1,sortOrder=1,rowHeight=26,dragY,clickCount;boolean dragging;Object pressed;
        Store(boolean tools){super(tools?"highres.build.tool.rows":"highres.build.material.rows",0,0,520,1);isTools=tools;sizeFlags=FIXED_HEIGHT;}
        HighResBuildWindow owner(){return HighResBuildWindow.this;}
        HighResCraftStoreHeader header(){return isTools?panel.toolHeader:panel.materialHeader;}
        String groupKey(InventoryMetaItem item){return CraftCatalog.clean(item.getBaseName())+":"+item.getMaterialId();}
        String itemLabel(InventoryMetaItem item){return item.getDisplayName()+" · "+MaterialUtilities.getMaterialString(item.getMaterialId())+(item.getRarity()>0?" · "+rarityName(item.getRarity()):"")+(item.getTemperatureStateText().isEmpty()?"":" · "+item.getTemperatureStateText());}
        final class Group {
            final String key;final List<InventoryMetaItem> items=new ArrayList<>();
            Group(String key){this.key=key;}
            String label(){InventoryMetaItem item=items.get(0);return item.getBaseName()+" · "+MaterialUtilities.getMaterialString(item.getMaterialId())+" ("+items.size()+")";}
            float mass(){float value=0;for(InventoryMetaItem i:items)value+=i.getWeight();return value;}
            double value(int column){if(column==3)return mass();double value=0,kg=0;for(InventoryMetaItem i:items){value+=(column==1?i.getQuality():i.getDamage())*i.getWeight();kg+=i.getWeight();}return kg==0?0:value/kg;}
        }
        void sort(int column){sortOrder=sortColumn==column?-sortOrder:1;sortColumn=column;resizeRows();}
        void resizeRows(){
            rows.clear();lines.clear();Map<String,Group> grouped=new LinkedHashMap<>();
            for(InventoryMetaItem item:stock){HighResCraftRecipe.Requirement r=requirement(item);if((item.getId()!=unfinishedId&&(r==null?toolReservations.contains(item.getId()):r.tool))!=isTools||!itemLabel(item).toLowerCase(Locale.ROOT).contains(query))continue;rows.add(item);grouped.computeIfAbsent(groupKey(item),Group::new).items.add(item);}
            List<Group> groups=new ArrayList<>(grouped.values());
            if(sortColumn>=0){java.text.Collator collator=java.text.Collator.getInstance(Locale.forLanguageTag(HudMessages.language()));groups.sort((a,b)->sortOrder*(sortColumn==0?collator.compare(a.label(),b.label()):Double.compare(a.value(sortColumn),b.value(sortColumn))));for(Group g:groups)g.items.sort((a,b)->sortOrder*(sortColumn==0?collator.compare(itemLabel(a),itemLabel(b)):Double.compare(sortColumn==1?a.getQuality():sortColumn==2?a.getDamage():a.getWeight(),sortColumn==1?b.getQuality():sortColumn==2?b.getDamage():b.getWeight())));}
            for(Group g:groups)if(g.items.size()>1){lines.add(g);if(!collapsed.contains(g.key))lines.addAll(g.items);}else lines.addAll(g.items);
            rowHeight=Math.max(26,com.wurmonline.client.renderer.gui.text.HudTextFonts.body(HighResDataTypography.pixels(14),false).getHeight()+6);
            HighResRecipeWindow.resizeContent(this,Math.max(1,(isTools?panel.tools:panel.materials).width-20),Math.max(rowHeight,lines.size()*rowHeight));
        }
        Object entry(int py){if(py<y)return null;int index=(py-y)/rowHeight;return index<lines.size()?lines.get(index):null;}
        InventoryMetaItem at(int py){Object entry=entry(py);return entry instanceof Group?((Group)entry).items.get(0):entry instanceof InventoryMetaItem?(InventoryMetaItem)entry:null;}
        List<InventoryMetaItem> currentItems(Object entry){List<InventoryMetaItem> result=new ArrayList<>();for(InventoryMetaItem i:stock)if(entry instanceof Group?groupKey(i).equals(((Group)entry).key):entry instanceof InventoryMetaItem&&i.getId()==((InventoryMetaItem)entry).getId())result.add(i);return result;}
        @Override protected void leftPressed(int px,int py,int clicks){pressed=entry(py);clickCount=clicks;dragY=py;dragging=false;}
        @Override protected void mouseDragged(int px,int py){if(pressed!=null&&Math.abs(py-dragY)>5){dragging=true;dragY=py;}}
        @Override protected void leftReleased(int px,int py){
            if(pressed!=null){List<InventoryMetaItem> moving=currentItems(pressed);if(dragging){InventoryMetaItem anchor=at(py);if(!moving.contains(anchor)){stock.removeAll(moving);int at=anchor==null?stock.size():stock.indexOf(anchor);stock.addAll(Math.max(0,at),moving);sortColumn=-1;preferred.clear();preferredRequirement="";regroup();tick();}}
                else if(contains(px,py)){Object released=entry(py);if(pressed instanceof Group&&released instanceof Group&&((Group)pressed).key.equals(((Group)released).key)){String key=((Group)pressed).key;if(!collapsed.remove(key))collapsed.add(key);resizeRows();}
                    else if(!moving.isEmpty()&&released instanceof InventoryMetaItem&&moving.get(0).getId()==((InventoryMetaItem)released).getId()){
                        if(clickCount>=2&&clickCount%2==0){removeReservation(moving.get(0).getId());tick();}else selectNext(moving.get(0));}}}
            pressed=null;dragging=false;
        }
        @Override protected void rightPressed(int px,int py,int clicks){InventoryMetaItem item=at(py);if(item==null)return;owner.popupRequested(px,py,item.getDisplayName(),item.getId());}
        @Override public void pick(PickData pick,int px,int py){Object row=entry(py);if(row instanceof Group){Group g=(Group)row;pick.addText(g.label()+" · "+mass(g.mass())+" · QL "+ql((float)g.value(1))+" · DMG "+ql((float)g.value(2)));return;}InventoryMetaItem item=at(py);if(item==null){pick.addText(HudMessages.tr(isTools?"Drop tools here":"Drop materials here"));return;}pick.addText(itemLabel(item)+" · "+mass(item.getWeight())+" · QL "+ql(item.getQuality())+" · DMG "+ql(item.getDamage()));HighResCraftRecipe.Requirement r=requirement(item);String why=invalid(r,item);if(!why.isEmpty())pick.addText(why);else pick.addText(HudMessages.tr(nextItems.contains(item.getId())?"Selected for next step":"Click to use for the next step"));if(item.getRarity()>currentRarity())pick.addText(HudMessages.tr("Material rarity transfer per successful step: {0}%; no drumroll required",percent(storedTransfer(item)*100)));}
        void cell(Queue q,String value,int column,int baseline,UiColor color,boolean bold){int[] widths=header().columns;int left=x;for(int n=0;n<column;n++)left+=widths[n];com.wurmonline.client.renderer.gui.text.TextFont font=com.wurmonline.client.renderer.gui.text.HudTextFonts.body(HighResDataTypography.pixels(14),bold);String shown=HighResWindowIntegration.ellipsis(value,font,Math.max(0,widths[column]-10));font.moveTo(left+Math.max(4,widths[column]-6-font.getWidth(shown)),baseline);font.paint(q,shown,color.red,color.green,color.blue,1f);}
        @Override protected void renderComponent(Queue q,float ignored){ChamomiloUiV1Canvas ui=new ChamomiloUiV1Canvas(this);ui.begin(q);if(lines.isEmpty())text(q,HudMessages.tr(isTools?"Drop tools here":"Drop materials here"),x+12,y+rowHeight-5,width-24,UiColor.MUTED,false);
            for(int i=0;i<lines.size();i++){int top=y+i*rowHeight;WurmScrollPanel viewport=isTools?panel.tools:panel.materials;if(top+rowHeight<viewport.y||top>viewport.y+viewport.height)continue;Object row=lines.get(i);int baseline=top+rowHeight-5;
                if(row instanceof Group){Group group=(Group)row;boolean chosenGroup=false;for(InventoryMetaItem item:group.items)if(nextItems.contains(item.getId())&&invalid(requirement(item),item).isEmpty())chosenGroup=true;ui.fill(UiColor.rgb(chosenGroup&&collapsed.contains(group.key)?0x285c35:0x30291e),1f,x,top,width,rowHeight-1);(collapsed.contains(group.key)?UiIcon.CHEVRON_RIGHT:UiIcon.CHEVRON_DOWN).paint(ui,UiColor.MUTED,1f,x+5,top+(rowHeight-10)/2,10);text(q,group.label(),x+20,baseline,header().columns[0]-26,UiColor.TEXT,true);cell(q,ql((float)group.value(1)),1,baseline,UiColor.TEXT,true);cell(q,ql((float)group.value(2)),2,baseline,UiColor.TEXT,true);cell(q,mass(group.mass()),3,baseline,UiColor.TEXT,true);continue;}
                InventoryMetaItem item=(InventoryMetaItem)row;String why=invalid(requirement(item),item);boolean chosen=nextItems.contains(item.getId());ui.fill(UiColor.rgb(!why.isEmpty()?0x672b26:chosen?0x285c35:i%2==0?0x29231b:0x211c17),1f,x,top,width,rowHeight-1);
                if(chosen&&why.isEmpty())ui.fill(UiColor.rgb(0x84e397),1f,x,top,3,rowHeight-1);
                UiColor color=!why.isEmpty()?UiColor.rgb(0xffa99c):item.getRarity()>0?UiColor.rgb(0xffd96b):UiColor.TEXT;icon(q,IconLoader.getIcon(item.getType()),x+16,top+(rowHeight-20)/2,20,1f);text(q,itemLabel(item),x+40,baseline,header().columns[0]-46,color,chosen);cell(q,ql(item.getQuality()),1,baseline,color,chosen);cell(q,ql(item.getDamage()),2,baseline,color,chosen);cell(q,mass(item.getWeight()),3,baseline,color,chosen);
            }if(dragging)ui.fill(UiColor.rgb(0x9acb76),1f,x,dragY,width,2);
        }
    }
}
