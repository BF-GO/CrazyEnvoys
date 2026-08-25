package com.badbones69.crazyenvoys.bootstrap;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class ArmorBreakerBootstrap implements PluginBootstrap {

    public static final String ENCHANTMENT_ID = "crazyenvoys:armor_breaker";
    private static final String NAME_PATH = "ui.enchantments.armor-breaker";
    private static final String INVALID_NAME_PATH = "logs.armor-breaker-name-invalid";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public void bootstrap(@NotNull final BootstrapContext context) {
        final Component description = loadDescription(context);

        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event ->
                event.registry().register(
                        EnchantmentKeys.create(Key.key(ENCHANTMENT_ID)),
                        builder -> configure(builder, description)
                )
        ));
    }

    private static void configure(
            @NotNull final EnchantmentRegistryEntry.Builder builder,
            @NotNull final Component description
    ) {
        final var supportedItems = RegistrySet.keySet(RegistryKey.ITEM, List.of(
                ItemTypeKeys.WOODEN_SWORD, ItemTypeKeys.STONE_SWORD, ItemTypeKeys.COPPER_SWORD,
                ItemTypeKeys.GOLDEN_SWORD, ItemTypeKeys.IRON_SWORD, ItemTypeKeys.DIAMOND_SWORD,
                ItemTypeKeys.NETHERITE_SWORD,
                ItemTypeKeys.WOODEN_AXE, ItemTypeKeys.STONE_AXE, ItemTypeKeys.COPPER_AXE,
                ItemTypeKeys.GOLDEN_AXE, ItemTypeKeys.IRON_AXE, ItemTypeKeys.DIAMOND_AXE,
                ItemTypeKeys.NETHERITE_AXE,
                ItemTypeKeys.WOODEN_PICKAXE, ItemTypeKeys.STONE_PICKAXE, ItemTypeKeys.COPPER_PICKAXE,
                ItemTypeKeys.GOLDEN_PICKAXE, ItemTypeKeys.IRON_PICKAXE, ItemTypeKeys.DIAMOND_PICKAXE,
                ItemTypeKeys.NETHERITE_PICKAXE,
                ItemTypeKeys.WOODEN_SHOVEL, ItemTypeKeys.STONE_SHOVEL, ItemTypeKeys.COPPER_SHOVEL,
                ItemTypeKeys.GOLDEN_SHOVEL, ItemTypeKeys.IRON_SHOVEL, ItemTypeKeys.DIAMOND_SHOVEL,
                ItemTypeKeys.NETHERITE_SHOVEL,
                ItemTypeKeys.WOODEN_HOE, ItemTypeKeys.STONE_HOE, ItemTypeKeys.COPPER_HOE,
                ItemTypeKeys.GOLDEN_HOE, ItemTypeKeys.IRON_HOE, ItemTypeKeys.DIAMOND_HOE,
                ItemTypeKeys.NETHERITE_HOE,
                ItemTypeKeys.SHEARS, ItemTypeKeys.MACE, ItemTypeKeys.TRIDENT,
                ItemTypeKeys.BOW, ItemTypeKeys.CROSSBOW
        ));

        builder.description(description)
                .supportedItems(supportedItems)
                .primaryItems(supportedItems)
                .anvilCost(8)
                .maxLevel(3)
                .weight(1)
                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 1))
                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(3, 1))
                .activeSlots(EquipmentSlotGroup.MAINHAND);
    }

    private static Component loadDescription(@NotNull final BootstrapContext context) {
        final String locale = resolveLocale(context.getDataDirectory());
        final String configured = readLocalized(
                context.getDataDirectory(), locale, NAME_PATH, "<gold>Armor Breaker</gold>"
        );

        try {
            return MINI_MESSAGE.deserialize(configured);
        } catch (RuntimeException exception) {
            context.getLogger().warn(readLocalized(
                    context.getDataDirectory(),
                    locale,
                    INVALID_NAME_PATH,
                    "Could not parse the Armor Breaker display name; using a safe fallback."
            ), exception);
            return Component.text("Armor Breaker");
        }
    }

    private static String resolveLocale(@NotNull final Path dataDirectory) {
        String locale = "ru-RU";
        final Path configPath = dataDirectory.resolve("config.yml");
        if (Files.isRegularFile(configPath)) {
            final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configPath.toFile());
            locale = configuration.getString("root.language", locale);
        }

        if (locale == null) return "en-US";
        locale = locale.trim();
        if (!locale.matches("[A-Za-z0-9][A-Za-z0-9_-]{1,31}")) return "en-US";

        final boolean external = Files.isRegularFile(dataDirectory.resolve("locale").resolve(locale + ".yml"));
        final boolean bundled = ArmorBreakerBootstrap.class.getClassLoader().getResource("locale/" + locale + ".yml") != null;
        return external || bundled ? locale : "en-US";
    }

    private static String readLocalized(
            @NotNull final Path dataDirectory,
            @NotNull final String locale,
            @NotNull final String key,
            @NotNull final String fallback
    ) {
        String value = readLocaleFile(dataDirectory.resolve("locale").resolve(locale + ".yml"), key);
        if (value == null) value = readBundledLocale(locale, key);
        if (value == null && !"en-US".equals(locale)) value = readBundledLocale("en-US", key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String readLocaleFile(@NotNull final Path path, @NotNull final String key) {
        if (!Files.isRegularFile(path)) return null;
        return YamlConfiguration.loadConfiguration(path.toFile()).getString(key);
    }

    private static String readBundledLocale(@NotNull final String locale, @NotNull final String key) {
        try (InputStream input = ArmorBreakerBootstrap.class.getClassLoader().getResourceAsStream(
                "locale/" + locale + ".yml"
        )) {
            if (input == null) return null;
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8)
            ).getString(key);
        } catch (IOException ignored) {
            return null;
        }
    }
}
