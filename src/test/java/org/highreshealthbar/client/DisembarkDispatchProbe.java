package org.highreshealthbar.client;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.shared.constants.PlayerAction;
import java.lang.reflect.Field;
import org.highreshud.core.ActionOrigin;
import org.highreshud.core.HighResHudApi;
import sun.misc.Unsafe;

/** Native transport fixture, isolated in a Javassist loader by the JUnit test. */
public final class DisembarkDispatchProbe {
    public static World world;
    public static PlayerObj player;
    public static CreatureCellRenderable carrier;
    public static long carrierId;
    private static int sends;

    public static void capture(PlayerAction action, long[] targets) {
        if (action != PlayerAction.DISEMBARK || targets.length != 1
                || targets[0] != carrierId || targets[0] == 0
                || HighResHudApi.core().actions().currentOrigin() != ActionOrigin.HUD) {
            throw new AssertionError("Disembark must send one HUD action to the current carrier");
        }
        sends++;
    }

    public static void main(String[] args) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        HighResHealthBarMod mod = new HighResHealthBarMod();
        Field hudField = HighResHealthBarMod.class.getDeclaredField("hud");
        hudField.setAccessible(true);
        mod.disembark(); // No HUD.
        hudField.set(mod, unsafe.allocateInstance(HeadsUpDisplay.class));
        mod.disembark(); // No world.
        world = (World) unsafe.allocateInstance(World.class);
        mod.disembark(); // No player.
        player = (PlayerObj) unsafe.allocateInstance(PlayerObj.class);
        mod.disembark(); // No carrier.
        if (sends != 0) throw new AssertionError("Detached player must not send Disembark");
        carrier = (CreatureCellRenderable) unsafe.allocateInstance(CreatureCellRenderable.class);
        carrierId = 0x12345678901L; // Creature ID.
        mod.disembark();
        carrierId = 0x23456789002L; // Vehicle item ID; use the new carrier at click time.
        mod.disembark();
        carrier = null;
        mod.disembark(); // Dismounted between rendering and clicking.
        if (sends != 2) throw new AssertionError("Exactly one request per mounted click");
        if (HighResHudApi.core().actions().currentOrigin() != ActionOrigin.USER)
            throw new AssertionError("Disembark must restore the caller's action origin");
    }
}
