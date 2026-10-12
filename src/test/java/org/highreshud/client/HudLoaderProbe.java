package org.highreshud.client;

import javassist.ClassPool;
import javassist.Loader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import java.lang.reflect.Method;
import java.util.Properties;

/** Separate process with the mod JAR absent from the parent classpath, like Wurm. */
public final class HudLoaderProbe {
    public static void main(String[] args) throws Exception {
        ClassPool pool = HookManager.getInstance().getClassPool();
        pool.appendClassPath(args[0]);
        Loader loader = HookManager.getInstance().getLoader();
        // DelegatedLauncher shares the hook manager and Javassist with its child.
        loader.delegateLoadingOf("javassist.");
        loader.delegateLoadingOf("org.gotti.wurmunlimited.modloader.");
        // Match the startup order seen in client.log: other mods define these
        // native classes before HighResHud.preInit is invoked.
        for (String name : new String[]{"WurmComponent", "WurmBorderPanel", "WWindow", "InventoryWindow", "WButton",
                "WurmArrayPanel", "WurmTreeList", "WurmInputField", "WurmScrollPanel"})
            loader.loadClass("com.wurmonline.client.renderer.gui." + name);
        Class<?> messages = loader.loadClass("org.highreshud.client.i18n.HudMessages");
        if (messages.getResourceAsStream("messages.tsv") != null)
            throw new AssertionError("Fixture must hide the mod's resource from the parent classpath");
        Class<?> entry = loader.loadClass("org.highreshud.client.HighResHudMod");
        Object mod = entry.newInstance();
        entry.getMethod("configure",Properties.class).invoke(mod,new Properties());
        entry.getMethod("preInit").invoke(mod);
        for (String name : new String[]{"HeadsUpDisplay", "Renderer"})
            loader.loadClass("com.wurmonline.client.renderer.gui." + name).getDeclaredMethods();
        Method translate = messages.getMethod("translate",String.class,String.class,Object[].class);
        String[] codes = (String[])entry.getMethod("getSupportedLanguages").invoke(mod);
        if (codes.length != 4) throw new AssertionError("Expected all four catalogues");
        for (String code : codes) {
            String value = (String)translate.invoke(null,code,"Activate",new Object[0]);
            if (value == null || value.isEmpty() || !code.equals("en") && value.equals("Activate"))
                throw new AssertionError("Missing packaged translation: " + code);
        }
        Class<?> updater = loader.loadClass("org.chamomilo.wurm.update.UpdaterMessages");
        if (updater.getResourceAsStream("i18n/messages_en.properties") != null)
            throw new AssertionError("Embedded updater resources must also be hidden from the parent");
        for (String code : codes) {
            Object catalogue = updater.getMethod("forLanguage",String.class).invoke(null,code);
            if (!code.equals(updater.getMethod("languageCode").invoke(catalogue)))
                throw new AssertionError("Missing embedded updater catalogue: " + code);
        }
        System.out.println("HUD_LOADER_OK: preloaded GUI bases, real mod preInit/configure and all four languages through shared Javassist Loader; mod resources hidden");
    }
}
