package org.highresfightinghud.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Persistent knowledge, isolated by server and character. */
public final class CombatKnowledgeBook {
    private static final long FLUSH_INTERVAL_NANOS = 2_000_000_000L;

    private final Path rootDirectory;
    private final Map<String, CombatKnowledge> creatures =
            new LinkedHashMap<>();
    private Path activeFile;
    private boolean dirty;
    private long lastFlushNanos;

    public CombatKnowledgeBook(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public synchronized void activate(String server, String character) {
        Path next = rootDirectory.resolve(safe(server))
                .resolve(safe(character) + ".properties");
        if (next.equals(activeFile)) return;
        flush();
        activeFile = next;
        creatures.clear();
        load();
    }

    public synchronized CombatKnowledge.Snapshot snapshot(
            String creatureKey, int currentStance) {
        return creature(creatureKey).snapshot(currentStance);
    }

    public synchronized void recordEncounter(String creatureKey) {
        creature(creatureKey).recordEncounter();
        dirty = true;
    }

    public synchronized void recordKill(String creatureKey) {
        creature(creatureKey).recordKill();
        dirty = true;
    }

    public synchronized void observe(String creatureKey,
                                     CombatObservation observation,
                                     int playerStance, int targetStance,
                                     boolean shieldEligible) {
        if (observation == null) return;
        creature(creatureKey).observe(observation, playerStance,
                targetStance, shieldEligible);
        dirty = true;
    }

    public synchronized void tick(long nowNanos) {
        if (dirty && nowNanos - lastFlushNanos >= FLUSH_INTERVAL_NANOS) {
            flush();
        }
    }

    public synchronized void flush() {
        if (!dirty || activeFile == null) return;
        Properties properties = new Properties();
        properties.setProperty("format", "1");
        for (Map.Entry<String, CombatKnowledge> entry : creatures.entrySet()) {
            String encoded = encode(entry.getKey());
            properties.setProperty("mob." + encoded + ".name", entry.getKey());
            entry.getValue().store(properties, "mob." + encoded + ".");
        }
        try {
            Files.createDirectories(activeFile.getParent());
            Path temporary = activeFile.resolveSibling(
                    activeFile.getFileName().toString() + ".tmp");
            try (OutputStream stream = Files.newOutputStream(temporary)) {
                properties.store(stream,
                        "High-res Fighting HUD learned creature data");
            }
            try {
                Files.move(temporary, activeFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, activeFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            lastFlushNanos = System.nanoTime();
        } catch (IOException ignored) {
            // The in-memory model remains usable and will retry next tick.
        }
    }

    public synchronized Path activeFile() {
        return activeFile;
    }

    private CombatKnowledge creature(String key) {
        String normalized = key == null || key.trim().isEmpty()
                ? "unknown" : key.trim().toLowerCase(Locale.ROOT);
        CombatKnowledge result = creatures.get(normalized);
        if (result == null) {
            result = new CombatKnowledge();
            creatures.put(normalized, result);
        }
        return result;
    }

    private void load() {
        if (activeFile == null || !Files.isRegularFile(activeFile)) return;
        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(activeFile)) {
            properties.load(stream);
            for (String name : properties.stringPropertyNames()) {
                if (!name.startsWith("mob.") || !name.endsWith(".name")) {
                    continue;
                }
                String encoded = name.substring(4, name.length() - 5);
                String key = properties.getProperty(name, decode(encoded));
                creatures.put(key, CombatKnowledge.load(properties,
                        "mob." + encoded + "."));
            }
        } catch (IOException | RuntimeException ignored) {
            creatures.clear();
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value),
                    StandardCharsets.UTF_8);
        } catch (RuntimeException ignored) {
            return "unknown";
        }
    }

    private static String safe(String value) {
        String result = value == null ? "unknown" : value.trim()
                .replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("^_+|_+$", "");
        return result.isEmpty() ? "unknown" : result;
    }
}
