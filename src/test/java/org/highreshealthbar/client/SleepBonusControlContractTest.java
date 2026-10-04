package org.highreshealthbar.client;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

public class SleepBonusControlContractTest {
    @Test
    public void sleepGaugeOwnsAButtonAndActivationCountdown() throws Exception {
        File root = new File(System.getProperty("highResHudProjectDir"));
        String panel = read(root,
                "src/main/java/com/wurmonline/client/renderer/gui/"
                        + "HighResHealthBar.java");
        String layout = read(root,
                "src/main/java/com/wurmonline/client/renderer/gui/"
                        + "HighResHealthBarLayout.java");

        assertTrue(layout.contains("SLEEP_BUTTON_WIDTH = 76"));
        assertTrue(panel.contains("drawSleepBonusControl"));
        assertTrue(panel.contains("active ? \"Deactivate\" : \"Activate\""));
        assertTrue(panel.contains("overSleepBonusButton(mouseX, mouseY)"));
        assertTrue(panel.contains("sleepBonusActivationCooldownSeconds()"));
        assertTrue(panel.contains("SleepBonusToggleTracker.formatCountdown"));
    }

    private static String read(File root, String path) throws Exception {
        return new String(Files.readAllBytes(new File(root, path).toPath()),
                StandardCharsets.UTF_8);
    }
}
