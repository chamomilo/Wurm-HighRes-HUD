package org.highreshealthbar.client;

import java.util.Locale;
import java.util.regex.Pattern;

/** Removes transient client-mod decorations from carrier creature names. */
public final class MountStatusText {
    private static final Pattern LEADING_DISTANCE = Pattern.compile(
            "(?i)^\\s*(?:\\[\\s*)?\\d+(?:[\\.,]\\d+)?\\s*m"
                    + "(?:\\s*\\])?\\s*(?:[-:|]\\s*)?");
    private static final Pattern QUALITY = Pattern.compile(
            "(?i)\\bql\\s*[:=]?\\s*(\\d+(?:[\\.,]\\d+)?)");
    private static final Pattern DAMAGE = Pattern.compile(
            "(?i)\\b(?:dam(?:age)?|dmg)\\s*[:=]?\\s*"
                    + "(\\d+(?:[\\.,]\\d+)?)");

    private MountStatusText() {
    }

    public static String cleanCreatureName(String value) {
        if (value == null) return "";
        return LEADING_DISTANCE.matcher(value).replaceFirst("").trim();
    }

    public static String vehicleQuality(String value) {
        return metric(value, QUALITY);
    }

    public static String vehicleDamage(String value) {
        return metric(value, DAMAGE);
    }

    public static String vehicleQuality(float value) {
        return metric(value);
    }

    public static String vehicleDamage(float value) {
        return metric(value);
    }

    private static String metric(String source, Pattern pattern) {
        java.util.regex.Matcher matcher = pattern.matcher(
                source == null ? "" : source);
        if (!matcher.find()) return "--";
        try {
            float value = Float.parseFloat(matcher.group(1).replace(',', '.'));
            return twoDecimals(value);
        } catch (NumberFormatException ignored) {
            return matcher.group(1);
        }
    }

    private static String metric(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return "--";
        return twoDecimals(value);
    }

    private static String twoDecimals(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
