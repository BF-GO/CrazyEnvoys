package com.badbones69.crazyenvoys.api.enums;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.properties.Property;
import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.api.CrazyManager;
import com.ryderbelserion.fusion.core.utils.StringUtils;
import com.ryderbelserion.fusion.kyori.utils.AdvUtils;
import com.ryderbelserion.fusion.paper.FusionPaper;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.badbones69.crazyenvoys.config.ConfigManager;
import com.badbones69.crazyenvoys.config.types.ConfigKeys;
import com.badbones69.crazyenvoys.config.types.MessageKeys;
import org.jspecify.annotations.NonNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Messages {

    ended(MessageKeys.envoy_ended, true),
    wave_ended(MessageKeys.envoy_wave_ended),
    wave_ended_manual(MessageKeys.envoy_wave_ended_manual),
    wave_guide(MessageKeys.envoy_wave_guide, true),
    warning(MessageKeys.envoy_warning),
    started(MessageKeys.envoy_started, true),
    started_player(MessageKeys.envoy_started_player, true),
    on_going(MessageKeys.hologram_on_going),
    not_running(MessageKeys.hologram_not_running),
    reloaded(MessageKeys.envoy_plugin_reloaded),
    time_left(MessageKeys.envoy_time_left),
    used_flare(MessageKeys.envoy_used_flare),
    give_flare(MessageKeys.envoy_give_flare),
    new_center(MessageKeys.envoy_new_center),
    not_online(MessageKeys.not_online),
    given_flare(MessageKeys.envoy_received_flare),
    force_start(MessageKeys.envoy_force_start),
    not_started(MessageKeys.envoy_not_started),
    envoys_remaining(MessageKeys.envoys_remaining, true),
    force_end(MessageKeys.envoy_force_ended),
    drops_page(MessageKeys.drops_page),
    drops_format(MessageKeys.drops_format),
    drops_available(MessageKeys.drops_available),
    drops_possibilities(MessageKeys.drops_possibilities),
    drops_hint(MessageKeys.drops_hint),
    spawn_area(MessageKeys.spawn_area),
    player_only(MessageKeys.player_only),
    must_be_console_sender(MessageKeys.must_be_console_sender),
    not_a_number(MessageKeys.not_a_number),
    add_location(MessageKeys.envoy_add_location),
    remove_location(MessageKeys.envoy_remove_location),
    cooldown_left(MessageKeys.cooldown_left),
    countdown_in_progress(MessageKeys.countdown_in_progress),
    no_permission(MessageKeys.no_permission),
    no_claim_permission(MessageKeys.no_claim_permission),
    time_till_event(MessageKeys.envoy_time_till_event, true),
    cant_use_flares(MessageKeys.envoy_cant_use_flare),
    already_started(MessageKeys.envoy_already_started),
    enter_editor_mode(MessageKeys.enter_editor_mode),
    leave_editor_mode(MessageKeys.exit_editor_mode),
    editor_clear_locations(MessageKeys.envoy_clear_locations),
    editor_clear_failure(MessageKeys.envoy_clear_failure),
    not_enough_players(MessageKeys.not_enough_players),
    stop_ignoring_messages(MessageKeys.stop_ignoring_messages),
    start_ignoring_messages(MessageKeys.start_ignoring_messages),
    kicked_from_editor_mode(MessageKeys.envoy_kicked_from_editor_mode),
    not_in_world_guard_region(MessageKeys.not_in_world_guard_region),
    no_spawn_locations_found(MessageKeys.no_spawn_locations_found),
    command_not_found(MessageKeys.unknown_command),
    correct_usage(MessageKeys.correct_usage),
    day(MessageKeys.time_placeholder_day),
    hour(MessageKeys.time_placeholder_hour),
    minute(MessageKeys.time_placeholder_minute),
    second(MessageKeys.time_placeholder_second),
    envoy_locations(MessageKeys.envoy_locations),
    location_format(MessageKeys.location_format),
    world_overworld(MessageKeys.world_overworld),
    world_nether(MessageKeys.world_nether),
    world_end(MessageKeys.world_end),

    error_migrating(MessageKeys.error_migrating),
    migration_not_available(MessageKeys.migration_not_available),
    migration_plugin_not_enabled(MessageKeys.migration_plugin_not_enabled),
    migration_no_crates_available(MessageKeys.migration_no_crates_available),
    successfully_migrated(MessageKeys.successfully_migrated, true),
    successfully_migrated_users(MessageKeys.successfully_migrated_users, true),

    lacking_flag(MessageKeys.lacking_flag),

    empty_tier_warning(MessageKeys.empty_tier_warning),
    not_applicable(MessageKeys.not_applicable),
    command_prefix(MessageKeys.command_prefix),
    envoy_menu_title(MessageKeys.envoy_menu_title),
    prize_load_error_item(MessageKeys.prize_load_error_item),
    flare_item_name(MessageKeys.flare_item_name),
    flare_item_lore(MessageKeys.flare_item_lore, true),
    grace_period_unlocked(MessageKeys.grace_period_unlocked),
    grace_period_time_unit(MessageKeys.grace_period_time_unit),
    armor_set_activated(MessageKeys.armor_set_activated),
    armor_set_deactivated(MessageKeys.armor_set_deactivated),
    armor_breaker_name(MessageKeys.armor_breaker_name),
    armor_breaker_awarded(MessageKeys.armor_breaker_awarded),

    log_done(MessageKeys.log_done),
    log_config_migrated(MessageKeys.log_config_migrated),
    log_prize_error(MessageKeys.log_prize_error),
    log_locations_retry(MessageKeys.log_locations_retry),
    log_recovery_error(MessageKeys.log_recovery_error),
    log_hologram_enabled(MessageKeys.log_hologram_enabled),
    log_center_world_missing(MessageKeys.log_center_world_missing),
    log_visuals_failed(MessageKeys.log_visuals_failed),
    log_hologram_cleanup_failed(MessageKeys.log_hologram_cleanup_failed),
    log_task_cancel_failed(MessageKeys.log_task_cancel_failed),
    log_generation_partial(MessageKeys.log_generation_partial),
    log_generation_area_limited(MessageKeys.log_generation_area_limited),
    log_tier_capacity_limited(MessageKeys.log_tier_capacity_limited),
    log_armor_set_invalid(MessageKeys.log_armor_set_invalid),
    log_rare_enchantment_invalid(MessageKeys.log_rare_enchantment_invalid),
    log_armor_breaker_unavailable(MessageKeys.log_armor_breaker_unavailable),
    log_armor_breaker_name_invalid(MessageKeys.log_armor_breaker_name_invalid),
    log_chunk_load_failed(MessageKeys.log_chunk_load_failed),
    log_chunk_request_failed(MessageKeys.log_chunk_request_failed),
    log_no_tiers(MessageKeys.log_no_tiers),
    log_no_valid_locations(MessageKeys.log_no_valid_locations),
    log_signal_cancel_failed(MessageKeys.log_signal_cancel_failed),
    log_location_deserialize_failed(MessageKeys.log_location_deserialize_failed),
    log_center_repair_attempt(MessageKeys.log_center_repair_attempt),
    log_center_repair_failed(MessageKeys.log_center_repair_failed),
    log_center_repair_success(MessageKeys.log_center_repair_success),
    log_locations_repair_attempt(MessageKeys.log_locations_repair_attempt),
    log_locations_repair_success(MessageKeys.log_locations_repair_success),
    log_locations_repair_failed(MessageKeys.log_locations_repair_failed),
    log_scheduler_failed(MessageKeys.log_scheduler_failed),
    log_entity_retired(MessageKeys.log_entity_retired),
    log_start_begin_failed(MessageKeys.log_start_begin_failed),
    log_start_generation_failed(MessageKeys.log_start_generation_failed),
    log_start_spawn_failed(MessageKeys.log_start_spawn_failed),
    log_start_activation_failed(MessageKeys.log_start_activation_failed),
    log_start_processing_failed(MessageKeys.log_start_processing_failed),
    log_hologram_missing(MessageKeys.log_hologram_missing, true),
    log_center_debug(MessageKeys.log_center_debug),
    log_locale_directory_failed(MessageKeys.log_locale_directory_failed),
    log_locale_not_found(MessageKeys.log_locale_not_found),
    log_locale_migration_failed(MessageKeys.log_locale_migration_failed),
    log_locale_invalid(MessageKeys.log_locale_invalid),
    log_locale_prepare_failed(MessageKeys.log_locale_prepare_failed),
    log_tier_templates_failed(MessageKeys.log_tier_templates_failed),
    log_config_comments_failed(MessageKeys.log_config_comments_failed),

    help(MessageKeys.help, true);

    private Property<String> property;

    private Property<List<String>> listProperty;

    private boolean isList = false;

    /**
     * Used for strings
     *
     * @param property the property
     */
    Messages(@NonNull final Property<String> property) {
        this.property = property;
    }

    /**
     * Used for string lists
     *
     * @param listProperty the list property
     * @param isList Defines if it's a list or not.
     */
    Messages(@NonNull final Property<List<String>> listProperty, final boolean isList) {
        this.listProperty = listProperty;

        this.isList = isList;
    }

    private @NotNull final CrazyEnvoys plugin = CrazyEnvoys.get();

    private final FusionPaper fusion = this.plugin.getFusion();

    public @NonNull final String getString() {
        return ConfigManager.getMessages().getProperty(this.property);
    }

    public static @NonNull String displayWorldName(@NonNull final World world) {
        return switch (world.getName().toLowerCase(java.util.Locale.ROOT)) {
            case "world" -> world_overworld.getString();
            case "world_nether" -> world_nether.getString();
            case "world_the_end" -> world_end.getString();
            default -> world.getName();
        };
    }

    public @NonNull final List<String> getList() {
        return List.copyOf(ConfigManager.getMessages().getProperty(this.listProperty));
    }

    public void sendMessage(@NotNull final Audience sender, @NotNull final String placeholder, @NotNull final String replacement) {
        final State state = ConfigManager.getConfig().getProperty(ConfigKeys.message_state);

        switch (state) {
            case send_message -> sendRichMessage(sender, placeholder, replacement);
            case send_actionbar -> sendActionBar(sender, placeholder, replacement);
        }
    }

    public void sendMessage(@NotNull final Audience sender, @NotNull final Map<String, String> placeholders) {
        final State state = ConfigManager.getConfig().getProperty(ConfigKeys.message_state);

        switch (state) {
            case send_message -> sendRichMessage(sender, placeholders);
            case send_actionbar -> sendActionBar(sender, placeholders);
        }
    }

    public void sendMessage(@NotNull final Audience sender) {
        final State state = ConfigManager.getConfig().getProperty(ConfigKeys.message_state);

        switch (state) {
            case send_message -> sendRichMessage(sender);
            case send_actionbar -> sendActionBar(sender);
        }
    }

    public void sendRichMessage(@NotNull final Audience sender, @NotNull final String placeholder, @NotNull final String replacement) {
        sendRichMessage(sender, Map.of(placeholder, replacement));
    }

    public void sendRichMessage(@NotNull final Audience sender, @NotNull final Map<String, String> placeholders) {
        final String value = getMessage(sender, placeholders);

        if (value.isBlank()) return;

        sender.sendMessage(this.fusion.asComponent(value));
    }

    public void sendRichMessage(@NotNull final Audience sender) {
        sendRichMessage(sender, Map.of());
    }

    public void sendActionBar(@NotNull final Audience sender, @NotNull final String placeholder, @NotNull final String replacement) {
        sendActionBar(sender, Map.of(placeholder, replacement));
    }

    public void sendActionBar(@NotNull final Audience sender, @NotNull final Map<String, String> placeholders) {
        final String value = getMessage(sender, placeholders);

        if (value.isBlank()) return;

        if (sender instanceof Player player) {
            player.sendActionBar(this.fusion.asComponent(value));
        } else {
            sender.sendMessage(this.fusion.asComponent(value));
        }
    }

    public void sendActionBar(@NotNull final Audience sender) {
        sendActionBar(sender, Map.of());
    }

    public String getMessage(@NotNull final Audience sender, @NotNull final String placeholder, @NotNull final String replacement) {
        final Map<String, String> placeholders = new HashMap<>();

        placeholders.put(placeholder, replacement);

        return getMessage(sender, placeholders);
    }

    public String getMessage(@NotNull final Audience sender, @NotNull final Map<String, String> placeholders) {
        return parse(sender, placeholders);
    }

    public String getMessage(@NotNull final Audience sender) {
        return getMessage(sender, new HashMap<>());
    }

    public String getMessage(@NotNull final Map<String, String> placeholders) {
        return getMessage(Audience.empty(), placeholders);
    }

    public String getMessage() {
        return getMessage(Audience.empty(), new HashMap<>());
    }

    public void broadcast(final boolean isIgnoring, @NonNull final Map<String, String> placeholders) {
        broadcast(isIgnoring, "", placeholders);
    }

    public void broadcast(final boolean isIgnoring, @NonNull final String permission, @NonNull final Map<String, String> placeholders) {
        broadcast(isIgnoring, permission.isBlank() ? List.of() : List.of(permission), placeholders);
    }

    public void broadcast(final boolean isIgnoring, @NonNull final Collection<String> permissions, @NonNull final Map<String, String> placeholders) {
        final Server server = this.plugin.getServer();
        final CrazyManager crazyManager = this.plugin.getCrazyManager();

        final SettingsManager config = ConfigManager.getConfig();
        final boolean worldMessages = config.getProperty(ConfigKeys.envoys_world_messages);
        final List<String> worlds = List.copyOf(config.getProperty(ConfigKeys.envoys_allowed_worlds));
        final List<String> requiredPermissions = permissions.stream().filter(permission -> !permission.isBlank()).toList();
        final Map<String, String> values = Map.copyOf(placeholders);

        crazyManager.getScheduler().supplyGlobal("snapshot recipients for " + name(), () -> {
            sendMessage(server.getConsoleSender(), values);
            return List.copyOf(server.getOnlinePlayers());
        }).thenAccept(players -> players.forEach(player -> crazyManager.getScheduler().runEntity(
                player, "broadcast " + name() + " to " + player.getUniqueId(), () -> {
                    if (worldMessages && !worlds.contains(player.getWorld().getName())) return;
                    if (isIgnoring && crazyManager.isIgnoringMessages(player.getUniqueId())) return;
                    if (!requiredPermissions.isEmpty() && requiredPermissions.stream().noneMatch(player::hasPermission)) return;

                    sendMessage(player, values);
                }
        )));
    }

    public void broadcast(final boolean isIgnoring) {
        broadcast(isIgnoring, new HashMap<>());
    }

    public void migrate() {
        if (this.isList) {
            final SettingsManager messages = ConfigManager.getMessages();
            messages.setProperty(this.listProperty, AdvUtils.convert(messages.getProperty(this.listProperty), true));

            return;
        }

        final SettingsManager messages = ConfigManager.getMessages();
        messages.setProperty(this.property, AdvUtils.convert(messages.getProperty(this.property), true));
    }

    private @NonNull String parse(@NotNull final Audience sender, @NonNull final Map<String, String> placeholders) {
        final Map<String, String> origin = new HashMap<>(placeholders);

        final String configuredPrefix = ConfigManager.getConfig().getProperty(ConfigKeys.command_prefix);
        final String defaultPrefix = ConfigKeys.command_prefix.getDefaultValue();
        final String prefix = configuredPrefix.isBlank() || configuredPrefix.equals(defaultPrefix)
                ? ConfigManager.getMessages().getProperty(MessageKeys.command_prefix)
                : configuredPrefix;

        origin.putIfAbsent("{prefix}", prefix);

        return this.fusion.parse(sender, this.isList ? StringUtils.toString(getList()) : getString(), origin);
    }
}
