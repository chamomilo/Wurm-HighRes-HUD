package org.highreshud.client.i18n;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.highreshud.client.HudPackagedResources;

/** Mod-owned display text only: server messages, commands and model keys stay intact. */
public final class HudMessages {
    private static final String[] CODES = {"en", "pt-BR", "de", "ru"};
    private static final Map<String, String[]> STRINGS = load();
    private static final Pattern ARGUMENT = Pattern.compile("\\{(\\d+)\\}");
    private static volatile String language = "en";
    private static volatile int languageIndex;
    private static volatile long revision;
    private HudMessages() { }

    public static String[] languageCodes() { return CODES.clone(); }
    public static String language() { return language; }
    public static long revision() { return revision; }
    public static String normalize(String code) {
        if (code == null) throw new IllegalArgumentException("Language is required");
        String tag = Locale.forLanguageTag(code.trim().replace('_', '-')).toLanguageTag();
        for (String supported : CODES) if (supported.equalsIgnoreCase(tag)) return supported;
        throw new IllegalArgumentException("Unsupported HUD language: " + code);
    }
    public static synchronized void useLanguage(String code) {
        String selected = normalize(code);
        if (!selected.equals(language)) {
            languageIndex = Arrays.asList(CODES).indexOf(selected);
            language = selected; revision++;
        }
    }
    public static boolean contains(String source) { return STRINGS.containsKey(source); }
    public static Set<String> sources() { return Collections.unmodifiableSet(STRINGS.keySet()); }
    public static String tr(String source, Object... arguments) {
        return format(tr(source), arguments);
    }
    public static String tr(String source) {
        String[] variants = STRINGS.get(source);
        return variants == null ? source : variants[languageIndex];
    }
    public static String translate(String code, String source, Object... arguments) {
        String selected = normalize(code);
        int index = Arrays.asList(CODES).indexOf(selected);
        String[] variants = STRINGS.get(source);
        // Dynamic native action names and unknown server-specific statuses retain their original text.
        return format(variants == null ? source : variants[index], arguments);
    }
    private static String format(String value, Object... arguments) {
        if (arguments.length == 0) return value;
        Matcher matcher = ARGUMENT.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            int argument = Integer.parseInt(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(argument < arguments.length
                    ? String.valueOf(arguments[argument]) : matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }
    /** Translate stored, generated Examine details at display time, preserving names/effects. */
    public static String detail(String source) {
        if ("GM".equals(source)) return tr("Game master");
        if (contains(source)) return tr(source);
        for (String prefix : new String[]{"Rune: ", "Care ", "Brand ", "Owner "})
            if (source.startsWith(prefix)) return tr(prefix) + source.substring(prefix.length());
        if (source.matches("Pregnant ~\\d+d"))
            return tr("Pregnant ~{0}d", source.substring(10, source.length() - 1));
        if (source.matches("Engaging ~\\d/3")) return tr("Engaging ~{0}/3", source.substring(10, 11));
        return source;
    }
    private static Map<String, String[]> load() {
        Map<String, String[]> result = new LinkedHashMap<>();
        try (InputStream stream = HudPackagedResources.open("org/highreshud/client/i18n/messages.tsv")) {
            if (stream == null) throw new IllegalStateException("Missing HUD translations");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null;) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] fields = line.split("\t", -1);
                if (fields.length != CODES.length) throw new IllegalStateException("Invalid translation row: " + line);
                for (String field : fields) if (field.isEmpty()) throw new IllegalStateException("Empty HUD translation: " + line);
                if (result.put(fields[0], fields) != null) throw new IllegalStateException("Duplicate translation: " + fields[0]);
            }
        } catch (IOException failure) { throw new IllegalStateException("Cannot load HUD translations", failure); }
        return Collections.unmodifiableMap(result);
    }
}
