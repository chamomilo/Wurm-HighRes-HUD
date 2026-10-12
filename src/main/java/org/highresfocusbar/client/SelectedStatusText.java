package org.highresfocusbar.client;

import java.util.ArrayList;
import java.util.List;
import org.highreshud.client.i18n.HudMessages;

/** Applies the Select Bar's one status-ordering rule in one place. */
public final class SelectedStatusText {
    private SelectedStatusText() {
    }

    public static String compose(SelectedExamineState.Snapshot examine,
                                 String butcherStatus) {
        List<String> parts = new ArrayList<>();
        if (examine != null && examine.hasQualityAndDamage()) {
            // QL and Dam are deliberately first, before every type-specific
            // state. This ordering is a UI contract, not a best-effort hint.
            parts.add(HudMessages.tr("QL {0} · Dam {1}",
                    String.format(java.util.Locale.ROOT, "%.2f", examine.quality()),
                    String.format(java.util.Locale.ROOT, "%.2f", examine.damage())));
        }
        String butcher = clean(butcherStatus);
        if (!butcher.isEmpty()) parts.add(HudMessages.detail(butcher));
        if (examine != null) for (String detail : examine.details()) parts.add(HudMessages.detail(detail));
        return join(parts);
    }

    public static List<String> hoverDetails(SelectedExamineState.Snapshot examine, String butcherStatus) {
        List<String> result = new ArrayList<>();
        if (examine != null && examine.hasQualityAndDamage()) {
            result.add(HudMessages.tr("Item quality: {0}", String.format(java.util.Locale.ROOT, "%.2f", examine.quality())));
            result.add(HudMessages.tr("Item damage: {0}", String.format(java.util.Locale.ROOT, "%.2f", examine.damage())));
        }
        String butcher = clean(butcherStatus);
        if (!butcher.isEmpty()) result.add(HudMessages.detail(butcher));
        if (examine != null) for (String detail : examine.details()) result.add(HudMessages.detail(detail));
        return result;
    }

    private static String join(List<String> parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            String value = clean(part);
            if (value.isEmpty()) continue;
            if (result.length() > 0) result.append(" · ");
            result.append(value);
        }
        return result.toString();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
