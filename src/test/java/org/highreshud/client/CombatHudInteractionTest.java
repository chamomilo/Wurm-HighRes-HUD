package org.highreshud.client;

import javassist.*;
import org.junit.Test;

/** Execute production routing and native command overloads without a GPU or connection. */
public class CombatHudInteractionTest {
    @Test public void targetHandoffFocusAndSpecialMovesRespectNativeState() throws Throwable {
        ClassPool pool = new ClassPool(true);
        String gui = "com.wurmonline.client.renderer.gui.";
        String probe = gui + "CombatHudInteractionProbe";
        String creature = "com.wurmonline.client.renderer.cell.CreatureCellRenderable";
        for (String name : new String[]{gui + "HeadsUpDisplay", gui + "SelectBar", gui + "TargetWindow", creature}) {
            for (CtClass type = pool.get(name); type != null; type = type.getSuperclass())
                if (type.getClassInitializer() != null) type.getClassInitializer().setBody("{}");
        }
        pool.get(gui + "text.TextFont").getClassInitializer().setBody("{}");
        pool.get("com.wurmonline.client.options.Options").getClassInitializer().setBody("{}");
        pool.get(gui + "text.TextFont").getDeclaredMethod("getText",new CtClass[]{pool.get("java.lang.String")})
                .setBody("{ return new " + gui + "HudButtonProbe.ProbeFont(12,false); }");
        pool.get(gui + "text.ChamomiloUiV1Fonts").getDeclaredMethod("caption",new CtClass[]{CtClass.intType,
                CtClass.booleanType,pool.get("org.chamomilo.wurm.ui.v1.UiDensity")})
                .setBody("{ return new " + gui + "HudButtonProbe.ProbeFont($1,$2,$3); }");
        pool.get(creature).getDeclaredMethod("getId").setBody("{ return " + probe + ".id($0); }");
        pool.get(creature).getDeclaredMethod("isItem").setBody("{ return " + probe + ".isItem($0); }");
        pool.get(creature).getDeclaredMethod("getHoverName").setBody("{ return \"Venerable huge spider\"; }");
        pool.get(creature).getMethod("setTarget","(Z)V").setBody("{}");
        pool.get(gui + "SelectBar").getDeclaredMethod("setSelected")
                .setBody("{ this.selectedUnit=$1; org.highreshud.client.HighResHudRuntime.selectionChanged(this); }");
        pool.get(gui + "TargetWindow").getDeclaredMethod("setTarget")
                .setBody("{ this.creature=$3; this.targetName=$2; }");
        pool.get(gui + "TargetWindow").getDeclaredMethod("clearTarget")
                .setBody("{ this.creature=null; this.targetName=\"\"; }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("isComponentEnabled")
                .setBody("{ return " + probe + ".enabled; }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("toggleComponent",new CtClass[]{pool.get(gui+"WurmComponent"),CtClass.booleanType})
                .setBody("{ " + probe + ".enabled=$2; return $2; }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("sendAction",new CtClass[]{
                pool.get("com.wurmonline.shared.constants.PlayerAction"),pool.get("long[]")})
                .setBody("{ " + probe + ".capture($1,$2); }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("setTargetCreature")
                .insertAfter("org.highreshud.client.HighResHudRuntime.targetChanged(this);");
        pool.get(gui + "FightWindowComponent").getDeclaredMethod("setLocation")
                .setBody("{ this.x=$1; this.y=$2; this.width=$3; this.height=$4; }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("getWorld").setBody("{ return " + probe + ".world; }");
        CtClass world = pool.get("com.wurmonline.client.game.World");
        if (world.getClassInitializer() != null) world.getClassInitializer().setBody("{}");
        world.getDeclaredMethod("getPlayerPosX").setBody("{ return 0f; }");
        world.getDeclaredMethod("getPlayerPosY").setBody("{ return 0f; }");
        pool.get(creature).getMethod("getXPos","()F").setBody("{ return " + probe + ".targetX; }");
        pool.get(creature).getMethod("getYPos","()F").setBody("{ return " + probe + ".targetY; }");
        new Loader(pool).run(probe,new String[0]);
    }
}
