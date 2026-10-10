package org.highreshealthbar.client;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;
import org.junit.Test;

/** Executes the real controller and native single-target HUD send overload. */
public class DisembarkDispatchTest {
    @Test
    public void targetsCurrentCarrierWithoutSurfaceTileLookup() throws Throwable {
        ClassPool pool = new ClassPool(true);
        String probe = DisembarkDispatchProbe.class.getName();
        String game = "com.wurmonline.client.game.";
        String gui = "com.wurmonline.client.renderer.gui.";
        // No constructors, graphics initialization or live connection are needed.
        for (String name : new String[]{game + "World", game + "PlayerObj",
                gui + "HeadsUpDisplay", "com.wurmonline.client.renderer.cell.CreatureCellRenderable"}) {
            for (CtClass type = pool.get(name); type != null; type = type.getSuperclass()) {
                if (type.getClassInitializer() != null) type.getClassInitializer().setBody("{}");
            }
        }
        pool.get(game + "World").getDeclaredMethod("getPlayer")
                .setBody("{ return " + probe + ".player; }");
        pool.get(game + "World").getDeclaredMethod("sendLocalAction")
                .setBody("{ throw new AssertionError((Object)\"Disembark must not target a surface tile\"); }");
        pool.get(game + "PlayerObj").getDeclaredMethod("getCarrierCreature")
                .setBody("{ return " + probe + ".carrier; }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("getWorld")
                .setBody("{ return " + probe + ".world; }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("sendAction", new CtClass[]{
                pool.get("com.wurmonline.shared.constants.PlayerAction"), pool.get("long[]")})
                .setBody("{ " + probe + ".capture($1, $2); }");
        pool.get("com.wurmonline.client.renderer.cell.CreatureCellRenderable")
                .getDeclaredMethod("getId").setBody("{ return " + probe + ".carrierId; }");
        new Loader(pool).run(probe, new String[0]);
    }
}
