package com.badbones69.crazyenvoys.config;

import com.badbones69.crazyenvoys.config.types.ConfigKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierTemplateTest {

    @TempDir
    Path temporaryDirectory;

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().strict(true).build();
    private static final Map<String, Integer> LIMITS = Map.of(
            "Basic.yml", 0,
            "Lucky.yml", 5,
            "Titan.yml", 3,
            "Cursed.yml", 2,
            "Doom.yml", 1
    );
    private static final Map<String, Integer> PRIZE_COUNTS = Map.of(
            "Basic.yml", 11,
            "Lucky.yml", 12,
            "Titan.yml", 13,
            "Cursed.yml", 15,
            "Doom.yml", 14
    );
    private static final Map<String, Integer> ARMOR_WEIGHTS = Map.of(
            "Basic.yml", 3,
            "Lucky.yml", 3,
            "Titan.yml", 4,
            "Cursed.yml", 4,
            "Doom.yml", 5
    );
    private static final Map<String, Double> ARMOR_BREAKER_CHANCES = Map.of(
            "Basic.yml", 0.0D,
            "Lucky.yml", 0.0D,
            "Titan.yml", 2.0D,
            "Cursed.yml", 1.0D,
            "Doom.yml", 0.5D
    );
    private static final Map<String, Integer> ARMOR_BREAKER_LEVELS = Map.of(
            "Basic.yml", 0,
            "Lucky.yml", 0,
            "Titan.yml", 1,
            "Cursed.yml", 2,
            "Doom.yml", 3
    );
    private static final Map<String, List<String>> SET_EFFECTS = Map.of(
            "Basic.yml", List.of("speed:1", "night_vision:1"),
            "Lucky.yml", List.of("speed:1", "haste:2", "luck:2"),
            "Titan.yml", List.of("strength:1", "resistance:1", "haste:2"),
            "Cursed.yml", List.of("strength:2", "speed:2", "fire_resistance:1"),
            "Doom.yml", List.of("strength:3", "speed:2", "resistance:2", "fire_resistance:1", "regeneration:1")
    );
    private static final Map<String, Integer> RARE_WEIGHTS = Map.of(
            "Кирка ЖКХ «Мы уже выехали»", 2,
            "Лопата участкового", 2,
            "Банхаммер без права апелляции", 1,
            "Ножницы бюджетного секвестра", 1
    );

    @Test
    void russianIsTheCleanInstallDefaultAndAllTemplatesAreBundled() {
        assertEquals("ru-RU", ConfigKeys.locale_file.getDefaultValue());
        assertEquals(List.of("Basic.yml", "Lucky.yml", "Titan.yml", "Cursed.yml", "Doom.yml"), TierTemplateManager.TEMPLATES);

        for (String template : TierTemplateManager.TEMPLATES) {
            assertNotNull(TierTemplateTest.class.getClassLoader().getResource(
                    "tier-templates/ru-RU/" + template
            ));
        }
    }

    @Test
    void cleanRussianInstallExtractsFiveTiersWithoutOverwritingExistingFiles() throws Exception {
        final Path tiers = this.temporaryDirectory.resolve("tiers");
        TierTemplateManager.installLocalizedDefaults(tiers, "ru-RU", path ->
                TierTemplateTest.class.getClassLoader().getResourceAsStream(path)
        );

        for (String template : TierTemplateManager.TEMPLATES) {
            assertTrue(Files.isRegularFile(tiers.resolve(template)), template);
            assertEquals(TierTemplateManager.TEMPLATE_VERSION,
                    YamlConfiguration.loadConfiguration(tiers.resolve(template).toFile())
                            .getInt("Settings.Template-Version"));
        }

        final Path basic = tiers.resolve("Basic.yml");
        final YamlConfiguration customized = YamlConfiguration.loadConfiguration(basic.toFile());
        customized.set("custom", "keep");
        customized.save(basic.toFile());
        TierTemplateManager.installLocalizedDefaults(tiers, "ru-RU", path ->
                TierTemplateTest.class.getClassLoader().getResourceAsStream(path)
        );

        assertEquals("keep", YamlConfiguration.loadConfiguration(basic.toFile()).getString("custom"));
        assertFalse(Files.exists(this.temporaryDirectory.resolve("backups")));
    }

    @Test
    void oldManagedTiersAreBackedUpAndUpgradedOnlyOnce() throws Exception {
        final Path tiers = this.temporaryDirectory.resolve("tiers");
        Files.createDirectories(tiers);
        for (final String template : TierTemplateManager.TEMPLATES) {
            Files.writeString(tiers.resolve(template), "Settings:\n  Template-Version: 1\ncustom: " + template + "\n");
        }
        Files.writeString(tiers.resolve("Custom.yml"), "custom: untouched\n");

        TierTemplateManager.installLocalizedDefaults(tiers, "ru-RU", path ->
                TierTemplateTest.class.getClassLoader().getResourceAsStream(path)
        );

        final Path backups = this.temporaryDirectory.resolve("backups");
        final List<Path> backupDirectories;
        try (var paths = Files.list(backups)) {
            backupDirectories = paths.toList();
        }
        assertEquals(1, backupDirectories.size());
        for (final String template : TierTemplateManager.TEMPLATES) {
            assertTrue(Files.isRegularFile(backupDirectories.getFirst().resolve(template)));
            assertEquals(template, YamlConfiguration.loadConfiguration(
                    backupDirectories.getFirst().resolve(template).toFile()
            ).getString("custom"));
            assertEquals(TierTemplateManager.TEMPLATE_VERSION,
                    YamlConfiguration.loadConfiguration(tiers.resolve(template).toFile())
                            .getInt("Settings.Template-Version"));
        }
        assertEquals("untouched", YamlConfiguration.loadConfiguration(
                tiers.resolve("Custom.yml").toFile()
        ).getString("custom"));

        final Path basic = tiers.resolve("Basic.yml");
        final YamlConfiguration customized = YamlConfiguration.loadConfiguration(basic.toFile());
        customized.set("custom", "preserved after update");
        customized.save(basic.toFile());
        TierTemplateManager.installLocalizedDefaults(tiers, "ru-RU", path ->
                TierTemplateTest.class.getClassLoader().getResourceAsStream(path)
        );
        assertEquals("preserved after update", YamlConfiguration.loadConfiguration(basic.toFile()).getString("custom"));
        try (var paths = Files.list(backups)) {
            assertEquals(1, paths.count());
        }
    }

    @Test
    void russianTemplatesHaveValidWeightsItemsAndMiniMessage() throws Exception {
        int totalSpawnWeight = 0;
        int totalPrizes = 0;
        final StringBuilder allItems = new StringBuilder();
        final Set<String> foundRarePrizes = new java.util.HashSet<>();

        for (String template : TierTemplateManager.TEMPLATES) {
            final YamlConfiguration configuration = load("tier-templates/ru-RU/" + template);
            final ConfigurationSection settings = configuration.getConfigurationSection("Settings");
            final ConfigurationSection prizes = configuration.getConfigurationSection("Prizes");
            assertNotNull(settings, template);
            assertNotNull(prizes, template);

            assertEquals(LIMITS.get(template), settings.getInt("Max-Per-Event"), template);
            assertEquals(TierTemplateManager.TEMPLATE_VERSION, settings.getInt("Template-Version"), template);
            assertFalse(settings.getBoolean("Bulk-Prizes.Toggle"), template);
            totalSpawnWeight += settings.getInt("Spawn-Chance");

            final ConfigurationSection armorBreaker = settings.getConfigurationSection(
                    "Rare-Enchantments.Armor-Breaker"
            );
            assertNotNull(armorBreaker, template + " armor breaker settings");
            assertEquals(ARMOR_BREAKER_CHANCES.get(template), armorBreaker.getDouble("Chance"), template);
            assertEquals(ARMOR_BREAKER_LEVELS.get(template), armorBreaker.getInt("Level"), template);
            assertEquals(ARMOR_BREAKER_LEVELS.get(template) > 0, armorBreaker.getBoolean("Toggle"), template);

            final ConfigurationSection armorSet = settings.getConfigurationSection("Armor-Set");
            assertNotNull(armorSet, template + " armor set");
            assertTrue(armorSet.getString("Id", "").matches("[a-z0-9_-]+"), template + " armor set id");
            assertEquals(SET_EFFECTS.get(template), armorSet.getStringList("Effects"), template + " armor effects");
            parse(armorSet.getString("Display-Name", ""), template + " armor set display name");

            parse(settings.getString("Display-Name", ""), template + " display name");
            settings.getStringList("Prize-Message").forEach(value -> parse(value, template + " prize message"));
            settings.getStringList("Hologram").forEach(value -> parse(value, template + " hologram"));

            int totalPrizeWeight = 0;
            final java.util.Set<String> armorPieces = new java.util.HashSet<>();
            assertEquals(PRIZE_COUNTS.get(template), prizes.getKeys(false).size(), template + " prize count");
            totalPrizes += prizes.getKeys(false).size();

            for (String id : prizes.getKeys(false)) {
                final ConfigurationSection prize = prizes.getConfigurationSection(id);
                assertNotNull(prize, template + " prize " + id);
                assertTrue(prize.getStringList("Commands").isEmpty(), template + " prize " + id + " contains commands");
                assertFalse(prize.getStringList("Items").isEmpty(), template + " prize " + id + " has no items");

                totalPrizeWeight += prize.getInt("Chance");
                parse(prize.getString("DisplayName", ""), template + " prize " + id + " display name");
                prize.getStringList("Messages").forEach(value -> parse(value, template + " prize " + id + " message"));

                final String prizeName = prize.getString("DisplayName", "");
                if (RARE_WEIGHTS.containsKey(prizeName)) {
                    assertEquals(RARE_WEIGHTS.get(prizeName), prize.getInt("Chance"), prizeName + " weight");
                    foundRarePrizes.add(prizeName);
                }

                final String armorPiece = prize.getString("Armor-Set-Piece", "");
                if (!armorPiece.isBlank()) {
                    assertTrue(Set.of("head", "chest", "legs", "feet").contains(armorPiece), template + " prize " + id + " armor slot");
                    assertTrue(armorPieces.add(armorPiece), template + " duplicate armor slot " + armorPiece);
                    assertEquals(ARMOR_WEIGHTS.get(template), prize.getInt("Chance"), template + " armor weight");
                    assertEquals(1, prize.getStringList("Items").size(), template + " armor prize item count");
                }

                int armorItems = 0;
                for (String item : prize.getStringList("Items")) {
                    allItems.append(item.toLowerCase(Locale.ROOT)).append('\n');
                    final String lowerItem = item.toLowerCase(Locale.ROOT);
                    if (lowerItem.contains("_helmet") || lowerItem.contains("_chestplate")
                            || lowerItem.contains("_leggings") || lowerItem.contains("_boots")) {
                        armorItems++;
                    }
                    for (String option : item.split(", ")) {
                        if (option.startsWith("name:") || option.startsWith("lore:")) {
                            parse(option.substring(option.indexOf(':') + 1), template + " prize " + id + " item text");
                        }
                    }
                }
                assertTrue(armorItems <= 1, template + " prize " + id + " contains a complete armor bundle");
            }

            assertEquals(100, totalPrizeWeight, template + " prize weights");
            assertEquals(Set.of("head", "chest", "legs", "feet"), armorPieces, template + " armor pieces");
        }

        assertEquals(100, totalSpawnWeight);
        assertEquals(65, totalPrizes);
        assertEquals(RARE_WEIGHTS.keySet(), foundRarePrizes);
        final String items = allItems.toString();
        assertTrue(items.contains("sharpness:20"));
        assertTrue(items.contains("efficiency:20"));
        assertTrue(items.contains("protection:10"));
        assertTrue(items.contains("quick_charge:5"));
        assertFalse(items.contains("quick_charge:8"));
        assertFalse(items.contains("quick_charge:10"));
        assertFalse(items.contains("quick_charge:15"));
        assertTrue(items.contains("multishot:1"));
        assertTrue(items.contains("piercing:15"));
        assertTrue(items.contains("name:<gradient:#e040fb:#d50000><bold>кирка жкх «мы уже выехали»</bold></gradient>"));
        assertTrue(items.contains("efficiency:18, fortune:8, sharpness:10"));
        assertTrue(items.contains("name:<gradient:#e040fb:#d50000><bold>лопата участкового</bold></gradient>"));
        assertTrue(items.contains("density:25, breach:20, wind_burst:15"));
        assertTrue(items.contains("item:shears"));
        assertTrue(items.contains("sharpness:25, efficiency:20, looting:10, fire_aspect:10"));
        assertFalse(items.contains("unbreakable-item"));
    }

    @Test
    void noBundledTierUsesEconomyCommands() throws Exception {
        for (String template : List.of("Basic.yml", "Lucky.yml", "Titan.yml")) {
            final YamlConfiguration configuration = load("tiers/" + template);
            final String serialized = configuration.saveToString().toLowerCase(Locale.ROOT);

            assertFalse(serialized.contains("eco give"), template);
            assertFalse(serialized.contains("$1,000"), template);
        }
    }

    private static void parse(final String value, final String context) {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> MINI_MESSAGE.deserialize(value), context
        );
    }

    private static YamlConfiguration load(final String resource) throws Exception {
        try (InputStream input = TierTemplateTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            final YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            return configuration;
        }
    }
}
