package com.badbones69.crazyenvoys.api.objects.misc;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public enum ArmorSetPiece {

    HEAD("head", "_HELMET"),
    CHEST("chest", "_CHESTPLATE"),
    LEGS("legs", "_LEGGINGS"),
    FEET("feet", "_BOOTS");

    private final String configName;
    private final String materialSuffix;

    ArmorSetPiece(@NotNull final String configName, @NotNull final String materialSuffix) {
        this.configName = configName;
        this.materialSuffix = materialSuffix;
    }

    public @NotNull String getConfigName() {
        return this.configName;
    }

    public boolean matches(@NotNull final Material material) {
        return material.name().endsWith(this.materialSuffix);
    }

    public static @NotNull Optional<ArmorSetPiece> fromConfig(final String value) {
        if (value == null || value.isBlank()) return Optional.empty();

        final String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (final ArmorSetPiece piece : values()) {
            if (piece.configName.equals(normalized)) return Optional.of(piece);
        }

        return Optional.empty();
    }
}
