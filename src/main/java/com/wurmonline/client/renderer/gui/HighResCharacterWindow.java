package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.PlayerBodyRenderable;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.renderer.gui.text.HudTextFonts;
import com.wurmonline.client.resources.textures.Texture;
import com.wurmonline.shared.constants.PlayerAction;
import java.util.*;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.HudMessages;
import org.highreshud.client.ui.HudCaptionGroup;

/** Presentation and shared geometry; Wurm retains inventory listeners and all item actions. */
final class HighResCharacterWindow extends HighResNativeWindow {
    static final UiScale SCALE = new UiScale(1.25f);
    static final int WIDTH = 410, HEIGHT = 668, SLOT = 55;
    final PaperDollInventory character;
    final HighResHudActionButton model, zones;
    final ChamomiloUiV1CompactButton unequip;
    private long equipmentCaptionRevision=-1;
    final ChamomiloUiV1CompactButton previousWounds,nextWounds;
    private static final int WOUNDS_PER_PAGE=7;
    private int woundPage;
    private byte woundPart=-1;
    private final PortraitPoseController pose;
    private boolean modelPressed,modelDragged;
    private int modelPressX,modelPressY;
    private final HudCaptionGroup views = new HudCaptionGroup(125, 28, UiScale.BASE, 18, "Model", "Body zones");
    private final List<Part> parts = Arrays.asList(
            new Part(2,"Head",144,100,39,45), new Part(3,"Torso",123,145,78,74),
            new Part(4,"Legs",124,219,74,151), new Part(5,"Left arm",189,150,28,93),
            new Part(6,"Right arm",109,149,23,94), new Part(7,"Left hand",204,243,23,40),
            new Part(8,"Right hand",107,243,16,39), new Part(9,"Left foot",164,369,38,28),
            new Part(10,"Right foot",130,369,34,28), new Part(25,"Face",153,114,22,20));
    static final class Part {
        final byte id; final String name; final int x,y,w,h;
        Part(int id,String name,int x,int y,int w,int h) { this.id=(byte)id;this.name=name;this.x=x;this.y=y;this.w=w;this.h=h; }
    }
    HighResCharacterWindow(PaperDollInventory character, HeadsUpDisplay hud) {
        super("highres.character", "Character", character, hud); this.character=character;
        pose=new PortraitPoseController(((Number)HighResWindowAccess.get(character,"rotation")).floatValue(),new Random(),System.nanoTime());
        model=new HighResHudActionButton(character,125,28,views,"Model",()->view(true));
        zones=new HighResHudActionButton(character,125,28,views,"Body zones",()->view(false));
        unequip=new ChamomiloUiV1CompactButton(HudMessages.tr("Unequip all"),200,UiScale.BASE,this::unequipAll);
        unequip.parent=character;
        previousWounds=new ChamomiloUiV1CompactButton(UiIcon.CHEVRON_UP,24,UiScale.BASE,()->{woundPage--;layoutWounds();});
        nextWounds=new ChamomiloUiV1CompactButton(UiIcon.CHEVRON_DOWN,24,UiScale.BASE,()->{woundPage++;layoutWounds();});
        previousWounds.parent=nextWounds.parent=character;
    }
    @Override void install() {
        super.install();
        character.setSize(WIDTH,HEIGHT);
        HighResWindowAccess.set(character,"showPlayer",true);
        tick();
    }
    @Override void tick() {
        updatePose(System.nanoTime());
        header.refresh();
        model.setPosition(px(59),py(76)); zones.setPosition(model.x+129,model.y);
        if(equipmentCaptionRevision!=HudMessages.revision()) {
            String caption=HudMessages.tr("Unequip all");
            UiCompactLayout layout=UiCompactLayout.group(new String[]{caption},new int[]{200},UiScale.BASE,true)[0];
            unequip.applyLayout(caption,layout);
            unequip.setHoverString(HudMessages.tr("Unequip all armor"));
            equipmentCaptionRevision=HudMessages.revision();
        }
        unequip.setPosition(px(59)+(SCALE.pixels(205)-unequip.width)/2,character.y+HEIGHT-32);
        unequip.setEnabled(bodyTarget()!=null);
        for(PaperDollSlot slot:slots().values()) {
            Object definition=definition(slot.getEquipmentSlot());
            if(definition==null)continue;
            slot.setPosition(px(number(definition,"xPosition")),py(number(definition,"yPosition")));
            size(slot,SLOT,SLOT);
            PaperDollItem item=slot.getEquippedItem();
            if(item!=null) { size(item,40,40);item.setPosition(slot.getXPosition()+7,slot.getYPosition()+7); }
            Part part=part(slot.getEquipmentSlot());
            if(part!=null) {
                Object button=HighResWindowAccess.get(slot,"woundbutton");
                HighResWindowAccess.set(button,"x",px(part.x));HighResWindowAccess.set(button,"y",py(part.y));
                size(button,SCALE.pixels(part.w),SCALE.pixels(part.h));
            }
        }
        corner("equippedWeightItem",7,23,34,58);
        corner("bodyItem",7,412,34,58);
        corner("workingHandItem",282,418,38,50);
        layoutWounds();
    }
    private InventoryMetaItem bodyTarget() {
        PaperDollSlot body=(PaperDollSlot)HighResWindowAccess.get(character,"bodyItem");
        return body==null?null:body.getItem();
    }
    private void unequipAll() {
        InventoryMetaItem body=bodyTarget();
        // The body target makes the stock Unequip action remove all armor.
        if(body!=null)owner.sendAction(PlayerAction.UNEQUIP_ITEM,body.getId());
    }
    void updatePose(long now) {
        float rotation=((Number)HighResWindowAccess.get(character,"rotation")).floatValue();
        if(modelPressed && rotation!=pose.rotation())modelDragged=true;
        if(modelPressed || rotation!=pose.rotation() || !modelView() || character.minimized || !owner.getComponents().contains(character))
            pose.setRotation(rotation,now);
        if(modelPressed || !modelView() || character.minimized || !owner.getComponents().contains(character))return;
        pose.tick(now);
        HighResWindowAccess.set(character,"rotation",pose.rotation());
    }
    void modelPressed(int x,int y) {
        if(!(Boolean)HighResWindowAccess.get(character,"leftClickedAtPlayer"))return;
        modelPressed=true;modelDragged=false;modelPressX=x;modelPressY=y;
        pose.setRotation(((Number)HighResWindowAccess.get(character,"rotation")).floatValue(),System.nanoTime());
    }
    void modelReleased(int x,int y) {
        if(!modelPressed)return;
        modelPressed=false;
        long now=System.nanoTime();
        float rotation=((Number)HighResWindowAccess.get(character,"rotation")).floatValue();
        boolean dragged=modelDragged || rotation!=pose.rotation() || Math.abs(x-modelPressX)>4 || Math.abs(y-modelPressY)>4;
        pose.setRotation(rotation,now);
        if(!dragged)pose.randomize(now);
    }
    private void corner(String name,int x,int y,int w,int h) {
        PaperDollSlot slot=(PaperDollSlot)HighResWindowAccess.get(character,name);
        if(slot!=null) {slot.setPosition(px(x),py(y));size(slot,SCALE.pixels(w),SCALE.pixels(h));}
    }
    private static void size(Object value,int w,int h) {
        if(number(value,"width")!=w)HighResWindowAccess.set(value,"width",w);
        if(number(value,"height")!=h)HighResWindowAccess.set(value,"height",h);
    }
    static int number(Object value,String name) { return ((Number)HighResWindowAccess.get(value,name)).intValue(); }
    int px(int x) {return character.x+Math.max(0,(character.width-WIDTH)/2)+FRAME+SCALE.pixels(x-3);}
    int py(int y) {return character.y+TITLE_HEIGHT+SCALE.pixels(y-21);}
    @SuppressWarnings("unchecked") Map<Long,PaperDollSlot> slots() {return (Map<Long,PaperDollSlot>)HighResWindowAccess.get(character,"frameList");}
    @SuppressWarnings("unchecked") private List<PaperDollItem> wounds(PaperDollSlot slot) {return (List<PaperDollItem>)HighResWindowAccess.get(slot,"woundList");}
    private Object definition(byte id) {
        for(Object definition:(List<?>)HighResWindowAccess.get(character,"slotList"))if(number(definition,"id")==id)return definition;
        return null;
    }
    private Part part(byte id) {for(Part part:parts)if(part.id==id)return part;return null;}
    private PaperDollSlot slot(byte id) {for(PaperDollSlot slot:slots().values())if(slot.getEquipmentSlot()==id)return slot;return null;}
    boolean modelView() {return (Boolean)HighResWindowAccess.get(character,"showPlayer");}
    void view(boolean modelView) {
        modelPressed=false;
        woundPart=-1;woundPage=0;
        HighResWindowAccess.set(character,"showPlayer",modelView);
        HighResWindowAccess.set(character,"leftClickedAtPlayer",false);
        for(PaperDollSlot slot:slots().values())slot.leftReleased();
        HighResWindowAccess.set(character,"lastPressedWoundSlot",(byte)-1);
    }
    WurmComponent componentAt(int x,int y) {
        tick();
        if(y<header.y+header.height)return header.getComponentAt(x,y);
        if(model.contains(x,y))return model;
        if(zones.contains(x,y))return zones;
        if(unequip.contains(x,y))return unequip;
        if(!modelView())for(PaperDollSlot slot:slots().values())if(slot.isWoundButtonOpen()&&wounds(slot).size()>WOUNDS_PER_PAGE) {
            if(previousWounds.contains(x,y))return previousWounds;
            if(nextWounds.contains(x,y))return nextWounds;
        }
        return character;
    }
    int position(byte id,boolean vertical,boolean wound) {
        if(wound) {Part part=part(id);return part==null?0:vertical?py(part.y):px(part.x);}
        Object definition=definition(id);
        return definition==null?(vertical?character.y:character.x):vertical?py(number(definition,"yPosition")):px(number(definition,"xPosition"));
    }
    byte hit(int x,int y,boolean wound) {
        tick();
        if(wound) {
            if(modelView())return -1;
            Part face=part((byte)25);
            if(inside(x,y,px(face.x),py(face.y),SCALE.pixels(face.w),SCALE.pixels(face.h)))return face.id;
            for(Part part:parts)if(inside(x,y,px(part.x),py(part.y),SCALE.pixels(part.w),SCALE.pixels(part.h)))return part.id;
        } else for(PaperDollSlot slot:slots().values())if(slot.checkIfHovered(x,y))return slot.getEquipmentSlot();
        return -1;
    }
    boolean playerHit(int x,int y) {return modelView()&&inside(x,y,px(59),py(100),SCALE.pixels(205),SCALE.pixels(297));}
    static boolean inside(int x,int y,int left,int top,int width,int height) {return x>=left&&x<left+width&&y>=top&&y<top+height;}
    private void layoutWounds() {
        for(PaperDollSlot slot:slots().values())if(slot.isWoundButtonOpen()) {
            if(woundPart!=slot.getEquipmentSlot()) {woundPart=slot.getEquipmentSlot();woundPage=0;}
            woundPage=Math.max(0,Math.min(woundPage,Math.max(0,(wounds(slot).size()-1)/WOUNDS_PER_PAGE)));
            int top=py(171), left=px(65);
            int index=0;
            for(PaperDollItem wound:wounds(slot)) {
                size(wound,32,32);
                boolean visible=index>=woundPage*WOUNDS_PER_PAGE&&index<(woundPage+1)*WOUNDS_PER_PAGE;
                wound.setPosition(visible?left+6:-10000,visible?top+6:-10000);
                if(visible)top+=46;index++;
            }
            previousWounds.setPosition(left+191,py(150)+2);nextWounds.setPosition(left+217,py(150)+2);
            previousWounds.setEnabled(woundPage>0);nextWounds.setEnabled((woundPage+1)*WOUNDS_PER_PAGE<wounds(slot).size());
            previousWounds.setHoverString(HudMessages.tr("Previous wounds"));nextWounds.setHoverString(HudMessages.tr("Next wounds"));
        }
    }
    PaperDollItem woundAt(int x,int y) {
        if(modelView())return null;
        layoutWounds();
        for(PaperDollSlot slot:slots().values())if(slot.isWoundButtonOpen())for(PaperDollItem wound:wounds(slot))
            if(wound.getXPosition()>0&&inside(x,y,px(65),wound.getYPosition()-6,245,46))return wound;
        return null;
    }
    @Override void render(Queue queue) {
        tick();
        UiPainter.background(canvas.begin(queue),UiBackground.LEATHER,UiScale.BASE,1f,character.x,character.y,character.width,character.height);
        if(!character.minimized) {
            UiPainter.background(canvas,UiBackground.SOLID,UiScale.BASE,1f,px(59),py(100),SCALE.pixels(205),SCALE.pixels(297));
            if(modelView())renderModel(queue); else renderParts(queue);
            for(PaperDollSlot slot:slots().values())renderSlot(queue,slot);
            renderCorner(queue,"equippedWeightItem");
            renderCorner(queue,"bodyItem");
            renderCorner(queue,"workingHandItem");
            model.render(queue,1f);zones.render(queue,1f);
            canvas.fill(UiColor.rgb(0xb4a578),1f,(modelView()?model:zones).x+8,model.y+25,109,2);
            int count=0; for(PaperDollSlot slot:slots().values())count+=wounds(slot).size();
            text(queue,HudMessages.tr("Wounds: {0}",count),px(59),py(474),UiColor.TEXT,14);
            if(!modelView()) {
                for(int i=0;i<5;i++)canvas.fill(zoneColor(i==0?0:i==1?1:(i-1)*20),1f,px(59)+i*52,py(479),42,4);
                text(queue,HudMessages.tr("Healthy → Critical"),px(59),py(498),UiColor.MUTED,14);
                renderWoundList(queue);
            } else text(queue,HudMessages.tr("Drag to rotate"),px(59),py(495),UiColor.MUTED,14);
            unequip.render(queue,1f);
        }
        header.render(queue,1f);UiPainter.frame(canvas,FRAME,1f,character.x,character.y,character.width,character.height);
    }
    private void renderModel(Queue queue) {
        PlayerBodyRenderable body=(PlayerBodyRenderable)HighResWindowAccess.get(character,"playerBody");
        if(body==null)return;
        PaperDollRenderer renderer=owner.getPaperDollRendererInventory();
        if(renderer==null)return;
        int w=SCALE.pixels(205),h=SCALE.pixels(297);
        renderer.requestRender(body,((Number)HighResWindowAccess.get(character,"rotation")).floatValue(),w*2,h*2,false);
        Texture texture=renderer.getTexture();
        if(texture!=null)Renderer.texturedQuadAlphaBlend(queue,texture,1f,1f,1f,1f,px(59),py(100),w,h,0f,1f,1f,-1f);
    }
    private void renderSlot(Queue queue,PaperDollSlot slot) {
        int x=slot.getXPosition(),y=slot.getYPosition();
        UiPainter.background(canvas,UiBackground.SOLID,UiScale.BASE,1f,x,y,SLOT,SLOT);
        PaperDollItem item=slot.getEquippedItem();
        UiColor color=slot.isAvailable()?UiColor.TEXT:UiColor.MUTED;
        if(item!=null) {
            item.render(queue,1f,1f,1f,1f);
            float damage=item.getDamage();
            if(damage>0)canvas.fill(damageColor(damage),1f,x+5,y+SLOT-8,Math.max(2,Math.round((SLOT-10)*Math.min(100,damage)/100)),3);
            if(item.getId()==owner.getActiveToolItemId())color=UiColor.rgb(0xfff09a);
        } else HighResCharacterArtwork.slot(queue,slot.getEquipmentSlot(),slot.isAvailable(),x+5,y+5);
        UiPainter.frame(canvas,3,1f,x,y,SLOT,SLOT);
        if(item!=null&&item.getId()==owner.getActiveToolItemId())canvas.fill(color,1f,x+6,y+SLOT-5,SLOT-12,2);
        if(slot.hasWounds()) {
            canvas.fill(damageColor(slot.getTotalWoundDamage()),1f,x+SLOT-14,y+5,9,9);
            text(queue,Integer.toString(wounds(slot).size()),x+SLOT-15,y+27,UiColor.TEXT,12);
        }
    }
    private void renderCorner(Queue queue,String field) {
        PaperDollSlot slot=(PaperDollSlot)HighResWindowAccess.get(character,field);
        int x=field.equals("workingHandItem")?px(282):px(7),y=field.equals("bodyItem")?py(412):field.equals("workingHandItem")?py(418):py(23);
        int w=field.equals("workingHandItem")?SCALE.pixels(38):SCALE.pixels(34),h=field.equals("workingHandItem")?SCALE.pixels(50):SCALE.pixels(58);
        boolean active=!field.equals("equippedWeightItem") && cornerActive(field);
        UiColor tint=active?UiColor.rgb(0xfff09a):new UiColor(1f,1f,1f);
        if(field.equals("equippedWeightItem")) {
            HighResCharacterArtwork.icon(queue,HighResCharacterArtwork.WEIGHT,tint,x+(w-34)/2,y+6,34,38);
            InventoryMetaItem inventory=(InventoryMetaItem)HighResWindowAccess.get(character,"inventoryItem");
            float total=(slot==null?0:slot.getWeight())+(inventory==null?0:inventory.getWeight());
            String value=String.format(Locale.ROOT,"%.1f",total);
            TextFont font=HudTextFonts.body(12,false);
            text(queue,value,x+(w-font.getWidth(value))/2,y+h-11,UiColor.TEXT,12);
        } else if(field.equals("bodyItem"))
            HighResCharacterArtwork.icon(queue,HighResCharacterArtwork.BODY,tint,x+(w-35)/2,y+(h-60)/2,35,60);
        else HighResCharacterArtwork.icon(queue,HighResCharacterArtwork.HAND,tint,x+(w-38)/2,y+6,38,44);
        if(active)canvas.fill(UiColor.rgb(0xfff09a),1f,x+5,y+h-7,w-10,3);
    }
    boolean cornerActive(String field) {
        PaperDollSlot slot=(PaperDollSlot)HighResWindowAccess.get(character,field);
        return slot!=null && slot.getItem()!=null && slot.getItem().getId()==owner.getActiveToolItemId();
    }
    static UiColor damageColor(float damage) {
        float value=Float.isFinite(damage)?Math.max(0,Math.min(60,damage)):0;
        int[] stops={0x668957,0xc9b856,0xce813a,0xb84437};
        int step=Math.min(2,(int)(value/20));float fraction=(value-step*20)/20;
        int a=stops[step],b=stops[step+1];
        return new UiColor((((a>>16)&255)*(1-fraction)+((b>>16)&255)*fraction)/255f,
                (((a>>8)&255)*(1-fraction)+((b>>8)&255)*fraction)/255f,((a&255)*(1-fraction)+(b&255)*fraction)/255f);
    }
    static UiColor zoneColor(float damage) {
        return !Float.isFinite(damage)||damage<=0?new UiColor(1f,1f,1f):damageColor(damage);
    }
    private void renderParts(Queue queue) {
        int left=px(59),top=py(100),width=SCALE.pixels(205),height=SCALE.pixels(297);
        HighResCharacterArtwork.icon(queue,HighResCharacterArtwork.ANATOMY,zoneColor(0),left,top,width,height);
        for(Part part:parts) {
            PaperDollSlot slot=slot(part.id);float damage=slot==null?0:slot.getTotalWoundDamage();
            int x=px(part.x),y=py(part.y),w=SCALE.pixels(part.w),h=SCALE.pixels(part.h);
            if(part.id==25&&damage<=0)continue;
            HighResCharacterArtwork.draw(queue,HighResCharacterArtwork.ANATOMY,zoneColor(damage),x,y,w,h,
                    (x-left)/(float)width,(y-top)/(float)height,(x+w-left)/(float)width,(y+h-top)/(float)height);
            if(slot!=null&&slot.hasWounds()) {
                int cx=x+w/2,cy=part.id==4?y+16:y+h/2;
                canvas.fill(UiColor.TEXT,1f,cx-6,cy-2,12,4);canvas.fill(UiColor.TEXT,1f,cx-2,cy-6,4,12);
                text(queue,String.format(Locale.ROOT,"%.0f",damage),cx-10,cy+24,UiColor.TEXT,14);
            }
        }
    }
    private void renderWoundList(Queue queue) {
        for(PaperDollSlot slot:slots().values())if(slot.isWoundButtonOpen()&&slot.hasWounds()) {
            int x=px(65),y=py(150),w=245,h=26+Math.min(WOUNDS_PER_PAGE,wounds(slot).size()-woundPage*WOUNDS_PER_PAGE)*46;
            UiPainter.background(canvas,UiBackground.LEATHER,UiScale.BASE,1f,x,y,w,h);
            Part part=part(slot.getEquipmentSlot());text(queue,HudMessages.tr(part==null?"Wounds":part.name),x+8,y+18,UiColor.TEXT,14);
            for(PaperDollItem wound:wounds(slot)) {
                if(wound.getXPosition()<0)continue;
                wound.render(queue,1f,1f,1f,1f);
                String label=wound.getItemName()+" · "+String.format(Locale.ROOT,"%.1f",wound.getDamage());
                text(queue,HighResWindowIntegration.ellipsis(label,HudTextFonts.body(14,false),w-54),x+46,wound.getYPosition()+14,UiColor.TEXT,14);
                String treatment=(wound.isBandaged()?HudMessages.tr("Bandaged"):"")+(wound.isTreated()?" "+HudMessages.tr("Treated"):"");
                text(queue,treatment,x+46,wound.getYPosition()+30,UiColor.MUTED,12);
            }
            UiPainter.frame(canvas,3,1f,x,y,w,h);
            if(wounds(slot).size()>WOUNDS_PER_PAGE) {previousWounds.render(queue,1f);nextWounds.render(queue,1f);}
        }
    }
    private static void text(Queue queue,String value,int x,int y,UiColor color,int pixels) {
        TextFont font=HudTextFonts.body(pixels,false);font.moveTo(x,y);font.paint(queue,value,color.red,color.green,color.blue,1f);
    }
    boolean pick(PickData pick,int x,int y) {
        if(woundAt(x,y)!=null)return false;
        byte id=hit(x,y,true);Part part=part(id);
        if(part==null)return false;
        PaperDollSlot slot=slot(id);pick.addText(HudMessages.tr(part.name));
        pick.addText(HudMessages.tr("Wounds: {0}",slot==null?0:wounds(slot).size()));
        if(slot!=null&&slot.hasWounds())pick.addText(HudMessages.tr("Damage: {0}%",String.format(Locale.ROOT,"%.1f",slot.getTotalWoundDamage())));
        return true;
    }
}
