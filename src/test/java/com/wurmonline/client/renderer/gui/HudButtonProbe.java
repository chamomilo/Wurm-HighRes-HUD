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
    private static Graphics2D graphics;
    private static float textAlpha;
    private static BufferedImage actionAtlas;
    private static final java.util.List<float[]> iconDraws=new java.util.ArrayList<>();
    private static float[] combatIcon;
    public static com.wurmonline.client.game.World world;
    public static String actionString = "";
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

    public static final class ProbeFont extends TextFont {
        final int size; final boolean bold; final UiDensity density; int x,y;
        public ProbeFont(int size,boolean bold) { this(size,bold,UiDensity.HIGH); }
        public ProbeFont(int size,boolean bold,UiDensity density) { this.size=size;this.bold=bold;this.density=density; }
        public void moveTo(int x,int y) {this.x=x;this.y=y;}
        public int paint(Queue q,String s,float r,float g,float b,float a) {
            Rectangle ink=UiTypography.ink(s,size,bold,density);ink.translate(x,y);
            texts.add(new PaintedText(s,ink,y));
            textAlpha=a;graphics.setFont(UiTypography.font(size,bold,density));
            graphics.setComposite(AlphaComposite.SrcOver);graphics.setColor(new Color(r,g,b,a));
            graphics.drawString(s,x,y);return getWidth(s);
        }
        public int getWidth(String s) {return UiTypography.width(s,size,bold,density);}
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
            check(POSITION_Y+POSITION_HEIGHT+8==SPECIAL_Y,"Position and special frames have an eight-pixel gap");
            for(int i=0;i<6;i++) {
                check(specialCellX(i)>=SPECIAL_X+3 && specialCellX(i)+SPECIAL_BUTTON_SIZE<=SPECIAL_X+SPECIAL_WIDTH-3,
                        "Special buttons stay inside their frame");
            }
            check(specialCellY()+SPECIAL_BUTTON_SIZE<=SPECIAL_Y+SPECIAL_HEIGHT-3,"Special buttons leave the bottom frame clear");
            check(UiTypography.width("8888",14,true,UiDensity.LOW)+1<=DISTANCE_VALUE_WIDTH,"Four digits fit the larger shared Chamomilo font");
            check(DISTANCE_LABEL_X+UiTypography.width("Distance",14,true,UiDensity.LOW)+3<=DISTANCE_VALUE_X,"Distance label leaves space before the digits");
            check(DISTANCE_VALUE_X+DISTANCE_VALUE_WIDTH+4<=RANGE_ICON_X,"Four-digit distance does not touch the larger range icon");
            check(FOOTING_LABEL_X+UiTypography.width("Footing",14,true,UiDensity.LOW)+4<=FOOTING_ICON_X,"Footing label clears its larger icon");
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
                new HudCaptionGroup(FOCUS_BUTTON_WIDTH,FOCUS_BUTTON_HEIGHT,HudSkin.COMPACT,"Combat focus"),"Combat focus",()->{});
        focus.setLocation(x+FOCUS_BUTTON_X,y+FOCUS_BUTTON_Y,FOCUS_BUTTON_WIDTH,FOCUS_BUTTON_HEIGHT);
        focus.caption("Combat focus",false);field(fighter,"focusButton",focus);
        return fighter;
    }
    private static void renderNativeBlock(HighResFightingHud fighter,String method) throws Exception {
        java.lang.reflect.Method paint=HighResFightingHud.class.getDeclaredMethod(method,Queue.class);paint.setAccessible(true);
        texts.clear();paint.invoke(fighter,new Object[]{null});
    }
    private static void verifyAnalysisText(int x,int y) {
        check(texts.size()==12,"All six analysis lines are rendered with shadows");
        int previousBottom=y+ANALYSIS_Y+3;
        for(int i=0;i<texts.size();i+=2) {
            Rectangle ink=texts.get(i).ink;
            check(ink.x>=x+ANALYSIS_X+8 && ink.x+ink.width<=x+ANALYSIS_X+ANALYSIS_WIDTH-8,"Analysis text remains inside horizontal frame padding");
            check(ink.y>=previousBottom+2 && ink.y+ink.height<=y+ANALYSIS_Y+ANALYSIS_HEIGHT-8,"Analysis rows clear the top/bottom rails and each other");
            previousBottom=ink.y+ink.height;
        }
        check(texts.get(6).value.startsWith("BEST LOW"),"Production analysis displays the best available unarmed zone");
    }
    public static void main(String[] args) throws Exception {
        WurmComponent.SCREEN_WIDTH=1920;WurmComponent.SCREEN_HEIGHT=1080;
        BufferedImage sheet=new BufferedImage(850,870,BufferedImage.TYPE_INT_RGB);
        graphics=sheet.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        canvas=new PreviewCanvas();
        verifySpecialButtons();
        actionAtlas=ImageIO.read(new File("resource-pack/gui/highres-select-actions.png"));
        canvas.fill(UiColor.rgb(0x171610),1,0,0,850,870);
        label("High-res HUD 0.2.3 / Chamomilo UI 0.4.4",24,28,18);
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
        HighResHudActionButton noTarget=new HighResHudActionButton(null,96,19,
                new HudCaptionGroup(96,19,HudSkin.COMPACT,"No target"),"No target",()->{});
        int fx=24,fy=242;HudSkin.fightingBack(canvas,fx,fy);
        HighResFightingHud live=productionFighter(fx,fy);
        HudSkin.panel(canvas,fx+ANALYSIS_X,fy+ANALYSIS_Y,ANALYSIS_WIDTH,ANALYSIS_HEIGHT);
        HudSkin.panel(canvas,fx+COMBAT_X,fy+COMBAT_Y,COMBAT_SIZE,COMBAT_HEIGHT);
        HudSkin.panel(canvas,fx+MODE_X,fy+MODE_Y,MODE_WIDTH,MODE_HEIGHT);HudSkin.panel(canvas,fx+POSITION_X,fy+POSITION_Y,POSITION_WIDTH,POSITION_HEIGHT);
        HudSkin.panel(canvas,fx+SPECIAL_X,fy+SPECIAL_Y,SPECIAL_WIDTH,SPECIAL_HEIGHT);HudSkin.panel(canvas,fx+107,fy+90,314,36);
        button(noTarget,fx+5,fy+4,"No target",true,false);
        renderNativeBlock(live,"renderFocus");
        check(texts.get(2).baseline==texts.get(4).baseline,"Focus level and readiness share one baseline");
        check(texts.get(4).value.equals("Engaging ~0/3"),"Single-row Focus retains the full engagement status");
        for(String status:new String[]{"Engaging ~0/3","Ready to try ~","Not in combat","Maximum focus","On the ground","Unavailable"}) {
            check(FOCUS_TEXT_X+UiTypography.width("Focus 0/5",14,true,UiDensity.LOW)+FOCUS_TEXT_GAP
                    +UiTypography.width(status,14,false,UiDensity.LOW)<=FOCUS_X+FOCUS_WIDTH-8,"Focus status fits its single-row block");
        }
        label("Target portrait",fx+10,fy+76,10);label("Old unicorn",fx+140,fy+21,14);
        gauge(HudSkin.targetCell(fx,fy,false,1),.74f,0x198f35);
        UiRect healthWell=HudSkin.targetCell(fx,fy,false,1),relationWell=HudSkin.targetCell(fx,fy,false,2);
        label("Health: 74%",healthWell.x+107,healthWell.y+16,12);
        label("Hostile",relationWell.x+6,relationWell.y+16,12);
        HudSkin.targetFront(canvas,fx,fy,false);
        renderNativeBlock(live,"renderAnalysis");verifyAnalysisText(fx,fy);
        renderNativeBlock(live,"renderCombatSections");
        for(PaintedText text:texts) if(text.value.equals("Distance") || text.value.equals("1234") || text.value.equals("Footing")) {
            check(text.ink.y>=fy+POSITION_Y+20 && text.ink.y+text.ink.height<=fy+POSITION_Y+POSITION_HEIGHT-3,"Larger positioning text fits below its heading");
            Rectangle range=new Rectangle(fx+RANGE_ICON_X,fy+POSITION_ICON_Y,POSITION_ICON_SIZE,POSITION_ICON_SIZE);
            Rectangle footing=new Rectangle(fx+FOOTING_ICON_X,fy+POSITION_ICON_Y,POSITION_ICON_SIZE,POSITION_ICON_SIZE);
            check(!text.ink.intersects(range) && !text.ink.intersects(footing),"Larger positioning icons do not cover labels or digits");
        }
        UiIcon.CHEVRON_UP.paint(canvas,UiColor.TEXT,1,fx+RANGE_ICON_X,fy+POSITION_ICON_Y,POSITION_ICON_SIZE);
        UiIcon.CHEVRON_UP.paint(canvas,UiColor.TEXT,1,fx+FOOTING_ICON_X,fy+POSITION_ICON_Y,POSITION_ICON_SIZE);
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            int index=row*3+col;
            int bx=fx+org.highresfightinghud.client.FightingHudLayout.stanceCellX(index,38);
            int by=fy+org.highresfightinghud.client.FightingHudLayout.stanceCellY(index,38);
            UiPainter.button(canvas,index<3?.5f:1f,0,HudSkin.COMPACT,1,bx,by,38,38);
            UiIcon.CHEVRON_UP.paint(canvas,UiColor.TEXT,index<3?.4f:1f,bx+11,by+11,16);
        }
        for(int i=0;i<4;i++)UiPainter.button(canvas,1,0,HudSkin.COMPACT,1,fx+MODE_X+(MODE_WIDTH-28)/2,fy+28+i*31,28,28);
        for(int i=0;i<6;i++) {
            HighResCombatHint special=specialButton(fx+specialCellX(i),fy+specialCellY(),new boolean[]{i<2});
            special.renderButton(null);
        }
        label("Fighting HUD: 676 x 282 (unchanged); unarmed: head attacks unavailable",fx,fy+303,12);
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
        button(noTarget,tx+5,ty+4,"No target",true,false);
        gauge(HudSkin.targetCell(tx,ty,true,1),.74f,0x198f35);
        gauge(HudSkin.targetCell(tx,ty,true,2),.4f,0x2678c7);
        HudSkin.targetFront(canvas,tx,ty,true);label("Old unicorn",tx+140,ty+21,14);
        label("Focusing...",tx+217,ty+61,12);label("Hostile",tx+116,ty+81,12);
        HighResFightingHud active=productionFighter(tx,ty);
        java.lang.reflect.Field stateField=HighResFightingHud.class.getDeclaredField("focusState");stateField.setAccessible(true);
        ((org.highresfightinghud.client.CombatFocusState)stateField.get(active)).levelReceived((byte)2,"");
        actionString="Focusing...";renderNativeBlock(active,"renderFocus");actionString="";
        graphics.dispose();File file=new File(args[0]);file.getParentFile().mkdirs();ImageIO.write(sheet,"png",file);
        System.out.println("HUD_UI_OK: native release, disabled, drag cancellation, fixed-alpha pixels, full-slot glass at 0/50/100%, real quick-action atlas, press geometry, pointer targets, single-row Focus, analysis bounds, larger Position and available-attack recommendation; preview="+file);
    }
}
