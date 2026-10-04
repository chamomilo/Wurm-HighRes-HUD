package org.highresfightinghud.client;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative parser for vanilla English WU 1.9 combat messages. */
public final class CombatObservationParser {
    private static final Pattern BODY_PART = Pattern.compile(
            "(?i)\\b(?:in|to) the ([a-z][a-z '-]{1,40})(?:[.!]| and | but )");

    private CombatObservationParser() {
    }

    public static CombatObservation parse(String title, String message,
                                           String targetName) {
        if (!isCombatTitle(title) || message == null) return null;
        String value = message.trim();
        if (value.isEmpty()) return null;
        String lower = value.toLowerCase(Locale.ROOT);
        String target = cleanTarget(targetName);

        if (lower.startsWith("you miss with the ")
                || lower.startsWith("you barely miss with the ")) {
            return observation(CombatObservation.Direction.OUTGOING,
                    CombatObservation.Outcome.MISS, lower);
        }
        if (lower.startsWith("your attack glances off ")) {
            return observation(CombatObservation.Direction.OUTGOING,
                    CombatObservation.Outcome.GLANCE, lower);
        }
        if (lower.startsWith("you parry with your ")) {
            return observation(CombatObservation.Direction.INCOMING,
                    CombatObservation.Outcome.PARRY, lower);
        }
        if (lower.startsWith("you raise your shield")
                || lower.contains(" your shield and parry against ")) {
            return observation(CombatObservation.Direction.INCOMING,
                    CombatObservation.Outcome.SHIELD, lower);
        }
        if (lower.startsWith("you evade the blow")) {
            return observation(CombatObservation.Direction.INCOMING,
                    CombatObservation.Outcome.EVADE, lower);
        }
        if (lower.contains(" glances off your armour")) {
            return observation(CombatObservation.Direction.INCOMING,
                    CombatObservation.Outcome.GLANCE, lower);
        }

        boolean mentionsTarget = target.isEmpty() || lower.contains(target);
        if (mentionsTarget && lower.contains(" parries with ")
                && !lower.startsWith("you ")) {
            return observation(CombatObservation.Direction.OUTGOING,
                    CombatObservation.Outcome.PARRY, lower);
        }
        if (mentionsTarget && lower.startsWith("you ")
                && containsAttackVerb(lower)) {
            return observation(CombatObservation.Direction.OUTGOING,
                    CombatObservation.Outcome.HIT, lower);
        }
        if (mentionsTarget && !lower.startsWith("you ")
                && lower.contains(" you") && isMiss(lower)) {
            return observation(CombatObservation.Direction.INCOMING,
                    CombatObservation.Outcome.MISS, lower);
        }
        if (mentionsTarget && !lower.startsWith("you ")
                && lower.contains(" you") && containsAttackVerb(lower)) {
            return observation(CombatObservation.Direction.INCOMING,
                    CombatObservation.Outcome.HIT, lower);
        }
        return null;
    }

    public static boolean isCombatTitle(String title) {
        if (title == null) return false;
        String value = title.trim().toLowerCase(Locale.ROOT);
        return value.equals("combat") || value.equals(":combat");
    }

    private static CombatObservation observation(
            CombatObservation.Direction direction,
            CombatObservation.Outcome outcome, String lower) {
        return new CombatObservation(direction, outcome, damageType(lower),
                bodyPart(lower), severity(lower));
    }

    private static boolean containsAttackVerb(String value) {
        return containsWord(value, "maul") || containsWord(value, "hit")
                || containsWord(value, "strike") || containsWord(value, "kick")
                || containsWord(value, "bite") || containsWord(value, "claw")
                || containsWord(value, "slash") || containsWord(value, "cut")
                || containsWord(value, "pierce") || containsWord(value, "sting")
                || containsWord(value, "headbutt") || containsWord(value, "bash")
                || containsWord(value, "burn") || containsWord(value, "scorch")
                || containsWord(value, "freeze") || containsWord(value, "acid");
    }

    private static boolean isMiss(String value) {
        return value.contains(" misses you") || value.contains(" barely misses")
                || value.contains(" isn't even close")
                || value.contains(" swings a huge hole in the air");
    }

    private static boolean containsWord(String value, String root) {
        return value.matches(".*\\b" + Pattern.quote(root) + "[a-z]*\\b.*");
    }

    private static CombatObservation.DamageType damageType(String value) {
        if (containsWord(value, "bite")) return CombatObservation.DamageType.BITE;
        if (containsWord(value, "slash") || containsWord(value, "cut")
                || containsWord(value, "claw") || containsWord(value, "rake")) {
            return CombatObservation.DamageType.SLASH;
        }
        if (containsWord(value, "pierce") || containsWord(value, "sting")
                || containsWord(value, "gore") || containsWord(value, "stab")) {
            return CombatObservation.DamageType.PIERCE;
        }
        if (containsWord(value, "burn") || containsWord(value, "scorch")
                || containsWord(value, "fire")) return CombatObservation.DamageType.BURN;
        if (containsWord(value, "cold") || containsWord(value, "freeze")
                || containsWord(value, "frost")) return CombatObservation.DamageType.COLD;
        if (containsWord(value, "acid") || containsWord(value, "corrode")) {
            return CombatObservation.DamageType.ACID;
        }
        if (containsWord(value, "poison")) return CombatObservation.DamageType.POISON;
        if (containsWord(value, "internal")) return CombatObservation.DamageType.INTERNAL;
        if (containsWord(value, "maul") || containsWord(value, "kick")
                || containsWord(value, "headbutt") || containsWord(value, "bash")
                || containsWord(value, "strike") || containsWord(value, "hit")) {
            return CombatObservation.DamageType.CRUSH;
        }
        return CombatObservation.DamageType.UNKNOWN;
    }

    private static String bodyPart(String value) {
        Matcher matcher = BODY_PART.matcher(value);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static double severity(String value) {
        if (value.contains("deadly hard")) return 1.0;
        if (value.contains("extremely hard")) return 0.90;
        if (value.contains("very hard")) return 0.72;
        if (value.contains("pretty hard")) return 0.52;
        if (value.matches(".*\\bhard\\b.*")) return 0.42;
        if (value.contains("very lightly")) return 0.09;
        if (value.contains("lightly")) return 0.18;
        if (value.contains("unnoticeably")) return 0.03;
        return 0.25;
    }

    private static String cleanTarget(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceFirst("^(?:a|an|the)\\s+", "").trim();
    }
}
