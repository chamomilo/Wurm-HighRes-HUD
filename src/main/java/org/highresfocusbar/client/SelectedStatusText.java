package org.highresfocusbar.client;

import java.util.ArrayList;
import java.util.List;

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
            parts.add(FocusMath.qualityDamage(
                    examine.quality(), examine.damage()));
        }
        String butcher = clean(butcherStatus);
        if (!butcher.isEmpty()) parts.add(butcher);
        if (examine != null) parts.addAll(examine.details());
        return join(parts);
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
