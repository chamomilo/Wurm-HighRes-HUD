package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.shared.constants.PlayerAction;
import java.lang.reflect.Field;
import java.util.*;
import org.highresfightinghud.client.*;
import org.highresfocusbar.client.HighResFocusBarMod;
import org.highresfocusbar.client.SelectAutoCloseTimer;
import org.highreshud.client.HighResHudRuntime;
import org.highreshud.client.state.ActionProgressState;
import org.highreshud.client.ui.*;
import org.highreshud.core.*;
import sun.misc.Unsafe;
import static org.highresfightinghud.client.FightingHudLayout.*;

public final class CombatHudInteractionProbe {
    private static Unsafe unsafe;
    private static final Map<PickableUnit,Long> ids=new IdentityHashMap<>();
    private static final Set<PickableUnit> items=Collections.newSetFromMap(new IdentityHashMap<PickableUnit,Boolean>());
    private static final List<Short> sends=new ArrayList<>();
    public static boolean enabled=true;
    public static com.wurmonline.client.game.World world;
    public static float targetX, targetY;
    public static long id(PickableUnit unit) { return ids.get(unit); }
    public static boolean isItem(PickableUnit unit) { return items.contains(unit); }
    public static void capture(PlayerAction action,long[] targets) {
        check(targets.length==1 && targets[0]==-1L,"Native combat command retains its transport target");
        check(HighResHudApi.core().actions().currentOrigin()==ActionOrigin.HUD,"HUD command has the HUD origin");
        sends.add(action.getId());
    }
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    private static void field(Object object,String name,Object value) throws Exception {
        Class<?> type=object.getClass();Field f=null;
        while(type!=null) { try {f=type.getDeclaredField(name);break;}catch(NoSuchFieldException e){type=type.getSuperclass();} }
        if(f==null)throw new NoSuchFieldException(name);f.setAccessible(true);f.set(object,value);
    }
    private static CreatureCellRenderable creature(long id,boolean item) throws Exception {
        CreatureCellRenderable value=(CreatureCellRenderable)unsafe.allocateInstance(CreatureCellRenderable.class);
        ids.put(value,id);if(item)items.add(value);return value;
    }
    private static AttackButtonComponent attack(short command) throws Exception {
        AttackButtonComponent value=(AttackButtonComponent)unsafe.allocateInstance(AttackButtonComponent.class);
        value.command=command;value.commandName="Test native action";return value;
    }
    public static void main(String[] args) throws Exception {
        Field uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);unsafe=(Unsafe)uf.get(null);
        WurmComponent.SCREEN_WIDTH=1920;WurmComponent.SCREEN_HEIGHT=1080;
        HighResFightingHudSettings.collectCombatKnowledge=false;
        HeadsUpDisplay owner=(HeadsUpDisplay)unsafe.allocateInstance(HeadsUpDisplay.class);WurmComponent.hud=owner;
        SelectBar bar=(SelectBar)unsafe.allocateInstance(SelectBar.class);
        TargetWindow targetWindow=(TargetWindow)unsafe.allocateInstance(TargetWindow.class);
        field(owner,"selectBar",bar);field(owner,"targetWindow",targetWindow);
        HighResFocusBarMod selectMod=new HighResFocusBarMod(new ActionProgressState());
        HighResFocusBar select=(HighResFocusBar)unsafe.allocateInstance(HighResFocusBar.class);
        org.highresfocusbar.client.FocusBarState selectState=new org.highresfocusbar.client.FocusBarState();
        field(selectMod,"hud",owner);field(selectMod,"panel",select);field(selectMod,"state",selectState);
        field(select,"owner",owner);field(select,"selectBar",bar);field(select,"state",selectState);
        SelectAutoCloseTimer timer=new SelectAutoCloseTimer();field(select,"autoClose",timer);
        HighResFightingHudMod fighterMod=new HighResFightingHudMod(new ActionProgressState());
        HighResFightingHud fighter=(HighResFightingHud)unsafe.allocateInstance(HighResFightingHud.class);
        field(fighterMod,"hud",owner);field(fighterMod,"panel",fighter);
        field(fighter,"owner",owner);field(fighter,"targetWindow",targetWindow);
        field(fighter,"targetId",Long.MIN_VALUE);field(fighter,"progressState",new FocusBarState());
        field(fighter,"examineQueue",new CombatExamineQueue());field(fighter,"focusState",new CombatFocusState());
        field(fighter,"modeButtons",new ArrayList<StaticComponent>());
        field(fighter,"attackButtons",new ArrayList<StaticComponent>());
        field(fighter,"positionControls",new ArrayList<StaticComponent>());
        field(fighter,"specialButtons",new ArrayList<StaticComponent>());
        field(fighter,"combatHints",new ArrayList<HighResCombatHint>());
        HighResHudActionButton clear=new HighResHudActionButton(fighter,NO_TARGET_WIDTH,NO_TARGET_HEIGHT,
                new HudCaptionGroup(NO_TARGET_WIDTH,NO_TARGET_HEIGHT,HudSkin.COMPACT,"No target"),"No target",()->{
                    try { java.lang.reflect.Method m=HighResFightingHud.class.getDeclaredMethod("clearCombatTarget");
                        m.setAccessible(true);m.invoke(fighter); }catch(Exception e){throw new AssertionError(e);}
                });
        HighResHudActionButton focus=new HighResHudActionButton(fighter,FOCUS_BUTTON_WIDTH,FOCUS_BUTTON_HEIGHT,
                new HudCaptionGroup(FOCUS_BUTTON_WIDTH,FOCUS_BUTTON_HEIGHT,HudSkin.COMPACT,"Combat focus"),"Combat focus",()->{
                    try { java.lang.reflect.Method m=HighResFightingHud.class.getDeclaredMethod("attemptFocus");
                        m.setAccessible(true);m.invoke(fighter); }catch(Exception e){throw new AssertionError(e);}
                });
        field(fighter,"noTargetButton",clear);field(fighter,"focusButton",focus);
        CreatureCellRenderable spider=creature(100,false),other=creature(200,false),corpse=creature(300,true);
        bar.selectedUnit=spider;selectState.selectionChanged(100);field(select,"contentVisible",true);
        owner.setTargetCreature(100,spider);
        check(bar.selectedUnit==null && select.width==0 && select.height==0,"Selecting a combat target immediately closes pinned Select");
        check(targetWindow.creature==spider && fighter.presentedTargetId()==100,"Handoff preserves the combat target");
        java.lang.reflect.Method subject=HighResFocusBar.class.getDeclaredMethod("selectedSubject");subject.setAccessible(true);
        check(subject.invoke(select)==null,"Handoff clears the 750 ms portrait latch");
        bar.setSelected(spider);check(bar.selectedUnit==null,"Reselecting the displayed combat target cannot duplicate the card");
        bar.setSelected(other);owner.setTargetCreature(100,spider);
        check(bar.selectedUnit==other,"An unrelated selected subject stays selected");
        bar.setSelected(corpse);owner.setTargetCreature(100,spider);
        check(bar.selectedUnit==corpse,"Corpse selection is preserved");
        bar.selectedUnit=null;field(select,"latchedSelectedSubject",spider);field(select,"selectedLastSeen",System.nanoTime());
        owner.setTargetCreature(100,spider);check(subject.invoke(select)==null,"Native transient deselection also clears the matching latch");
        check(fighter.getComponentAt(NO_TARGET_X+2,NO_TARGET_Y+2)==clear,"No target participates in native child routing");
        clear.leftPressed(NO_TARGET_X+2,NO_TARGET_Y+2,0);check(sends.isEmpty(),"No target does not activate on press");
        clear.leftReleased(NO_TARGET_X+2,NO_TARGET_Y+2);
        check(sends.size()==1 && sends.get(0)==PlayerAction.NO_TARGET.getId(),"Exactly one native No target command");
        check(targetWindow.creature==null && fighter.width==0,"No target clears the native target and closes an idle fighter card");
        clear.leftPressed(NO_TARGET_X+2,NO_TARGET_Y+2,0);clear.leftReleased(NO_TARGET_X+2,NO_TARGET_Y+2);
        check(sends.size()==1,"Absent target cannot send another clear");

        FightWindowComponent nativeFight=(FightWindowComponent)unsafe.allocateInstance(FightWindowComponent.class);
        field(nativeFight,"fighting",true);field(nativeFight,"advancedComponents",new ArrayList<StaticComponent>());
        field(nativeFight,"fightAgressive",attack((short)1));field(nativeFight,"fightNormal",attack((short)2));
        field(nativeFight,"fightDefensive",attack((short)3));
        field(nativeFight,"toggleRanged",unsafe.allocateInstance(AttackButtonRangedComponent.class));
        Class<?> focusType=Class.forName("com.wurmonline.client.renderer.gui.FightWindowComponent$FocusButtonComponent");
        AttackButtonComponent nativeFocus=(AttackButtonComponent)unsafe.allocateInstance(focusType);
        nativeFocus.command=340;field(nativeFocus,"focusLevel",0);field(nativeFocus,"focusLevelMessage","");field(nativeFight,"focusB",nativeFocus);
        AttackButtonComponent bash=attack((short)105);field(nativeFight,"shieldBash",bash);
        field(nativeFight,"distanceMeter",unsafe.allocateInstance(DistMeterComponent.class));
        field(nativeFight,"balanceMeter",unsafe.allocateInstance(FootingIndicatorComponent.class));
        AttackButtonStanceComponent[] stances=new AttackButtonStanceComponent[12];
        for(int i:ATTACK_STANCE_GRID)stances[i]=(AttackButtonStanceComponent)unsafe.allocateInstance(AttackButtonStanceComponent.class);
        field(nativeFight,"combatStances",stances);
        AttackButtonComponent[] specials=new AttackButtonComponent[5];
        for(int i=0;i<5;i++)specials[i]=attack((short)(500+i));field(nativeFight,"specialMoves",specials);
        field(fighter,"fightOptions",nativeFight);field(fighter,"nativeChildrenUnavailable",false);
        owner.setTargetCreature(100,spider);
        focus.leftPressed(FOCUS_BUTTON_X+2,FOCUS_BUTTON_Y+2,0);focus.leftReleased(FOCUS_BUTTON_X+2,FOCUS_BUTTON_Y+2);
        check(sends.size()==1,"Focus is inactive before initial engagement");
        fighter.focusLevelReceived((byte)2,"Focused on the enemy");fighter.targetChanged();
        focus.leftPressed(FOCUS_BUTTON_X+2,FOCUS_BUTTON_Y+2,0);focus.leftReleased(FOCUS_BUTTON_X+2,FOCUS_BUTTON_Y+2);
        check(sends.size()==2 && sends.get(1)==340,"Ready Focus dispatches the native Focus command");
        fighter.targetChanged();focus.leftPressed(FOCUS_BUTTON_X+2,FOCUS_BUTTON_Y+2,0);focus.leftReleased(FOCUS_BUTTON_X+2,FOCUS_BUTTON_Y+2);
        check(sends.size()==2,"Pending Focus cannot be spammed");
        fighter.focusLevelReceived((byte)5,"Maximum focus");fighter.targetChanged();
        focus.leftPressed(FOCUS_BUTTON_X+2,FOCUS_BUTTON_Y+2,0);focus.leftReleased(FOCUS_BUTTON_X+2,FOCUS_BUTTON_Y+2);
        check(sends.size()==2,"Maximum Focus is inactive");
        field(fighter,"loadout",EquipmentLoadout.EMPTY);
        CombatKnowledgeBook attackKnowledge=new CombatKnowledgeBook(java.nio.file.Paths.get("build","native-knowledge-probe"));
        field(fighter,"knowledgeBook",attackKnowledge);
        java.lang.reflect.Method attackContext=HighResFightingHud.class.getDeclaredMethod("combatContextKey");attackContext.setAccessible(true);
        String attackKey=(String)attackContext.invoke(fighter);
        for(CombatObservation.Outcome outcome:new CombatObservation.Outcome[]{CombatObservation.Outcome.HIT,CombatObservation.Outcome.MISS})
            attackKnowledge.observe(attackKey,new CombatObservation(CombatObservation.Direction.OUTGOING,outcome,
                    CombatObservation.DamageType.CRUSH,"body",.2),0,0,false);
        java.lang.reflect.Method recommendation=HighResFightingHud.class.getDeclaredMethod("combatRecommendation");recommendation.setAccessible(true);
        check(((CombatKnowledge.Snapshot)recommendation.invoke(fighter)).bestStance==7,"Full native attack grid allows the model's head recommendation");
        for(int i=0;i<3;i++)stances[ATTACK_STANCE_GRID[i]].hidden=true;
        check(((CombatKnowledge.Snapshot)recommendation.invoke(fighter)).bestStance==10,"Unarmed native head restrictions select the next available zone");
        for(int i=0;i<3;i++)stances[ATTACK_STANCE_GRID[i]].hidden=false;
        check(((CombatKnowledge.Snapshot)recommendation.invoke(fighter)).bestStance==7,"A server availability change refreshes the recommendation immediately");
        for(int i:ATTACK_STANCE_GRID)stances[i].hidden=true;
        check(((CombatKnowledge.Snapshot)recommendation.invoke(fighter)).bestStance==-1,"An entirely unavailable native grid has no recommendation");
        for(int i:ATTACK_STANCE_GRID)stances[i].hidden=false;
        int sx=specialCellX(1)+2,sy=specialCellY()+2;
        StaticComponent special=fighter.getComponentAt(sx,sy);special.leftPressed(sx,sy,0);special.leftReleased(sx,sy);
        check(sends.size()==2,"Unarmed special cannot send a stale native binding");
        @SuppressWarnings("unchecked") List<StaticComponent> advanced=(List<StaticComponent>)get(nativeFight,"advancedComponents");
        advanced.add(specials[0]);special.leftPressed(sx,sy,0);special.leftReleased(sx,sy);
        check(sends.size()==3 && sends.get(2)==500,"Server-granted special retains its native binding");
        check(special instanceof HighResCombatHint && special.width==SPECIAL_BUTTON_SIZE,
                "Special uses the complete Chamomilo button as its native input target");
        special.leftPressed(sx,sy,0);special.mouseDragged(sx+SPECIAL_BUTTON_SIZE,sy);
        special.mouseDragged(sx,sy);special.leftReleased(sx,sy);
        check(sends.size()==3,"Dragging away cancels a special even after reentry");
        for(int i=0;i<specials.length;i++) {
            int edge=specialCellX(i)+SPECIAL_BUTTON_SIZE;
            check(fighter.getComponentAt(edge,specialCellY()+2)==fighter,"Gaps between special buttons cannot dispatch an action");
        }
        field(nativeFight,"stunned",true);special.leftPressed(sx,sy,0);special.leftReleased(sx,sy);
        check(sends.size()==3,"Stun disables a server-granted move");
        check(((CombatKnowledge.Snapshot)recommendation.invoke(fighter)).bestStance==-1,"Stun also clears the model recommendation");
        check(HighResHudApi.core().actions().currentOrigin()==ActionOrigin.USER,"Native combat dispatch restores caller origin");
        java.lang.reflect.Method distance=HighResFightingHud.class.getDeclaredMethod("targetDistance");distance.setAccessible(true);
        check("—".equals(distance.invoke(fighter)),"No world yields an unavailable distance instead of zero");
        world=(com.wurmonline.client.game.World)unsafe.allocateInstance(com.wurmonline.client.game.World.class);
        targetX=123.9f;targetY=0f;
        check("123".equals(distance.invoke(fighter)),"Distance omits fractions and unit suffixes");
        targetX=1234.9f;
        check("1234".equals(distance.invoke(fighter)),"Four-digit target distances remain complete");
        targetX=3;targetY=4;check("5".equals(distance.invoke(fighter)),"Distance uses both horizontal coordinates");
        targetX=Float.NaN;check("—".equals(distance.invoke(fighter)),"Invalid positions never become a false zero distance");
        world=null;
    }
    private static Object get(Object object,String name) throws Exception {
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
}
