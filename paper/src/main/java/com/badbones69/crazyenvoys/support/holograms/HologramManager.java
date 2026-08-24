package com.badbones69.crazyenvoys.support.holograms;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.api.objects.misc.Tier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public abstract class HologramManager {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    protected CrazyEnvoys plugin = CrazyEnvoys.get();
    
    public abstract void createHologram(final Location location, final Tier tier, final String id);

    public abstract void removeHologram(final String id);

    public abstract boolean exists(final String id);

    public abstract void purge(final boolean isShutdown);

    public abstract String getName();

    protected @NotNull final String name() {
        return this.plugin.getName().toLowerCase() + "-" + UUID.randomUUID();
    }

    protected @NotNull final String name(final String id) {
        return this.plugin.getName().toLowerCase() + "-" + id;
    }

    protected @NotNull final Vector getVector(@NotNull final Tier tier) {
        return new Vector(0.5, tier.getHoloHeight(), 0.5);
    }

    protected @Nullable final String color(@NotNull final String message) {
        if (message.isEmpty()) return null;

        return LEGACY.serialize(this.plugin.getFusion().asComponent(message));
    }

    protected @NotNull final String miniMessage(@NotNull final String message) {
        return MINI_MESSAGE.serialize(this.plugin.getFusion().asComponent(message));
    }
}
