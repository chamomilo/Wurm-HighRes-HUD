package org.highreshud.client.craft;

/** Shared mass/heat validation and an explicitly bounded QL projection. */
public final class CraftReadiness {
    public static final float MASS_EPSILON=.00005f;
    private CraftReadiness(){}
    public static boolean enough(float available,float needed){return !Float.isNaN(needed)&&available+MASS_EPSILON>=needed;}
    public static boolean usable(float mass,float needed,int temperature,boolean glowing){return enough(mass,needed)&&(!glowing||temperature==5);}
    public static float effectiveQuality(float quality,float damage){return quality*Math.max(1f,100f-damage)/100f;}
    /** Successful-step upper bound: the actual skill roll can produce a smaller increase. */
    public static float nextQualityUpperBound(float current,float material,int steps,boolean wool) {
        int divisor=Math.max(1,steps+(wool||steps<=2?6:0));
        return Math.min(100f,current+Math.max(0f,material)/divisor);
    }
}
