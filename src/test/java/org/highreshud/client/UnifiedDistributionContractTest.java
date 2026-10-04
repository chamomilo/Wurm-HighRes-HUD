package org.highreshud.client;

import org.highresfightinghud.client.HighResFightingHudSettings;
import org.highresfocusbar.client.HighResFocusBarSettings;
import org.highreshealthbar.client.HighResHealthBarSettings;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnifiedDistributionContractTest {
    @Test
    public void oneModEntryPointConfiguresAllPanels() throws Exception {
        File root = new File(System.getProperty("highResHudProjectDir"));
        Properties mod = new Properties();
        try (FileInputStream input = new FileInputStream(
                new File(root, "mods/highres-hud.properties"))) {
            mod.load(input);
        }
        assertEquals(HighResHudMod.class.getName(), mod.getProperty("classname"));
        assertEquals("highres-hud-" + HighResHudMod.VERSION + ".jar",
                mod.getProperty("classpath"));

        new HighResHudMod().configure(mod);
        assertTrue(HighResHealthBarSettings.enabledByDefault);
        assertTrue(HighResFocusBarSettings.enabledByDefault);
        assertTrue(HighResFightingHudSettings.enabledByDefault);
        assertEquals(mod.getProperty("resourcePack"),
                HighResHealthBarSettings.resourcePack);
        assertEquals(mod.getProperty("resourcePack"),
                HighResFocusBarSettings.resourcePack);
        assertEquals(mod.getProperty("resourcePack"),
                HighResFightingHudSettings.resourcePack);
    }

    @Test
    public void resourceMappingsContainAllThreePanels() throws Exception {
        File root = new File(System.getProperty("highResHudProjectDir"));
        String mappings = new String(Files.readAllBytes(new File(root,
                "resource-pack/mappings.txt").toPath()), StandardCharsets.UTF_8);
        assertTrue(mappings.contains("img.highreshealthbar.frame"));
        assertTrue(mappings.contains("img.highresselectbar.frame"));
        assertTrue(mappings.contains("img.highresfightinghud.frame"));
    }

    @Test
    public void unifiedInstallerOwnsHooksInsteadOfLegacyEntrypoints()
            throws Exception {
        File root = new File(System.getProperty("highResHudProjectDir"));
        String source = new String(Files.readAllBytes(new File(root,
                "src/main/java/org/highreshud/client/UnifiedHookInstaller.java")
                .toPath()), StandardCharsets.UTF_8);
        assertTrue(source.contains("HighResHudRuntime"));
        assertTrue(source.contains(".actionSent($1,$2,$3)"));
        assertTrue(source.contains(".serverText($1,$5)"));
        assertFalse(source.contains("HighResFocusBarMod.actionSent($3"));
        assertFalse(source.contains("HighResFightingHudMod.actionSent($3"));
    }

    @Test
    public void componentAdaptersContainNoStandaloneModLoadersOrHooks()
            throws Exception {
        File root = new File(System.getProperty("highResHudProjectDir"));
        String[] adapters = {
                "src/main/java/org/highreshealthbar/client/HighResHealthBarMod.java",
                "src/main/java/org/highresfocusbar/client/HighResFocusBarMod.java",
                "src/main/java/org/highresfightinghud/client/HighResFightingHudMod.java"
        };
        for (String adapter : adapters) {
            String source = new String(Files.readAllBytes(
                    new File(root, adapter).toPath()), StandardCharsets.UTF_8);
            assertFalse(adapter, source.contains("implements WurmClientMod"));
            assertFalse(adapter, source.contains("HookManager"));
            assertFalse(adapter, source.contains("void preInit("));
        }
    }
}
