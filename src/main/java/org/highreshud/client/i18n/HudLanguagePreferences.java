package org.highreshud.client.i18n;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** The HUD's own user choice survives updater Off and manual ZIP upgrades. */
public final class HudLanguagePreferences {
    private final Path file;
    private String language = "en";
    public HudLanguagePreferences(Path file) {
        this.file = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(this.file)) return;
        Properties saved = new Properties();
        try (InputStream stream = Files.newInputStream(this.file)) {
            saved.load(stream);
            language = HudMessages.normalize(saved.getProperty("language", "en"));
        } catch (IOException | IllegalArgumentException failure) {
            java.util.logging.Logger.getLogger("HighResHud").log(java.util.logging.Level.WARNING,
                    "Cannot read HUD language preference; using English", failure);
        }
    }
    public synchronized String language() { return language; }
    public synchronized void save(String code) throws IOException {
        String selected = HudMessages.normalize(code);
        if (selected.equals(language)) return;
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "hud-language-", ".tmp");
        try {
            Properties saved = new Properties();
            saved.setProperty("language", selected);
            try (OutputStream stream = Files.newOutputStream(temporary)) {
                saved.store(stream, "High-res HUD user language");
            }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException atomicFailure) {
                try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
                catch (IOException failure) { failure.addSuppressed(atomicFailure); throw failure; }
            }
            language = selected;
        } finally { Files.deleteIfExists(temporary); }
    }
}
