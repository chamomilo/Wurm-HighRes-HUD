package org.highreshud.client;

import javassist.ClassPool;
import javassist.CtClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/** Compiles the complete unified injection set against the pinned Wurm jars. */
public class UnifiedHookCompilationTest {
    @Test
    public void everyUnifiedHookCompilesAgainstClient() throws Exception {
        File libs = new File(System.getProperty("wurmClientLibDir"));
        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new File(libs, "client-patched.jar")
                .getAbsolutePath());
        pool.appendClassPath(new File(libs, "common.jar").getAbsolutePath());

        UnifiedHookInstaller.installOn(pool);

        String[] changed = {
                "com.wurmonline.client.resources.Resources",
                "com.wurmonline.client.resources.textures.IconLoader",
                "com.wurmonline.client.renderer.gui.HeadsUpDisplay",
                "com.wurmonline.client.renderer.gui.SelectBar",
                "com.wurmonline.client.renderer.WorldRender",
                "com.wurmonline.client.comm.SimpleServerConnectionClass",
                "com.wurmonline.client.renderer.gui.FightWindowComponent",
                "com.wurmonline.client.comm.ServerConnectionListenerClass",
                "com.wurmonline.client.renderer.cell.CellRenderer",
                "com.wurmonline.client.renderer.cell.CreatureCellRenderable",
                "com.wurmonline.client.renderer.gui.ChatPanelComponent"
        };
        for (String name : changed) {
            CtClass type = pool.get(name);
            assertTrue(name, type.toBytecode().length > 0);
        }
    }
}
