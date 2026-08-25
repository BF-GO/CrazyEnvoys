package com.badbones69.crazyenvoys.api.objects.misc;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.Methods;
import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.api.enums.PersistentKeys;
import com.badbones69.crazyenvoys.util.ItemUtil;
import com.ryderbelserion.fusion.core.api.enums.Level;
import com.ryderbelserion.fusion.core.api.exceptions.FusionException;
import com.ryderbelserion.fusion.paper.FusionPaper;
import com.ryderbelserion.fusion.paper.utils.ColorUtils;
import com.ryderbelserion.fusion.paper.utils.ItemUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.*;

public class Tier {

    private final CrazyEnvoys plugin = CrazyEnvoys.get();

    private final FusionPaper fusion = this.plugin.getFusion();

    private final ItemStack itemStack;

    private final String name;

    private String displayName;

    private int maxPerEvent;

    private ArmorSetDefinition armorSetDefinition;

    private final boolean armorSetConfigured;

    private RareEnchantmentDefinition armorBreakerDefinition = RareEnchantmentDefinition.disabled();

    private boolean claimPermissionToggle;

    private String claimPermission;
    
    private int spawnChance;
    private boolean useChance;
    private boolean bulkToggle;
    private boolean bulkRandom;
    private int bulkMax;
    private boolean holoToggle;
    private double holoHeight;
    private volatile List<String> holoMessage;
    private boolean fireworkToggle;
    private volatile List<Color> fireworkColors = List.of();
    private boolean signalFlareToggle;
    private String signalFlareTimer;
    private volatile List<Color> signalFlareColors = List.of();
    private volatile List<Prize> prizes = List.of();
    private volatile List<String> prizeMessage = new ArrayList<>();

    public Tier(
            final boolean claimPermissionToggle,
            @NotNull final String claimPermission,
            final boolean useChance,
            final int spawnChance,
            final boolean bulkToggle,
            final boolean bulkRandom,
            final int maxBulk,
            final boolean holoToggle,
            final int holoRange,
            final double holoHeight,
            @NotNull final List<String> holoMessage,
            @NotNull final YamlConfiguration configuration,
            @NotNull final Path path
    ) {
        this.claimPermissionToggle = claimPermissionToggle;
        this.claimPermission = claimPermission;
        this.useChance = useChance;
        this.spawnChance = spawnChance;
        this.bulkToggle = bulkToggle;
        this.bulkRandom = bulkRandom;
        this.bulkMax = maxBulk;
        this.holoToggle = holoToggle;
        this.holoRange = holoRange;
        this.holoHeight = holoHeight;
        this.holoMessage = List.copyOf(holoMessage);

        final ItemType itemType = ItemUtils.getItemType(configuration.getString("Settings.Placed-Block", "chest").toLowerCase());

        this.itemStack = itemType == null ? ItemType.CHEST.createItemStack() : itemType.createItemStack();

        this.fireworkToggle = configuration.getBoolean("Settings.Firework-Toggle", false);

        final List<String> colors = configuration.getStringList("Settings.Firework-Colors");

        if (colors.isEmpty()) {
            setFireworkColors(Arrays.asList(Color.GRAY, Color.BLACK, Color.ORANGE));
        } else {
            colors.forEach(color -> addFireworkColor(ColorUtils.getColor(color)));
        }

        final Map<String, String> placeholders = Map.of(
                "%player%", "{player}",
                "%reward", "{reward}",
                "%Player%", "{player}",
                "%tier%", "{tier}"
        );

        for (final String message : configuration.getStringList("Settings.Prize-Message")) {
            if (message.isBlank()) continue;

            this.prizeMessage.add(this.fusion.replacePlaceholders(message, placeholders));
        }

        setSignalFlareToggle(configuration.getBoolean("Settings.Signal-Flare.Toggle", false));
        setSignalFlareTimer(configuration.getString("Settings.Signal-Flare.Time", "40s"));

        if (configuration.getStringList("Settings.Signal-Flare.Colors").isEmpty()) {
            setSignalFlareColors(Arrays.asList(Color.GRAY, Color.BLACK, Color.ORANGE));
        } else {
            configuration.getStringList("Settings.Signal-Flare.Colors").forEach(color -> addSignalFlareColor(ColorUtils.getColor(color)));
        }

        this.name = path.getFileName().toString().replace(".yml", "");
        this.displayName = configuration.getString("Settings.Display-Name", this.name);
        if (this.displayName == null || this.displayName.isBlank()) this.displayName = this.name;
        this.maxPerEvent = Math.max(0, configuration.getInt("Settings.Max-Per-Event", 0));
        this.armorBreakerDefinition = parseArmorBreaker(configuration.getConfigurationSection(
                "Settings.Rare-Enchantments.Armor-Breaker"
        ));
        final ConfigurationSection armorSetSection = configuration.getConfigurationSection("Settings.Armor-Set");
        this.armorSetConfigured = armorSetSection != null;
        this.armorSetDefinition = parseArmorSet(armorSetSection);

        final ConfigurationSection prizes = configuration.getConfigurationSection("Prizes");

        if (prizes == null) {
            throw new FusionException("Failed to find the prizes section in %s".formatted(this.name));
        }

        String armorSetError = null;

        for (final String id : prizes.getKeys(false)) {
            final ConfigurationSection prize = prizes.getConfigurationSection(id);

            if (prize == null) continue;

            final List<String> commands = new ArrayList<>();

            for (final String command : prize.getStringList("Commands")) {
                if (command.isBlank()) continue;

                commands.add(this.fusion.replacePlaceholders(command, placeholders));
            }

            final List<String> messages = new ArrayList<>();

            for (final String message : prize.getStringList("Messages")) {
                if (message.isBlank()) continue;

                messages.add(this.fusion.replacePlaceholders(message, placeholders));
            }

            final Prize loadedPrize = new Prize(id).setDisplayName(prize.getString("DisplayName", ""))
                    .setChance(prize.getInt("Chance", 10))
                    .setDropItems(prize.getBoolean("Drop-Items", true))
                    .setItemBuilders(ItemUtil.convertStringList(prize.getStringList("Items"), id))
                    .setCommands(commands).setMessages(messages);

            final String pieceName = prize.getString("Armor-Set-Piece", "");
            if (!pieceName.isBlank()) {
                final Optional<ArmorSetPiece> piece = ArmorSetPiece.fromConfig(pieceName);
                if (piece.isPresent()) loadedPrize.setArmorSetPiece(piece.get());
                else armorSetError = "unknown armor set piece " + pieceName;
            }

            addPrize(loadedPrize);
        }

        validateArmorSet(armorSetError);
    }

    private RareEnchantmentDefinition parseArmorBreaker(final ConfigurationSection section) {
        if (section == null || !section.getBoolean("Toggle", false)) {
            return RareEnchantmentDefinition.disabled();
        }

        try {
            return new RareEnchantmentDefinition(
                    true,
                    section.getDouble("Chance", 0.0D),
                    section.getInt("Level", 0)
            );
        } catch (IllegalArgumentException exception) {
            this.fusion.log(Level.WARNING, Messages.log_rare_enchantment_invalid.getMessage(Map.of(
                    "{tier}", this.name,
                    "{reason}", exception.getMessage()
            )));
            return RareEnchantmentDefinition.disabled();
        }
    }

    private ArmorSetDefinition parseArmorSet(final ConfigurationSection section) {
        if (section == null) return null;

        final String id = section.getString("Id", "").trim().toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]+")) return invalidArmorSet("invalid id");

        final String configuredDisplayName = section.getString("Display-Name", id);
        final String displayName = configuredDisplayName == null || configuredDisplayName.isBlank() ? id : configuredDisplayName;
        final Map<PotionEffectType, Integer> effects = new LinkedHashMap<>();

        for (final String value : section.getStringList("Effects")) {
            final String[] parts = value.trim().toLowerCase(Locale.ROOT).split(":", 2);
            if (parts.length != 2 || !parts[0].matches("[a-z0-9_]+")) {
                return invalidArmorSet("invalid effect " + value);
            }

            final int level;
            try {
                level = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                return invalidArmorSet("invalid effect level " + value);
            }

            if (level < 1) return invalidArmorSet("effect levels must be positive");

            final PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(parts[0]));
            if (type == null) return invalidArmorSet("unknown effect " + parts[0]);
            if (effects.putIfAbsent(type, level) != null) return invalidArmorSet("duplicate effect " + parts[0]);
        }

        if (effects.isEmpty()) return invalidArmorSet("no effects configured");

        return new ArmorSetDefinition(id, displayName, effects);
    }

    private void validateArmorSet(final String configurationError) {
        if (configurationError != null) {
            invalidateArmorSet(configurationError);
            return;
        }

        final boolean hasPieces = this.prizes.stream().anyMatch(prize -> prize.getArmorSetPiece().isPresent());
        if (this.armorSetDefinition == null) {
            if (hasPieces && !this.armorSetConfigured) invalidateArmorSet("pieces configured without a set definition");
            return;
        }

        final EnumMap<ArmorSetPiece, Prize> pieces = new EnumMap<>(ArmorSetPiece.class);
        for (final Prize prize : this.prizes) {
            final Optional<ArmorSetPiece> configuredPiece = prize.getArmorSetPiece();
            if (configuredPiece.isEmpty()) continue;

            final ArmorSetPiece piece = configuredPiece.get();
            if (pieces.putIfAbsent(piece, prize) != null) {
                invalidateArmorSet("duplicate piece " + piece.getConfigName());
                return;
            }

            if (prize.getItemBuilders().size() != 1) {
                invalidateArmorSet("piece " + piece.getConfigName() + " must contain exactly one item");
                return;
            }

            final ItemStack item = prize.getItemBuilders().getFirst().asItemStack();
            if (!piece.matches(item.getType())) {
                invalidateArmorSet("piece " + piece.getConfigName() + " has the wrong item type");
                return;
            }
        }

        if (pieces.size() != ArmorSetPiece.values().length) {
            invalidateArmorSet("all four armor pieces are required");
            return;
        }

        for (final Map.Entry<ArmorSetPiece, Prize> entry : pieces.entrySet()) {
            entry.getValue().getItemBuilders().getFirst().setPersistentString(
                    PersistentKeys.armor_set_id.getNamespacedKey(), this.armorSetDefinition.id()
            );
            entry.getValue().getItemBuilders().getFirst().setPersistentString(
                    PersistentKeys.armor_set_piece.getNamespacedKey(), entry.getKey().getConfigName()
            );
        }
    }

    private ArmorSetDefinition invalidArmorSet(@NotNull final String reason) {
        logInvalidArmorSet(reason);
        return null;
    }

    private void invalidateArmorSet(@NotNull final String reason) {
        this.armorSetDefinition = null;
        logInvalidArmorSet(reason);
    }

    private void logInvalidArmorSet(@NotNull final String reason) {
        this.fusion.log(Level.WARNING, Messages.log_armor_set_invalid.getMessage(Map.of(
                "{tier}", this.name,
                "{reason}", reason
        )));
    }

    // Check if the envoy is allowed to require the claim permission.
    public boolean isClaimPermissionToggleEnabled() {
        return this.claimPermissionToggle;
    }

    // Set the boolean toggle.
    public Tier setClaimPermissionToggle(boolean claimPermissionToggle) {
        this.claimPermissionToggle = claimPermissionToggle;

        return this;
    }

    // Fetch the permission required to claim the envoy.
    public String getClaimPermission() {
        return this.claimPermission;
    }

    // Set the claim permission.
    public Tier setClaimPermission(String claimPermission) {
        this.claimPermission = claimPermission;

        return this;
    }

    public List<String> getPrizeMessage() {
        return List.copyOf(this.prizeMessage);
    }

    public void setPrizeMessage(List<String> prizeMessage) {
        this.prizeMessage = List.copyOf(prizeMessage);
    }

    /**
     * Get the name of the tier.
     *
     * @return The name of the tier.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the MiniMessage display name shown to players.
     *
     * @return The configured display name, or the technical tier name when unset.
     */
    public String getDisplayName() {
        return this.displayName;
    }

    public Tier setDisplayName(@NotNull final String displayName) {
        this.displayName = displayName.isBlank() ? this.name : displayName;

        return this;
    }

    /**
     * Get the maximum number of crates of this tier in one event.
     *
     * @return A positive limit, or zero for no limit.
     */
    public int getMaxPerEvent() {
        return this.maxPerEvent;
    }

    public Tier setMaxPerEvent(final int maxPerEvent) {
        this.maxPerEvent = Math.max(0, maxPerEvent);

        return this;
    }

    public Optional<ArmorSetDefinition> getArmorSetDefinition() {
        return Optional.ofNullable(this.armorSetDefinition);
    }

    /**
     * Get the immutable Armor Breaker bonus settings for this tier.
     */
    public RareEnchantmentDefinition getArmorBreakerDefinition() {
        return this.armorBreakerDefinition;
    }
    
    /**
     * Get the chance of the crate spawning.
     */
    public int getSpawnChance() {
        return this.spawnChance;
    }
    
    /**
     * Set the chance that the crate will spawn in the event.
     *
     * @param spawnChance The new chance the crate will spawn.
     */
    public Tier setSpawnChance(int spawnChance) {
        this.spawnChance = spawnChance;

        return this;
    }
    
    /**
     * Check to see if this tier uses a chance system for the prizes.
     */
    public boolean getUseChance() {
        return this.useChance;
    }
    
    /**
     * Set if the tier uses a chance system for the prizes.
     *
     * @param useChance True if it uses a chance system and false if not.
     */
    public Tier setUseChance(boolean useChance) {
        this.useChance = useChance;

        return this;
    }

    /**
     * Get the material of the block that acts as the crate.
     */
    public Material getPlacedBlockMaterial() {
        return this.itemStack.getType();
    }
    
    /**
     * Check to see if the bulk prizes option is on.
     */
    public boolean getBulkToggle() {
        return this.bulkToggle;
    }
    
    /**
     * Set if the bulk prize option is on.
     *
     * @param bulkToggle True if it can give multiple prizes and false if not.
     */
    public Tier setBulkToggle(boolean bulkToggle) {
        this.bulkToggle = bulkToggle;

        return this;
    }
    
    /**
     * Check if it picks a random amount of prizes.
     *
     * @return true if it picks from 1-max. false if it picks the max amount of prizes.
     */
    public boolean getBulkRandom() {
        return this.bulkRandom;
    }
    
    /**
     * Set if it picks a random amount of prizes from 1-max.
     *
     * @param bulkRandom True if it picks from 1-max and false if it picks the max amount of prizes.
     */
    public Tier setBulkRandom(boolean bulkRandom) {
        this.bulkRandom = bulkRandom;

        return this;
    }
    
    /**
     * Get the max amount of prizes a bulk can have.
     */
    public int getBulkMax() {
        return this.bulkMax;
    }
    
    /**
     * Set the max amount of prizes a bulk can have.
     *
     * @param bulkMax The max amount of prizes.
     */
    public Tier setBulkMax(int bulkMax) {
        this.bulkMax = bulkMax;

        return this;
    }
    
    /**
     * Check to see if holograms are on for the tier crate.
     */
    public boolean isHoloEnabled() {
        return this.holoToggle;
    }
    
    /**
     * Set if the tier uses holograms.
     *
     * @param holoToggle True if it does and false if not.
     */
    public Tier setHoloToggle(boolean holoToggle) {
        this.holoToggle = holoToggle;

        return this;
    }
    
    /**
     * Get the height of the hologram.
     */
    public double getHoloHeight() {
        return this.holoHeight;
    }
    
    /**
     * Set the height of the hologram.
     *
     * @param holoHeight The height as a Double.
     */
    public Tier setHoloHeight(Double holoHeight) {
        this.holoHeight = holoHeight;

        return this;
    }

    private int holoRange;

    /**
     * Set the range at which a hologram can be seen.
     *
     * @param holoRange the range
     * @return the object with updated information.
     */
    public Tier setHoloRange(int holoRange) {
        this.holoRange = holoRange;

        return this;
    }

    /**
     * Gets the range at which a hologram can be seen.
     *
     * @return the range
     */
    public int getHoloRange() {
        return this.holoRange;
    }

    /**
     * Get the hologram message with all the placeholders added to it.
     *
     * @return The hologram with all placeholders in it.
     */
    public List<String> getHoloMessage() {
        return Methods.getPlaceholders(this.holoMessage);
    }

    /**
     * Set the message that the hologram displays.
     *
     * @param holoMessage The message that is displayed. This auto color codes the message.
     */
    public Tier setHoloMessage(List<String> holoMessage) {
        this.holoMessage = List.copyOf(holoMessage);

        return this;
    }
    
    /**
     * Check if the tier crate shoots a firework when claimed.
     */
    public boolean getFireworkToggle() {
        return this.fireworkToggle;
    }
    
    /**
     * Set if the tier crate shoots a firework when claimed.
     *
     * @param fireworkToggle True if it does and false if not.
     */
    public Tier setFireworkToggle(boolean fireworkToggle) {
        this.fireworkToggle = fireworkToggle;

        return this;
    }
    
    /**
     * List of all the colors that the firework displays.
     */
    public List<Color> getFireworkColors() {
        return List.copyOf(this.fireworkColors);
    }
    
    /**
     * Set the colors of the firework.
     *
     * @param fireworkColors List of Colors of the firework.
     */
    public Tier setFireworkColors(List<Color> fireworkColors) {
        this.fireworkColors = List.copyOf(fireworkColors);

        return this;
    }
    
    /**
     * Add a color to the firework effects.
     *
     * @param fireworkColor A color to add to the firework effect.
     */
    public synchronized Tier addFireworkColor(Color fireworkColor) {
        final List<Color> colors = new ArrayList<>(this.fireworkColors);
        colors.add(fireworkColor);
        this.fireworkColors = List.copyOf(colors);

        return this;
    }
    
    /**
     * Check to see if the tier crate shoots signal fireworks for players to see where it is.
     */
    public boolean getSignalFlareToggle() {
        return this.signalFlareToggle;
    }
    
    /**
     * Set if the tier crates shoot fireworks in the air.
     *
     * @param signalFlareToggle True if it does and false if not.
     */
    public Tier setSignalFlareToggle(boolean signalFlareToggle) {
        this.signalFlareToggle = signalFlareToggle;

        return this;
    }
    
    /**
     * Get the amount of time when a flare is shot off.
     */
    public String getSignalFlareTimer() {
        return this.signalFlareTimer;
    }
    
    /**
     * Set the time for a flare to be shot off.
     *
     * @param signalFlareTimer The time until a flare is shot off. Examples: "15s", "1m, 10s", "15m"
     */
    public Tier setSignalFlareTimer(String signalFlareTimer) {
        this.signalFlareTimer = signalFlareTimer;

        return this;
    }
    
    /**
     * Get a list of Colors that the flare displays.
     */
    public List<Color> getSignalFlareColors() {
        return List.copyOf(this.signalFlareColors);
    }
    
    /**
     * Set the colors the flare will display.
     *
     * @param signalFlareColors List of colors the firework will be.
     */
    public Tier setSignalFlareColors(List<Color> signalFlareColors) {
        this.signalFlareColors = List.copyOf(signalFlareColors);

        return this;
    }
    
    /**
     * Add a color to the signal flare firework effect.
     *
     * @param signalFlareColors The color added to the firework effect.
     */
    public synchronized Tier addSignalFlareColor(Color signalFlareColors) {
        final List<Color> colors = new ArrayList<>(this.signalFlareColors);
        colors.add(signalFlareColors);
        this.signalFlareColors = List.copyOf(colors);

        return this;
    }
    
    /**
     * Get the prizes that can be found in the tier.
     */
    public List<Prize> getPrizes() {
        return List.copyOf(this.prizes);
    }
    
    /**
     * Set the list of prizes the tier has.
     *
     * @param prizes List of prizes.
     */
    public Tier setPrizes(List<Prize> prizes) {
        this.prizes = List.copyOf(prizes);

        return this;
    }
    
    /**
     * Add a prize to the tier.
     *
     * @param prize A new prize that is added to the list of prizes.
     */
    public synchronized Tier addPrize(Prize prize) {
        final List<Prize> prizes = new ArrayList<>(this.prizes);
        prizes.add(prize);
        this.prizes = List.copyOf(prizes);

        return this;
    }
}
