package com.badbones69.crazyenvoys.config;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.api.enums.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;

public final class TierTemplateManager {

    static final List<String> TEMPLATES = List.of("Basic.yml", "Lucky.yml", "Titan.yml", "Cursed.yml", "Doom.yml");
    static final int TEMPLATE_VERSION = 2;

    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private TierTemplateManager() {}

    public static void installLocalizedDefaults(final CrazyEnvoys plugin) {
        final Path dataDirectory = plugin.getDataPath();
        final Path tierDirectory = dataDirectory.resolve("tiers");

        try {
            installLocalizedDefaults(dataDirectory, tierDirectory, ConfigManager.getLocale(), plugin::getResource);
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, Messages.log_tier_templates_failed.getString(), exception);
        }
    }

    static void installLocalizedDefaults(
            final Path tierDirectory,
            final String locale,
            final Function<String, InputStream> resources
    ) throws IOException {
        final Path parent = tierDirectory.getParent();
        installLocalizedDefaults(parent == null ? tierDirectory : parent, tierDirectory, locale, resources);
    }

    static void installLocalizedDefaults(
            final Path dataDirectory,
            final Path tierDirectory,
            final String locale,
            final Function<String, InputStream> resources
    ) throws IOException {
        if (!"ru-RU".equals(locale)) return;

        Files.createDirectories(tierDirectory);
        final Map<String, byte[]> templates = loadTemplates(resources);

        final boolean anyYaml;
        try (var files = Files.list(tierDirectory)) {
            anyYaml = files.anyMatch(path -> path.getFileName().toString()
                    .toLowerCase(java.util.Locale.ROOT).endsWith(".yml"));
        }

        if (!anyYaml) {
            writeTemplates(tierDirectory, templates);
            return;
        }

        final List<Path> managedFiles = TEMPLATES.stream()
                .map(tierDirectory::resolve)
                .filter(Files::isRegularFile)
                .toList();
        if (managedFiles.isEmpty()) return;

        final boolean upgradeRequired = managedFiles.stream().anyMatch(path ->
                YamlConfiguration.loadConfiguration(path.toFile()).getInt("Settings.Template-Version", 0) < TEMPLATE_VERSION
        );
        if (!upgradeRequired) return;

        final Path backupDirectory = nextBackupDirectory(dataDirectory.resolve("backups"));
        Files.createDirectories(backupDirectory);
        for (final Path source : managedFiles) {
            Files.copy(source, backupDirectory.resolve(source.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
        }

        writeTemplates(tierDirectory, templates);
    }

    private static Map<String, byte[]> loadTemplates(final Function<String, InputStream> resources) throws IOException {
        final Map<String, byte[]> templates = new LinkedHashMap<>();
        for (final String template : TEMPLATES) {
            try (InputStream resource = resources.apply("tier-templates/ru-RU/" + template)) {
                if (resource == null) throw new IOException("Missing tier template " + template);
                templates.put(template, resource.readAllBytes());
            }
        }
        return templates;
    }

    private static void writeTemplates(final Path tierDirectory, final Map<String, byte[]> templates) throws IOException {
        for (final Map.Entry<String, byte[]> template : templates.entrySet()) {
            Files.write(tierDirectory.resolve(template.getKey()), template.getValue());
        }
    }

    private static Path nextBackupDirectory(final Path backupRoot) {
        final String baseName = "tiers-" + BACKUP_TIME.format(LocalDateTime.now());
        Path candidate = backupRoot.resolve(baseName);
        for (int suffix = 2; Files.exists(candidate); suffix++) {
            candidate = backupRoot.resolve(baseName + "-" + suffix);
        }
        return candidate;
    }
}
