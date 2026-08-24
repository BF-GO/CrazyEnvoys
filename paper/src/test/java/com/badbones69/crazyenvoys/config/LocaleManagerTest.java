package com.badbones69.crazyenvoys.config;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class LocaleManagerTest {

    private static final List<String> LOCALES = List.of("en-US", "fr-FR", "pl-PL", "ru-RU");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[A-Za-z0-9_]+}");

    @TempDir
    Path temporaryDirectory;

    @Test
    void bundledLocalesHaveMatchingKeysTypesAndPlaceholders() throws Exception {
        final Map<String, Object> english = flatten(load("en-US"));

        for (String locale : LOCALES) {
            final Map<String, Object> translated = flatten(load(locale));
            assertEquals(english.keySet(), translated.keySet(), locale + " keys differ from en-US");

            for (String key : english.keySet()) {
                assertEquals(valueType(english.get(key)), valueType(translated.get(key)), locale + " type differs at " + key);
                assertEquals(placeholders(english.get(key)), placeholders(translated.get(key)), locale + " placeholders differ at " + key);
            }
        }
    }

    @Test
    void everyDisplayStringParsesAsStrictMiniMessage() throws Exception {
        final MiniMessage miniMessage = MiniMessage.builder().strict(true).build();

        for (String locale : LOCALES) {
            for (Map.Entry<String, Object> entry : flatten(load(locale)).entrySet()) {
                for (String value : strings(entry.getValue())) {
                    assertDoesNotThrow(() -> miniMessage.deserialize(value), locale + " invalid MiniMessage at " + entry.getKey());
                }
            }
        }

        assertDoesNotThrow(() -> miniMessage.deserialize(
                "<#55AAFF>hex</#55AAFF> <gradient:#FF5555:#55AAFF>gradient</gradient> <rainbow>rainbow</rainbow>"
        ));
    }

    @Test
    void resolvesBundledCustomAndFallbackLocales() throws Exception {
        assertEquals("ru-RU", LocaleManager.resolve("RU-ru", temporaryDirectory.toFile(), LocaleManager.BUNDLED_LOCALES::contains).locale());

        Files.writeString(temporaryDirectory.resolve("custom.yml"), "misc:\n  not-applicable: custom\n");
        assertEquals("custom", LocaleManager.resolve("custom", temporaryDirectory.toFile(), LocaleManager.BUNDLED_LOCALES::contains).locale());

        final LocaleManager.Selection missing = LocaleManager.resolve("missing", temporaryDirectory.toFile(), LocaleManager.BUNDLED_LOCALES::contains);
        assertEquals("en-US", missing.locale());
        assertTrue(missing.missing());

        final LocaleManager.Selection invalid = LocaleManager.resolve("../ru-RU", temporaryDirectory.toFile(), LocaleManager.BUNDLED_LOCALES::contains);
        assertEquals("en-US", invalid.locale());
        assertTrue(invalid.invalid());
    }

    @Test
    void extractionAndUpdatesNeverOverwriteCustomValues() throws Exception {
        final File target = temporaryDirectory.resolve("ru-RU.yml").toFile();
        try (InputStream resource = resource("ru-RU")) {
            LocaleManager.mergeMissing(resource, target);
        }
        assertTrue(target.isFile());

        final YamlConfiguration customized = YamlConfiguration.loadConfiguration(target);
        customized.set("player.no-permission", "custom value");
        customized.set("ui.envoy-menu-title", null);
        customized.set("custom.keep", "untouched");
        customized.save(target);

        try (InputStream resource = resource("ru-RU")) {
            LocaleManager.mergeMissing(resource, target);
        }

        final YamlConfiguration updated = YamlConfiguration.loadConfiguration(target);
        assertEquals("custom value", updated.getString("player.no-permission"));
        assertEquals("untouched", updated.getString("custom.keep"));
        assertNotNull(updated.getString("ui.envoy-menu-title"));
    }

    @Test
    void configCommentBundlesMatchAndApplyingThemPreservesValues() throws Exception {
        Map<String, Object> english = flatten(loadResource("config-comments/en-US.yml"));
        for (String locale : LOCALES) {
            assertEquals(english.keySet(), flatten(loadResource("config-comments/" + locale + ".yml")).keySet());
        }

        final File config = temporaryDirectory.resolve("config.yml").toFile();
        Files.writeString(config.toPath(), "root:\n  language: ru-RU\nenvoys:\n  generation:\n    max-drops-amount: 37\ncustom: keep\n");
        try (InputStream comments = LocaleManagerTest.class.getClassLoader().getResourceAsStream("config-comments/ru-RU.yml")) {
            assertNotNull(comments);
            ConfigCommentManager.apply(config, comments);
        }

        final YamlConfiguration updated = new YamlConfiguration();
        updated.options().parseComments(true);
        updated.load(config);
        assertEquals("ru-RU", updated.getString("root.language"));
        assertEquals(37, updated.getInt("envoys.generation.max-drops-amount"));
        assertEquals("keep", updated.getString("custom"));
        assertTrue(updated.getComments("root.language").stream().anyMatch(line -> line.contains("Язык сервера")));
    }

    private static YamlConfiguration load(final String locale) throws Exception {
        return loadResource("locale/" + locale + ".yml");
    }

    private static YamlConfiguration loadResource(final String path) throws Exception {
        try (InputStream input = LocaleManagerTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Missing resource " + path);
            return YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static InputStream resource(final String locale) {
        final InputStream input = LocaleManagerTest.class.getClassLoader().getResourceAsStream("locale/" + locale + ".yml");
        assertNotNull(input, "Missing bundled locale " + locale);
        return input;
    }

    private static Map<String, Object> flatten(final YamlConfiguration yaml) {
        final Map<String, Object> values = new LinkedHashMap<>();
        for (String key : yaml.getKeys(true)) {
            final Object value = yaml.get(key);
            if (!(value instanceof ConfigurationSection)) values.put(key, value);
        }
        return values;
    }

    private static Class<?> valueType(final Object value) {
        return value instanceof List<?> ? List.class : String.class;
    }

    private static Set<String> placeholders(final Object value) {
        final Set<String> result = new LinkedHashSet<>();
        for (String line : strings(value)) {
            final Matcher matcher = PLACEHOLDER.matcher(line);
            while (matcher.find()) result.add(matcher.group());
        }
        return result;
    }

    private static List<String> strings(final Object value) {
        if (value instanceof List<?> list) {
            final List<String> result = new ArrayList<>(list.size());
            for (Object item : list) result.add(String.valueOf(item));
            return result;
        }
        return List.of(String.valueOf(value));
    }
}
