package com.badbones69.crazyenvoys.config;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.resource.YamlFileResourceOptions;
import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.config.migrate.ConfigMigration;
import com.badbones69.crazyenvoys.config.migrate.LocaleMigration;
import com.badbones69.crazyenvoys.config.types.ConfigKeys;
import com.badbones69.crazyenvoys.config.types.MessageKeys;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConfigManager {

    private static volatile SettingsManager messages;
    private static volatile SettingsManager config;
    private static volatile String locale = "en-US";

    private static File pluginDataFolder;
    private static ComponentLogger componentLogger;

    public static synchronized void load(final File dataFolder, final ComponentLogger logger) {
        pluginDataFolder = dataFolder;
        componentLogger = logger;

        YamlFileResourceOptions builder = YamlFileResourceOptions.builder().indentationSize(2).build();

        File configFile = new File(dataFolder, "config.yml");

        config = SettingsManagerBuilder
                .withYamlFile(configFile, builder)
                .migrationService(new ConfigMigration())
                .configurationData(ConfigurationDataBuilder.createConfiguration(ConfigKeys.class))
                .create();

        final String migratedConfig = copyPluginConfig(dataFolder, config);
        messages = loadMessages(config.getProperty(ConfigKeys.locale_file), builder);
        ConfigCommentManager.update(configFile, locale);
        config.reload();

        if (migratedConfig != null) {
            logger.warn(Messages.log_config_migrated.getMessage(Map.of("{file}", migratedConfig)));
        }

        File file = new File(dataFolder, "data.yml");
        if (file.exists()) file.renameTo(new File(dataFolder, "users.yml"));
    }

    public static synchronized void refresh() {
        config.reload();

        YamlFileResourceOptions builder = YamlFileResourceOptions.builder().indentationSize(2).build();
        messages = loadMessages(config.getProperty(ConfigKeys.locale_file), builder);
        ConfigCommentManager.update(new File(pluginDataFolder, "config.yml"), locale);
        config.reload();
    }

    public static SettingsManager getConfig() {
        return config;
    }

    public static SettingsManager getMessages() {
        return messages;
    }

    public static String getLocale() {
        return locale;
    }

    private static SettingsManager loadMessages(final String configuredLocale, final YamlFileResourceOptions builder) {
        final File localeDir = new File(pluginDataFolder, "locale");
        if (!localeDir.exists() && !localeDir.mkdirs()) {
            componentLogger.warn(bootstrapMessage(configuredLocale, "logs.locale-directory-failed",
                    "Could not create locale directory at {path}.", Map.of("{path}", localeDir.getAbsolutePath())));
        }

        final File oldFile = new File(pluginDataFolder, "Messages.yml");
        final LocaleManager.Selection selection = LocaleManager.resolve(
                configuredLocale,
                localeDir,
                selected -> hasBundledLocale(selected) || oldFile.isFile()
        );
        String selected = selection.locale();
        if (selection.invalid()) {
            componentLogger.warn(bootstrapMessage("en-US", "logs.locale-invalid",
                    "Invalid locale name {locale}. Falling back to en-US.", Map.of("{locale}", String.valueOf(configuredLocale))));
        } else if (selection.missing()) {
            componentLogger.warn(bootstrapMessage("en-US", "logs.locale-not-found",
                    "Locale {locale} was not found. Falling back to en-US.", Map.of("{locale}", String.valueOf(configuredLocale))));
        }

        File messagesFile = new File(localeDir, selected + ".yml");

        if (oldFile.exists() && !messagesFile.exists() && !oldFile.renameTo(messagesFile)) {
            componentLogger.warn(bootstrapMessage(selected, "logs.locale-migration-failed",
                    "Could not migrate {source} to {target}.", Map.of(
                            "{source}", oldFile.getAbsolutePath(),
                            "{target}", messagesFile.getAbsolutePath()
                    )));
        }

        if (hasBundledLocale(selected)) {
            mergeBundledLocale(selected, messagesFile);
        } else if (messagesFile.isFile()) {
            mergeBundledLocale("en-US", messagesFile);
        }

        locale = selected;

        return SettingsManagerBuilder
                .withYamlFile(messagesFile, builder)
                .migrationService(new LocaleMigration())
                .configurationData(ConfigurationDataBuilder.createConfiguration(MessageKeys.class))
                .create();
    }

    private static boolean hasBundledLocale(final String selected) {
        try (InputStream resource = CrazyEnvoys.get().getResource("locale/" + selected + ".yml")) {
            return resource != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void mergeBundledLocale(final String selected, final File targetFile) {
        final String resourcePath = "locale/" + selected + ".yml";

        try (InputStream resource = CrazyEnvoys.get().getResource(resourcePath)) {
            if (resource == null) return;

            LocaleManager.mergeMissing(resource, targetFile);
        } catch (IOException exception) {
            componentLogger.error(bootstrapMessage(selected, "logs.locale-prepare-failed",
                    "Could not prepare locale {locale}.", Map.of("{locale}", selected)), exception);
        }
    }

    private static String bootstrapMessage(final String localeId, final String path, final String fallback,
                                           final Map<String, String> placeholders) {
        String value = fallback;
        final String selected = localeId == null ? "en-US" : localeId.trim();
        final File file = pluginDataFolder == null ? null : new File(new File(pluginDataFolder, "locale"), selected + ".yml");

        if (file != null && file.isFile()) {
            value = YamlConfiguration.loadConfiguration(file).getString(path, fallback);
        } else {
            try (InputStream resource = CrazyEnvoys.get().getResource("locale/" + selected + ".yml")) {
                if (resource != null) {
                    final YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(resource, StandardCharsets.UTF_8)
                    );
                    value = bundled.getString(path, fallback);
                }
            } catch (IOException ignored) {}
        }

        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            value = value.replace(placeholder.getKey(), placeholder.getValue());
        }

        return value;
    }

    private static String copyPluginConfig(final File dataFolder, final SettingsManager config) {
        File input = new File(dataFolder, "plugin-config.yml");

        if (!input.exists()) return null;

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(input);

        String language = configuration.getString("language", "ru-RU");

        String prefix = configuration.getString("command_prefix", "");
        String consolePrefix = configuration.getString("console_prefix", "");

        config.setProperty(ConfigKeys.locale_file, language);
        config.setProperty(ConfigKeys.command_prefix, prefix);
        config.setProperty(ConfigKeys.console_prefix, consolePrefix);

        // Save to file.
        config.save();

        return input.delete() ? input.getName() : null;
    }
}
