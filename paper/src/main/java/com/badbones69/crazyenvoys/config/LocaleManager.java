package com.badbones69.crazyenvoys.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class LocaleManager {

    public static final Set<String> BUNDLED_LOCALES = Set.of("en-US", "fr-FR", "pl-PL", "ru-RU");
    private static final Pattern LOCALE_ID = Pattern.compile("[A-Za-z0-9_-]+");

    private LocaleManager() {}

    public static Selection resolve(final String configuredLocale, final File localeDirectory,
                                    final Predicate<String> bundledLocale) {
        final String requested = configuredLocale == null || configuredLocale.isBlank()
                ? "en-US"
                : configuredLocale.trim();

        if (!LOCALE_ID.matcher(requested).matches()) return new Selection("en-US", true, false);

        String canonical = requested;
        for (String bundled : BUNDLED_LOCALES) {
            if (bundled.equalsIgnoreCase(requested)) {
                canonical = bundled;
                break;
            }
        }

        final boolean exists = bundledLocale.test(canonical) || new File(localeDirectory, canonical + ".yml").isFile();
        return exists ? new Selection(canonical, false, false) : new Selection("en-US", false, true);
    }

    public static void mergeMissing(final InputStream bundledResource, final File targetFile) throws IOException {
        if (!targetFile.exists()) {
            Files.copy(bundledResource, targetFile.toPath());
            return;
        }

        final YamlConfiguration target = loadWithComments(targetFile);
        final YamlConfiguration bundled = loadWithComments(new InputStreamReader(bundledResource, StandardCharsets.UTF_8));
        boolean changed = migrateShape(target, bundled);
        changed |= migrateBundledDefaults(target, bundled);

        for (String path : bundled.getKeys(true)) {
            if (bundled.get(path) instanceof ConfigurationSection || target.contains(path)) continue;

            target.set(path, bundled.get(path));
            changed = true;
        }

        if (!Objects.equals(target.options().getHeader(), bundled.options().getHeader())) {
            target.options().setHeader(bundled.options().getHeader());
            changed = true;
        }

        for (String path : bundled.getKeys(true)) {
            if (!target.getComments(path).equals(bundled.getComments(path))) {
                target.setComments(path, bundled.getComments(path));
                changed = true;
            }
            if (!target.getInlineComments(path).equals(bundled.getInlineComments(path))) {
                target.setInlineComments(path, bundled.getInlineComments(path));
                changed = true;
            }
        }

        if (changed) target.save(targetFile);
    }

    private static YamlConfiguration loadWithComments(final File file) throws IOException {
        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().parseComments(true);
        try {
            yaml.load(file);
        } catch (InvalidConfigurationException exception) {
            throw new IOException("Invalid YAML in " + file, exception);
        }
        return yaml;
    }

    private static YamlConfiguration loadWithComments(final InputStreamReader reader) throws IOException {
        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().parseComments(true);
        try {
            yaml.load(reader);
        } catch (InvalidConfigurationException exception) {
            throw new IOException("Invalid bundled locale YAML", exception);
        }
        return yaml;
    }

    private static boolean migrateShape(final YamlConfiguration target, final YamlConfiguration bundled) {
        boolean changed = false;

        if (target.isString("envoys.started")) {
            final String value = target.getString("envoys.started", "");
            target.set("envoys.started", null);
            target.set("envoys.started.list", List.of(value));
            changed = true;
        }

        changed |= stringToList(target, "envoys.started-player");
        changed |= stringToList(target, "envoys.time-till-event");
        changed |= stringToList(target, "envoys.left");
        changed |= stringToList(target, "envoys.ended");

        final String commandNotFound = target.getString("misc.command-not-found", "");
        if (commandNotFound.contains("{usage}") && !target.contains("misc.correct-usage")) {
            target.set("misc.correct-usage", commandNotFound);
            target.set("misc.command-not-found", bundled.getString("misc.command-not-found"));
            changed = true;
        }

        return changed;
    }

    private static boolean migrateBundledDefaults(final YamlConfiguration target, final YamlConfiguration bundled) {
        boolean changed = false;

        changed |= replaceIfUnchanged(target, bundled, "player.no-permission-to-claim",
                "{prefix}<red>Руки убрал от тайника.</red> <gray>Даже в Судную ночь нужны правильные права.</gray>");
        changed |= replaceIfUnchanged(target, bundled, "envoys.already-started",
                "{prefix}<yellow>Судная ночь уже идёт.</yellow> <gray>Вторую луну на небо пока не завезли.</gray>");
        changed |= replaceIfUnchanged(target, bundled, "envoys.not-started",
                "{prefix}<gray>Сейчас тихо: законы работают, тайники спят.</gray>");
        changed |= replaceIfUnchanged(target, bundled, "envoys.warning",
                "{prefix}<gradient:#ff1744:#ff6d00><bold>ВНИМАНИЕ</bold></gradient> <gray>До отмены правил <white>{time}</white>. Прячьте алмазы и здравый смысл.</gray>");
        changed |= replaceIfUnchanged(target, bundled, "envoys.started.list", List.of(
                "{prefix}<gradient:#ff1744:#7c4dff><bold>СУДНАЯ НОЧЬ НАЧАЛАСЬ</bold></gradient> <gray>На карте появилось тайников: <white>{amount}</white>.</gray>"
        ));
        changed |= replaceIfUnchanged(target, bundled, "envoys.ended", List.of(
                "{prefix}<gradient:#80cbc4:#64b5f6><bold>РАССВЕТ</bold></gradient> <gray>Законы снова включены. Кто выжил — тот молодец.</gray>"
        ));
        changed |= replaceIfUnchanged(target, bundled, "envoys.kicked-from-editor-mode",
                "{prefix}<yellow>Редактор закрыт: Судная ночь уже началась.</yellow> <gray>Ремонт во время пожара запрещён.</gray>");
        changed |= replaceIfUnchanged(target, bundled, "envoys.time-left",
                "{prefix}<gray>До рассвета <white>{time}</white>. Успей сделать вид, что у тебя был план.</gray>");
        changed |= replaceIfUnchanged(target, bundled, "envoys.time-till-event", List.of(
                "{prefix}<gray>До следующей Судной ночи <white>{time}</white>. Пока можно жить прилично.</gray>"
        ));
        changed |= replaceIfUnchanged(target, bundled, "envoys.hologram-placeholders.on-going", "Судная ночь идёт");
        changed |= replaceIfUnchanged(target, bundled, "envoys.hologram-placeholders.not-running", "До сирены тихо");
        changed |= replaceIfUnchanged(target, bundled, "misc.not-a-number",
                "{prefix}<red>Это не число.</red> <gray>Даже в ночь без правил математика ещё работает.</gray>");
        changed |= replaceIfUnchanged(target, bundled, "ui.command-prefix",
                "<gradient:#ff1744:#ff6d00><bold>СУДНАЯ НОЧЬ</bold></gradient> <dark_gray>»</dark_gray> ");
        changed |= replaceIfUnchanged(target, bundled, "ui.flare.lore", List.of(
                "<gray>ПКМ — и законы уходят</gray>",
                "<gray>в неоплачиваемый отпуск.</gray>",
                "<dark_gray>Нажимать с драматичным лицом.</dark_gray>"
        ));
        changed |= replaceIfUnchanged(target, bundled, "logs.start-begin-failed", "Не удалось запустить Судную ночь.");
        changed |= replaceIfUnchanged(target, bundled, "envoys.envoy-locations",
                "<gradient:#ff1744:#ff6d00><bold>ВСЕ ТАЙНИКИ</bold></gradient>\\n<dark_gray>[ID] [Мир]: [X], [Y], [Z]</dark_gray> {locations}");
        changed |= replaceIfUnchanged(target, bundled, "envoys.location-format",
                "\\n<dark_gray>[</dark_gray><#ff6d00>{id}</#ff6d00><dark_gray>]</dark_gray> <gray>{world}</gray><dark_gray>:</dark_gray> <white>{x}, {y}, {z}</white>");

        return changed;
    }

    private static boolean replaceIfUnchanged(final YamlConfiguration target, final YamlConfiguration bundled,
                                              final String path, final Object previousDefault) {
        if (!Objects.equals(target.get(path), previousDefault) || !bundled.contains(path)) return false;

        target.set(path, bundled.get(path));
        return true;
    }

    private static boolean stringToList(final YamlConfiguration target, final String path) {
        if (!target.isString(path)) return false;

        final String value = target.getString(path, "");
        target.set(path, null);
        target.set(path + (path.equals("envoys.started-player") ? ".list" : ""), List.of(value));
        return true;
    }

    public record Selection(String locale, boolean invalid, boolean missing) {}
}
