package com.badbones69.crazyenvoys.config;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.api.enums.Messages;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ConfigCommentManager {

    private ConfigCommentManager() {}

    public static void update(final File configFile, final String locale) {
        InputStream resource = CrazyEnvoys.get().getResource("config-comments/" + locale + ".yml");
        if (resource == null) {
            resource = CrazyEnvoys.get().getResource("config-comments/en-US.yml");
        }
        if (resource == null || !configFile.isFile()) return;

        try (InputStream selectedResource = resource) {
            apply(configFile, selectedResource);
        } catch (IOException | InvalidConfigurationException exception) {
            CrazyEnvoys.get().getLogger().log(
                    java.util.logging.Level.WARNING,
                    Messages.log_config_comments_failed.getString(),
                    exception
            );
        }
    }

    static void apply(final File configFile, final InputStream resource)
            throws IOException, InvalidConfigurationException {
        final YamlConfiguration translations = new YamlConfiguration();
        translations.load(new InputStreamReader(resource, StandardCharsets.UTF_8));

        final YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        config.load(configFile);
        config.options().setHeader(translations.getStringList("header"));

        final ConfigurationSection comments = translations.getConfigurationSection("comments");
        if (comments != null) {
            for (String path : comments.getKeys(true)) {
                final Object value = comments.get(path);
                if (value instanceof ConfigurationSection) continue;

                final List<String> lines = value instanceof List<?>
                        ? comments.getStringList(path)
                        : List.of(String.valueOf(value));
                config.setComments(path, lines);
            }
        }

        config.save(configFile);
    }
}
