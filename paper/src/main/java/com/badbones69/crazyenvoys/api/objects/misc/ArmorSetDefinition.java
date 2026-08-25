package com.badbones69.crazyenvoys.api.objects.misc;

import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public record ArmorSetDefinition(
        @NotNull String id,
        @NotNull String displayName,
        @NotNull Map<PotionEffectType, Integer> effects
) {

    public ArmorSetDefinition {
        id = id.trim();
        displayName = displayName.trim();
        effects = Map.copyOf(new LinkedHashMap<>(effects));
    }

    public @NotNull String getId() {
        return this.id;
    }

    public @NotNull String getDisplayName() {
        return this.displayName;
    }

    public @NotNull Map<PotionEffectType, Integer> getEffects() {
        return this.effects;
    }
}
