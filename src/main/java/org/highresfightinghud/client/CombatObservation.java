package org.highresfightinghud.client;

/** One unambiguous combat-log outcome used by the learned creature model. */
public final class CombatObservation {
    public enum Direction { OUTGOING, INCOMING }
    public enum Outcome { HIT, MISS, PARRY, GLANCE, SHIELD, EVADE }
    public enum DamageType {
        CRUSH, SLASH, PIERCE, BITE, BURN, COLD, ACID, POISON, INTERNAL,
        UNKNOWN
    }

    private final Direction direction;
    private final Outcome outcome;
    private final DamageType damageType;
    private final String bodyPart;
    private final double severity;

    public CombatObservation(Direction direction, Outcome outcome,
                             DamageType damageType, String bodyPart,
                             double severity) {
        this.direction = direction;
        this.outcome = outcome;
        this.damageType = damageType == null ? DamageType.UNKNOWN : damageType;
        this.bodyPart = bodyPart == null ? "" : bodyPart;
        this.severity = Math.max(0.0, Math.min(1.0, severity));
    }

    public Direction direction() { return direction; }
    public Outcome outcome() { return outcome; }
    public DamageType damageType() { return damageType; }
    public String bodyPart() { return bodyPart; }
    public double severity() { return severity; }
}
