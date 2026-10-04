package org.highresfocusbar.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Examine-derived metadata for selected world subjects. The outgoing target
 * ID owns every recognized response line, so GroundItem renderables (forges,
 * ovens, containers, and similar objects) are handled exactly like the older
 * creature-backed vehicle representation.
 */
public final class SelectedExamineState {
    private static final long NONE = Long.MIN_VALUE;
    private static final long EXPECTATION_NANOS = 15_000_000_000L;

    private static final Pattern QUALITY = Pattern.compile(
            "(?i)\\bql\\s*[:=]?\\s*([0-9]+(?:[\\.,][0-9]+)?)");
    private static final Pattern DAMAGE = Pattern.compile(
            "(?i)\\b(?:dam(?:age)?|dmg)\\s*[:=]?\\s*"
                    + "([0-9]+(?:[\\.,][0-9]+)?)");
    private static final Pattern PREGNANT = Pattern.compile(
            "(?i)\\bwill deliver in about\\s+([0-9]+)\\s+days?\\b");
    private static final Pattern CARE = Pattern.compile(
            "(?i)\\bit is being taken care of by\\s+([^.!?]+)");
    private static final Pattern BRAND = Pattern.compile(
            "(?i)\\bit has been branded by and belongs to the settlement of\\s+"
                    + "([^.!?]+)");
    private static final Pattern OWNER = Pattern.compile(
            "(?i)\\bthe name of the owner,\\s*([^,]+),\\s*has been etched\\b");
    private static final Pattern RUNE = Pattern.compile(
            "(?i)(?:^|\\s)rune\\s*:\\s*([^\\r\\n]+)");

    private static final String RUNE_STATUS = "rune";
    private static final String LOYALTY = "loyalty";
    private static final String FIRE = "fire";
    private static final String HEAT = "heat";
    private static final String LOCK = "lock";
    private static final String SECURED = "secured";
    private static final String PREGNANCY = "pregnancy";
    private static final String GROOMING = "grooming";
    private static final String CARE_STATUS = "care";
    private static final String BRAND_STATUS = "brand";
    private static final String OWNER_STATUS = "owner";
    private static final String SEAL = "seal";
    private static final String[] DETAIL_ORDER = new String[]{
            RUNE_STATUS, LOYALTY, FIRE, HEAT, LOCK, SECURED, PREGNANCY,
            GROOMING, CARE_STATUS, BRAND_STATUS, OWNER_STATUS, SEAL
    };

    private final Map<Long, Snapshot> byId = new HashMap<>();
    private long expectedId = NONE;
    private long expectedUntil;

    public synchronized void expect(long targetId) {
        if (targetId == NONE) return;
        // A fresh Examine is authoritative. Do not display a stale fire,
        // lock, load, or animal state while the replacement response arrives.
        byId.remove(targetId);
        expectedId = targetId;
        expectedUntil = System.nanoTime() + EXPECTATION_NANOS;
    }

    public synchronized boolean observe(String title, String message) {
        return observeFor(NONE, title, message);
    }

    /**
     * Uses the outgoing Examine target when available and the currently
     * selected subject as a fallback for stock action paths that bypass the
     * outgoing-action callback.
     */
    public synchronized boolean observeFor(long selectedId,
                                            String title, String message) {
        return observeResultFor(selectedId, title, message)
                == Observation.EXAMINE;
    }

    public synchronized Observation observeResultFor(long selectedId,
                                                       String title,
                                                       String message) {
        if (!isEventTitle(title)) return Observation.NONE;
        long now = System.nanoTime();
        long targetId = expectedId != NONE && now <= expectedUntil
                ? expectedId : selectedId;
        if (targetId == NONE) return Observation.NONE;
        if (isRetryableFailure(message)) {
            return Observation.RETRYABLE_FAILURE;
        }

        Delta parsed = parse(message);
        if (!parsed.examineRelated) return Observation.NONE;

        Snapshot previous = byId.get(targetId);
        Float quality = parsed.quality != null ? parsed.quality
                : previous == null ? null : previous.quality;
        Float damage = parsed.damage != null ? parsed.damage
                : previous == null ? null : previous.damage;
        Map<String, String> details = previous == null
                ? new LinkedHashMap<String, String>()
                : new LinkedHashMap<String, String>(previous.details);
        details.putAll(parsed.details);
        byId.put(targetId, new Snapshot(quality, damage, details));
        return Observation.EXAMINE;
    }

    public synchronized Snapshot get(long targetId) {
        return byId.get(targetId);
    }

    public static boolean isEventTitle(String title) {
        if (title == null) return false;
        String normalized = title.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("event") || normalized.equals(":event");
    }

    private static boolean isRetryableFailure(String message) {
        if (message == null) return false;
        String value = message.trim().toLowerCase(Locale.ROOT);
        return value.equals("you are too far away to do that.")
                || value.equals("you are too far away from that right now.")
                || value.startsWith("you can't reach ")
                || value.startsWith("you need to get closer to ")
                || value.startsWith("you need to be closer to ")
                || value.endsWith(" is too far away.")
                || value.endsWith(" is in the way.");
    }

    private static Delta parse(String message) {
        Delta result = new Delta();
        if (message == null || message.trim().isEmpty()) return result;

        String lower = message.trim().toLowerCase(Locale.ROOT);
        result.quality = metric(message, QUALITY);
        result.damage = metric(message, DAMAGE);
        if (result.quality != null || result.damage != null) {
            result.examineRelated = true;
        }
        if (lower.startsWith("you are looking at ")) {
            result.examineRelated = true;
        }

        rune(result, message);
        loyalty(result, lower);
        fire(result, lower);
        heat(result, lower);
        lock(result, lower);
        creatureCare(result, message, lower);
        ownership(result, message, lower);
        return result;
    }

    private static void rune(Delta result, String message) {
        Matcher rune = RUNE.matcher(message);
        if (!rune.find()) return;
        String effect = compact(rune.group(1), 48);
        if (!effect.isEmpty()) {
            result.detail(RUNE_STATUS, "Rune: " + effect);
        }
    }

    private static Float metric(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) return null;
        try {
            return Float.valueOf(Float.parseFloat(
                    matcher.group(1).replace(',', '.')));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void loyalty(Delta result, String value) {
        if (value.contains(" looks extremely loyal.")) {
            result.detail(LOYALTY, "Extremely loyal");
        } else if (value.contains(" looks trusting.")) {
            result.detail(LOYALTY, "Trusting");
        } else if (value.contains(" acts loyal.")) {
            result.detail(LOYALTY, "Loyal");
        } else if (value.contains(" looks tame.")) {
            result.detail(LOYALTY, "Tame");
        } else if (value.contains(" looks calm.")) {
            result.detail(LOYALTY, "Calm");
        } else if (value.contains(" looks submissive.")) {
            result.detail(LOYALTY, "Submissive");
        } else if (value.contains(" acts nervously.")) {
            result.detail(LOYALTY, "Nervous");
        } else if (value.contains(" looks upset.")) {
            result.detail(LOYALTY, "Upset");
        }
    }

    private static void fire(Delta result, String value) {
        if (value.contains("the fire is not lit.")) {
            result.detail(FIRE, "Unlit");
        } else if (value.contains("wisps of smoke steadily coming out")) {
            result.detail(FIRE, "Smoking");
        } else if (value.contains("a few red glowing coals can be found")) {
            result.detail(FIRE, "Embers");
        } else if (value.contains("a layer of ashes is starting to form")) {
            result.detail(FIRE, "Glowing coals");
        } else if (value.contains("only a hot, red glowing bed of coal")) {
            result.detail(FIRE, "Hot coals");
        } else if (value.contains("a few flames still dance on the fire")) {
            result.detail(FIRE, "Dying flames");
        } else if (value.contains("the fire is starting to fade.")) {
            result.detail(FIRE, "Fading");
        } else if (value.contains("the fire burns with wild flames")) {
            result.detail(FIRE, "Wild flames");
        } else if (value.contains("the fire burns steadily and will still burn"
                + " for a long time.")) {
            result.detail(FIRE, "Long burn");
        }
    }

    private static void heat(Delta result, String value) {
        if (value.contains("it is glowing from the heat.")) {
            result.detail(HEAT, "Glowing");
        } else if (value.contains("it is searing hot.")) {
            result.detail(HEAT, "Searing");
        } else if (value.contains("it is boiling.")) {
            result.detail(HEAT, "Boiling");
        } else if (value.contains("it is hot.")) {
            result.detail(HEAT, "Hot");
        } else if (value.contains("it is very warm.")) {
            result.detail(HEAT, "Warm");
        } else if (value.contains("it is frozen.")) {
            result.detail(HEAT, "Frozen");
        }
    }

    private static void lock(Delta result, String value) {
        if (value.contains("quality, which is unlocked.")) {
            result.detail(LOCK, "Unlocked");
        } else if (value.contains("it is locked with a lock of")) {
            result.detail(LOCK, "Locked");
        }
        if (value.contains("it has a security seal")) {
            result.detail(SEAL, "Sealed");
        }
    }

    private static void creatureCare(Delta result, String message,
                                     String value) {
        Matcher pregnant = PREGNANT.matcher(message);
        if (pregnant.find()) {
            result.detail(PREGNANCY,
                    "Pregnant ~" + pregnant.group(1) + "d");
        }
        if (value.contains("this creature could use some grooming.")) {
            result.detail(GROOMING, "Ungroomed");
        }
        Matcher care = CARE.matcher(message);
        if (care.find()) {
            result.detail(CARE_STATUS,
                    "Care " + compact(care.group(1), 24));
        }
        Matcher brand = BRAND.matcher(message);
        if (brand.find()) {
            result.detail(BRAND_STATUS,
                    "Brand " + compact(brand.group(1), 24));
        }
    }

    private static void ownership(Delta result, String message,
                                  String value) {
        if (value.contains("has been firmly secured to the ground by")
                || value.contains("is firmly planted in the ground.")) {
            result.detail(SECURED, "Secured");
        }
        Matcher owner = OWNER.matcher(message);
        if (owner.find()) {
            result.detail(OWNER_STATUS,
                    "Owner " + compact(owner.group(1), 24));
        }
    }

    private static String compact(String value, int limit) {
        if (value == null) return "";
        String result = value.trim().replaceAll("\\s+", " ");
        if (result.length() <= limit) return result;
        return result.substring(0, Math.max(0, limit - 1)).trim() + "…";
    }

    private static final class Delta {
        private boolean examineRelated;
        private Float quality;
        private Float damage;
        private final Map<String, String> details = new LinkedHashMap<>();

        private void detail(String category, String text) {
            examineRelated = true;
            details.put(category, text);
        }
    }

    public enum Observation {
        NONE,
        EXAMINE,
        RETRYABLE_FAILURE
    }

    public static final class Snapshot {
        private final Float quality;
        private final Float damage;
        private final Map<String, String> details;

        private Snapshot(Float quality, Float damage,
                         Map<String, String> details) {
            this.quality = quality;
            this.damage = damage;
            this.details = details;
        }

        public boolean hasQualityAndDamage() {
            return quality != null && damage != null
                    && Float.isFinite(quality.floatValue())
                    && Float.isFinite(damage.floatValue());
        }

        public float quality() {
            return quality == null ? Float.NaN : quality.floatValue();
        }

        public float damage() {
            return damage == null ? Float.NaN : damage.floatValue();
        }

        public List<String> details() {
            List<String> result = new ArrayList<>();
            for (String category : DETAIL_ORDER) {
                String detail = details.get(category);
                if (detail != null && !detail.isEmpty()) result.add(detail);
            }
            return result;
        }
    }
}
