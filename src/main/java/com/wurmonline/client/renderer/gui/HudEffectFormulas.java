package com.wurmonline.client.renderer.gui;

/** Pure calculations mirrored from the pinned Wurm Unlimited server. */
final class HudEffectFormulas {
    private static final float FOOD_FULL_REGEN_THRESHOLD = 20535f / 65535f;

    private HudEffectFormulas() {
    }

    static float waterStaminaModifier(float value) {
        float water = clamp(value);
        return water * water * water;
    }

    static float foodStaminaModifier(float value) {
        float food = clamp(value);
        if (food >= FOOD_FULL_REGEN_THRESHOLD) return 1f;
        float ratio = food / FOOD_FULL_REGEN_THRESHOLD;
        return ratio * ratio;
    }

    static float staminaDrainModifier(float caloriesValue) {
        return 1f / (1f + clamp(caloriesValue) / 3f);
    }

    static float waterUsageModifier(float carbsValue) {
        return oneThirdReduction(carbsValue);
    }

    static float foodUsageModifier(float proteinsValue) {
        return oneThirdReduction(proteinsValue);
    }

    static float sleepBonusConsumptionModifier(float fatsValue) {
        return oneThirdReduction(fatsValue);
    }

    static float favorRegenerationModifier(float fatsValue) {
        return 1f + clamp(fatsValue) / 3f;
    }

    static float staminaRegenerationModifier(float waterValue, float foodValue,
                                              boolean sleepBonusActive) {
        float modifier = waterStaminaModifier(waterValue)
                * foodStaminaModifier(foodValue);
        return sleepBonusActive ? modifier * 3f : modifier;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float oneThirdReduction(float value) {
        return 1f - clamp(value) / 3f;
    }
}
