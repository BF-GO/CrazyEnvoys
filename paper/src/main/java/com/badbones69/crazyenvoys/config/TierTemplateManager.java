package com.badbones69.crazyenvoys.config;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.api.enums.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TierTemplateManager {

    private static final List<String> TEMPLATES = List.of("Basic.yml", "Lucky.yml", "Titan.yml");

    private TierTemplateManager() {}

    public static void installLocalizedDefaults(final CrazyEnvoys plugin) {
        if (!ConfigManager.getLocale().equals("ru-RU")) return;

        final Path tierDirectory = plugin.getDataPath().resolve("tiers");

        try {
            Files.createDirectories(tierDirectory);
            try (var files = Files.list(tierDirectory)) {
                if (files.anyMatch(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".yml"))) return;
            }

            for (String template : TEMPLATES) {
                try (InputStream resource = plugin.getResource("tier-templates/ru-RU/" + template)) {
                    if (resource != null) Files.copy(resource, tierDirectory.resolve(template));
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_tier_templates_failed.getString(), exception);
        }
    }
}
