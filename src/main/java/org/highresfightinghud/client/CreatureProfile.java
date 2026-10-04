package org.highresfightinghud.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Stable server-local identity extracted from Wurm's decorated creature name. */
public final class CreatureProfile {
    private static final Set<String> AGES = new HashSet<>(Arrays.asList(
            "young", "adolescent", "mature", "aged", "old", "venerable"));
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList(
            "alert", "angry", "champion", "diseased", "fierce", "greenish",
            "hardened", "lurking", "raging", "scared", "slow", "sly"));

    private final String displayName;
    private final String creatureType;
    private final String age;
    private final String condition;

    private CreatureProfile(String displayName, String creatureType,
                            String age, String condition) {
        this.displayName = displayName;
        this.creatureType = creatureType;
        this.age = age;
        this.condition = condition;
    }

    public static CreatureProfile fromName(String name) {
        String display = clean(name);
        String normalized = display.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9' -]", " ")
                .replaceAll("\\s+", " ").trim();
        normalized = normalized.replaceFirst("^(?:a|an|the)\\s+", "");
        if (normalized.isEmpty()) {
            return new CreatureProfile(display, "unknown", "", "");
        }
        String age = "";
        String condition = "";
        String[] words = normalized.split(" ");
        int firstTypeWord = 0;
        while (firstTypeWord < words.length) {
            String word = words[firstTypeWord];
            if (AGES.contains(word)) age = word;
            else if (MODIFIERS.contains(word)) condition = append(condition, word);
            else break;
            firstTypeWord++;
        }
        StringBuilder type = new StringBuilder();
        for (int i = firstTypeWord; i < words.length; i++) {
            if (type.length() > 0) type.append(' ');
            type.append(words[i]);
        }
        String resultType = type.length() == 0 ? normalized : type.toString();
        return new CreatureProfile(display, resultType, age, condition);
    }

    public CreatureProfile withExamineName(String examinedName) {
        CreatureProfile examined = fromName(examinedName);
        if ("unknown".equals(examined.creatureType)) return this;
        return examined;
    }

    public String displayName() {
        return displayName;
    }

    public String creatureType() {
        return creatureType;
    }

    public String age() {
        return age;
    }

    public String condition() {
        return condition;
    }

    public String knowledgeKey() {
        return creatureType;
    }

    public String variantKey() {
        return creatureType + '|' + age + '|' + condition;
    }

    public String descriptor() {
        String result = age;
        result = append(result, condition);
        return result.isEmpty() ? creatureType : result + " " + creatureType;
    }

    private static String append(String left, String right) {
        if (right == null || right.isEmpty()) return left;
        return left == null || left.isEmpty() ? right : left + " " + right;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
