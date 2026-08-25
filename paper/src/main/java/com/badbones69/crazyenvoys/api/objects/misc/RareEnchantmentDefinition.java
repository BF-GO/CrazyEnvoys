package com.badbones69.crazyenvoys.api.objects.misc;

import org.jetbrains.annotations.NotNull;

import java.util.random.RandomGenerator;

/**
 * Immutable per-tier settings for the Armor Breaker bonus roll.
 */
public record RareEnchantmentDefinition(boolean enabled, double chance, int level) {

    public static final int MAX_LEVEL = 3;

    public RareEnchantmentDefinition {
        if (!Double.isFinite(chance) || chance < 0.0D || chance > 100.0D) {
            throw new IllegalArgumentException("chance must be between 0 and 100");
        }
        if (enabled && (level < 1 || level > MAX_LEVEL)) {
            throw new IllegalArgumentException("level must be between 1 and " + MAX_LEVEL);
        }
        if (!enabled) {
            chance = 0.0D;
            level = 0;
        }
    }

    public static RareEnchantmentDefinition disabled() {
        return new RareEnchantmentDefinition(false, 0.0D, 0);
    }

    public boolean succeeds(final double percentile) {
        return this.enabled && percentile >= 0.0D && percentile < this.chance;
    }

    public boolean roll(@NotNull final RandomGenerator random) {
        return succeeds(random.nextDouble(100.0D));
    }
}
