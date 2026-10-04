package org.highreshud.client.portrait;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Read-only schemaVersion=1 database used by the production HUD. */
public final class PortraitProfileRepository {
    public static final String BUNDLED_RESOURCE = "portrait-profiles.properties";

    private final List<PortraitProfile> profiles;

    private PortraitProfileRepository(List<PortraitProfile> profiles) {
        this.profiles = Collections.unmodifiableList(profiles);
    }

    public static PortraitProfileRepository load(File optionalOverride)
            throws IOException {
        Map<String, PortraitProfile> bundled;
        try (InputStream input = openBundled(optionalOverride)) {
            if (input == null) {
                throw new IOException("Bundled portrait database is missing: "
                        + BUNDLED_RESOURCE);
            }
            bundled = read(input);
        }

        List<PortraitProfile> ordered = new ArrayList<>();
        if (optionalOverride != null && optionalOverride.isFile()) {
            try (InputStream input = new BufferedInputStream(
                    new FileInputStream(optionalOverride))) {
                // Override profiles are deliberately resolved first, even when
                // their generated ids differ from the bundled snapshot.
                ordered.addAll(read(input).values());
            }
        }
        ordered.addAll(bundled.values());
        return new PortraitProfileRepository(ordered);
    }

    /**
     * Wurm's shared mod classloader can load classes from the mod JAR while
     * hiding package resources from Class.getResourceAsStream. The dist also
     * carries the immutable snapshot beside the JAR, so try both mechanisms.
     */
    private static InputStream openBundled(File optionalOverride)
            throws IOException {
        return openBundled(optionalOverride, true);
    }

    static InputStream openExternalBundledForTest(File optionalOverride)
            throws IOException {
        return openBundled(optionalOverride, false);
    }

    private static InputStream openBundled(File optionalOverride,
                                           boolean useClasspath)
            throws IOException {
        InputStream input = null;
        if (useClasspath) {
            input = PortraitProfileRepository.class
                    .getResourceAsStream(BUNDLED_RESOURCE);
            if (input != null) return input;

            String absolute = "org/highresfocusbar/client/portrait/"
                    + BUNDLED_RESOURCE;
            ClassLoader ownLoader = PortraitProfileRepository.class.getClassLoader();
            if (ownLoader != null) input = ownLoader.getResourceAsStream(absolute);
            if (input != null) return input;
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null) {
                input = contextLoader.getResourceAsStream(absolute);
            }
            if (input != null) return input;
        }

        File parent = optionalOverride == null ? null : optionalOverride.getParentFile();
        File external = parent == null ? null
                : new File(parent, "portrait-profiles.bundled.properties");
        return external != null && external.isFile()
                ? new BufferedInputStream(new FileInputStream(external)) : null;
    }

    static PortraitProfileRepository loadForTest(InputStream input)
            throws IOException {
        return new PortraitProfileRepository(
                new ArrayList<>(read(input).values()));
    }

    public PortraitProfile resolve(String mapping, String resourcePath) {
        String cleanMapping = clean(mapping);
        String cleanResource = clean(resourcePath);
        for (PortraitProfile profile : profiles) {
            if (profile.matchType == PortraitProfile.MatchType.EXACT
                    && !cleanMapping.isEmpty()
                    && cleanMapping.equalsIgnoreCase(profile.match)) {
                return profile.copy();
            }
        }
        for (PortraitProfile profile : profiles) {
            if (!cleanResource.isEmpty()
                    && cleanResource.equalsIgnoreCase(profile.resourcePath)) {
                return profile.copy();
            }
        }

        String lower = cleanMapping.toLowerCase(Locale.ROOT);
        PortraitProfile best = null;
        int bestLength = -1;
        for (PortraitProfile profile : profiles) {
            if (profile.matchType != PortraitProfile.MatchType.PREFIX) continue;
            String prefix = profile.match.toLowerCase(Locale.ROOT);
            if (!prefix.isEmpty() && lower.startsWith(prefix)
                    && prefix.length() > bestLength) {
                best = profile;
                bestLength = prefix.length();
            }
        }
        return best == null ? null : best.copy();
    }

    /**
     * A few server creature variants expose a stale/default model mapping
     * while their live renderable already owns the correct model resource.
     * For a live creature that concrete resource is authoritative; items keep
     * the normal mapping-first behaviour because their wrapper is frequently
     * replaced or decorated at runtime.
     */
    public PortraitProfile resolveForSubject(String mapping,
                                             String resourcePath,
                                             boolean liveCreature) {
        if (liveCreature && !clean(resourcePath).isEmpty()) {
            PortraitProfile resourceProfile = resolve("", resourcePath);
            if (resourceProfile != null) return resourceProfile;
        }
        return resolve(mapping, resourcePath);
    }

    public int size() {
        return profiles.size();
    }

    private static Map<String, PortraitProfile> read(InputStream input)
            throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        Map<String, PortraitProfile> loaded = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("profile.")) continue;
            int separator = key.indexOf('.', "profile.".length());
            if (separator < 0) continue;
            String id = key.substring("profile.".length(), separator);
            if (id.isEmpty()) continue;
            PortraitProfile profile = loaded.get(id);
            if (profile == null) {
                profile = new PortraitProfile();
                profile.id = id;
                loaded.put(id, profile);
            }
            assign(profile, key.substring(separator + 1),
                    properties.getProperty(key));
        }
        Map<String, PortraitProfile> valid = new LinkedHashMap<>();
        for (PortraitProfile profile : loaded.values()) {
            profile.normalize();
            if (!profile.match.isEmpty() || !profile.resourcePath.isEmpty()) {
                valid.put(profile.id, profile);
            }
        }
        return valid;
    }

    private static void assign(PortraitProfile profile, String field,
                               String value) {
        try {
            switch (field) {
                case "match": profile.match = clean(value); break;
                case "resourcePath": profile.resourcePath = clean(value); break;
                case "matchType": profile.matchType = PortraitProfile.MatchType
                        .valueOf(clean(value).toUpperCase(Locale.ROOT)); break;
                case "anchor": profile.anchor = PortraitProfile.Anchor
                        .valueOf(clean(value).toUpperCase(Locale.ROOT)); break;
                case "joint": profile.joint = clean(value); break;
                case "offset": {
                    float[] v = floats(value, 3);
                    profile.offsetX = v[0]; profile.offsetY = v[1];
                    profile.offsetZ = v[2]; break;
                }
                case "camera": {
                    float[] v = floats(value, 3);
                    profile.cameraX = v[0]; profile.cameraY = v[1];
                    profile.cameraDistance = v[2]; break;
                }
                case "fov": profile.fov = Float.parseFloat(value); break;
                case "zoom": profile.zoom = Float.parseFloat(value); break;
                case "zoomRange": {
                    float[] v = floats(value, 2);
                    profile.zoomMin = v[0]; profile.zoomMax = v[1]; break;
                }
                case "yaw": profile.yaw = Float.parseFloat(value); break;
                case "yawRange": {
                    float[] v = floats(value, 2);
                    profile.yawMin = v[0]; profile.yawMax = v[1]; break;
                }
                case "pitch": profile.pitch = Float.parseFloat(value); break;
                case "pitchRange": {
                    float[] v = floats(value, 2);
                    profile.pitchMin = v[0]; profile.pitchMax = v[1]; break;
                }
                case "mode": profile.mode = PortraitProfile.Mode
                        .valueOf(clean(value).toUpperCase(Locale.ROOT)); break;
                case "crop": {
                    float[] v = floats(value, 4);
                    profile.cropX = v[0]; profile.cropY = v[1];
                    profile.cropWidth = v[2]; profile.cropHeight = v[3]; break;
                }
                case "autoTurnSpeed": profile.autoTurnSpeed =
                        Float.parseFloat(value); break;
                case "mouseRotation": profile.mouseRotation =
                        Boolean.parseBoolean(value); break;
                case "fallbackIcon": profile.fallbackIcon = clean(value); break;
                default: break;
            }
        } catch (RuntimeException ignored) {
            // A malformed optional field falls back to PortraitProfile defaults.
        }
    }

    private static float[] floats(String value, int expected) {
        String[] parts = clean(value).split(",");
        if (parts.length != expected) throw new IllegalArgumentException(value);
        float[] result = new float[expected];
        for (int i = 0; i < expected; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
