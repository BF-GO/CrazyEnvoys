package com.badbones69.crazyenvoys.api.objects;

import com.badbones69.crazyenvoys.api.objects.misc.Tier;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class EventSession {

    public enum Phase {
        RESOLVING,
        ACTIVE,
        STOPPING,
        STOPPED
    }

    public enum StartMode {
        GLOBAL,
        FLARE
    }

    public enum CrateState {
        FALLING,
        ACTIVE,
        CLAIMING,
        CLAIMED,
        REMOVING,
        REMOVED
    }

    public record ActiveCrate(long sessionId, Block block, Tier tier, AtomicReference<CrateState> state) {
        public ActiveCrate(final long sessionId, final Block block, final Tier tier, final CrateState state) {
            this(sessionId, block, tier, new AtomicReference<>(state));
        }
    }

    public record FallingCrate(long sessionId, Block block, Tier tier) {}

    public record SpawnOrigin(UUID worldId, int blockX, int blockZ) {
        public SpawnOrigin {
            Objects.requireNonNull(worldId, "worldId");
        }

        public static SpawnOrigin from(final Player player) {
            final Location location = player.getLocation();
            return new SpawnOrigin(location.getWorld().getUID(), location.getBlockX(), location.getBlockZ());
        }
    }

    public record PlayerPosition(String world, double x, double y, double z) {
        public static PlayerPosition from(final Player player) {
            final Location location = player.getLocation();
            return new PlayerPosition(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
        }

        public boolean isNear(final Location location, final double horizontal, final double vertical) {
            if (!location.getWorld().getName().equals(this.world)) return false;

            return Math.abs(location.getX() - this.x) <= horizontal
                    && Math.abs(location.getY() - this.y) <= vertical
                    && Math.abs(location.getZ() - this.z) <= horizontal;
        }
    }

    private final long id;
    private final @Nullable String starterName;
    private final StartMode startMode;
    private final @Nullable SpawnOrigin spawnOrigin;
    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.RESOLVING);
    private final AtomicBoolean generationCompleted = new AtomicBoolean();
    private final AtomicInteger remainingCrates = new AtomicInteger();
    private final CompletableFuture<Boolean> startFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> cleanupFuture = new CompletableFuture<>();

    private final List<Block> generatedLocations = new CopyOnWriteArrayList<>();
    private final Set<Block> spawnedLocations = ConcurrentHashMap.newKeySet();
    private final Map<Block, ActiveCrate> activeCrates = new ConcurrentHashMap<>();
    private final Map<Entity, FallingCrate> fallingCrates = new ConcurrentHashMap<>();
    private final Map<Location, ScheduledTask> signalTasks = new ConcurrentHashMap<>();

    public EventSession(final long id, @Nullable final Player starter) {
        this(id, starter == null ? null : starter.getName(), StartMode.GLOBAL, null);
    }

    public EventSession(final long id, @Nullable final String starterName, final StartMode startMode,
                        @Nullable final SpawnOrigin spawnOrigin) {
        this.id = id;
        this.starterName = starterName;
        this.startMode = Objects.requireNonNull(startMode, "startMode");
        this.spawnOrigin = spawnOrigin;

        if (startMode == StartMode.FLARE && spawnOrigin == null) {
            throw new IllegalArgumentException("A flare session requires a spawn origin");
        }
    }

    public long id() {
        return this.id;
    }

    public @Nullable String starterName() {
        return this.starterName;
    }

    public StartMode startMode() {
        return this.startMode;
    }

    public @Nullable SpawnOrigin spawnOrigin() {
        return this.spawnOrigin;
    }

    public boolean affectsGlobalSchedule() {
        return this.startMode == StartMode.GLOBAL;
    }

    public AtomicReference<Phase> phase() {
        return this.phase;
    }

    public AtomicBoolean generationCompleted() {
        return this.generationCompleted;
    }

    public AtomicInteger remainingCrates() {
        return this.remainingCrates;
    }

    public CompletableFuture<Boolean> startFuture() {
        return this.startFuture;
    }

    public CompletableFuture<Void> cleanupFuture() {
        return this.cleanupFuture;
    }

    public List<Block> generatedLocations() {
        return this.generatedLocations;
    }

    public Set<Block> spawnedLocations() {
        return this.spawnedLocations;
    }

    public Map<Block, ActiveCrate> activeCrates() {
        return this.activeCrates;
    }

    public Map<Entity, FallingCrate> fallingCrates() {
        return this.fallingCrates;
    }

    public Map<Location, ScheduledTask> signalTasks() {
        return this.signalTasks;
    }
}
