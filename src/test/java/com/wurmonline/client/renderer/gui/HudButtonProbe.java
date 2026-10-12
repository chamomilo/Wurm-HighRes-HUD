package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import javax.imageio.ImageIO;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.ui.HudCaptionGroup;
import org.highreshud.client.ui.HudSkin;
import static org.highresfightinghud.client.FightingHudLayout.*;

/** Actual compact button paint/input plus a production-painter layout contact sheet. */
public final class HudButtonProbe {
    public static PreviewCanvas canvas;
    static Graphics2D graphics;
    private static final java.util.LinkedList<Shape> clips = new java.util.LinkedList<>();
    public static boolean clip(int x, int y, int width, int height) {
        clips.addLast(graphics.getClip()); graphics.clipRect(x, y, Math.max(0, width), Math.max(0, height));
        return width > 0 && height > 0;
    }
    public static void unclip() { graphics.setClip(clips.removeLast()); }
    private static float textAlpha;
    private static BufferedImage actionAtlas;
    private static final java.util.Map<Integer,BufferedImage> characterImages=new java.util.HashMap<>();
    private static final java.util.List<float[]> iconDraws=new java.util.ArrayList<>();
    private static float[] combatIcon;
    public static com.wurmonline.client.game.World world;
    public static String actionString = "";
    public static org.chamomilo.wurm.update.UpdatePreferences updaterPreferences;
    private static final java.util.List<PaintedText> texts = new java.util.ArrayList<>();
    private static final class PaintedText {
        final String value; final Rectangle ink; final int baseline;
        PaintedText(String value, Rectangle ink, int baseline) { this.value=value; this.ink=ink; this.baseline=baseline; }
    }

    public static void combatIconQuad(float r,float g,float b,float a,float x,float y,float w,float h) {
        combatIcon=new float[]{r,g,b,a,x,y,w,h};
        UiIcon.CHECK.paint(canvas,new UiColor(r,g,b),a,Math.round(x),Math.round(y),Math.round(w));
    }

    public static void atlasQuad(float r,float g,float b,float a,float x,float y,float w,float h,float u,float v,float uw,float vh) {
        iconDraws.add(new float[]{x,y,w,h,u,v,uw,vh});
        check(a==1f,"Quick action icons keep fixed opacity");
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.drawImage(actionAtlas,Math.round(x),Math.round(y),Math.round(x+w),Math.round(y+h),
                Math.round(u*512),Math.round(v*512),Math.round((u+uw)*512),Math.round((v+vh)*512),null);
    }
    public static void characterArtwork(Queue queue,int asset,UiColor tint,int x,int y,int width,int height,float u0,float v0,float u1,float v1) throws Exception {
        if(asset==1)anatomyTints.add(new float[]{tint.red,tint.green,tint.blue});
        BufferedImage source=characterImages.get(asset);
        if(source==null) {
            String[] names={"slots","anatomy","weight","body","hand"};
            source=ImageIO.read(new File("resource-pack/gui/highres-character-"+names[asset]+".png"));
            characterImages.put(asset,source);
        }
        BufferedImage image=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int py=0;py<image.getHeight();py++)for(int px=0;px<image.getWidth();px++) {
            int color=source.getRGB(px,py);
            image.setRGB(px,py,(color&0xff000000)|(Math.round(((color>>16)&255)*tint.red)<<16)
                    |(Math.round(((color>>8)&255)*tint.green)<<8)|Math.round((color&255)*tint.blue));
        }
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.drawImage(image,x,y,x+width,y+height,Math.round(u0*image.getWidth()),Math.round(v0*image.getHeight()),
                Math.round(u1*image.getWidth()),Math.round(v1*image.getHeight()),null);
    }
    public static final java.util.List<float[]> anatomyTints=new java.util.ArrayList<float[]>();

    public static final class ProbeFont extends TextFont {
        final int size; final boolean bold; final UiDensity density; int x,y;
        public ProbeFont(int size,boolean bold) { this(size,bold,UiDensity.HIGH); }
        public ProbeFont(int size,boolean bold,UiDensity density) { this.size=size;this.bold=bold;this.density=density; }
        public void moveTo(int x,int y) {this.x=x;this.y=y;}
        public int paint(Queue q,String s,float r,float g,float b,float a) {
            if(InventoryOpacityPreview.captures(q)) {InventoryOpacityPreview.text(q,this,s,r,g,b,a);return getWidth(s);}
            Rectangle ink=UiTypography.font(size,bold,density).createGlyphVector(
                    new java.awt.font.FontRenderContext(null,true,false),s).getPixelBounds(null,x,y);
            texts.add(new PaintedText(s,ink,y));
            textAlpha=a;graphics.setFont(UiTypography.font(size,bold,density));
            graphics.setComposite(AlphaComposite.SrcOver);graphics.setColor(new Color(r,g,b,a));
            graphics.drawString(s,x,y);return getWidth(s);
        }
        public int getWidth(String s) {return metrics().stringWidth(s);}
        public int getWidth(char[] s,int offset,int count) {return getWidth(new String(s,offset,count));}
        public int getHeight() {return metrics().getHeight();}
        private FontMetrics metrics() { return new java.awt.Canvas().getFontMetrics(UiTypography.font(size,bold,density)); }
        public int getAscent() {return metrics().getAscent();}
        public int getDescent() {return metrics().getDescent();}
        public int getLeading() {return 0;}
    }
    public static final class PreviewCanvas implements UiCanvas {
        final EnumMap<UiAsset,BufferedImage> images=new EnumMap<>(UiAsset.class);
        boolean fixedAlpha=true;
        PreviewCanvas() throws Exception {
            for(UiAsset asset:UiAsset.values())
                images.put(asset,ImageIO.read(UiResources.openResource(asset.path())));
        }
        public void fill(UiColor c,float a,int x,int y,int w,int h) {
            if(w<=0||h<=0)return;
            graphics.setComposite(AlphaComposite.SrcOver);graphics.setColor(new Color(c.red,c.green,c.blue,a));
            graphics.fillRect(x,y,w,h);
        }
        public boolean texture(UiAsset asset,float tint,float a,int x,int y,int w,int h,float u0,float v0,float u1,float v1) {
            fixedAlpha &= a==1.0f;
            BufferedImage source=images.get(asset),image=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
            for(int py=0;py<image.getHeight();py++)for(int px=0;px<image.getWidth();px++) {
                int color=source.getRGB(px,py);
                image.setRGB(px,py,(color&0xff000000)|(Math.round(((color>>16)&255)*tint)<<16)
                        |(Math.round(((color>>8)&255)*tint)<<8)|Math.round((color&255)*tint));
            }
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,a));
            graphics.drawImage(image,x,y,x+w,y+h,Math.round(u0*image.getWidth()),Math.round(v0*image.getHeight()),
                    Math.round(u1*image.getWidth()),Math.round(v1*image.getHeight()),null);
            return true;
        }
    }
    private static void check(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
    private static void label(String s,int x,int y,int size) {
        graphics.setComposite(AlphaComposite.SrcOver);graphics.setFont(UiTypography.font(size,false,UiDensity.LOW));
        graphics.setColor(new Color(243,232,196));graphics.drawString(s,x,y);
    }
    private static void button(HighResHudActionButton button,int x,int y,String caption,boolean enabled,boolean hover) {
        button.setLocation(x,y,button.width,button.height);button.caption(caption,enabled);
        check(button.x==x && button.y==y,"Native button keeps its requested position");
        if(hover)button.mouseMoved(x+1,y+1);else button.mouseExited();
        button.renderComponent(null,.15f);
    }
    private static void gauge(int x,int y,int w,int h,float value,int rgb) {
        gauge(new UiRect(x,y,w,h),value,rgb);
    }
    private static void gauge(UiRect cell,float value,int rgb) {
        HudSkin.gauge(canvas,cell,value,UiColor.rgb(rgb));HudSkin.glass(canvas,cell);
    }
    private static void field(Object target,String name,Object value) throws Exception {
        java.lang.reflect.Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    private static void nativeGaugeChecks() throws Exception {
        java.lang.reflect.Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        sun.misc.Unsafe unsafe=(sun.misc.Unsafe)f.get(null);
        HighResHealthBar health=(HighResHealthBar)unsafe.allocateInstance(HighResHealthBar.class);
        field(health,"ui",new ChamomiloUiV1Canvas(health));field(health,"headerLayout",HeaderLayout.hidden());
        field(health,"previous",new float[3]);field(health,"spentFlashUntil",new long[9]);
        HighResFocusBar select=(HighResFocusBar)unsafe.allocateInstance(HighResFocusBar.class);
        field(select,"ui",new ChamomiloUiV1Canvas(select));
        HighResFightingHud fighter=(HighResFightingHud)unsafe.allocateInstance(HighResFightingHud.class);
        field(fighter,"ui",new ChamomiloUiV1Canvas(fighter));
        org.highresfocusbar.client.HighResFocusBarSettings.animateGaugeShine=false;
        org.highresfightinghud.client.HighResFightingHudSettings.animateGaugeShine=false;
        java.lang.reflect.Method healthPaint=HighResHealthBar.class.getDeclaredMethod("drawSolidGauge",Queue.class,
                int.class,int.class,int.class,int.class,float.class,float.class,float.class,float.class,
                int.class,float.class,float.class,float.class);healthPaint.setAccessible(true);
        java.lang.reflect.Method selectPaint=HighResFocusBar.class.getDeclaredMethod("renderGauge",Queue.class,
                int.class,int.class,int.class,int.class,float.class,float.class,float.class,float.class);selectPaint.setAccessible(true);
        java.lang.reflect.Method fighterPaint=HighResFightingHud.class.getDeclaredMethod("renderGauge",Queue.class,
                int.class,int.class,int.class,int.class,float.class,float.class,float.class,float.class);fighterPaint.setAccessible(true);
        Graphics2D main=graphics;
        try {
            for(int kind=0;kind<3;kind++)for(float value:new float[]{0,.5f,1}) {
                boolean isHealth=kind==0;
                UiRect well=isHealth?HudSkin.healthGauge(0,0,8):kind==1?HudSkin.selectHealth(0,0):HudSkin.targetCell(0,0,false,1);
                BufferedImage actual=new BufferedImage(438,108,BufferedImage.TYPE_INT_ARGB);
                graphics=actual.createGraphics();
                if(isHealth) {
                    float[] previous=new float[9];java.util.Arrays.fill(previous,-1f);field(health,"previousGaugeValues",previous);
                    // Deliberately wrong legacy bounds: the main gauge must resolve the grid cell itself.
                    healthPaint.invoke(health,null,114,86,200,12,value,.1f,.6f,.2f,8,0f,0f,0f);
                } else (kind==1?selectPaint:fighterPaint).invoke(kind==1?select:fighter,null,
                        well.x,well.y,well.width,well.height,value,.1f,.6f,.2f);
                graphics.dispose();
                BufferedImage expected=new BufferedImage(438,108,BufferedImage.TYPE_INT_ARGB);
                graphics=expected.createGraphics();
                HudSkin.gauge(canvas,well,value,new UiColor(.1f,.6f,.2f));HudSkin.glass(canvas,well);graphics.dispose();
                check(java.util.Arrays.equals(actual.getRGB(0,0,438,108,null,0,438),expected.getRGB(0,0,438,108,null,0,438)),
                        "Native "+(isHealth?"Healthbar":kind==1?"Select bar":"Fighting HUD")+" uses the complete slot and canonical glass at value "+value);
                check(actual.getRGB(well.x+well.width-1,well.y)!=actual.getRGB(well.x+well.width-1,well.y+well.height/2),
                        "Glass remains visible across the unfilled side and at zero value");
            }
        } finally {graphics=main;}
    }
    private static void nativeActionRow(int x,int y) throws Exception {
        java.lang.reflect.Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        sun.misc.Unsafe unsafe=(sun.misc.Unsafe)f.get(null);
        HighResFocusBar select=(HighResFocusBar)unsafe.allocateInstance(HighResFocusBar.class);
        select.x=x;select.y=y;field(select,"ui",new ChamomiloUiV1Canvas(select));
        field(select,"small",new ProbeFont(10,false));field(select,"bold",new ProbeFont(14,true));
        field(select,"showHotkeys",true);field(select,"contentVisible",true);
        org.highresfocusbar.client.FocusBarState state=new org.highresfocusbar.client.FocusBarState();
        com.wurmonline.shared.constants.PlayerAction[] actions={com.wurmonline.shared.constants.PlayerAction.EXAMINE,
                com.wurmonline.shared.constants.PlayerAction.DIG,com.wurmonline.shared.constants.PlayerAction.CULTIVATE,
                com.wurmonline.shared.constants.PlayerAction.FORAGE};
        state.mirrorActions((byte)1,(byte)1,java.util.Arrays.asList(actions));field(select,"state",state);
        java.util.Map<Short,SelectBarButtonProperty> properties=new java.util.HashMap<>();
        int[] columns={1,2,6,7};int[] rows={0,0,1,1};
        for(int i=0;i<actions.length;i++)properties.put(actions[i].getId(),
                new SelectBarButtonProperty(actions[i].getId(),actions[i].getName(),columns[i],rows[i]));
        field(select,"actionProperties",properties);
        field(select,"actionAtlas",unsafe.allocateInstance(com.wurmonline.client.resources.textures.ResourceTexture.class));
        java.lang.reflect.Method paint=HighResFocusBar.class.getDeclaredMethod("renderActions",Queue.class);paint.setAccessible(true);
        java.lang.reflect.Method pick=HighResFocusBar.class.getDeclaredMethod("actionIndexAt",int.class,int.class);pick.setAccessible(true);
        iconDraws.clear();paint.invoke(select,new Object[]{null});
        check(iconDraws.size()==4,"Draw exactly the available quick actions");
        float[] first=iconDraws.get(0);
        for(int i=0;i<4;i++) {
            float[] draw=iconDraws.get(i);
            check(draw[0]==x+110+i*28 && draw[1]==y+69 && draw[2]==22 && draw[3]==22,"Icons stay centred in touching, non-overlapping buttons at the shelf edge");
            check(Math.round(draw[4]*512)==columns[i]*32+3 && Math.round(draw[5]*512)==rows[i]*32+3
                    && Math.round(draw[6]*512)==26 && Math.round(draw[7]*512)==26,"Exclude the old atlas frame");
            check(((Integer)pick.invoke(select,x+121+i*28,y+80))==i,"Paint and pointer target refer to the same quick action");
            check(((Integer)pick.invoke(select,x+107+i*28,y+80))==i,"The shared edge belongs to the button on its right");
            check(((Integer)pick.invoke(select,x+134+i*28,y+80))==i,"The last pixel belongs to the button on its left");
        }
        check(((Integer)pick.invoke(select,x+106,y+80))==-1,"The portrait rail is outside the first action");
        field(select,"pressedActionIndex",0);field(select,"pressedActionUntil",System.nanoTime()+1_000_000_000L);
        iconDraws.clear();paint.invoke(select,new Object[]{null});float[] pressed=iconDraws.get(0);
        check(pressed[0]==first[0]+1 && pressed[1]==first[1]+1 && pressed[2]==first[2] && pressed[3]==first[3],
                "Press offsets the icon without shrinking or moving its click target");
        check(((Integer)pick.invoke(select,x+121,y+80))==0,"Pressed icon retains its click target");
        field(select,"pressedActionUntil",0L);paint.invoke(select,new Object[]{null});
        Graphics2D main=graphics;graphics=new BufferedImage(1000,400,BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            java.util.List<com.wurmonline.shared.constants.PlayerAction> many=new java.util.ArrayList<>();
            many.add(actions[0]);
            for(int i=1;i<12;i++) {
                com.wurmonline.shared.constants.PlayerAction action=new com.wurmonline.shared.constants.PlayerAction("Test "+i,(short)(1000+i),65535);
                many.add(action);properties.put(action.getId(),properties.get(actions[0].getId()));
            }
            state.mirrorActions((byte)1,(byte)1,many);
            iconDraws.clear();paint.invoke(select,new Object[]{null});
            check(iconDraws.size()==10,"Ten touching actions fit beside the pager without panel growth");
            check(((Integer)pick.invoke(select,x+373,y+80))==9,"Last first-page action is reachable");
            check(((Integer)pick.invoke(select,x+386,y+80))==9,"The final action owns its rightmost pixel");
            check(((Integer)pick.invoke(select,x+387,y+80))==-1,"No action extends beyond the full row");
            check(((Integer)pick.invoke(select,x+403,y+80))==-1,"Pager never overlaps action hit areas");
            java.lang.reflect.Method next=HighResFocusBar.class.getDeclaredMethod("advanceActionPage",int.class);next.setAccessible(true);
            next.invoke(select,1);iconDraws.clear();paint.invoke(select,new Object[]{null});
            check(iconDraws.size()==2,"All remaining actions are reachable on the second page");
            check(((Integer)pick.invoke(select,x+121,y+80))==10,"Second-page buttons dispatch their native global indices");
            next.invoke(select,1);
            check(((Integer)pick.invoke(select,x+121,y+80))==0,"Paging wraps to the first page");
        } finally { graphics.dispose();graphics=main;state.mirrorActions((byte)1,(byte)1,java.util.Arrays.asList(actions)); }
    }
    private static HighResCombatHint specialButton(int x,int y,boolean[] ready) {
        AttackButtonComponent nativeIcon=new AttackButtonComponent("test-native-special","Native special",(short)500);
        nativeIcon.setSize(true);nativeIcon.colorMode=0xffffff;
        nativeIcon.setLocation(x,y,SPECIAL_BUTTON_SIZE,SPECIAL_BUTTON_SIZE);
        HighResCombatHint button=new HighResCombatHint(null,nativeIcon,()->ready[0],()->new String[]{"Native special"},true);
        button.at(x+1,y+1);
        return button;
    }
    private static void verifySpecialButtons() throws Exception {
        Graphics2D main=graphics;
        try {
            boolean[] ready={true};
            HighResCombatHint button=specialButton(0,0,ready);
            BufferedImage reference=null;
            for(float alpha:new float[]{0,.2f,.65f,1}) {
                BufferedImage image=new BufferedImage(SPECIAL_BUTTON_SIZE,SPECIAL_BUTTON_SIZE,BufferedImage.TYPE_INT_ARGB);
                graphics=image.createGraphics();button.renderComponent(null,alpha);graphics.dispose();
                check(combatIcon[3]==1 && combatIcon[4]==SPECIAL_ICON_INSET && combatIcon[5]==SPECIAL_ICON_INSET
                        && combatIcon[6]==SPECIAL_BUTTON_SIZE-2*SPECIAL_ICON_INSET,"Native icon stays inset inside the button face");
                if(reference==null)reference=image;
                else check(java.util.Arrays.equals(reference.getRGB(0,0,SPECIAL_BUTTON_SIZE,SPECIAL_BUTTON_SIZE,null,0,SPECIAL_BUTTON_SIZE),
                        image.getRGB(0,0,SPECIAL_BUTTON_SIZE,SPECIAL_BUTTON_SIZE,null,0,SPECIAL_BUTTON_SIZE)),"Special button has stable opacity");
                check(button.width==SPECIAL_BUTTON_SIZE && button.x==0,"Special draw restores native hit geometry");
            }
            graphics=new BufferedImage(SPECIAL_BUTTON_SIZE,SPECIAL_BUTTON_SIZE,BufferedImage.TYPE_INT_ARGB).createGraphics();
            button.mouseMoved(2,2);button.renderComponent(null,.2f);
            check(combatIcon[0]==1f && combatIcon[1]==240f/255f,"Hover highlights the special icon with the Chamomilo color");
            button.leftPressed(2,2,0);
            java.lang.reflect.Field motionField=HighResCombatHint.class.getDeclaredField("motion");motionField.setAccessible(true);
            ((UiButtonMotion)motionField.get(button)).setAnimationsEnabled(false);
            button.renderComponent(null,.2f);
            check(combatIcon[4]==SPECIAL_ICON_INSET+1 && combatIcon[6]==24,"Press moves the icon one pixel without scaling");
            button.mouseExited();ready[0]=false;button.renderComponent(null,.2f);
            check(combatIcon[3]==.4f && combatIcon[4]==SPECIAL_ICON_INSET,"Disabled native foreground keeps its stable material opacity");
            graphics.dispose();
            check(POSITION_Y+POSITION_HEIGHT+2==SPECIAL_Y,"Position clears the special buttons by two pixels");
            for(int i=0;i<6;i++) {
                check(specialCellX(i)>=SPECIAL_X+3 && specialCellX(i)+SPECIAL_BUTTON_SIZE<=SPECIAL_X+SPECIAL_WIDTH-3,
                        "Special buttons stay inside their frame");
            }
            check(specialCellY()+SPECIAL_BUTTON_SIZE<=SPECIAL_Y+SPECIAL_HEIGHT,"Special buttons stay inside the panel");
            check(new ProbeFont(12,true,UiDensity.LOW).getWidth("8888")+1<=DISTANCE_VALUE_WIDTH,"Four digits fit the readable shared Chamomilo font");
            check(DISTANCE_LABEL_X+new ProbeFont(12,true,UiDensity.LOW).getWidth("Distance")+3<=DISTANCE_VALUE_X,"Distance label leaves space before the digits");
            check(DISTANCE_VALUE_X+DISTANCE_VALUE_WIDTH+4<=RANGE_ICON_X,"Four-digit distance does not touch the larger range icon");
            check(FOOTING_LABEL_X+new ProbeFont(12,true,UiDensity.LOW).getWidth("Footing")+3<=FOOTING_ICON_X,"Footing label clears its icon");
        } finally { graphics=main; }
    }
    private static HighResFightingHud productionFighter(int x,int y) throws Exception {
        java.lang.reflect.Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);
        sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
        HighResFightingHud fighter=(HighResFightingHud)unsafe.allocateInstance(HighResFightingHud.class);
        fighter.setLocation(x,y,PANEL_WIDTH,PANEL_HEIGHT);
        field(fighter,"ui",new ChamomiloUiV1Canvas(fighter));
        field(fighter,"regular",com.wurmonline.client.renderer.gui.text.HudTextFonts.regular());
        field(fighter,"bold",com.wurmonline.client.renderer.gui.text.HudTextFonts.bold());
        field(fighter,"small",com.wurmonline.client.renderer.gui.text.HudTextFonts.small());
        field(fighter,"title",com.wurmonline.client.renderer.gui.text.HudTextFonts.title());
        field(fighter,"owner",unsafe.allocateInstance(HeadsUpDisplay.class));
        world=(com.wurmonline.client.game.World)unsafe.allocateInstance(com.wurmonline.client.game.World.class);
        field(fighter,"target",unsafe.allocateInstance(com.wurmonline.client.renderer.cell.CreatureCellRenderable.class));
        org.highresfightinghud.client.CreatureProfile profile=org.highresfightinghud.client.CreatureProfile.fromName("Old unicorn");
        field(fighter,"targetProfile",profile);field(fighter,"loadout",org.highresfightinghud.client.EquipmentLoadout.EMPTY);
        org.highresfightinghud.client.CombatKnowledgeBook book=new org.highresfightinghud.client.CombatKnowledgeBook(java.nio.file.Paths.get("build","preview-knowledge"));
        field(fighter,"knowledgeBook",book);book.recordEncounter(profile.knowledgeKey());book.recordKill(profile.knowledgeKey());
        java.lang.reflect.Method context=HighResFightingHud.class.getDeclaredMethod("combatContextKey");context.setAccessible(true);
        String key=(String)context.invoke(fighter);
        for(int i=0;i<31;i++) {
            org.highresfightinghud.client.CombatObservation event=new org.highresfightinghud.client.CombatObservation(
                    org.highresfightinghud.client.CombatObservation.Direction.OUTGOING,
                    i<7?org.highresfightinghud.client.CombatObservation.Outcome.HIT:org.highresfightinghud.client.CombatObservation.Outcome.MISS,
                    org.highresfightinghud.client.CombatObservation.DamageType.CRUSH,"body",.29);
            book.observe(key,event,0,0,false);book.observe(profile.knowledgeKey(),event,0,0,false);
        }
        FightWindowComponent nativeFight=(FightWindowComponent)unsafe.allocateInstance(FightWindowComponent.class);
        field(nativeFight,"fighting",true);field(fighter,"fightOptions",nativeFight);field(fighter,"nativeChildrenResolved",true);
        java.util.List<StaticComponent> attacks=new java.util.ArrayList<>();
        for(int i=0;i<9;i++) {
            AttackButtonComponent button=new AttackButtonComponent("test-native-stance","Native stance",(short)0);
            button.hidden=i<3;attacks.add(button);
        }
        field(fighter,"attackButtons",attacks);field(fighter,"specialButtons",new java.util.ArrayList<StaticComponent>());
        field(fighter,"nativeFocus",new AttackButtonComponent("test-native-focus","Native focus",(short)340));
        org.highresfightinghud.client.CombatFocusState state=new org.highresfightinghud.client.CombatFocusState();state.combatChanged(true);
        field(fighter,"focusState",state);
        HighResHudActionButton focus=new HighResHudActionButton(fighter,FOCUS_BUTTON_WIDTH,FOCUS_BUTTON_HEIGHT,
                new HudCaptionGroup(FOCUS_BUTTON_WIDTH,FOCUS_BUTTON_HEIGHT,HudSkin.COMPACT,14,"Combat focus"),"Combat focus",()->{});
        focus.setLocation(x+FOCUS_BUTTON_X,y+FOCUS_BUTTON_Y,FOCUS_BUTTON_WIDTH,FOCUS_BUTTON_HEIGHT);
        focus.caption("Combat focus",false);field(fighter,"focusButton",focus);
        // The fixture paints production blocks without the native attachment path.
        // Match an already-visible panel after that path has sized its children.
        field(fighter,"contentVisible",true);fighter.width=PANEL_WIDTH;fighter.height=PANEL_HEIGHT;
        return fighter;
    }
    private static void renderNativeBlock(HighResFightingHud fighter,String method) throws Exception {
        java.lang.reflect.Method paint=HighResFightingHud.class.getDeclaredMethod(method,Queue.class);paint.setAccessible(true);
        texts.clear();paint.invoke(fighter,new Object[]{null});
    }
    private static void verifyAnalysisText(int x,int y) {
        check(texts.size()==8,"All four analysis lines are rendered with shadows");
        int previousBottom=y+ANALYSIS_Y+3;
        for(int i=0;i<texts.size();i+=2) {
            Rectangle ink=texts.get(i).ink;
            check(ink.x>=x+ANALYSIS_X+ANALYSIS_TEXT_INSET && ink.x+ink.width<=x+ANALYSIS_X+ANALYSIS_WIDTH-ANALYSIS_TEXT_INSET,"Analysis text remains inside horizontal frame padding");
            check(ink.y>=previousBottom+1 && ink.y+ink.height<=y+ANALYSIS_Y+ANALYSIS_HEIGHT-4,"Analysis rows clear the top/bottom rails and each other");
            previousBottom=ink.y+ink.height;
        }
        check(texts.get(0).value.startsWith(org.highreshud.client.i18n.HudMessages.tr("BEST ")),
                "Production analysis displays a translated available unarmed zone");
        if (org.highreshud.client.i18n.HudMessages.language().equals("en"))
            check(texts.get(0).value.startsWith("BEST LOW"),"Production analysis displays the best available unarmed zone");
    }
    private static void verifyFullHoverHints() throws Exception {
        java.lang.reflect.Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);
        sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
        HighResFightingHud fighter=productionFighter(0,0);
        field(fighter,"progressState",new org.highresfightinghud.client.FocusBarState());
        HighResHudActionButton clear=new HighResHudActionButton(fighter,NO_TARGET_WIDTH,NO_TARGET_HEIGHT,
                new HudCaptionGroup(NO_TARGET_WIDTH,NO_TARGET_HEIGHT,HudSkin.COMPACT,14,"No target"),"No target",()->{});
        clear.setLocation(NO_TARGET_X,NO_TARGET_Y,NO_TARGET_WIDTH,NO_TARGET_HEIGHT);field(fighter,"noTargetButton",clear);
        java.util.List<StaticComponent> modes=new java.util.ArrayList<>();
        for(int i=0;i<4;i++) modes.add(new AttackButtonComponent("mode","Native mode",(short)i));
        field(fighter,"modeButtons",modes);
        org.highreshud.client.state.CreatureRelationState.observe((com.wurmonline.client.renderer.cell.CreatureCellRenderable)
                HighResWindowAccess.get(fighter,"target"),com.wurmonline.shared.constants.AttitudeConstants.ATTITUDE_GM);
        HighResFocusBar select=(HighResFocusBar)unsafe.allocateInstance(HighResFocusBar.class);
        select.setLocation(0,0,438,126);field(select,"contentVisible",true);field(select,"showDistance",true);
        org.highresfocusbar.client.FocusBarState state=new org.highresfocusbar.client.FocusBarState();
        state.mirrorProgress(.5f,"Long server action title",false);field(select,"state",state);
        field(select,"autoClose",new org.highresfocusbar.client.SelectAutoCloseTimer());
        SelectBar nativeSelect=(SelectBar)unsafe.allocateInstance(SelectBar.class);
        nativeSelect.selectedUnit=(com.wurmonline.client.renderer.PickableUnit)java.lang.reflect.Proxy.newProxyInstance(
                HudButtonProbe.class.getClassLoader(),new Class[]{com.wurmonline.client.renderer.PickableUnit.class},(proxy,method,arguments)->{
                    if(method.getName().equals("getId"))return 7L;
                    if(method.getReturnType()==String.class)return "Selected test item";
                    return null;
                });
        field(select,"selectBar",nativeSelect);
        org.highresfocusbar.client.SelectedExamineState examined=new org.highresfocusbar.client.SelectedExamineState();
        check(examined.observeFor(7L,":Event","QL 90.0 Dam 1.0"),"Select metadata fixture");
        field(select,"selectedExamine",examined);
        for(String code:org.highreshud.client.i18n.HudMessages.languageCodes()) {
            org.highreshud.client.i18n.HudMessages.useLanguage(code);
            com.wurmonline.client.renderer.PickData pick=new com.wurmonline.client.renderer.PickData(null);
            select.pick(pick,120,52);
            check(pick.getText().contains(org.highreshud.client.i18n.HudMessages.tr("Item quality: {0}","90.00"))
                    && pick.getText().contains(org.highreshud.client.i18n.HudMessages.tr("Item damage: {0}","1.00")),"Select explains both abbreviations: "+code);
            pick.reset();select.pick(pick,120,100);
            check(pick.getText().contains("Long server action title") && pick.getText().contains(org.highreshud.client.i18n.HudMessages.tr("Action progress: {0}%",50)),"Select progress full title and value");
            pick.reset();fighter.pick(pick,INFO_X+10,34);
            check(pick.getText().stream().anyMatch(line->line.startsWith(org.highreshud.client.i18n.HudMessages.tr("Health: "))),"Fighting health hint: "+code);
            pick.reset();fighter.pick(pick,INFO_X+10,65);
            check(pick.getText().contains(org.highreshud.client.i18n.HudMessages.tr("Creature attitude: {0}",org.highreshud.client.i18n.HudMessages.tr("Game master"))),"GM abbreviation expanded");
            pick.reset();fighter.pick(pick,POSITION_X+10,POSITION_Y+10);
            check(pick.getText().contains(org.highreshud.client.i18n.HudMessages.tr("Distance to combat target: {0} m","1234")),"Combat distance includes units");
            pick.reset();fighter.pick(pick,FOOTING_LABEL_X+3,POSITION_Y+10);
            check(pick.getText().contains(org.highreshud.client.i18n.HudMessages.tr("Position indicator: terrain and balance advantage, not an action.")),"Footing label explains indicator");
            java.util.List<?> attacks=(java.util.List<?>)HighResWindowAccess.get(fighter,"attackButtons");
            for(Object attack:attacks) {pick.reset();check(fighter.pickNativeControl((WurmComponent)attack,pick) && pick.getText().size()==1,"Every attack icon has a full zone hint");}
            for(StaticComponent mode:modes) {pick.reset();check(fighter.pickNativeControl(mode,pick) && pick.getText().size()==1,"Every mode icon has a hint");}
            org.highresfightinghud.client.FocusBarState progress=(org.highresfightinghud.client.FocusBarState)HighResWindowAccess.get(fighter,"progressState");
            progress.mirrorProgress(.5f,"Full combat action title",false);
            UiRect cell=HudSkin.targetCell(0,0,true,2);pick.reset();fighter.pick(pick,cell.x+3,cell.y+3);
            check(pick.getText().contains("Full combat action title") && pick.getText().contains(org.highreshud.client.i18n.HudMessages.tr("Action progress: {0}%",50)),"Combat progress hint");
            progress.clearProgress();
        }
        org.highreshud.client.i18n.HudMessages.useLanguage("en");
    }
    public static void main(String[] args) throws Exception {
        WurmComponent.SCREEN_WIDTH=1920;WurmComponent.SCREEN_HEIGHT=1080;
        BufferedImage sheet=new BufferedImage(850,870,BufferedImage.TYPE_INT_RGB);
        graphics=sheet.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        canvas=new PreviewCanvas();
        updaterLanguageChecks();
        verifyFullHoverHints();
        verifySpecialButtons();
        actionAtlas=ImageIO.read(new File("resource-pack/gui/highres-select-actions.png"));
        canvas.fill(UiColor.rgb(0x171610),1,0,0,850,870);
        label("High-res HUD 0.4.2 / Chamomilo UI 0.4.9",24,28,18);
        label("Production artwork and compact captions; portraits and game data are placeholders",24,49,12);
        final int[] clicks={0};
        HudCaptionGroup sleepGroup=new HudCaptionGroup(76,14,HudSkin.COMPACT,"Activate","Deactivate");
        HighResHudActionButton sleep=new HighResHudActionButton(null,76,14,sleepGroup,"Activate",()->clicks[0]++);
        sleep.setLocation(0,0,76,14);
        check(sleep.width==76 && sleep.height==14,"Compact control dimensions stay exact");
        sleep.leftPressed(2,2,0);check(clicks[0]==0,"No action on press");
        sleep.leftReleased(2,2);check(clicks[0]==1,"Exactly one release action");
        sleep.leftPressed(2,2,0);sleep.mouseDragged(90,20);sleep.mouseDragged(2,2);sleep.leftReleased(2,2);
        check(clicks[0]==1,"Drag away cancels even after reentry");
        sleep.caption("Activate",false);sleep.leftPressed(2,2,0);sleep.leftReleased(2,2);check(clicks[0]==1,"Disabled action blocked");
        sleep.caption("Activate",true);
        // Use an idle control so press animation is independent of the alpha experiment.
        HighResHudActionButton fixed=new HighResHudActionButton(null,76,14,sleepGroup,"Activate",()->{});
        BufferedImage alphaReference=null;
        for(float alpha:new float[]{0,.2f,.65f,1}) {
            BufferedImage checkImage=new BufferedImage(76,14,BufferedImage.TYPE_INT_ARGB);
            Graphics2D main=graphics;graphics=checkImage.createGraphics();
            fixed.mouseExited();fixed.renderComponent(null,alpha);graphics.dispose();graphics=main;
            check(textAlpha==1.0f && canvas.fixedAlpha,"Fixed alpha for text and textures");
            if(alphaReference!=null)check(java.util.Arrays.equals(alphaReference.getRGB(0,0,76,14,null,0,76),
                    checkImage.getRGB(0,0,76,14,null,0,76)),"Incoming HUD alpha cannot change button pixels");
            alphaReference=checkImage;
        }
        nativeGaugeChecks();
        int hx=24,hy=78;HudSkin.healthBack(canvas,hx,hy);
        label("Player portrait",hx+13,hy+51,10);
        gauge(HudSkin.healthGauge(hx,hy,0),.84f,0x199b35);
        gauge(HudSkin.healthGauge(hx,hy,1),.72f,0x2678c7);gauge(HudSkin.healthGauge(hx,hy,2),.61f,0x16a03e);
        for(int i=0;i<4;i++)gauge(HudSkin.healthGauge(hx,hy,i+3),.8f-i*.1f,0x94713d);
        gauge(HudSkin.healthGauge(hx,hy,7),.5f,0x788793);gauge(HudSkin.healthGauge(hx,hy,8),.66f,0xb921c7);
        java.lang.reflect.Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);
        HighResHealthBar labels=(HighResHealthBar)((sun.misc.Unsafe)uf.get(null)).allocateInstance(HighResHealthBar.class);
        field(labels,"smallText",com.wurmonline.client.renderer.gui.text.HudTextFonts.small());
        java.lang.reflect.Method paintLabel=HighResHealthBar.class.getDeclaredMethod("paintGaugeLabel",Queue.class,
                String.class,int.class,int.class,int.class);paintLabel.setAccessible(true);
        String[] names={"Stamina / HP","Water","Food","Calories","Carbs","Fats","Proteins","Sleep bonus: OFF","Favor"};
        for(int i=0;i<names.length;i++) {
            UiRect well=HudSkin.healthGauge(hx,hy,i);
            paintLabel.invoke(labels,null,names[i],well.x,well.y,well.height);
        }
        HudSkin.healthFront(canvas,hx,hy);button(sleep,hx+241,hy+66,"Activate",true,false);
        label("Healthbar: 323 x 108 body (unchanged)",hx,hy+129,12);
        int sx=370,sy=78;HudSkin.selectBack(canvas,sx,sy);label("Selected portrait",sx+12,sy+51,10);
        gauge(HudSkin.selectHealth(sx,sy),.73f,0x198f35);gauge(HudSkin.selectProgress(sx,sy),.52f,0x2678c7);
        HudSkin.selectFront(canvas,sx,sy);label("Selected creature",sx+117,sy+18,14);label("healthy / friendly",sx+114,sy+58,12);
        nativeActionRow(sx,sy);
        UiPainter.button(canvas,1,0,HudSkin.COMPACT,1,sx+412,sy+2,22,22);UiIcon.CLOSE.paint(canvas,UiColor.TEXT,1,sx+417,sy+7,12);
        label("Select bar: 438 x 108 body (unchanged)",sx,sy+129,12);
        HighResHudActionButton noTarget=new HighResHudActionButton(null,NO_TARGET_WIDTH,NO_TARGET_HEIGHT,
                new HudCaptionGroup(NO_TARGET_WIDTH,NO_TARGET_HEIGHT,HudSkin.COMPACT,14,"No target"),"No target",()->{});
        int fx=24,fy=242;HudSkin.fightingBack(canvas,fx,fy);
        HighResFightingHud live=productionFighter(fx,fy);
        HudSkin.panel(canvas,fx+ANALYSIS_X,fy+ANALYSIS_Y,ANALYSIS_WIDTH,ANALYSIS_HEIGHT);
        HudSkin.panel(canvas,fx+COMBAT_X,fy+COMBAT_Y,COMBAT_SIZE,COMBAT_HEIGHT);
        HudSkin.panel(canvas,fx+MODE_X,fy+MODE_Y,MODE_WIDTH,MODE_HEIGHT);HudSkin.panel(canvas,fx+POSITION_X,fy+POSITION_Y,POSITION_WIDTH,POSITION_HEIGHT);
        button(noTarget,fx+NO_TARGET_X,fy+NO_TARGET_Y,"No target",true,false);
        renderNativeBlock(live,"renderFocus");
        check(texts.get(2).baseline==texts.get(4).baseline,"Focus level and readiness share one baseline");
        check(texts.get(4).value.equals("Engaging ~0/3"),"Single-row Focus retains the full engagement status");
        for(String status:new String[]{"Engaging ~0/3","Ready to try ~","Not in combat","Maximum focus","On the ground","Unavailable"}) {
            check(FOCUS_TEXT_X+UiTypography.width("Focus 0/5",14,true,UiDensity.LOW)+FOCUS_TEXT_GAP
                    +UiTypography.width(status,14,false,UiDensity.LOW)<=FOCUS_X+FOCUS_WIDTH-8,"Focus status fits its single-row block");
        }
        label("Target",fx+PORTRAIT_X+9,fy+PORTRAIT_Y+30,10);
        UiRect nameWell=HudSkin.targetCell(fx,fy,false,0);
        label("Old unicorn",nameWell.x+6,nameWell.y+16,14);
        gauge(HudSkin.targetCell(fx,fy,false,1),.74f,0x198f35);
        UiRect healthWell=HudSkin.targetCell(fx,fy,false,1),relationWell=HudSkin.targetCell(fx,fy,false,2);
        label("Health: 74%",healthWell.x+85,healthWell.y+16,12);
        label("Hostile",relationWell.x+6,relationWell.y+16,12);
        HudSkin.targetFront(canvas,fx,fy,false);
        renderNativeBlock(live,"renderAnalysis");verifyAnalysisText(fx,fy);
        renderNativeBlock(live,"renderCombatSections");
        for(PaintedText text:texts) if(text.value.equals("Distance") || text.value.equals("1234") || text.value.equals("Footing")) {
            check(text.ink.y>=fy+POSITION_Y+3 && text.ink.y+text.ink.height<=fy+POSITION_Y+POSITION_HEIGHT-3,"Positioning text clears both frame rails");
            Rectangle range=new Rectangle(fx+RANGE_ICON_X,fy+POSITION_ICON_Y,POSITION_ICON_SIZE,POSITION_ICON_SIZE);
            Rectangle footing=new Rectangle(fx+FOOTING_ICON_X,fy+POSITION_ICON_Y,POSITION_ICON_SIZE,POSITION_ICON_SIZE);
            check(!text.ink.intersects(range) && !text.ink.intersects(footing),"Larger positioning icons do not cover labels or digits");
        }
        UiIcon.CHEVRON_UP.paint(canvas,UiColor.TEXT,1,fx+RANGE_ICON_X,fy+POSITION_ICON_Y,POSITION_ICON_SIZE);
        UiIcon.CHEVRON_UP.paint(canvas,UiColor.TEXT,1,fx+FOOTING_ICON_X,fy+POSITION_ICON_Y,POSITION_ICON_SIZE);
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            int index=row*3+col;
            int bx=fx+stanceCellX(index,STANCE_BUTTON_SIZE);
            int by=fy+stanceCellY(index,STANCE_BUTTON_SIZE);
            UiPainter.button(canvas,index<3?.5f:1f,0,HudSkin.COMPACT,1,bx,by,STANCE_BUTTON_SIZE,STANCE_BUTTON_SIZE);
            UiIcon.CHEVRON_UP.paint(canvas,UiColor.TEXT,index<3?.4f:1f,bx+8,by+8,16);
        }
        for(int i=0;i<4;i++)UiPainter.button(canvas,1,0,HudSkin.COMPACT,1,fx+modeCellX(),fy+modeCellY(i),MODE_BUTTON_SIZE,MODE_BUTTON_SIZE);
        for(int i=0;i<6;i++) {
            HighResCombatHint special=specialButton(fx+specialCellX(i),fy+specialCellY(),new boolean[]{i<2});
            special.renderButton(null);
        }
        label("Fighting HUD: 520 x 186; 49.3% less area; 32 px stance / 24 px mode buttons",fx,fy+209,12);
        label("Compact captions: normal / bold hover / disabled",24,584,14);
        sleep.mouseExited(); // reset motion after input checks
        button(sleep,24,600,"Activate",true,false);
        button(sleep,108,600,"Deactivate",true,true);
        button(sleep,192,600,"Activate",false,false);
        HighResHudActionButton ride=new HighResHudActionButton(null,76,16,
                new HudCaptionGroup(76,16,HudSkin.COMPACT,"Disembark"),"Disembark",()->{});
        button(ride,284,599,"Disembark",true,false);button(ride,368,599,"Disembark",true,true);
        label("Shared Sleep caption size: "+sleepGroup.fontPixels+" px, baseline: "+sleepGroup.baseline+" px",24,644,12);
        label("Target card during an action: the progress row appears only while active",24,676,14);
        int tx=24,ty=694;
        button(noTarget,tx+NO_TARGET_X,ty+NO_TARGET_Y,"No target",true,false);
        gauge(HudSkin.targetCell(tx,ty,true,1),.74f,0x198f35);
        gauge(HudSkin.targetCell(tx,ty,true,2),.4f,0x2678c7);
        HudSkin.targetFront(canvas,tx,ty,true);label("Old unicorn",tx+INFO_X+8,ty+INFO_Y+17,14);
        UiRect progressWell=HudSkin.targetCell(tx,ty,true,2),statusWell=HudSkin.targetCell(tx,ty,true,3);
        label("Focusing...",progressWell.x+8,progressWell.y+13,12);label("Hostile",statusWell.x+6,statusWell.y+12,12);
        HighResFightingHud active=productionFighter(tx,ty);
        java.lang.reflect.Field stateField=HighResFightingHud.class.getDeclaredField("focusState");stateField.setAccessible(true);
        ((org.highresfightinghud.client.CombatFocusState)stateField.get(active)).levelReceived((byte)2,"");
        actionString="Focusing...";renderNativeBlock(active,"renderFocus");actionString="";
        graphics.dispose();File file=new File(args[0]);file.getParentFile().mkdirs();ImageIO.write(sheet,"png",file);
        localizationPreview(new File(file.getParentFile(), "hud-languages-preview.png"));
        System.out.println("HUD_UI_OK: updater-only language choice, immediate repaint of existing Healthbar/Select/Fighting controls, saved choice, native release, disabled, drag cancellation, fixed-alpha pixels, full-slot glass at 0/50/100%, real quick-action atlas, press geometry, pointer targets, single-row Focus, analysis bounds, compact Position and available-attack recommendation; preview="+file);
    }

    static void previewGraphics(BufferedImage image) {
        graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    static void finishPreview() { graphics.dispose(); }

    private static void updaterLanguageChecks() throws Exception {
        org.highreshud.client.HighResHudMod mod = new org.highreshud.client.HighResHudMod();
        java.nio.file.Path folder = java.nio.file.Files.createTempDirectory("hud-updater-language-");
        java.nio.file.Path file = folder.resolve("highres-hud.properties");
        java.nio.file.Path sharedFile = folder.resolve("updater.properties");
        updaterPreferences=new org.chamomilo.wurm.update.UpdatePreferences(sharedFile);
        field(mod,"languagePreferences",new org.highreshud.client.i18n.HudLanguagePreferences(file));
        org.chamomilo.wurm.update.SharedLanguageCoordinator.modInitialized(
                new org.gotti.wurmunlimited.modloader.interfaces.ModEntry<org.highreshud.client.HighResHudMod>() {
                    public String getName() { return "highres-hud"; }
                    public java.util.Properties getProperties() { return new java.util.Properties(); }
                    public org.highreshud.client.HighResHudMod getWurmMod() { return mod; }
                });
        java.lang.reflect.Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);
        sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
        // Create each panel/control once, before any updater selection. Never attach,
        // recreate, reopen or call applyPending between selecting and painting.
        HighResFightingHud fighter=productionFighter(12,12);
        HighResHealthBar health=(HighResHealthBar)unsafe.allocateInstance(HighResHealthBar.class);
        field(health,"smallText",com.wurmonline.client.renderer.gui.text.HudTextFonts.small());
        java.lang.reflect.Field player=HealthBar.class.getDeclaredField("player");player.setAccessible(true);
        player.set(health,unsafe.allocateInstance(player.getType()));
        java.lang.reflect.Method healthPaint=HighResHealthBar.class.getDeclaredMethod("drawGaugeLabels",Queue.class,int.class,int.class);
        healthPaint.setAccessible(true);
        HighResFocusBar select=(HighResFocusBar)unsafe.allocateInstance(HighResFocusBar.class);
        select.setLocation(12,260,438,126);
        field(select,"owner",unsafe.allocateInstance(HeadsUpDisplay.class));
        field(select,"regular",com.wurmonline.client.renderer.gui.text.HudTextFonts.regular());
        field(select,"showDistance",true);
        java.lang.reflect.Method selectPaint=HighResFocusBar.class.getDeclaredMethod("renderDistanceFooter",Queue.class,
                com.wurmonline.client.renderer.PickableUnit.class);selectPaint.setAccessible(true);
        HudCaptionGroup group=new HudCaptionGroup(76,14,HudSkin.COMPACT,"Activate","Deactivate");
        HighResHudActionButton existing=new HighResHudActionButton(health,76,14,group,"Activate",()->{});
        int screenWidth=WurmComponent.SCREEN_WIDTH, screenHeight=WurmComponent.SCREEN_HEIGHT;
        try {
            for (int[] viewport : new int[][]{{800,600},{1920,1080},{2560,1440},{3840,2160}}) {
                WurmComponent.SCREEN_WIDTH=viewport[0]; WurmComponent.SCREEN_HEIGHT=viewport[1];
            for (String code : org.highreshud.client.i18n.HudMessages.languageCodes()) {
                org.chamomilo.wurm.update.SharedLanguageCoordinator.selectLanguage(code.equals("ru") ? "en" : "ru");
                org.chamomilo.wurm.update.SharedLanguageCoordinator.selectLanguage(code);
                check(mod.getLanguage().equals(code),"Updater sets the production language synchronously");
                check(new org.chamomilo.wurm.update.UpdatePreferences(sharedFile).getForcedLanguage().equals(code),
                        "Updater retains the shared choice");
                check(new org.highreshud.client.i18n.HudLanguagePreferences(file).language().equals(code),
                        "Updater selection saves the HUD preference immediately");
                texts.clear(); existing.renderComponent(null,1f);
                check(texts.stream().anyMatch(t -> t.value.equals(org.highreshud.client.i18n.HudMessages.tr("Activate"))),
                        "The already-created native button redraws the selected language");
                check(existing.width==76 && existing.height==14,"Updater selection preserves button geometry");
                texts.clear(); healthPaint.invoke(health,null,0,107);
                check(texts.stream().anyMatch(t -> t.value.equals(org.highreshud.client.i18n.HudMessages.tr("Water"))),
                        "Existing Healthbar labels repaint immediately");
                texts.clear(); selectPaint.invoke(select,null,null);
                check(texts.stream().anyMatch(t -> t.value.equals(org.highreshud.client.i18n.HudMessages.tr("Distance:"))),
                        "Existing Select footer repaints immediately");
                renderNativeBlock(fighter,"renderFocus");
                check(texts.stream().anyMatch(t -> t.value.equals(org.highreshud.client.i18n.HudMessages.tr("Combat focus"))),
                        "Existing Fighting focus control repaints immediately");
                renderNativeBlock(fighter,"renderAnalysis");verifyAnalysisText(12,12);
                check(texts.stream().anyMatch(t -> t.value.contains("31/31")),"Language changes retain learned samples");
                check(fighter.x==12 && fighter.y==12 && fighter.width==PANEL_WIDTH && fighter.height==PANEL_HEIGHT,
                        "Updater language selection retains the existing Fighting panel bounds");
            }
            }
        } finally {
            org.highreshud.client.i18n.HudMessages.useLanguage("en");
            java.nio.file.Files.deleteIfExists(file); java.nio.file.Files.deleteIfExists(sharedFile);
            java.nio.file.Files.deleteIfExists(folder);
            WurmComponent.SCREEN_WIDTH=screenWidth; WurmComponent.SCREEN_HEIGHT=screenHeight;
        }
    }

    private static void localizationPreview(File file) throws Exception {
        int screenHeight = WurmComponent.SCREEN_HEIGHT;
        WurmComponent.SCREEN_HEIGHT = 2160; // A tall contact sheet must not trigger native viewport clamping.
        org.highreshud.client.i18n.HudMessages.useLanguage("en");
        HudCaptionGroup sleepGroup = new HudCaptionGroup(76,14,HudSkin.COMPACT,"Activate","Deactivate");
        HighResHudActionButton sleep = new HighResHudActionButton(null,76,14,sleepGroup,"Activate",()->{});
        HighResHudActionButton ride = new HighResHudActionButton(null,76,16,
                new HudCaptionGroup(76,16,HudSkin.COMPACT,"Disembark"),"Disembark",()->{});
        HighResHudActionButton noTarget = new HighResHudActionButton(null,NO_TARGET_WIDTH,NO_TARGET_HEIGHT,
                new HudCaptionGroup(NO_TARGET_WIDTH,NO_TARGET_HEIGHT,HudSkin.COMPACT,14,"No target"),"No target",()->{});
        BufferedImage sheet = new BufferedImage(820, 1120, BufferedImage.TYPE_INT_ARGB);
        graphics = sheet.createGraphics();
        graphics.setColor(new Color(25,21,18)); graphics.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        try {
            int row = 0;
            for (String code : org.highreshud.client.i18n.HudMessages.languageCodes()) {
                org.highreshud.client.i18n.HudMessages.useLanguage(code);
                int top = row++ * 280;
                label(code + " — live native HUD text and action buttons", 24, top + 25, 14);
                button(sleep,24,top+37,"Activate",true,false);
                button(sleep,108,top+37,"Deactivate",true,true);
                button(ride,200,top+36,"Disembark",true,false);
                button(noTarget,292,top+35,"No target",true,false);
                check(sleep.width==76 && sleep.height==14 && ride.width==76,
                        "Language selection never resizes live buttons");
                int fx = 24, fy = top + 72;
                HudSkin.fightingBack(canvas, fx, fy);
                HighResFightingHud live = productionFighter(fx, fy);
                texts.clear(); renderNativeBlock(live,"renderFocus");
                check(texts.stream().anyMatch(t -> t.value.equals(org.highreshud.client.i18n.HudMessages.tr("Combat focus"))),
                        "Native focus button relocalizes in " + code);
                renderNativeBlock(live,"renderAnalysis"); verifyAnalysisText(fx,fy);
                texts.clear(); renderNativeBlock(live,"renderCombatSections");
                Rectangle value = null;
                for (PaintedText t : texts) if (t.value.equals("1234")) value = t.ink;
                for (PaintedText t : texts) if (t.value.equals(org.highreshud.client.i18n.HudMessages.tr("Distance")))
                    check(value==null || !t.ink.intersects(value),"Translated Distance cannot overlap its value in " + code);
                for (PaintedText t : texts) if (t.value.equals(org.highreshud.client.i18n.HudMessages.tr("Footing")))
                    check(!t.ink.intersects(new Rectangle(fx+FOOTING_ICON_X,fy+POSITION_ICON_Y,POSITION_ICON_SIZE,POSITION_ICON_SIZE)),
                            "Translated Footing cannot overlap its indicator in " + code);
                label(org.highreshud.client.i18n.HudMessages.tr("Sleep bonus: OFF"),580,top+107,12);
                label(org.highreshud.client.i18n.HudMessages.tr("Water"),580,top+132,12);
                label(org.highreshud.client.i18n.HudMessages.tr("Food"),580,top+157,12);
            }
            ImageIO.write(sheet,"png",file);
        } finally {
            graphics.dispose(); org.highreshud.client.i18n.HudMessages.useLanguage("en");
            WurmComponent.SCREEN_HEIGHT = screenHeight;
        }
    }
}
