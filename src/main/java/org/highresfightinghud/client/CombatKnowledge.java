package org.highresfightinghud.client;

import java.util.Locale;
import java.util.Properties;

/** Accumulated, server-local observations for one normalized creature type. */
public final class CombatKnowledge {
    private static final int STANCE_CAPACITY = 15;

    private long encounters;
    private long kills;
    private long outgoingAttempts;
    private long outgoingHits;
    private long outgoingMisses;
    private long outgoingParries;
    private long outgoingGlances;
    private long outgoingEvades;
    private long incomingAttempts;
    private long incomingHits;
    private long incomingMisses;
    private long incomingParries;
    private long incomingGlances;
    private long incomingShieldBlocks;
    private long incomingEvades;
    private long shieldOpportunities;
    private long damageSamples;
    private double severityTotal;
    private double maximumSeverity;
    private final long[] stanceAttempts = new long[STANCE_CAPACITY];
    private final long[] stanceHits = new long[STANCE_CAPACITY];
    private final long[][] pairAttempts =
            new long[STANCE_CAPACITY][STANCE_CAPACITY];
    private final long[][] pairHits =
            new long[STANCE_CAPACITY][STANCE_CAPACITY];
    private final long[] incomingTypes =
            new long[CombatObservation.DamageType.values().length];

    public synchronized void recordEncounter() {
        encounters++;
    }

    public synchronized void recordKill() {
        kills++;
    }

    public synchronized void observe(CombatObservation observation,
                                     int playerStance, int targetStance,
                                     boolean shieldEligible) {
        if (observation == null) return;
        int ours = safeStance(playerStance);
        int theirs = safeStance(targetStance);
        if (observation.direction() == CombatObservation.Direction.OUTGOING) {
            outgoingAttempts++;
            stanceAttempts[ours]++;
            pairAttempts[ours][theirs]++;
            switch (observation.outcome()) {
                case HIT:
                    outgoingHits++;
                    stanceHits[ours]++;
                    pairHits[ours][theirs]++;
                    damageSamples++;
                    severityTotal += observation.severity();
                    maximumSeverity = Math.max(maximumSeverity,
                            observation.severity());
                    break;
                case MISS:
                    outgoingMisses++;
                    break;
                case PARRY:
                    outgoingParries++;
                    break;
                case GLANCE:
                    outgoingGlances++;
                    break;
                case EVADE:
                    outgoingEvades++;
                    break;
                default:
                    break;
            }
            return;
        }

        incomingAttempts++;
        if (shieldEligible) shieldOpportunities++;
        int type = observation.damageType().ordinal();
        if (type >= 0 && type < incomingTypes.length
                && observation.damageType()
                != CombatObservation.DamageType.UNKNOWN) {
            incomingTypes[type]++;
        }
        switch (observation.outcome()) {
            case HIT:
                incomingHits++;
                break;
            case MISS:
                incomingMisses++;
                break;
            case PARRY:
                incomingParries++;
                break;
            case GLANCE:
                incomingGlances++;
                break;
            case SHIELD:
                if (shieldEligible) incomingShieldBlocks++;
                break;
            case EVADE:
                incomingEvades++;
                break;
            default:
                break;
        }
    }

    public synchronized Snapshot snapshot(int currentStance) {
        Probability hit = probability(outgoingHits, outgoingAttempts);
        Probability parry = probability(outgoingParries, outgoingAttempts);
        Probability glance = probability(outgoingGlances, outgoingAttempts);
        Probability shield = probability(incomingShieldBlocks,
                shieldOpportunities);

        int bestStance = 0;
        double bestScore = -1.0;
        double globalRate = hit.value;
        for (int i = 0; i < AttackStanceModel.count(); i++) {
            int stance = AttackStanceModel.idAt(i);
            double learnedRate = (stanceHits[stance] + globalRate * 12.0)
                    / (stanceAttempts[stance] + 12.0);
            double score = learnedRate
                    * AttackStanceModel.damagePrior(stance);
            if (score > bestScore) {
                bestScore = score;
                bestStance = stance;
            }
        }
        int safeCurrent = safeStance(currentStance);
        double currentRate = (stanceHits[safeCurrent] + globalRate * 12.0)
                / (stanceAttempts[safeCurrent] + 12.0);
        double currentScore = currentRate
                * AttackStanceModel.damagePrior(safeCurrent);
        double gain = currentScore <= 0.0001 ? 0.0
                : (bestScore / currentScore - 1.0) * 100.0;

        return new Snapshot(encounters, kills, outgoingAttempts,
                incomingAttempts, hit, parry, glance, shield,
                studyPercent(), damageSamples == 0 ? 0.0
                : severityTotal / damageSamples, maximumSeverity,
                bestStance, Math.max(0.0, gain), bestScore,
                stanceAttempts[bestStance], topDamageTypes());
    }

    public synchronized long encounters() { return encounters; }
    public synchronized long kills() { return kills; }
    public synchronized long outgoingAttempts() { return outgoingAttempts; }
    public synchronized long incomingAttempts() { return incomingAttempts; }
    public synchronized long stanceAttempts(int stance) {
        return stanceAttempts[safeStance(stance)];
    }

    synchronized void store(Properties properties, String prefix) {
        put(properties, prefix, "encounters", encounters);
        put(properties, prefix, "kills", kills);
        put(properties, prefix, "out.attempts", outgoingAttempts);
        put(properties, prefix, "out.hits", outgoingHits);
        put(properties, prefix, "out.misses", outgoingMisses);
        put(properties, prefix, "out.parries", outgoingParries);
        put(properties, prefix, "out.glances", outgoingGlances);
        put(properties, prefix, "out.evades", outgoingEvades);
        put(properties, prefix, "in.attempts", incomingAttempts);
        put(properties, prefix, "in.hits", incomingHits);
        put(properties, prefix, "in.misses", incomingMisses);
        put(properties, prefix, "in.parries", incomingParries);
        put(properties, prefix, "in.glances", incomingGlances);
        put(properties, prefix, "in.shield", incomingShieldBlocks);
        put(properties, prefix, "in.evades", incomingEvades);
        put(properties, prefix, "in.shieldOpportunities", shieldOpportunities);
        put(properties, prefix, "damage.samples", damageSamples);
        properties.setProperty(prefix + "damage.total",
                Double.toString(severityTotal));
        properties.setProperty(prefix + "damage.max",
                Double.toString(maximumSeverity));
        properties.setProperty(prefix + "stance.attempts",
                join(stanceAttempts));
        properties.setProperty(prefix + "stance.hits", join(stanceHits));
        properties.setProperty(prefix + "incoming.types", join(incomingTypes));
        properties.setProperty(prefix + "pairs", joinPairs());
    }

    static CombatKnowledge load(Properties properties, String prefix) {
        CombatKnowledge result = new CombatKnowledge();
        result.encounters = number(properties, prefix, "encounters");
        result.kills = number(properties, prefix, "kills");
        result.outgoingAttempts = number(properties, prefix, "out.attempts");
        result.outgoingHits = number(properties, prefix, "out.hits");
        result.outgoingMisses = number(properties, prefix, "out.misses");
        result.outgoingParries = number(properties, prefix, "out.parries");
        result.outgoingGlances = number(properties, prefix, "out.glances");
        result.outgoingEvades = number(properties, prefix, "out.evades");
        result.incomingAttempts = number(properties, prefix, "in.attempts");
        result.incomingHits = number(properties, prefix, "in.hits");
        result.incomingMisses = number(properties, prefix, "in.misses");
        result.incomingParries = number(properties, prefix, "in.parries");
        result.incomingGlances = number(properties, prefix, "in.glances");
        result.incomingShieldBlocks = number(properties, prefix, "in.shield");
        result.incomingEvades = number(properties, prefix, "in.evades");
        result.shieldOpportunities = number(properties, prefix,
                "in.shieldOpportunities");
        result.damageSamples = number(properties, prefix, "damage.samples");
        result.severityTotal = decimal(properties, prefix, "damage.total");
        result.maximumSeverity = decimal(properties, prefix, "damage.max");
        parseArray(properties.getProperty(prefix + "stance.attempts"),
                result.stanceAttempts);
        parseArray(properties.getProperty(prefix + "stance.hits"),
                result.stanceHits);
        parseArray(properties.getProperty(prefix + "incoming.types"),
                result.incomingTypes);
        result.parsePairs(properties.getProperty(prefix + "pairs"));
        return result;
    }

    private int studyPercent() {
        double attacks = Math.min(1.0, outgoingAttempts / 300.0);
        double damage = Math.min(1.0, damageSamples / 150.0);
        double stance = 0.0;
        for (int i = 0; i < AttackStanceModel.count(); i++) {
            stance += Math.min(1.0,
                    stanceAttempts[AttackStanceModel.idAt(i)] / 20.0);
        }
        stance /= AttackStanceModel.count();
        double incoming = Math.min(1.0, incomingAttempts / 200.0);
        double killKnowledge = Math.min(1.0, kills / 25.0);
        return (int) Math.round(100.0 * (0.30 * attacks + 0.25 * damage
                + 0.20 * stance + 0.15 * incoming
                + 0.10 * killKnowledge));
    }

    private String topDamageTypes() {
        String first = "";
        String second = "";
        long firstCount = 0;
        long secondCount = 0;
        CombatObservation.DamageType[] types =
                CombatObservation.DamageType.values();
        for (int i = 0; i < incomingTypes.length; i++) {
            long count = incomingTypes[i];
            if (count > firstCount) {
                second = first;
                secondCount = firstCount;
                first = typeLabel(types[i]);
                firstCount = count;
            } else if (count > secondCount) {
                second = typeLabel(types[i]);
                secondCount = count;
            }
        }
        if (firstCount == 0) return "unknown";
        return secondCount == 0 ? first : first + "/" + second;
    }

    private static String typeLabel(CombatObservation.DamageType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private static Probability probability(long successes, long trials) {
        if (trials <= 0) return new Probability(0.0, 0.0, 1.0, 0L);
        double adjustedTrials = trials + 2.0;
        double adjustedSuccesses = successes + 1.0;
        double p = adjustedSuccesses / adjustedTrials;
        double z = 1.96;
        double denominator = 1.0 + z * z / adjustedTrials;
        double centre = (p + z * z / (2.0 * adjustedTrials)) / denominator;
        double radius = z * Math.sqrt((p * (1.0 - p)
                + z * z / (4.0 * adjustedTrials)) / adjustedTrials)
                / denominator;
        return new Probability(p, Math.max(0.0, centre - radius),
                Math.min(1.0, centre + radius), trials);
    }

    private static int safeStance(int stance) {
        return stance >= 0 && stance < STANCE_CAPACITY ? stance : 0;
    }

    private static void put(Properties properties, String prefix,
                            String name, long value) {
        properties.setProperty(prefix + name, Long.toString(value));
    }

    private static long number(Properties properties, String prefix,
                               String name) {
        try {
            return Math.max(0L, Long.parseLong(properties.getProperty(
                    prefix + name, "0")));
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    private static double decimal(Properties properties, String prefix,
                                  String name) {
        try {
            return Math.max(0.0, Double.parseDouble(properties.getProperty(
                    prefix + name, "0")));
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private static String join(long[] values) {
        StringBuilder result = new StringBuilder();
        for (long value : values) {
            if (result.length() > 0) result.append(',');
            result.append(value);
        }
        return result.toString();
    }

    private static void parseArray(String value, long[] destination) {
        if (value == null || value.trim().isEmpty()) return;
        String[] parts = value.split(",");
        for (int i = 0; i < parts.length && i < destination.length; i++) {
            try {
                destination[i] = Math.max(0L, Long.parseLong(parts[i]));
            } catch (RuntimeException ignored) {
                destination[i] = 0L;
            }
        }
    }

    private String joinPairs() {
        StringBuilder result = new StringBuilder();
        for (int ours = 0; ours < STANCE_CAPACITY; ours++) {
            for (int theirs = 0; theirs < STANCE_CAPACITY; theirs++) {
                if (pairAttempts[ours][theirs] == 0) continue;
                if (result.length() > 0) result.append(',');
                result.append(ours).append('-').append(theirs).append(':')
                        .append(pairAttempts[ours][theirs]).append(':')
                        .append(pairHits[ours][theirs]);
            }
        }
        return result.toString();
    }

    private void parsePairs(String value) {
        if (value == null || value.trim().isEmpty()) return;
        for (String entry : value.split(",")) {
            String[] values = entry.split(":");
            String[] stances = values.length == 3
                    ? values[0].split("-") : new String[0];
            if (stances.length != 2) continue;
            try {
                int ours = safeStance(Integer.parseInt(stances[0]));
                int theirs = safeStance(Integer.parseInt(stances[1]));
                pairAttempts[ours][theirs] = Math.max(0L,
                        Long.parseLong(values[1]));
                pairHits[ours][theirs] = Math.max(0L,
                        Long.parseLong(values[2]));
            } catch (RuntimeException ignored) {
                // Ignore a corrupt sparse tuple and preserve the rest.
            }
        }
    }

    public static final class Probability {
        public final double value;
        public final double lower;
        public final double upper;
        public final long samples;

        Probability(double value, double lower, double upper, long samples) {
            this.value = value;
            this.lower = lower;
            this.upper = upper;
            this.samples = samples;
        }
    }

    public static final class Snapshot {
        public final long encounters;
        public final long kills;
        public final long outgoingAttempts;
        public final long incomingAttempts;
        public final Probability hitChance;
        public final Probability parryChance;
        public final Probability glanceChance;
        public final Probability shieldBlockChance;
        public final int studyPercent;
        public final double meanDamageIndex;
        public final double maximumDamageIndex;
        public final int bestStance;
        public final double projectedGainPercent;
        public final double bestScore;
        public final long bestStanceSamples;
        public final String incomingDamageTypes;

        Snapshot(long encounters, long kills, long outgoingAttempts,
                 long incomingAttempts, Probability hitChance,
                 Probability parryChance, Probability glanceChance,
                 Probability shieldBlockChance, int studyPercent,
                 double meanDamageIndex, double maximumDamageIndex,
                 int bestStance, double projectedGainPercent,
                 double bestScore, long bestStanceSamples,
                 String incomingDamageTypes) {
            this.encounters = encounters;
            this.kills = kills;
            this.outgoingAttempts = outgoingAttempts;
            this.incomingAttempts = incomingAttempts;
            this.hitChance = hitChance;
            this.parryChance = parryChance;
            this.glanceChance = glanceChance;
            this.shieldBlockChance = shieldBlockChance;
            this.studyPercent = studyPercent;
            this.meanDamageIndex = meanDamageIndex;
            this.maximumDamageIndex = maximumDamageIndex;
            this.bestStance = bestStance;
            this.projectedGainPercent = projectedGainPercent;
            this.bestScore = bestScore;
            this.bestStanceSamples = bestStanceSamples;
            this.incomingDamageTypes = incomingDamageTypes;
        }
    }
}
