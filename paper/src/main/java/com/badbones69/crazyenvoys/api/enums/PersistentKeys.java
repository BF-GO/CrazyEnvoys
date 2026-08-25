package com.badbones69.crazyenvoys.api.enums;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public enum PersistentKeys {

    no_firework_damage("firework"),
    falling_envoy_session("falling_envoy_session"),
    prize_item("prize_item"),
    armor_set_id("armor_set_id"),
    armor_set_piece("armor_set_piece"),
    armor_breaker_projectile("armor_breaker_projectile"),
    envoy_flare("envoy_flare");

    private final @NotNull CrazyEnvoys plugin = CrazyEnvoys.get();

    private final String NamespacedKey;

    PersistentKeys(String NamespacedKey) {
        this.NamespacedKey = NamespacedKey;
    }

    public NamespacedKey getNamespacedKey() {
        return new NamespacedKey(this.plugin, this.plugin.getName().toLowerCase() + "_" + this.NamespacedKey);
    }
}
