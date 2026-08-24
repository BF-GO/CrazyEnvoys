package com.badbones69.crazyenvoys.api;

import ch.jalu.configme.SettingsManager;
import com.Zrips.CMI.Modules.ModuleHandling.CMIModule;
import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.Methods;
import com.badbones69.crazyenvoys.api.enums.Files;
import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.api.enums.PersistentKeys;
import com.badbones69.crazyenvoys.api.events.EnvoyEndEvent;
import com.badbones69.crazyenvoys.api.events.EnvoyEndEvent.EnvoyEndReason;
import com.badbones69.crazyenvoys.api.events.EnvoyStartEvent;
import com.badbones69.crazyenvoys.api.events.EnvoyStartEvent.EnvoyStartReason;
import com.badbones69.crazyenvoys.api.objects.CoolDownSettings;
import com.badbones69.crazyenvoys.api.objects.EditorSettings;
import com.badbones69.crazyenvoys.api.objects.EventSession;
import com.badbones69.crazyenvoys.api.objects.FlareSettings;
import com.badbones69.crazyenvoys.api.objects.LocationSettings;
import com.badbones69.crazyenvoys.api.objects.misc.Tier;
import com.badbones69.crazyenvoys.config.ConfigManager;
import com.badbones69.crazyenvoys.config.TierTemplateManager;
import com.badbones69.crazyenvoys.config.types.ConfigKeys;
import com.badbones69.crazyenvoys.listeners.timer.CountdownTimer;
import com.badbones69.crazyenvoys.scheduler.SchedulerAdapter;
import com.badbones69.crazyenvoys.support.claims.WorldGuardSupport;
import com.badbones69.crazyenvoys.support.holograms.HologramManager;
import com.badbones69.crazyenvoys.support.holograms.types.CMIHologramsSupport;
import com.badbones69.crazyenvoys.support.holograms.types.DecentHologramsSupport;
import com.badbones69.crazyenvoys.support.holograms.types.FancyHologramsSupport;
import com.badbones69.crazyenvoys.util.MiscUtils;
import com.ryderbelserion.fusion.core.api.FusionKey;
import com.ryderbelserion.fusion.core.api.enums.Level;
import com.ryderbelserion.fusion.paper.FusionPaper;
import com.ryderbelserion.fusion.paper.files.PaperFileManager;
import com.ryderbelserion.fusion.paper.files.types.PaperCustomFile;
import com.ryderbelserion.fusion.paper.builders.folia.FoliaScheduler;
import com.ryderbelserion.fusion.paper.builders.folia.Scheduler;
import com.ryderbelserion.fusion.paper.utils.ItemUtils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.audience.Audience;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public class CrazyManager {

    private static final int MIN_GENERATION_ATTEMPTS = 100;
    private static final int ATTEMPTS_PER_DROP = 20;
    private static final int MAX_IN_FLIGHT_CHUNKS = 8;

    private @NotNull final CrazyEnvoys plugin = CrazyEnvoys.get();

    private @NotNull final Server server = this.plugin.getServer();

    private @NotNull final PluginManager pluginManager = this.server.getPluginManager();

    private @NotNull final FusionPaper fusion = this.plugin.getFusion();

    private @NotNull final SettingsManager config = ConfigManager.getConfig();

    private @NotNull final PaperFileManager fileManager = this.plugin.getFileManager();

    private @NotNull final FlareSettings flareSettings = this.plugin.getFlareSettings();

    private @NotNull final EditorSettings editorSettings = this.plugin.getEditorSettings();

    private @NotNull final CoolDownSettings coolDownSettings = this.plugin.getCoolDownSettings();

    private @NotNull final LocationSettings locationSettings = this.plugin.getLocationSettings();

    private @NotNull final SchedulerAdapter scheduler = new SchedulerAdapter(this.plugin);
    
    private volatile CountdownTimer countdownTimer;

    private volatile ScheduledTask runTimeTask;
    private volatile ScheduledTask coolDownTask;
    private volatile Calendar nextEnvoy;
    private volatile Calendar envoyTimeLeft;
    private final AtomicReference<EventSession> currentSession = new AtomicReference<>();
    private final AtomicLong sessionSequence = new AtomicLong();
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private volatile CompletionStage<Void> readiness = CompletableFuture.completedFuture(null);

    private volatile List<Block> lastGeneratedLocations = List.of();
    private volatile boolean autoTimer = true;
    private WorldGuardSupport worldGuardSupportVersion;
    private HologramManager holograms;
    private volatile Location center;
    private volatile String centerString;

    private final List<Tier> tiers = new CopyOnWriteArrayList<>();
    private final List<Tier> cachedChances = new CopyOnWriteArrayList<>();
    private final Set<Material> blacklistedBlocks = ConcurrentHashMap.newKeySet();
    private final Set<UUID> ignoreMessages = ConcurrentHashMap.newKeySet();
    private final List<Calendar> warnings = new CopyOnWriteArrayList<>();

    /**
     * Run this when you are starting up the server.
     */
    public void load() {
        this.shuttingDown.set(false);
        this.ready.set(false);
        this.readiness = loadEnvoys();
    }

    public CompletionStage<Void> whenReady() {
        return this.readiness;
    }

    private void getEnvoyTime(Calendar cal) {
        String time = this.config.getProperty(ConfigKeys.envoys_time);

        int hour = Integer.parseInt(time.split(" ")[0].split(":")[0]);
        int min = Integer.parseInt(time.split(" ")[0].split(":")[1]);

        int calender = Calendar.AM;

        if (time.split(" ")[1].equalsIgnoreCase("PM")) calender = Calendar.PM;

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.getTime(); // Without this makes the hours not change for some reason.
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.AM_PM, calender);

        if (cal.before(Calendar.getInstance())) cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 1);
    }

    /**
     * Run this when you need to reload the plugin or shut it down.
     */
    public void reload(boolean serverStop) {
        if (serverStop) {
            shutdown();
            return;
        }

        reloadAsync();
    }

    public CompletionStage<Void> reloadAsync() {
        this.ready.set(false);
        cancelEnvoyCooldownTime();

        return endEnvoyEventAsync().handle((unused, throwable) -> null)
                .thenCompose(unused -> this.scheduler.runGlobal("reload CrazyEnvoys state", () -> {
                    ConfigManager.refresh();
                    TierTemplateManager.installLocalizedDefaults(this.plugin);
                    this.fileManager.refresh(false);

                    final Calendar next = this.nextEnvoy;
                    if (next != null) Files.users.getConfiguration().set("Next-Envoy", next.getTimeInMillis());
                    Files.users.save();

                    this.locationSettings.clearSpawnLocations();
                    this.coolDownSettings.clearCoolDowns();
                }))
                .thenCompose(unused -> loadEnvoys());
    }

    private CompletionStage<Void> loadEnvoys() {
        // Clear all spawn locations.
        this.locationSettings.clearSpawnLocations();

        this.blacklistedBlocks.clear();
        this.cachedChances.clear();

        final FileConfiguration users = Files.users.getConfiguration();

        this.envoyTimeLeft = Calendar.getInstance();

        // Populate the array list.
        this.locationSettings.populateMap();

        if (!this.locationSettings.getFailedLocations().isEmpty()) {
            this.fusion.log(Level.WARNING, Messages.log_locations_retry.getMessage(Map.of(
                    "{amount}", String.valueOf(this.locationSettings.getFailedLocations().size())
            )));
        }

        loadCenter();

        if (this.config.getProperty(ConfigKeys.envoys_run_time_toggle)) {
            Calendar cal = Calendar.getInstance();

            if (this.config.getProperty(ConfigKeys.envoys_countdown)) {
                this.autoTimer = true;

                cal.setTimeInMillis(users.getLong("Next-Envoy"));

                if (Calendar.getInstance().after(cal)) cal.setTimeInMillis(getEnvoyCooldown().getTimeInMillis());
            } else {
                this.autoTimer = false;

                getEnvoyTime(cal);
            }

            this.nextEnvoy = cal;

            resetWarnings();
        } else {
            this.nextEnvoy = Calendar.getInstance();
        }

        //================================== Tiers Load ==================================//
        this.tiers.clear();

        final Path dataPath = this.plugin.getDataPath();

        for (final Path path : this.fusion.getFilesByPath(dataPath.resolve("tiers"), ".yml")) {
            final Optional<PaperCustomFile> file = this.fileManager.getPaperFile(path);

            if (file.isEmpty()) continue;

            final PaperCustomFile customFile = file.get();

            final YamlConfiguration configuration = customFile.getConfiguration();

            final Tier tier = new Tier(
                    configuration.getBoolean("Settings.Claim-Permission", false),
                    configuration.getString("Settings.Claim-Permission-Name", ""),
                    configuration.getBoolean("Settings.Use-Chance", false),
                    configuration.getInt("Settings.Spawn-Chance", 30),
                    configuration.getBoolean("Settings.Bulk-Prizes.Toggle", false),
                    configuration.getBoolean("Settings.Bulk-Prizes.Random", false),
                    configuration.getInt("Settings.Bulk-Prizes.Max-Bulk", 1),
                    configuration.getBoolean("Settings.Hologram-Toggle", false),
                    configuration.getInt("Settings.Hologram-Range", 8),
                    configuration.getDouble("Settings.Hologram-Height", 1.5),
                    configuration.getStringList("Settings.Hologram"),
                    configuration,
                    path
                    );

            this.tiers.add(tier);
        }

        // Loading the blacklisted blocks.
        this.blacklistedBlocks.add(Material.WATER);
        this.blacklistedBlocks.add(Material.LILY_PAD);
        this.blacklistedBlocks.add(Material.LAVA);
        this.blacklistedBlocks.add(Material.CHORUS_PLANT);
        this.blacklistedBlocks.add(Material.KELP_PLANT);
        this.blacklistedBlocks.add(Material.TALL_GRASS);
        this.blacklistedBlocks.add(Material.CHORUS_FLOWER);
        this.blacklistedBlocks.add(Material.SUNFLOWER);
        this.blacklistedBlocks.add(Material.IRON_BARS);
        this.blacklistedBlocks.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        this.blacklistedBlocks.add(Material.IRON_TRAPDOOR);
        this.blacklistedBlocks.add(Material.OAK_TRAPDOOR);
        this.blacklistedBlocks.add(Material.OAK_FENCE);
        this.blacklistedBlocks.add(Material.OAK_FENCE_GATE);
        this.blacklistedBlocks.add(Material.ACACIA_FENCE);
        this.blacklistedBlocks.add(Material.BIRCH_FENCE);
        this.blacklistedBlocks.add(Material.DARK_OAK_FENCE);
        this.blacklistedBlocks.add(Material.JUNGLE_FENCE);
        this.blacklistedBlocks.add(Material.NETHER_BRICK_FENCE);
        this.blacklistedBlocks.add(Material.SPRUCE_FENCE);
        this.blacklistedBlocks.add(Material.ACACIA_FENCE_GATE);
        this.blacklistedBlocks.add(Material.BIRCH_FENCE_GATE);
        this.blacklistedBlocks.add(Material.DARK_OAK_FENCE_GATE);
        this.blacklistedBlocks.add(Material.JUNGLE_FENCE_GATE);
        this.blacklistedBlocks.add(Material.SPRUCE_FENCE_GATE);
        this.blacklistedBlocks.add(Material.GLASS_PANE);
        this.blacklistedBlocks.add(Material.STONE_SLAB);

        if (this.fusion.isModReady(new FusionKey("crazyenvoys", "WorldEdit")) && this.fusion.isModReady(new FusionKey("crazyenvoys", "WorldGuard"))) {
            this.worldGuardSupportVersion = new WorldGuardSupport();
        }

        loadHolograms();

        this.locationSettings.fixLocations();

        this.flareSettings.load();

        rebuildTierCache();

        return recoverPersistedLocations().handle((unused, throwable) -> throwable)
                .thenCompose(throwable -> this.scheduler.runGlobal("finish startup recovery", () -> {
            if (this.shuttingDown.get()) return;

            if (throwable != null) {
                this.fusion.log(Level.ERROR, Messages.log_recovery_error.getMessage(Map.of(
                        "{error}", String.valueOf(throwable.getMessage())
                )));
            }

            this.ready.set(true);

            if (this.config.getProperty(ConfigKeys.envoys_run_time_toggle)) startEnvoyCountDown();
        }));
    }

    /**
     * Load the holograms.
     */
    public void loadHolograms() {
        final String pluginName = this.config.getProperty(ConfigKeys.hologram_plugin).toLowerCase();
        this.holograms = null;

        switch (pluginName) {
            case "decentholograms" -> {
                if (!this.fusion.isModReady(CrazyKeys.decent_holograms)) return;

                this.holograms = new DecentHologramsSupport();
            }

            case "fancyholograms" -> {
                if (!this.fusion.isModReady(CrazyKeys.fancy_holograms)) return;

                this.holograms = new FancyHologramsSupport();
            }

            case "cmi" -> {
                if (!this.fusion.isModReady(CrazyKeys.cmi_holograms) && !CMIModule.holograms.isEnabled()) return;

                this.holograms = new CMIHologramsSupport();
            }

            case "none" -> {}

            default -> {
                if (this.fusion.isModReady(CrazyKeys.decent_holograms)) {
                    if (this.holograms == null) {
                        this.holograms = new DecentHologramsSupport();
                    }

                    break;
                }

                if (this.fusion.isModReady(CrazyKeys.fancy_holograms)) {
                    this.holograms = new FancyHologramsSupport();

                    break;
                }

                if (this.fusion.isModReady(CrazyKeys.cmi_holograms) && !CMIModule.holograms.isEnabled()) {
                    this.holograms = new CMIHologramsSupport();
                }
            }
        }

        if (this.holograms == null) {
            Messages.log_hologram_missing.getList().forEach(line -> this.fusion.log(Level.WARNING, line));

            return;
        }

        this.fusion.log(Level.WARNING, Messages.log_hologram_enabled.getMessage(Map.of(
                "{plugin}", this.holograms.getName()
        )));
    }

    /**
     * Used when the plugin starts to control the count-down and when the event starts
     */
    public void startEnvoyCountDown() {
        cancelEnvoyCooldownTime();

        this.coolDownTask = new FoliaScheduler(this.plugin, Scheduler.global_scheduler) {
            @Override
            public void run() {
                if (!isEnvoyBusy()) {
                    final Calendar cal = Calendar.getInstance();

                    cal.clear(Calendar.MILLISECOND);

                    int online = server.getOnlinePlayers().size();

                    if (online == 0 && config.getProperty(ConfigKeys.envoys_ignore_empty_server)) return;
                    for (Calendar warn : getWarnings()) {
                        Calendar check = Calendar.getInstance();

                        check.setTimeInMillis(warn.getTimeInMillis());
                        check.clear(Calendar.MILLISECOND);

                        if (check.compareTo(cal) == 0) {
                            Messages.warning.broadcast(config.getProperty(ConfigKeys.envoys_ignore_behaviour_warning), Map.of(
                                    "{time}", getNextEnvoyTime()
                            ));
                        }
                    }

                    Calendar next = Calendar.getInstance();
                    next.setTimeInMillis(getNextEnvoy().getTimeInMillis());
                    next.clear(Calendar.MILLISECOND);

                    if (next.compareTo(cal) <= 0 && !isEnvoyBusy()) {
                        if (config.getProperty(ConfigKeys.envoys_minimum_players_toggle)) {
                            if (online < config.getProperty(ConfigKeys.envoys_minimum_players_amount)) {
                                Messages.not_enough_players.broadcast(config.getProperty(ConfigKeys.envoys_ignore_behaviour_not_enough_players), Map.of(
                                        "{amount}", String.valueOf(online)
                                ));

                                setNextEnvoy(getEnvoyCooldown());

                                resetWarnings();

                                return;
                            }
                        }

                        if (config.getProperty(ConfigKeys.envoys_random_locations) && isCenterUnloaded()) {
                            fusion.log(Level.WARNING, Messages.log_center_world_missing.getMessage(Map.of(
                                    "{center}", String.valueOf(centerString)
                            )));

                            setNextEnvoy(getEnvoyCooldown());

                            resetWarnings();

                            return;
                        }

                        EnvoyStartEvent event = new EnvoyStartEvent(autoTimer ? EnvoyStartReason.AUTO_TIMER : EnvoyStartReason.SPECIFIED_TIME);

                        pluginManager.callEvent(event);

                        if (!event.isCancelled()) startEnvoyEventAsync(null);
                    }
                }
            }
        }.runAtFixedRate( 20, 20);
    }

    /**
     * @param block The location you want the tier from.
     * @return The tier that location is.
     */
    public Tier getTier(Block block) {
        final EventSession session = this.currentSession.get();
        if (session == null) return null;

        final EventSession.ActiveCrate crate = session.activeCrates().get(block);

        return crate == null ? null : crate.tier();
    }

    /**
     * @return True if the envoy event is currently happening and false if not.
     */
    public boolean isEnvoyActive() {
        final EventSession session = this.currentSession.get();

        return session != null && session.phase().get() == EventSession.Phase.ACTIVE;
    }

    public boolean isEnvoyBusy() {
        final EventSession session = this.currentSession.get();

        return session != null && session.phase().get() != EventSession.Phase.STOPPED;
    }

    /**
     * Despawns all the active crates.
     */
    public void removeAllEnvoys(final boolean serverStop) {
        if (serverStop) {
            shutdown();
            return;
        }

        endEnvoyEventAsync();
    }

    public CompletionStage<Void> removeAllEnvoysAsync() {
        return endEnvoyEventAsync();
    }

    public CompletionStage<Void> removeAllEnvoysAsync(final boolean serverStop) {
        if (!serverStop) return endEnvoyEventAsync();

        shutdown();
        return CompletableFuture.completedFuture(null);
    }

    public WorldGuardSupport getWorldGuardPluginSupport() {
        return this.worldGuardSupportVersion;
    }

    public final HologramManager getHolograms() {
        return this.holograms;
    }

    /**
     * @return All the envoys that are active.
     */
    public Set<Block> getActiveEnvoys() {
        final EventSession session = this.currentSession.get();

        return session == null ? Set.of() : Set.copyOf(session.activeCrates().keySet());
    }

    /**
     * @param block The location you are checking.
     * @return Turn if it is and false if not.
     */
    public boolean isActiveEnvoy(Block block) {
        final EventSession session = this.currentSession.get();
        if (session == null) return false;

        final EventSession.ActiveCrate crate = session.activeCrates().get(block);

        return crate != null && crate.state().get() == EventSession.CrateState.ACTIVE;
    }

    /**
     * @param block The location you wish to add.
     */
    public void addActiveEnvoy(Block block, Tier tier) {
        final EventSession session = this.currentSession.get();
        if (session == null || session.phase().get() == EventSession.Phase.STOPPING) return;

        session.activeCrates().put(block, new EventSession.ActiveCrate(session.id(), block, tier, EventSession.CrateState.ACTIVE));
    }

    /**
     * @param block The location you wish to remove.
     */
    public void removeActiveEnvoy(Block block) {
        final EventSession session = this.currentSession.get();
        if (session != null) session.activeCrates().remove(block);
    }

    /**
     * @return The next envoy time as a calendar.
     */
    public Calendar getNextEnvoy() {
        final Calendar next = this.nextEnvoy;
        return next == null ? Calendar.getInstance() : (Calendar) next.clone();
    }

    /**
     * @param cal A calendar that has the next time the envoy will happen.
     */
    public void setNextEnvoy(Calendar cal) {
        this.nextEnvoy = (Calendar) cal.clone();
    }

    /**
     * @return The time till the next envoy.
     */
    public String getNextEnvoyTime() {
        String message = Methods.convertTimeToString(getNextEnvoy());

        if (message.equals("0" + Messages.second.getString())) message = Messages.on_going.getString();

        return message;
    }

    /**
     * @return All falling blocks are currently going.
     */
    public Map<Entity, Block> getFallingBlocks() {
        final EventSession session = this.currentSession.get();
        if (session == null) return Map.of();

        final Map<Entity, Block> snapshot = new HashMap<>();
        session.fallingCrates().forEach((entity, crate) -> snapshot.put(entity, crate.block()));

        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * @param entity Remove a falling block from the list.
     */
    public void removeFallingBlock(Entity entity) {
        final EventSession session = this.currentSession.get();
        if (session != null) session.fallingCrates().remove(entity);
    }

    public Tier beginClaim(final Block block) {
        final EventSession session = this.currentSession.get();
        if (session == null || session.phase().get() != EventSession.Phase.ACTIVE) return null;

        final EventSession.ActiveCrate crate = session.activeCrates().get(block);
        if (crate == null || !crate.state().compareAndSet(EventSession.CrateState.ACTIVE, EventSession.CrateState.CLAIMING)) return null;

        return crate.tier();
    }

    public void cancelClaim(final Block block) {
        final EventSession session = this.currentSession.get();
        if (session == null) return;

        final EventSession.ActiveCrate crate = session.activeCrates().get(block);
        if (crate != null) crate.state().compareAndSet(EventSession.CrateState.CLAIMING, EventSession.CrateState.ACTIVE);
    }

    public boolean completeClaim(final Block block) {
        return finishClaim(block).lastCrate();
    }

    public ClaimResult finishClaim(final Block block) {
        final EventSession session = this.currentSession.get();
        if (session == null || session.phase().get() != EventSession.Phase.ACTIVE) return new ClaimResult(false, false);

        final EventSession.ActiveCrate crate = session.activeCrates().get(block);
        if (crate == null || !crate.state().compareAndSet(EventSession.CrateState.CLAIMING, EventSession.CrateState.CLAIMED)) {
            return new ClaimResult(false, false);
        }

        session.activeCrates().remove(block, crate);
        this.locationSettings.removeActiveLocation(block);

        return new ClaimResult(true, session.remainingCrates().decrementAndGet() == 0);
    }

    public record ClaimResult(boolean claimed, boolean lastCrate) {}

    public int getRemainingCrates() {
        final EventSession session = this.currentSession.get();
        return session == null ? 0 : Math.max(0, session.remainingCrates().get());
    }

    public SchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    public boolean landFallingBlock(final Entity entity) {
        final EventSession session = this.currentSession.get();
        if (session == null) return false;

        final EventSession.Phase phase = session.phase().get();
        if (phase != EventSession.Phase.RESOLVING && phase != EventSession.Phase.ACTIVE) return false;

        final EventSession.FallingCrate falling = session.fallingCrates().get(entity);
        if (falling == null || falling.sessionId() != session.id()) return false;

        Block block = falling.block();
        if (block.getType() != Material.AIR && block.getY() + 1 < block.getWorld().getMaxHeight()) {
            block = block.getRelative(org.bukkit.block.BlockFace.UP);
        }
        if (block.getType() != Material.AIR || !session.fallingCrates().remove(entity, falling)) return false;

        entity.remove();
        if (this.currentSession.get() != session || session.phase().get() == EventSession.Phase.STOPPING
                || session.phase().get() == EventSession.Phase.STOPPED) {
            session.spawnedLocations().remove(falling.block());
            return true;
        }

        block.setType(falling.tier().getPlacedBlockMaterial());

        final Location location = block.getLocation();
        session.spawnedLocations().remove(falling.block());
        session.spawnedLocations().add(block);
        session.activeCrates().put(block, new EventSession.ActiveCrate(session.id(), block, falling.tier(), EventSession.CrateState.ACTIVE));
        this.locationSettings.addActiveLocation(block);

        try {
            if (falling.tier().isHoloEnabled() && this.holograms != null) {
                this.holograms.createHologram(location, falling.tier(), MiscUtils.toString(location));
            }
            if (falling.tier().getSignalFlareToggle()) startSignalFlare(session, location, falling.tier());
        } catch (Throwable throwable) {
            this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_visuals_failed.getMessage(Map.of(
                    "{session}", String.valueOf(session.id())
            )), throwable);
        }

        if (this.currentSession.get() != session || session.phase().get() == EventSession.Phase.STOPPING
                || session.phase().get() == EventSession.Phase.STOPPED) {
            block.setType(Material.AIR);
            try {
                if (this.holograms != null) this.holograms.removeHologram(MiscUtils.toString(location));
            } catch (Throwable throwable) {
                this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_hologram_cleanup_failed.getString(), throwable);
            }
            stopSignalFlare(location);
            session.activeCrates().remove(block);
            session.spawnedLocations().remove(block);
            this.locationSettings.removeActiveLocation(block);
        }

        return true;
    }

    /**
     * Call when you want to set the new warning.
     */
    public void resetWarnings() {
        this.warnings.clear();

        this.config.getProperty(ConfigKeys.envoys_warnings).forEach(time -> addWarning(makeWarning(time)));
    }

    /**
     * @param cal When adding a new warning.
     */
    public void addWarning(Calendar cal) {
        this.warnings.add((Calendar) cal.clone());
    }

    /**
     * @return All the current warnings.
     */
    public List<Calendar> getWarnings() {
        return this.warnings.stream().map(calendar -> (Calendar) calendar.clone()).toList();
    }

    /**
     * @param time The new time for the warning.
     * @return The new time as a calendar
     */
    public Calendar makeWarning(String time) {
        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(getNextEnvoy().getTimeInMillis());

        for (String i : time.split(" ")) {
            if (i.contains("d")) {
                cal.add(Calendar.DATE, -Integer.parseInt(i.replace("d", "")));
            } else if (i.contains("h")) {
                cal.add(Calendar.HOUR, -Integer.parseInt(i.replace("h", "")));
            } else if (i.contains("m")) {
                cal.add(Calendar.MINUTE, -Integer.parseInt(i.replace("m", "")));
            } else if (i.contains("s")) {
                cal.add(Calendar.SECOND, -Integer.parseInt(i.replace("s", "")));
            }
        }

        return cal;
    }

    /**
     * @return The time left in the current envoy event.
     */
    public String getEnvoyRunTimeLeft() {
        String message = Methods.convertTimeToString(this.envoyTimeLeft);

        if (message.equals("0" + Messages.second.getString())) message = Messages.not_running.getString();

        return message;
    }

    /**
     * Call when the run time needs canceled.
     */
    public void cancelEnvoyRunTime() {
        final ScheduledTask task = this.runTimeTask;
        this.runTimeTask = null;
        if (task == null) return;

        try {
            task.cancel();
        } catch (Throwable throwable) {
            this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_task_cancel_failed.getMessage(Map.of(
                    "{task}", "runtime"
            )), throwable);
        }
    }

    /**
     * Call when the cool downtime needs canceled.
     */
    public void cancelEnvoyCooldownTime() {
        final ScheduledTask task = this.coolDownTask;
        this.coolDownTask = null;
        if (task == null) return;

        try {
            task.cancel();
        } catch (Throwable throwable) {
            this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_task_cancel_failed.getMessage(Map.of(
                    "{task}", "cooldown"
            )), throwable);
        }
    }

    @Deprecated(forRemoval = false)
    public List<Block> generateSpawnLocations() {
        return this.lastGeneratedLocations;
    }

    public CompletionStage<List<Block>> generateSpawnLocationsAsync() {
        return this.scheduler.supplyGlobal("prepare envoy location generation", () -> true)
                .thenCompose(unused -> generateSpawnLocationsAsync(() -> false))
                .thenApply(locations -> {
                    final List<Block> snapshot = List.copyOf(locations);
                    this.lastGeneratedLocations = snapshot;
                    return snapshot;
                });
    }

    private CompletionStage<List<Block>> generateSpawnLocationsAsync(final BooleanSupplier cancelled) {
        final int maxSpawns = calculateMaxSpawns();

        if (maxSpawns <= 0) return CompletableFuture.completedFuture(List.of());

        if (this.config.getProperty(ConfigKeys.envoys_random_locations)) {
            if (!testCenter() || this.center == null || !this.center.isWorldLoaded()) {
                return CompletableFuture.completedFuture(List.of());
            }

            final int maxRadius = Math.max(0, Math.min(30_000_000, this.config.getProperty(ConfigKeys.envoys_max_radius)));
            final int minRadius = Math.max(0, Math.min(maxRadius, this.config.getProperty(ConfigKeys.envoys_min_radius)));
            final int maxAttempts = (int) Math.min(Integer.MAX_VALUE,
                    Math.max((long) MIN_GENERATION_ATTEMPTS, (long) maxSpawns * ATTEMPTS_PER_DROP));
            final int maxInFlight = Math.max(1, Math.min(MAX_IN_FLIGHT_CHUNKS, maxSpawns));
            final RandomLocationGenerator generator = new RandomLocationGenerator(
                    this.center.clone(), maxSpawns, minRadius, maxRadius, maxAttempts, maxInFlight, cancelled
            );

            return generator.start().thenApply(locations -> {
                if (locations.size() < maxSpawns) {
                    this.fusion.log(Level.WARNING, Messages.log_generation_partial.getMessage(Map.of(
                            "{generated}", String.valueOf(locations.size()),
                            "{requested}", String.valueOf(maxSpawns),
                            "{attempts}", String.valueOf(generator.attempts())
                    )));
                }

                return locations;
            });
        }

        final List<Block> configured = new ArrayList<>(this.locationSettings.getSpawnLocations());

        if (configured.size() > maxSpawns) {
            Collections.shuffle(configured, ThreadLocalRandom.current());
            configured.subList(maxSpawns, configured.size()).clear();
        }

        return resolveConfiguredLocations(configured, cancelled);
    }

    private int calculateMaxSpawns() {
        int maxSpawns;

        if (this.config.getProperty(ConfigKeys.envoys_max_drops_toggle)) {
            maxSpawns = this.config.getProperty(ConfigKeys.envoys_max_drops);
        } else if (this.config.getProperty(ConfigKeys.envoys_random_drops)) {
            final int minimum = this.config.getProperty(ConfigKeys.envoys_min_drops);
            final int maximum = this.config.getProperty(ConfigKeys.envoys_max_drops);
            final int lower = Math.max(0, minimum);
            final int upper = Math.max(lower, maximum);
            maxSpawns = lower >= upper
                    ? lower
                    : (int) ThreadLocalRandom.current().nextLong(lower, (long) upper + 1L);
        } else {
            maxSpawns = this.config.getProperty(ConfigKeys.envoys_random_locations)
                    ? this.config.getProperty(ConfigKeys.envoys_max_drops)
                    : this.locationSettings.getSpawnLocations().size();
        }

        if (this.config.getProperty(ConfigKeys.envoys_random_locations)) {
            final long maxRadius = Math.max(0, Math.min(30_000_000, this.config.getProperty(ConfigKeys.envoys_max_radius)));
            final long minRadius = Math.max(0, Math.min(maxRadius, this.config.getProperty(ConfigKeys.envoys_min_radius)));
            final long area = Math.max(0L, (maxRadius * 2L) * (maxRadius * 2L) - ((minRadius * 2L + 1L) * (minRadius * 2L + 1L)));

            if (maxSpawns > area) {
                maxSpawns = (int) Math.min(Integer.MAX_VALUE, area);
                this.fusion.log(Level.WARNING, Messages.log_generation_area_limited.getMessage(Map.of(
                        "{amount}", String.valueOf(maxSpawns)
                )));
            }
        }

        return Math.max(0, maxSpawns);
    }

    private CompletionStage<List<Block>> resolveConfiguredLocations(final List<Block> configured, final BooleanSupplier cancelled) {
        if (configured.isEmpty()) return CompletableFuture.completedFuture(List.of());
        return new ConfiguredLocationResolver(
                List.copyOf(configured), Math.max(1, Math.min(MAX_IN_FLIGHT_CHUNKS, configured.size())), cancelled
        ).start();
    }

    private CompletableFuture<Block> resolveConfiguredLocation(final Block block, final BooleanSupplier cancelled) {
        final CompletableFuture<Block> result = new CompletableFuture<>();
        final World world = block.getWorld();
        final int x = block.getX();
        final int y = block.getY();
        final int z = block.getZ();

        if (cancelled.getAsBoolean()) {
            result.complete(null);
            return result;
        }

        try {
            world.getChunkAtAsync(x >> 4, z >> 4, true, false).whenComplete((chunk, throwable) -> {
                if (throwable != null) {
                    this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_chunk_load_failed.getMessage(Map.of(
                            "{type}", "configured",
                            "{x}", String.valueOf(x),
                            "{z}", String.valueOf(z)
                    )), throwable);
                    result.complete(null);
                    return;
                }

                if (cancelled.getAsBoolean()) {
                    result.complete(null);
                    return;
                }

                this.scheduler.supplyRegion(new Location(world, x, y, z), "resolve configured envoy location", () -> {
                    if (cancelled.getAsBoolean()) return null;
                    return world.getBlockAt(x, y, z);
                }).whenComplete((value, error) -> {
                    if (error != null) result.complete(null);
                    else result.complete(value);
                });
            });
        } catch (Throwable throwable) {
            this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_chunk_request_failed.getMessage(Map.of(
                    "{type}", "configured",
                    "{x}", String.valueOf(x),
                    "{z}", String.valueOf(z)
            )), throwable);
            result.complete(null);
        }

        return result;
    }

    @NotNull
    private StringBuilder getStringBuilder() {
        StringBuilder locations = new StringBuilder();

        int x = 1;

        for (final Block block : this.locationSettings.getDropLocations()) {
            final Map<String, String> placeholders = new HashMap<>();

            placeholders.put("{id}", String.valueOf(x));
            placeholders.put("{world}", block.getWorld().getName());
            placeholders.put("{x}", String.valueOf(block.getX()));
            placeholders.put("{y}", String.valueOf(block.getY()));
            placeholders.put("{z}", String.valueOf(block.getZ()));

            locations.append(Messages.location_format.getMessage(Audience.empty(), placeholders).translateEscapes());

            x += 1;
        }

        return locations;
    }

    public boolean startEnvoyEvent() {
        return requestStart(null).accepted();
    }

    public boolean startEnvoyEvent(Player starter) {
        return requestStart(starter).accepted();
    }

    public CompletionStage<Boolean> startEnvoyEventAsync() {
        return startEnvoyEventAsync(null);
    }

    public CompletionStage<Boolean> startEnvoyEventAsync(Player starter) {
        return requestStart(starter).completion();
    }

    private StartRequest requestStart(final Player starter) {
        if (!this.ready.get() || this.tiers.isEmpty()) {
            if (this.tiers.isEmpty()) {
                this.fusion.log(Level.ERROR, Messages.log_no_tiers.getString());
            }

            return new StartRequest(false, CompletableFuture.completedFuture(false));
        }

        final EventSession session = new EventSession(this.sessionSequence.incrementAndGet(), starter);

        if (!this.currentSession.compareAndSet(null, session)) {
            return new StartRequest(false, CompletableFuture.completedFuture(false));
        }

        this.scheduler.runGlobal("begin envoy session " + session.id(), () -> beginStart(session))
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) failStart(session, Messages.log_start_begin_failed.getString(), throwable);
                });

        return new StartRequest(true, session.startFuture());
    }

    private void beginStart(final EventSession session) {
        if (!isResolving(session)) return;

        generateSpawnLocationsAsync(() -> !isResolving(session)).whenComplete((locations, throwable) -> {
            this.scheduler.runGlobal("process generated locations for session " + session.id(), () -> {
                    if (!isResolving(session)) return;

                    if (throwable != null) {
                        failStart(session, Messages.log_start_generation_failed.getString(), throwable);
                        return;
                    }

                    final List<Block> snapshot = locations == null ? List.of() : List.copyOf(locations);
                    session.generationCompleted().set(true);
                    this.lastGeneratedLocations = snapshot;
                    this.locationSettings.replaceDropLocations(snapshot);
                    session.generatedLocations().addAll(snapshot);

                    if (snapshot.isEmpty()) {
                        failNoLocations(session);
                        return;
                    }

                    Files.users.getConfiguration().set("Locations.Spawned", getBlockList(snapshot));
                    Files.users.save();

                    cleanupEditors();

                    snapshotPlayerPositions().thenCompose(positions -> spawnResolvedLocations(session, snapshot, positions))
                            .whenComplete((spawned, spawnError) -> {
                                this.scheduler.runGlobal("activate envoy session " + session.id(), () -> {
                                if (!isResolving(session)) return;

                                if (spawnError != null) {
                                    failStart(session, Messages.log_start_spawn_failed.getString(), spawnError);
                                    return;
                                }

                                if (spawned == null || spawned < 1) {
                                    failNoLocations(session);
                                    return;
                                }

                                activateSession(session, spawned);
                                }).whenComplete((unused, scheduleError) -> {
                                    if (scheduleError != null && isResolving(session)) {
                                        failStart(session, Messages.log_start_activation_failed.getString(), scheduleError);
                                    }
                                });
                            });
                }).whenComplete((unused, scheduleError) -> {
                    if (scheduleError != null && isResolving(session)) {
                        failStart(session, Messages.log_start_processing_failed.getString(), scheduleError);
                    }
                });
        });
    }

    private CompletableFuture<List<EventSession.PlayerPosition>> snapshotPlayerPositions() {
        return this.scheduler.supplyGlobal("snapshot online players", () -> List.copyOf(this.server.getOnlinePlayers()))
                .thenCompose(players -> {
                    if (players.isEmpty()) return CompletableFuture.completedFuture(List.of());

                    final List<EventSession.PlayerPosition> positions = new CopyOnWriteArrayList<>();
                    final List<CompletableFuture<EventSession.PlayerPosition>> futures = new ArrayList<>();

                    for (final Player player : players) {
                        final CompletableFuture<EventSession.PlayerPosition> future = this.scheduler.supplyEntity(
                                player, "snapshot player position", () -> EventSession.PlayerPosition.from(player)
                        );
                        future.whenComplete((position, throwable) -> {
                            if (throwable == null && position != null) positions.add(position);
                        });
                        futures.add(future);
                    }

                    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                            .handle((unused, throwable) -> List.copyOf(positions));
                });
    }

    private CompletableFuture<Integer> spawnResolvedLocations(final EventSession session, final List<Block> locations, final List<EventSession.PlayerPosition> players) {
        final java.util.concurrent.atomic.AtomicInteger spawned = new java.util.concurrent.atomic.AtomicInteger();
        final List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (final Block block : locations) {
            final CompletableFuture<Boolean> future = this.scheduler.supplyRegion(
                    block.getLocation(), "spawn envoy crate for session " + session.id(), () -> spawnEnvoy(session, block, players)
            );
            future.whenComplete((success, throwable) -> {
                if (throwable == null && Boolean.TRUE.equals(success)) spawned.incrementAndGet();
            });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .handle((unused, throwable) -> spawned.get());
    }

    private boolean spawnEnvoy(final EventSession session, final Block block, final List<EventSession.PlayerPosition> players) {
        if (!isResolving(session)) return false;
        if (block.getY() < block.getWorld().getMinHeight() || block.getY() >= block.getWorld().getMaxHeight()) return false;
        if (block.getType() != Material.AIR) return false;

        final Tier tier = pickRandomTier();
        if (tier == null) return false;

        final Location location = block.getLocation();
        final boolean useFallingBlock = this.config.getProperty(ConfigKeys.envoy_falling_block_toggle)
                && players.stream().anyMatch(position -> position.isNear(location, 40, 40));

        if (useFallingBlock) {
            final ItemType itemType = ItemUtils.getItemType(this.config.getProperty(ConfigKeys.envoy_falling_block_type).toLowerCase());
            if (itemType == null) return false;

            final int fallingHeight = this.config.getProperty(ConfigKeys.envoy_falling_height);
            final FallingBlock fallingBlock = block.getWorld().spawn(location.clone().add(.5, fallingHeight, .5), FallingBlock.class);
            session.fallingCrates().put(fallingBlock, new EventSession.FallingCrate(session.id(), block, tier));
            session.spawnedLocations().add(block);

            fallingBlock.setBlockData(itemType.createItemStack().getType().createBlockData());
            fallingBlock.setDropItem(false);
            fallingBlock.setHurtEntities(false);
            fallingBlock.getPersistentDataContainer().set(
                    PersistentKeys.falling_envoy_session.getNamespacedKey(), PersistentDataType.LONG, session.id()
            );

            if (!isResolving(session)) {
                session.fallingCrates().remove(fallingBlock);
                session.spawnedLocations().remove(block);
                fallingBlock.remove();
                return false;
            }

            return true;
        }

        session.spawnedLocations().add(block);
        block.setType(tier.getPlacedBlockMaterial());
        session.activeCrates().put(block, new EventSession.ActiveCrate(session.id(), block, tier, EventSession.CrateState.ACTIVE));
        this.locationSettings.addActiveLocation(block);

        try {
            if (tier.isHoloEnabled() && this.holograms != null) {
                this.holograms.createHologram(location, tier, MiscUtils.toString(location));
            }
            if (tier.getSignalFlareToggle()) startSignalFlare(session, location, tier);
        } catch (Throwable throwable) {
            this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_visuals_failed.getMessage(Map.of(
                    "{session}", String.valueOf(session.id())
            )), throwable);
        }

        if (!isResolving(session)) {
            block.setType(Material.AIR);
            try {
                if (this.holograms != null) this.holograms.removeHologram(MiscUtils.toString(location));
            } catch (Throwable throwable) {
                this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_hologram_cleanup_failed.getString(), throwable);
            }
            stopSignalFlare(location);
            session.activeCrates().remove(block);
            session.spawnedLocations().remove(block);
            this.locationSettings.removeActiveLocation(block);
            return false;
        }

        return true;
    }

    private void activateSession(final EventSession session, final int spawned) {
        final CountdownTimer graceTimer = this.config.getProperty(ConfigKeys.envoys_grace_period_toggle)
                ? new CountdownTimer(this.plugin, this.config.getProperty(ConfigKeys.envoys_grace_period_timer))
                : null;
        this.countdownTimer = graceTimer;
        session.remainingCrates().set(spawned);

        if (!session.phase().compareAndSet(EventSession.Phase.RESOLVING, EventSession.Phase.ACTIVE)) return;
        if (this.currentSession.get() != session) {
            session.phase().compareAndSet(EventSession.Phase.ACTIVE, EventSession.Phase.STOPPED);
            session.startFuture().complete(false);
            return;
        }

        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{amount}", String.valueOf(spawned));

        if (session.starterName() != null) {
            placeholders.put("{starter}", session.starterName());
            Messages.started_player.broadcast(this.config.getProperty(ConfigKeys.envoys_ignore_behaviour_started_player), placeholders);
        } else {
            Messages.started.broadcast(this.config.getProperty(ConfigKeys.envoys_ignore_behaviour_started), placeholders);
        }

        Messages.envoy_locations.broadcast(this.config.getProperty(ConfigKeys.envoys_locations_broadcast), "envoy.locations", Map.of(
                "{locations}", getStringBuilder().toString().translateEscapes()
        ));

        if (graceTimer != null) graceTimer.scheduleTimer();

        this.runTimeTask = new FoliaScheduler(this.plugin, Scheduler.global_scheduler) {
            @Override
            public void run() {
                if (currentSession.get() != session || session.phase().get() != EventSession.Phase.ACTIVE) return;

                pluginManager.callEvent(new EnvoyEndEvent(EnvoyEndReason.OUT_OF_TIME));
                Messages.ended.broadcast(config.getProperty(ConfigKeys.envoys_ignore_behaviour_ended));
                endEnvoyEventAsync();
            }
        }.runDelayed(getTimeSeconds(this.config.getProperty(ConfigKeys.envoys_run_time)) * 20L);

        if (this.currentSession.get() != session || session.phase().get() != EventSession.Phase.ACTIVE) {
            if (graceTimer != null) graceTimer.cancelSafely();
            cancelEnvoyRunTime();
            return;
        }

        this.envoyTimeLeft = getEnvoyRunTimeCalendar();
        session.startFuture().complete(true);
    }

    private void failNoLocations(final EventSession session) {
        setNextEnvoy(getEnvoyCooldown());
        resetWarnings();
        this.pluginManager.callEvent(new EnvoyEndEvent(EnvoyEndReason.NO_LOCATIONS_FOUND));
        Messages.no_spawn_locations_found.broadcast(this.config.getProperty(ConfigKeys.envoys_ignore_behaviour_no_spawn_locations_found));
        this.fusion.log(Level.WARNING, Messages.log_no_valid_locations.getString());
        session.startFuture().complete(false);
        endEnvoyEventAsync();
    }

    private void failStart(final EventSession session, final String message, final Throwable throwable) {
        if (this.currentSession.get() != session
                || !session.phase().compareAndSet(EventSession.Phase.RESOLVING, EventSession.Phase.STOPPED)) return;

        if (throwable == null) this.fusion.log(Level.WARNING, message);
        else this.plugin.getLogger().log(java.util.logging.Level.SEVERE, message, throwable);

        setNextEnvoy(getEnvoyCooldown());
        resetWarnings();
        session.startFuture().complete(false);
        session.cleanupFuture().complete(null);
        this.currentSession.compareAndSet(session, null);
    }

    private boolean isResolving(final EventSession session) {
        return this.currentSession.get() == session && session.phase().get() == EventSession.Phase.RESOLVING;
    }

    public void endEnvoyEvent() {
        endEnvoyEventAsync();
    }

    public CompletionStage<Void> endEnvoyEventAsync() {
        final EventSession session = this.currentSession.get();
        if (session == null) return CompletableFuture.completedFuture(null);

        final EventSession.Phase phase = session.phase().get();
        if (phase == EventSession.Phase.STOPPING || phase == EventSession.Phase.STOPPED) return session.cleanupFuture();
        if (!session.phase().compareAndSet(phase, EventSession.Phase.STOPPING)) return endEnvoyEventAsync();

        final Set<Block> spawnedSnapshot = Set.copyOf(session.spawnedLocations());
        final Set<Entity> fallingSnapshot = Set.copyOf(session.fallingCrates().keySet());
        final Map<Location, ScheduledTask> signalSnapshot = Map.copyOf(session.signalTasks());

        session.startFuture().complete(false);
        cancelEnvoyRunTime();
        if (this.countdownTimer != null) this.countdownTimer.cancelSafely();

        final List<CompletableFuture<Void>> cleanupTasks = new ArrayList<>();
        final AtomicBoolean cleanupFailed = new AtomicBoolean();

        signalSnapshot.forEach((location, signalTask) -> {
            final CompletableFuture<Void> task = this.scheduler.runRegion(location, "cancel envoy signal for session " + session.id(), () -> {
                try {
                    signalTask.cancel();
                } finally {
                    session.signalTasks().remove(location, signalTask);
                }
            });
            task.whenComplete((unused, throwable) -> {
                if (throwable != null) cleanupFailed.set(true);
            });
            cleanupTasks.add(task);
        });

        for (final Block block : spawnedSnapshot) {
            final CompletableFuture<Void> task = this.scheduler.runRegion(block.getLocation(), "clean envoy block for session " + session.id(), () -> {
                if (this.currentSession.get() != session || session.phase().get() != EventSession.Phase.STOPPING) return;

                final EventSession.ActiveCrate crate = session.activeCrates().get(block);
                if (crate == null || crate.sessionId() != session.id()) return;

                crate.state().set(EventSession.CrateState.REMOVING);
                block.setType(Material.AIR);

                if (this.holograms != null) this.holograms.removeHologram(MiscUtils.toString(block.getLocation()));

                this.locationSettings.removeActiveLocation(block);
                crate.state().set(EventSession.CrateState.REMOVED);
            });
            task.whenComplete((unused, throwable) -> {
                if (throwable != null) cleanupFailed.set(true);
            });
            cleanupTasks.add(task);
        }

        for (final Entity entity : fallingSnapshot) {
            final CompletableFuture<Void> task = this.scheduler.runEntity(entity, "remove falling envoy for session " + session.id(), () -> {
                if (this.currentSession.get() == session && session.phase().get() == EventSession.Phase.STOPPING) entity.remove();
            });
            task.whenComplete((unused, throwable) -> {
                if (throwable != null) cleanupFailed.set(true);
            });
            cleanupTasks.add(task);
        }

        CompletableFuture.allOf(cleanupTasks.toArray(CompletableFuture[]::new)).whenComplete((unused, throwable) ->
                this.scheduler.runGlobal("finish envoy cleanup for session " + session.id(), () -> {
                    if (!this.shuttingDown.get()) finishCleanup(session, cleanupFailed.get() || throwable != null);
                })
        );

        return session.cleanupFuture();
    }

    private void finishCleanup(final EventSession session, final boolean failed) {
        session.activeCrates().clear();
        session.fallingCrates().clear();
        session.signalTasks().clear();
        session.spawnedLocations().clear();
        this.locationSettings.clearActiveLocations();
        this.locationSettings.clearDropLocations();

        if (!failed) {
            Files.users.getConfiguration().set("Locations.Spawned", new ArrayList<>());
            Files.users.save();
        }

        if (this.config.getProperty(ConfigKeys.envoys_run_time_toggle)) {
            setNextEnvoy(getEnvoyCooldown());
            resetWarnings();
        }

        this.coolDownSettings.clearCoolDowns();
        session.phase().set(EventSession.Phase.STOPPED);
        this.currentSession.compareAndSet(session, null);

        if (failed) session.cleanupFuture().completeExceptionally(new IllegalStateException("One or more envoy cleanup tasks failed"));
        else session.cleanupFuture().complete(null);
    }

    public void shutdown() {
        this.shuttingDown.set(true);
        this.ready.set(false);
        cancelEnvoyRunTime();
        cancelEnvoyCooldownTime();

        final EventSession session = this.currentSession.get();
        if (session == null) return;

        if (!this.currentSession.compareAndSet(session, null)) return;
        session.phase().set(EventSession.Phase.STOPPED);
        session.startFuture().complete(false);
        session.cleanupFuture().complete(null);

        session.signalTasks().values().forEach(task -> {
            try {
                task.cancel();
            } catch (Throwable throwable) {
                this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_signal_cancel_failed.getString(), throwable);
            }
        });

    }

    private record StartRequest(boolean accepted, CompletionStage<Boolean> completion) {}

    /**
     * Get a list of all the tiers.
     *
     * @return List of all the tiers.
     */
    public List<Tier> getTiers() {
        return List.copyOf(this.tiers);
    }

    /**
     * Get a tier from its name.
     *
     * @param tierName The name of the tier.
     * @return Returns a tier or will return null if not tier is found.
     */
    public Tier getTier(String tierName) {
        for (Tier tier : this.tiers) {
            if (tier.getName().equalsIgnoreCase(tierName)) return tier;
        }

        return null;
    }

    /**
     * @param loc The location the signals will be at.
     * @param tier The tier the signal is.
     */
    public void startSignalFlare(final Location loc, final Tier tier) {
        final EventSession session = this.currentSession.get();
        if (session == null) return;

        startSignalFlare(session, loc, tier);
    }

    private void startSignalFlare(final EventSession session, final Location loc, final Tier tier) {
        final ScheduledTask task = new FoliaScheduler(this.plugin, loc) {
            @Override
            public void run() {
                if (currentSession.get() != session || session.phase().get() != EventSession.Phase.ACTIVE) return;
                firework(loc.clone().add(.5, 0, .5), tier);
            }
        }.runAtFixedRate(getTimeSeconds(tier.getSignalFlareTimer()) * 20L, getTimeSeconds(tier.getSignalFlareTimer()) * 20L);

        session.signalTasks().put(loc, task);
    }

    /**
     * @param loc The location that the signal is stopping.
     */
    public void stopSignalFlare(Location loc) {
        final EventSession session = this.currentSession.get();
        if (session == null) return;

        final ScheduledTask task = session.signalTasks().remove(loc);
        if (task == null) return;

        try {
            task.cancel();
        } catch (Throwable throwable) {
            this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_signal_cancel_failed.getString(), throwable);
        }
    }

    /**
     * @return The center location for the random crates.
     */
    public Location getCenter() {
        final Location current = this.center;
        return current == null ? null : current.clone();
    }

    /**
     * Sets the center location for the random crates.
     *
     * @param loc The new center location.
     */
    public void setCenter(Location loc) {
        final Location snapshot = loc.clone();
        final String serialized = Methods.getUnBuiltLocation(snapshot);
        this.center = snapshot;
        this.centerString = serialized;

        this.scheduler.runGlobal("save envoy center", () -> {
            Files.users.getConfiguration().set("Center", serialized);
            Files.users.save();
        });
    }

    /**
     * Check if a player is ignoring the messages.
     *
     * @param uuid The player's UUID.
     * @return True if they are ignoring them and false if not.
     */
    public boolean isIgnoringMessages(UUID uuid) {
        return this.ignoreMessages.contains(uuid);
    }

    /**
     * Make a player ignore the messages.
     *
     * @param uuid The player's UUID.
     */
    public void addIgnorePlayer(UUID uuid) {
        this.ignoreMessages.add(uuid);
    }

    /**
     * Make a player stop ignoring the messages.
     *
     * @param uuid The player's UUID.
     */
    public void removeIgnorePlayer(UUID uuid) {
        this.ignoreMessages.remove(uuid);
    }

    /**
     * Used to clean all spawn locations and set them back to air.
     */
    public void cleanLocations() {
        endEnvoyEventAsync().whenComplete((unused, throwable) -> recoverPersistedLocations());
    }

    private CompletableFuture<Void> recoverPersistedLocations() {
        final Set<Block> locations = ConcurrentHashMap.newKeySet();
        locations.addAll(this.locationSettings.getActiveLocations());
        locations.addAll(getLocationsFromStringList(Files.users.getConfiguration().getStringList("Locations.Spawned")));

        if (locations.isEmpty()) {
            this.locationSettings.clearActiveLocations();
            this.locationSettings.clearDropLocations();
            return CompletableFuture.completedFuture(null);
        }

        final AtomicBoolean failed = new AtomicBoolean();
        final List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (final Block original : locations) {
            if (original == null) continue;

            final CompletableFuture<Void> task = resolveConfiguredLocation(original, this.shuttingDown::get).thenCompose(block -> {
                if (block == null) {
                    failed.set(true);
                    return CompletableFuture.completedFuture(null);
                }

                return this.scheduler.supplyRegion(block.getLocation(), "recover persisted envoy location", () -> {
                    if (this.shuttingDown.get()) return List.<FallingBlock>of();

                    final Block above = block.getY() + 1 < block.getWorld().getMaxHeight()
                            ? block.getRelative(org.bukkit.block.BlockFace.UP)
                            : null;
                    clearRecoveredCrate(block);
                    if (above != null) clearRecoveredCrate(above);

                    if (this.holograms != null) {
                        this.holograms.removeHologram(MiscUtils.toString(block.getLocation()));
                        if (above != null) this.holograms.removeHologram(MiscUtils.toString(above.getLocation()));
                    }

                    final double radius = Math.max(8, this.config.getProperty(ConfigKeys.envoy_falling_height) + 4);
                    return block.getLocation().getNearbyEntitiesByType(FallingBlock.class, 1.5, radius, 1.5).stream()
                            .filter(entity -> entity.getPersistentDataContainer().has(PersistentKeys.falling_envoy_session.getNamespacedKey()))
                            .toList();
                }).thenCompose(entities -> {
                    final List<CompletableFuture<Void>> removals = new ArrayList<>();
                    for (final FallingBlock entity : entities) {
                        removals.add(this.scheduler.runEntity(entity, "remove recovered falling envoy", entity::remove));
                    }
                    return CompletableFuture.allOf(removals.toArray(CompletableFuture[]::new));
                });
            });

            task.whenComplete((unused, throwable) -> {
                if (throwable != null) failed.set(true);
            });
            tasks.add(task);
        }

        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new))
                .handle((unused, throwable) -> failed.get() || throwable != null)
                .thenCompose(recoveryFailed -> this.scheduler.runGlobal("finish persisted envoy recovery", () -> {
                    if (this.shuttingDown.get()) return;

                    this.locationSettings.clearActiveLocations();
                    this.locationSettings.clearDropLocations();

                    if (!recoveryFailed) {
                        Files.users.getConfiguration().set("Locations.Spawned", new ArrayList<>());
                        Files.users.save();
                    }
                }));
    }

    private void clearRecoveredCrate(final Block block) {
        final Material material = block.getType();
        final ItemType fallingType = ItemUtils.getItemType(this.config.getProperty(ConfigKeys.envoy_falling_block_type).toLowerCase());
        final boolean fallingMaterial = fallingType != null && fallingType.createItemStack().getType() == material;

        if (fallingMaterial || this.tiers.stream().anyMatch(tier -> tier.getPlacedBlockMaterial() == material)) {
            block.setType(Material.AIR);
        }
    }

    private boolean testCenter() {
        if (isCenterUnloaded()) { // Check to make sure the center exist and if not try to load it again.
            this.fusion.log(Level.WARNING, Messages.log_center_repair_attempt.getString());
            
            loadCenter();

            if (isCenterUnloaded()) { // If center still doesn't exist then it cancels the event.
                this.fusion.log(Level.WARNING, Messages.log_center_debug.getMessage(Map.of(
                        "{saved}", String.valueOf(centerString),
                        "{location}", String.valueOf(center),
                        "{world_loaded}", String.valueOf(center != null && center.getWorld() != null)
                )));
                this.fusion.log(Level.WARNING, Messages.log_center_repair_failed.getString());

                return false;
            } else {
                this.fusion.log(Level.WARNING, Messages.log_center_repair_success.getString());
            }
        }

        return true;
    }

    private void loadCenter() {
        FileConfiguration users = Files.users.getConfiguration();

        if (users.contains("Center")) {
            this.centerString = users.getString("Center");
            this.center = this.centerString == null ? null : Methods.getBuiltLocation(centerString);
        } else {
            this.center = this.server.getWorlds().getFirst().getSpawnLocation();
        }

        if (isCenterUnloaded()) {
            this.fusion.log(Level.WARNING, Messages.log_center_repair_failed.getString());
        }
    }

    private boolean isCenterUnloaded() {
        return this.center == null || this.center.getWorld() == null;
    }

    private Calendar getEnvoyCooldown() {
        Calendar cal = Calendar.getInstance();

        if (this.config.getProperty(ConfigKeys.envoys_countdown)) {
            String time = this.config.getProperty(ConfigKeys.envoys_cooldown);

            cal = Methods.getTimeFromString(time);
        } else {
            getEnvoyTime(cal);
        }

        return cal;
    }

    private Calendar getEnvoyRunTimeCalendar() {
        String time = this.config.getProperty(ConfigKeys.envoys_run_time).toLowerCase();

        return Methods.getTimeFromString(time);
    }

    private void firework(Location loc, Tier tier) {
        List<Color> colors = tier.getFireworkColors();

        Firework firework = loc.getWorld().spawn(loc, Firework.class);

        FireworkMeta fireworkMeta = firework.getFireworkMeta();

        fireworkMeta.addEffects(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(colors).trail(true).flicker(false).build());
        fireworkMeta.setPower(1);
        firework.setFireworkMeta(fireworkMeta);

        PersistentDataContainer container = firework.getPersistentDataContainer();

        container.set(PersistentKeys.no_firework_damage.getNamespacedKey(), PersistentDataType.BOOLEAN, true);
    }

    private void cleanupEditors() {
        final List<UUID> editors = List.copyOf(this.editorSettings.getEditors());
        this.editorSettings.clearEditors();

        for (final UUID uuid : editors) {
            final Player player = this.server.getPlayer(uuid);
            if (player == null) continue;

            this.scheduler.runEntity(player, "remove player from envoy editor mode", () -> {
                this.editorSettings.removeFakeBlocks(player);
                player.getInventory().removeItem(new ItemStack(Material.BEDROCK, 1));
                Messages.kicked_from_editor_mode.sendMessage(player);
            });
        }
    }

    private void rebuildTierCache() {
        this.cachedChances.clear();

        for (final Tier tier : this.tiers) {
            for (int index = 0; index < tier.getSpawnChance(); index++) {
                this.cachedChances.add(tier);
            }
        }
    }

    private final class ConfiguredLocationResolver {

        private final List<Block> configured;
        private final int maximumInFlight;
        private final BooleanSupplier cancelled;
        private final List<Block> resolved = new ArrayList<>();
        private final CompletableFuture<List<Block>> future = new CompletableFuture<>();
        private int cursor;
        private int inFlight;

        private ConfiguredLocationResolver(final List<Block> configured, final int maximumInFlight, final BooleanSupplier cancelled) {
            this.configured = configured;
            this.maximumInFlight = maximumInFlight;
            this.cancelled = cancelled;
        }

        private CompletableFuture<List<Block>> start() {
            pump();
            return this.future;
        }

        private synchronized void pump() {
            if (this.future.isDone()) return;
            if (this.cancelled.getAsBoolean()) {
                this.future.complete(List.copyOf(this.resolved));
                return;
            }

            while (this.cursor < this.configured.size() && this.inFlight < this.maximumInFlight) {
                final Block block = this.configured.get(this.cursor++);
                if (block == null) continue;

                this.inFlight++;
                resolveConfiguredLocation(block, this.cancelled).whenComplete(this::completeLocation);
            }

            if (this.cursor >= this.configured.size() && this.inFlight == 0) {
                this.future.complete(List.copyOf(this.resolved));
            }
        }

        private synchronized void completeLocation(final Block block, final Throwable throwable) {
            this.inFlight--;
            if (!this.future.isDone() && throwable == null && block != null && !this.resolved.contains(block)) {
                this.resolved.add(block);
            }
            pump();
        }
    }

    private final class RandomLocationGenerator {

        private final Location center;
        private final World world;
        private final int target;
        private final int minimumRadius;
        private final int maximumRadius;
        private final int maximumAttempts;
        private final int maximumInFlight;
        private final BooleanSupplier cancelled;
        private final Set<Long> candidates = ConcurrentHashMap.newKeySet();
        private final List<Block> results = new CopyOnWriteArrayList<>();
        private final CompletableFuture<List<Block>> future = new CompletableFuture<>();

        private int attempts;
        private int inFlight;

        private RandomLocationGenerator(
                final Location center,
                final int target,
                final int minimumRadius,
                final int maximumRadius,
                final int maximumAttempts,
                final int maximumInFlight,
                final BooleanSupplier cancelled
        ) {
            this.center = center;
            this.world = center.getWorld();
            this.target = target;
            this.minimumRadius = minimumRadius;
            this.maximumRadius = maximumRadius;
            this.maximumAttempts = maximumAttempts;
            this.maximumInFlight = maximumInFlight;
            this.cancelled = cancelled;
        }

        private CompletableFuture<List<Block>> start() {
            pump();
            return this.future;
        }

        private synchronized int attempts() {
            return this.attempts;
        }

        private synchronized void pump() {
            if (this.future.isDone()) return;

            if (this.cancelled.getAsBoolean() || this.maximumRadius <= 0) {
                this.future.complete(List.copyOf(this.results));
                return;
            }

            while (this.results.size() < this.target && this.inFlight < this.maximumInFlight && this.attempts < this.maximumAttempts) {
                this.attempts++;

                final int x = this.center.getBlockX() - this.maximumRadius + ThreadLocalRandom.current().nextInt(this.maximumRadius * 2);
                final int z = this.center.getBlockZ() - this.maximumRadius + ThreadLocalRandom.current().nextInt(this.maximumRadius * 2);

                if (Math.abs(x - this.center.getBlockX()) <= this.minimumRadius
                        && Math.abs(z - this.center.getBlockZ()) <= this.minimumRadius) {
                    continue;
                }

                final long key = ((long) x << 32) ^ (z & 0xffffffffL);
                if (!this.candidates.add(key)) continue;

                this.inFlight++;
                resolve(x, z).whenComplete(this::completeCandidate);
            }

            if (this.results.size() >= this.target || (this.attempts >= this.maximumAttempts && this.inFlight == 0)) {
                this.future.complete(List.copyOf(this.results));
            }
        }

        private CompletableFuture<Block> resolve(final int x, final int z) {
            final CompletableFuture<Block> result = new CompletableFuture<>();

            try {
                this.world.getChunkAtAsync(x >> 4, z >> 4, true, false).whenComplete((chunk, throwable) -> {
                    if (throwable != null || this.cancelled.getAsBoolean() || this.future.isDone()) {
                        if (throwable != null) {
                            plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_chunk_load_failed.getMessage(Map.of(
                                    "{type}", "random",
                                    "{x}", String.valueOf(x),
                                    "{z}", String.valueOf(z)
                            )), throwable);
                        }
                        result.complete(null);
                        return;
                    }

                    final Location owner = new Location(this.world, x, 0, z);
                    scheduler.supplyRegion(owner, "validate random envoy location at " + x + "," + z, () -> {
                        if (this.cancelled.getAsBoolean() || this.future.isDone()) return null;

                        final Block highest = this.world.getHighestBlockAt(x, z);
                        if (highest.getY() <= this.world.getMinHeight()) return null;
                        if (blacklistedBlocks.contains(highest.getType())) return null;

                        return highest.getType() == Material.AIR ? highest : highest.getRelative(org.bukkit.block.BlockFace.UP);
                    }).whenComplete((block, error) -> {
                        if (error != null) result.complete(null);
                        else result.complete(block);
                    });
                });
            } catch (Throwable throwable) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_chunk_request_failed.getMessage(Map.of(
                        "{type}", "random",
                        "{x}", String.valueOf(x),
                        "{z}", String.valueOf(z)
                )), throwable);
                result.complete(null);
            }

            return result;
        }

        private synchronized void completeCandidate(final Block block, final Throwable throwable) {
            this.inFlight--;

            if (!this.future.isDone() && throwable == null && block != null && !this.results.contains(block)) {
                this.results.add(block);
            }

            pump();
        }
    }

    private int getTimeSeconds(String time) {
        int seconds = 0;

        for (String i : time.split(" ")) {
            if (i.contains("d")) {
                seconds += Integer.parseInt(i.replace("d", "")) * 86400;
            } else if (i.contains("h")) {
                seconds += Integer.parseInt(i.replace("h", "")) * 3600;
            } else if (i.contains("m")) {
                seconds += Integer.parseInt(i.replace("m", "")) * 60;
            } else if (i.contains("s")) {
                seconds += Integer.parseInt(i.replace("s", ""));
            }
        }

        return seconds;
    }

    private Tier pickRandomTier() {
        if (this.cachedChances.isEmpty()) return null;

        return this.cachedChances.get(ThreadLocalRandom.current().nextInt(this.cachedChances.size()));
    }

    /**
     * @param location The location that you want to check.
     */
    public boolean isLocation(Location location) {
        for (Block block : this.locationSettings.getSpawnLocations()) {
            if (block.getLocation().equals(location)) return true;
        }

        return false;
    }

    public CountdownTimer getCountdownTimer() {
        return this.countdownTimer;
    }

    // Get world location.
    private List<String> getBlockList(List<Block> stringList) {
        List<String> strings = new ArrayList<>();

        for (Block block : stringList) {
            strings.add(Methods.getUnBuiltLocation(block.getLocation()));
        }

        return strings;
    }

    private List<Block> getLocationsFromStringList(List<String> locationsList) {
        List<Block> locations = new ArrayList<>();

        for (String location : locationsList) {
            try {
                locations.add(Methods.getBuiltLocation(location).getBlock());
            } catch (RuntimeException exception) {
                this.plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_location_deserialize_failed.getMessage(Map.of(
                        "{location}", location
                )), exception);
            }
        }

        return locations;
    }
}
