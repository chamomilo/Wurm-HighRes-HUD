package org.highreshealthbar.client;

/**
 * Keeps the last authoritative item-vehicle metadata. WU may briefly replace
 * a hidden inventory root with a zero-filled placeholder; quality zero is not
 * a usable vehicle QL and must not erase the preceding server value.
 */
public final class VehicleStatusCache {
    private static final long NONE = Long.MIN_VALUE;

    private long vehicleId = NONE;
    private String quality = "--";
    private String damage = "--";
    private boolean exact;

    public synchronized String[] observeFallback(long id, String details) {
        select(id);
        if (exact) return snapshot();
        return observeText(details, false);
    }

    public synchronized String[] observeExamine(long id, String details) {
        select(id);
        return observeText(details, true);
    }

    private String[] observeText(String details, boolean authoritative) {
        String parsedQuality = MountStatusText.vehicleQuality(details);
        if (validParsedQuality(parsedQuality)) {
            quality = parsedQuality;
            String parsedDamage = MountStatusText.vehicleDamage(details);
            if (!"--".equals(parsedDamage)) damage = parsedDamage;
            if (authoritative) exact = true;
        }
        return snapshot();
    }

    public synchronized String[] observeMetadata(long id, float currentQuality,
                                                  float currentDamage) {
        select(id);
        if (exact) return snapshot();
        if (validQuality(currentQuality)) {
            quality = MountStatusText.vehicleQuality(currentQuality);
            if (validDamage(currentDamage)) {
                damage = MountStatusText.vehicleDamage(currentDamage);
            }
        }
        return snapshot();
    }

    public synchronized void clear() {
        vehicleId = NONE;
        quality = "--";
        damage = "--";
        exact = false;
    }

    synchronized boolean isExact(long id) {
        return vehicleId == id && exact;
    }

    private void select(long id) {
        if (vehicleId == id) return;
        vehicleId = id;
        quality = "--";
        damage = "--";
        exact = false;
    }

    private String[] snapshot() {
        return new String[]{quality, damage};
    }

    private static boolean validQuality(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value)
                && value > 0.01f && value <= 100f;
    }

    private static boolean validDamage(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value)
                && value >= 0f && value <= 100f;
    }

    private static boolean validParsedQuality(String value) {
        if (value == null || "--".equals(value)) return false;
        try {
            return validQuality(Float.parseFloat(value.replace(',', '.')));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
