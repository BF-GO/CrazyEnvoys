package com.badbones69.crazyenvoys.api;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.api.enums.PersistentKeys;
import com.badbones69.crazyenvoys.api.objects.misc.ArmorSetDefinition;
import com.badbones69.crazyenvoys.api.objects.misc.ArmorSetPiece;
import com.badbones69.crazyenvoys.api.objects.misc.Tier;
import com.badbones69.crazyenvoys.scheduler.SchedulerAdapter;
import com.ryderbelserion.fusion.core.api.enums.Level;
import com.ryderbelserion.fusion.paper.builders.folia.FoliaScheduler;
import com.ryderbelserion.fusion.paper.builders.folia.Scheduler;
import io.papermc.paper.persistence.PersistentDataContainerView;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ArmorSetManager {

    private static final long CHECK_INTERVAL_TICKS = 40L;
    private static final int EFFECT_DURATION_TICKS = 100;
    private static final int REFRESH_THRESHOLD_TICKS = 60;

    private final CrazyEnvoys plugin;
    private final SchedulerAdapter scheduler;
    private final AtomicReference<Map<String, ArmorSetDefinition>> definitions = new AtomicReference<>(Map.of());
    private final Map<UUID, ActiveSet> activeSets = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile ScheduledTask task;

    public ArmorSetManager(@NotNull final CrazyEnvoys plugin, @NotNull final SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    public void reload(@NotNull final Collection<Tier> tiers) {
        final Map<String, List<ArmorSetDefinition>> grouped = new LinkedHashMap<>();
        for (final Tier tier : tiers) {
            tier.getArmorSetDefinition().ifPresent(definition ->
                    grouped.computeIfAbsent(definition.id(), ignored -> new ArrayList<>()).add(definition)
            );
        }

        final Map<String, ArmorSetDefinition> next = new LinkedHashMap<>();
        for (final Map.Entry<String, List<ArmorSetDefinition>> entry : grouped.entrySet()) {
            if (entry.getValue().size() == 1) {
                next.put(entry.getKey(), entry.getValue().getFirst());
                continue;
            }

            this.plugin.getFusion().log(Level.WARNING, Messages.log_armor_set_invalid.getMessage(Map.of(
                    "{tier}", entry.getKey(),
                    "{reason}", "duplicate set id"
            )));
        }

        this.definitions.set(Map.copyOf(next));
        start();
    }

    public Map<String, ArmorSetDefinition> getDefinitions() {
        return this.definitions.get();
    }

    public void shutdown() {
        this.running.set(false);
        this.activeSets.clear();
        this.definitions.set(Map.of());

        final ScheduledTask current = this.task;
        this.task = null;
        if (current != null) {
            try {
                current.cancel();
            } catch (Throwable throwable) {
                this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_task_cancel_failed.getMessage(Map.of(
                        "{task}", "armor-set"
                )), throwable);
            }
        }
    }

    private void start() {
        if (!this.running.compareAndSet(false, true)) return;

        this.task = new FoliaScheduler(this.plugin, Scheduler.global_scheduler) {
            @Override
            public void run() {
                inspectOnlinePlayers();
            }
        }.runAtFixedRate(CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    private void inspectOnlinePlayers() {
        if (!this.running.get()) return;

        final List<Player> players = List.copyOf(this.plugin.getServer().getOnlinePlayers());
        final Set<UUID> online = new HashSet<>();

        for (final Player player : players) {
            online.add(player.getUniqueId());
            this.scheduler.runEntity(player, "check armor set for " + player.getUniqueId(), () -> inspect(player));
        }

        this.activeSets.keySet().removeIf(uuid -> !online.contains(uuid));
    }

    private void inspect(@NotNull final Player player) {
        if (!this.running.get() || !player.isOnline()) return;

        final ArmorSetDefinition definition = findCompleteSet(player);
        final UUID uuid = player.getUniqueId();
        final ActiveSet previous = this.activeSets.get(uuid);

        if (definition == null) {
            if (previous != null && this.activeSets.remove(uuid, previous)) {
                Messages.armor_set_deactivated.sendMessage(player, Map.of("{set}", previous.displayName()));
            }
            return;
        }

        final ActiveSet current = new ActiveSet(definition.id(), definition.displayName());
        if (!current.equals(previous)) {
            if (previous != null) {
                Messages.armor_set_deactivated.sendMessage(player, Map.of("{set}", previous.displayName()));
            }

            this.activeSets.put(uuid, current);
            Messages.armor_set_activated.sendMessage(player, Map.of("{set}", definition.displayName()));
        }

        applyEffects(player, definition);
    }

    private ArmorSetDefinition findCompleteSet(@NotNull final Player player) {
        final PlayerInventory inventory = player.getInventory();
        final Map<ArmorSetPiece, ItemStack> armor = new EnumMap<>(ArmorSetPiece.class);
        armor.put(ArmorSetPiece.HEAD, inventory.getHelmet());
        armor.put(ArmorSetPiece.CHEST, inventory.getChestplate());
        armor.put(ArmorSetPiece.LEGS, inventory.getLeggings());
        armor.put(ArmorSetPiece.FEET, inventory.getBoots());

        final Map<ArmorSetPiece, ArmorMarker> markers = new EnumMap<>(ArmorSetPiece.class);
        for (final Map.Entry<ArmorSetPiece, ItemStack> entry : armor.entrySet()) {
            final ItemStack item = entry.getValue();
            if (item == null || item.isEmpty()) return null;

            final PersistentDataContainerView data = item.getPersistentDataContainer();
            final String itemSetId = data.get(PersistentKeys.armor_set_id.getNamespacedKey(), PersistentDataType.STRING);
            final String itemPiece = data.get(PersistentKeys.armor_set_piece.getNamespacedKey(), PersistentDataType.STRING);
            markers.put(entry.getKey(), new ArmorMarker(itemSetId, itemPiece));
        }

        final Map<String, ArmorSetDefinition> currentDefinitions = this.definitions.get();
        final String setId = findCompleteSetId(markers, currentDefinitions.keySet());
        return setId == null ? null : currentDefinitions.get(setId);
    }

    static @Nullable String findCompleteSetId(@NotNull final Map<ArmorSetPiece, ArmorMarker> markers,
                                               @NotNull final Set<String> validSetIds) {
        if (markers.size() != ArmorSetPiece.values().length) return null;

        String setId = null;
        for (final ArmorSetPiece piece : ArmorSetPiece.values()) {
            final ArmorMarker marker = markers.get(piece);
            if (marker == null || marker.setId() == null || marker.piece() == null) return null;
            if (!piece.getConfigName().equals(marker.piece())) return null;

            if (setId == null) setId = marker.setId();
            else if (!setId.equals(marker.setId())) return null;
        }

        return setId != null && validSetIds.contains(setId) ? setId : null;
    }

    private void applyEffects(@NotNull final Player player, @NotNull final ArmorSetDefinition definition) {
        for (final Map.Entry<PotionEffectType, Integer> entry : definition.effects().entrySet()) {
            final PotionEffectType type = entry.getKey();
            final int amplifier = entry.getValue() - 1;
            final PotionEffect current = player.getPotionEffect(type);

            if (current != null && !shouldApplyEffect(amplifier, current.getAmplifier(), current.getDuration())) continue;

            player.addPotionEffect(new PotionEffect(
                    type,
                    EFFECT_DURATION_TICKS,
                    amplifier,
                    true,
                    false,
                    true
            ), false);
        }
    }

    static boolean shouldApplyEffect(final int desiredAmplifier, final int currentAmplifier, final int currentDuration) {
        if (currentDuration > EFFECT_DURATION_TICKS) return false;
        if (currentAmplifier > desiredAmplifier) return false;
        return currentAmplifier != desiredAmplifier || currentDuration <= REFRESH_THRESHOLD_TICKS;
    }

    private record ActiveSet(@NotNull String id, @NotNull String displayName) {
    }

    record ArmorMarker(@Nullable String setId, @Nullable String piece) {
    }
}
