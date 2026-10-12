package org.highreshud.client;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/** Wurm's shared Javassist loader exposes mod classes but can hide their resources. */
public final class HudPackagedResources {
    private HudPackagedResources() { }

    public static InputStream open(String entryName) throws IOException {
        InputStream visible = HudPackagedResources.class.getResourceAsStream("/" + entryName);
        if (visible != null) return visible;
        Path archive = Paths.get("mods", "highres-hud", "highres-hud-" + HighResHudMod.VERSION + ".jar");
        if (!Files.isRegularFile(archive)) return null;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null || entry.isDirectory()) return null;
            try (InputStream input = zip.getInputStream(entry);
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                for (int count; (count = input.read(buffer)) != -1;) bytes.write(buffer, 0, count);
                return new ByteArrayInputStream(bytes.toByteArray());
            }
        }
    }
}
