package org.highreshud.client;

import java.io.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.regex.*;

/** Display-only filtering; skill values, affinities and native listeners are untouched. */
public final class SkillDetailSettings {
    // SkillLogic formats small native changes with %.6f; All also retains smaller rounded ticks.
    public static final String[] LEVELS = {"All", "1", "0.1", "0.01", "0.001", "0.0001", "0.00001", "0.000001"};
    private static final Pattern TICK = Pattern.compile("(?i)\\b(?:increased|decreased)\\s+by\\s+([+-]?\\d+(?:[.,]\\d+)?(?:[eE][+-]?\\d+)?)\\b");
    private static volatile int selected;
    private static boolean configured;
    private static Path file;
    private SkillDetailSettings() { }
    public static synchronized void load(Path path) {
        file = path.toAbsolutePath().normalize(); selected = 0; configured = false;
        if (!Files.isRegularFile(file)) return;
        try (InputStream input = Files.newInputStream(file)) {
            Properties saved = new Properties(); saved.load(input);
            int index = Integer.parseInt(saved.getProperty("level", "0"));
            validate(index); selected = index; configured = true;
        } catch (IOException | IllegalArgumentException failure) {
            java.util.logging.Logger.getLogger("HighResHud").warning("Cannot read skill detail preference: " + failure);
        }
    }
    public static int selectedIndex() { return selected; }
    public static synchronized boolean configured() { return configured; }
    public static synchronized void save(int index) {
        validate(index);
        if (file == null) load(Paths.get(System.getProperty("user.home"), ".chamomilo", "highres-hud-skills.properties"));
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "hud-skills-", ".tmp");
            try {
                Properties saved = new Properties(); saved.setProperty("level", Integer.toString(index));
                try (OutputStream output = Files.newOutputStream(temporary)) { saved.store(output, "High-res HUD skill detail level"); }
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException unavailable) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
            selected = index; configured = true;
        } catch (IOException failure) { throw new IllegalStateException("Cannot save skill detail level", failure); }
    }
    public static boolean hidden(String channel, String message) {
        int index = selected;
        if (index == 0 || channel == null || message == null || !(channel.equalsIgnoreCase(":Skills") || channel.equalsIgnoreCase("Skills"))) return false;
        Matcher match = TICK.matcher(message);
        if (!match.find()) return false;
        BigDecimal tick = new BigDecimal(match.group(1).replace(',', '.')).abs();
        return tick.compareTo(new BigDecimal(LEVELS[index])) < 0;
    }
    private static void validate(int index) {
        if (index < 0 || index >= LEVELS.length) throw new IllegalArgumentException("Unknown skill detail level " + index);
    }
}
