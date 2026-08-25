package com.badbones69.crazyenvoys.config.types;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;
import java.util.List;
import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class MessageKeys implements SettingsHolder {

    protected MessageKeys() {}

    @Override
    public void registerComments(CommentsConfiguration conf) {
        String[] header = {
                "Support: https://discord.gg/badbones-s-live-chat-182615261403283459",
                "Github: https://github.com/Crazy-Crew",
                "",
                "Issues: https://github.com/Crazy-Crew/CrazyEnvoys/issues",
                "Features: https://github.com/Crazy-Crew/CrazyEnvoys/issues",
                "",
                "Tips:",
                " 1. Make sure to use the {prefix} to add the prefix in front of messages.",
                " 2. If you wish to use more than one line for a message just go from a line to a list.",
                "Examples:",
                "  Line:",
                "    No-Permission: '{prefix}<red>You do not have permission to use that command.'",
                "  List:",
                "    No-Permission:",
                "      - '{prefix}<red>You do not have permission'",
                "      - '<red>to use that command. Please try another.'"
        };

        String[] deprecation = {
                "",
                "Warning: This section is subject to change so it is considered deprecated.",
                "This is your warning before the change happens.",
                ""
        };
        
        conf.setComment("player", header);
    }

    public static final Property<String> no_permission = newProperty("player.no-permission", "{prefix}<red>You do not have permission to use that command.");

    public static final Property<String> no_claim_permission = newProperty("player.no-permission-to-claim", "{prefix}<red>You do not have permission to claim that envoy.");

    public static final Property<String> envoy_already_started = newProperty("envoys.already-started", "{prefix}<red>There is already an envoy event running. Please stop it to start a new one.");

    public static final Property<String> envoy_force_start = newProperty("envoys.force-start", "{prefix}<gray>You have just started the envoy.");

    public static final Property<String> envoy_not_started = newProperty("envoys.not-started", "{prefix}<red>There is no envoy event going on at this time.");

    public static final Property<String> envoy_force_ended = newProperty("envoys.force-ended", "{prefix}<red>You have just ended the envoy.");

    public static final Property<String> envoy_warning = newProperty("envoys.warning", "{prefix}<red>[<dark_red>ALERT<red>] <gray>There is an envoy event happening in <gold>{time}.");

    public static final Property<List<String>> envoy_started = newListProperty("envoys.started.list", List.of(
            "{prefix}<gray>An envoy event has just started. <gold>{amount} <gray>crates have spawned around spawn for 5m."
    ));

    public static final Property<List<String>> envoy_started_player = newListProperty("envoys.started-player.list", List.of(
            "{prefix}<gray>An envoy event has just been started by {starter}. <gold>{amount} <gray>crates have spawned around spawn for 5m."
    ));

    public static final Property<List<String>> envoys_remaining = newListProperty("envoys.left", List.of(
            "{prefix}<gold>{player} <gray>has just found a tier envoy. There are now <gold>{amount} <gray>left to find."
    ));

    public static final Property<List<String>> envoy_ended = newListProperty("envoys.ended", List.of(
            "{prefix}<red>The envoy event has ended. Thanks for playing and please come back for the next one."
    ));

    public static final Property<String> envoy_wave_ended = newProperty("envoys.wave-ended", "{prefix}<gray>The current drop wave has ended. The next wave starts in <gold>{time}</gold>.</gray>");

    public static final Property<String> envoy_wave_ended_manual = newProperty("envoys.wave-ended-manual", "{prefix}<gray>The current drop wave has ended. The next wave requires a manual start.</gray>");

    public static final Property<String> not_enough_players = newProperty("envoys.not-enough-players", "{prefix}<gray>Not enough players are online to start the envoy event. Only <gold>{amount} <gray>players are online.");

    public static final Property<String> enter_editor_mode = newProperty("envoys.enter-editor-mode", "{prefix}<gray>You are now in editor mode.");

    public static final Property<String> exit_editor_mode = newProperty("envoys.leave-editor-mode", "{prefix}<gray>You have now left editor mode.");

    public static final Property<String> envoy_clear_locations = newProperty("envoys.editor-clear-locations", "{prefix}<gray>You have cleared all the editor spawn locations.");

    public static final Property<String> envoy_clear_failure = newProperty("envoys.editor-clear-failure", "{prefix}<gray>You must be in Editor mode to clear the spawn locations.");

    public static final Property<String> envoy_kicked_from_editor_mode = newProperty("envoys.kicked-from-editor-mode", "{prefix}<red>Sorry but an envoy is active. Please stop it or wait till it''s over.");

    public static final Property<String> envoy_add_location = newProperty("envoys.add-location", "{prefix}<gray>You have just added a spawn location.");

    public static final Property<String> envoy_remove_location = newProperty("envoys.remove-location", "{prefix}<red>You have just removed a spawn location.");

    public static final Property<String> envoy_time_left = newProperty("envoys.time-left", "{prefix}<gray>The current envoy has <gold>{time}<gray> left.");

    public static final Property<List<String>> envoy_time_till_event = newListProperty("envoys.time-till-event", List.of(
            "{prefix}<gray>The next envoy will start in <gold>{time}<gray>."
    ));

    public static final Property<String> envoy_used_flare = newProperty("envoys.flare.used-flare", "{prefix}<gray>You have just started an envoy event with a flare.");

    public static final Property<String> envoy_cant_use_flare = newProperty("envoys.flare.cant-use-flares", "{prefix}<red>You do not have permission to use flares.");

    public static final Property<String> envoy_give_flare = newProperty("envoys.flare.sent-flare", "{prefix}<gray>You have just given <gold>{player} {amount} <gray>flares.");

    public static final Property<String> envoy_received_flare = newProperty("envoys.flare.received-flare", "{prefix}<gray>You have been given <gold>{amount} <gray>flares.");

    public static final Property<String> envoy_new_center = newProperty("envoys.new-center", "{prefix}<gray>You have just set a new center for the random envoy crates.");

    public static final Property<String> not_in_world_guard_region = newProperty("envoys.not-in-world-guard-region", "{prefix}<red>You must be in the WarZone to use a flare.");

    public static final Property<String> start_ignoring_messages = newProperty("envoys.start-ignoring-messages", "{prefix}<gray>You are now ignoring the collecting messages.");

    public static final Property<String> stop_ignoring_messages = newProperty("envoys.stop-ignoring-messages", "{prefix}<gray>You now see all the collecting messages.");

    public static final Property<String> cooldown_left = newProperty("envoys.cooldown-left", "{prefix}<gray>You still have <gold>{time} <gray>till you can collect another crate.");

    public static final Property<String> countdown_in_progress = newProperty("envoys.countdown-in-progress", "{prefix}<gray>You cannot claim any envoys for another <gold>{time} seconds.");

    public static final Property<String> drops_available = newProperty("envoys.drops-available", "{prefix}<gray>List of all available envoys.");

    public static final Property<String> drops_possibilities = newProperty("envoys.drops-possibilities", "{prefix}<gray>List of location envoy''s may spawn at.");

    public static final Property<String> drops_hint = newProperty("envoys.drops-hint", "{prefix}<gray>Use <gold>/envoys drops</gold> to view the current crate coordinates.");

    public static final Property<String> drops_page = newProperty("envoys.drops-page", "{prefix}<gray>Use /crazyenvoys drops [page] to see more.");

    public static final Property<String> drops_format = newProperty("envoys.drops-format", "<gray>[<gold>{id}<gray>]: {world}, {x}, {y}, {z}");

    public static final Property<String> no_spawn_locations_found = newProperty("envoys.no-spawn-locations-found", "{prefix}<red>No spawn locations were found and so the event has been cancelled and the cooldown has been reset.");

    public static final Property<String> hologram_on_going = newProperty("envoys.hologram-placeholders.on-going", "On Going");

    public static final Property<String> hologram_not_running = newProperty("envoys.hologram-placeholders.not-running", "Not Running");

    public static final Property<String> time_placeholder_day = newProperty("envoys.time-placeholders.day", "d");
    public static final Property<String> time_placeholder_hour = newProperty("envoys.time-placeholders.hour", "h");
    public static final Property<String> time_placeholder_minute = newProperty("envoys.time-placeholders.minute", "m");
    public static final Property<String> time_placeholder_second = newProperty("envoys.time-placeholders.second", "s");

    public static final Property<String> envoy_locations = newProperty("envoys.envoy-locations", "<yellow><bold>All Envoy Locations:</bold></yellow><newline><red>[ID], [World]: [X], [Y], [Z]</red> {locations}");

    public static final Property<String> location_format = newProperty("envoys.location-format", "<newline><dark_gray>[<blue>{id}<dark_gray>] <red>{world}: {x}, {y}, {z}");

    public static final Property<String> world_overworld = newProperty("envoys.world-names.overworld", "Overworld");
    public static final Property<String> world_nether = newProperty("envoys.world-names.nether", "Nether");
    public static final Property<String> world_end = newProperty("envoys.world-names.end", "The End");

    @Comment("A list of available placeholders: {command}")
    public static final Property<String> unknown_command = newProperty("misc.command-not-found", "{prefix}<red>{command} is not a known command.");

    @Comment("A list of available placeholders: {usage}")
    public static final Property<String> correct_usage = newProperty("misc.correct-usage", "{prefix}<red>The correct usage for this command is <yellow>{usage}.");

    public static final Property<String> must_be_console_sender = newProperty("misc.must-be-console-sender", "{prefix}<red>You must be using console to use this command.");

    public static final Property<String> player_only = newProperty("misc.player-only", "{prefix}<red>Only players can use that command.");

    public static final Property<String> not_online = newProperty("misc.not-online", "{prefix}<red>That player is not online at this time.");

    public static final Property<String> not_a_number = newProperty("misc.not-a-number", "{prefix}<red>That is not a number.");

    public static final Property<String> envoy_plugin_reloaded = newProperty("misc.config-reload", "{prefix}<gray>You have just reloaded all the files.");

    @Comment("A list of available placeholders: {flag}, {usage}")
    public static final Property<String> lacking_flag = newProperty("misc.lacking-flag", "{prefix}<red>{flag} is not present in the command, expected format: {usage}");

    @Comment("A list of available placeholders: {file}, {type}, {reason}")
    public static final Property<String> error_migrating = newProperty("command.migrate.error", "{prefix}<red>We could not migrate <green>{file} <red>using <green>{type} <red>migration for <green>{reason}.");

    @Comment("A list of available placeholders: {prefix}")
    public static final Property<String> migration_not_available = newProperty("command.migrate.not-available", "{prefix}<green>This migration type is not available.");

    @Comment("A list of available placeholders: {name}")
    public static final Property<String> migration_plugin_not_enabled = newProperty("command.migrate.plugin-not-available", "{prefix}<green>The plugin <red>{name} <green>is not enabled. Cannot use as migration!");

    @Comment("A list of available placeholders: {prefix}")
    public static final Property<String> migration_no_crates_available = newProperty("command.migrate.no-envoys-available", "{prefix}<green>There is no envoys available for migration!");

    public static final Property<String> empty_tier_warning = newProperty("warnings.empty-tier", "<red>No prizes were found in the tier named {tier}. Please check its configuration.");

    public static final Property<String> not_applicable = newProperty("misc.not-applicable", "N/A");

    public static final Property<String> command_prefix = newProperty("ui.command-prefix", "<dark_gray>[<light_purple>CrazyEnvoys</light_purple>]</dark_gray> <dark_gray>»</dark_gray> ");

    public static final Property<String> envoy_menu_title = newProperty("ui.envoy-menu-title", "<red>Envoy Drops");

    public static final Property<String> prize_load_error_item = newProperty("ui.prize-load-error", "<red>Error found with prize: {prize}");

    public static final Property<String> flare_item_name = newProperty("ui.flare.name", "<bold><gray>(<dark_red>!<gray>)</bold> <red>Flare");

    public static final Property<List<String>> flare_item_lore = newListProperty("ui.flare.lore", List.of(
            "<gray>Right click me to",
            "<gray>start an envoy event."
    ));

    public static final Property<String> grace_period_unlocked = newProperty("ui.grace-period.unlocked", "<red>Ready to claim.");

    public static final Property<String> grace_period_time_unit = newProperty("ui.grace-period.time-unit", " seconds.");

    public static final Property<String> armor_set_activated = newProperty("ui.armor-set.activated", "{prefix}<green>Full armor set {set} activated.</green>");

    public static final Property<String> armor_set_deactivated = newProperty("ui.armor-set.deactivated", "{prefix}<gray>Armor set {set} is no longer complete.</gray>");

    public static final Property<String> armor_breaker_name = newProperty(
            "ui.enchantments.armor-breaker",
            "<gradient:#ffb300:#ff1744><bold>Armor Breaker</bold></gradient>"
    );

    public static final Property<String> armor_breaker_awarded = newProperty(
            "ui.rare-enchantment.armor-breaker-awarded",
            "{prefix}<gradient:#ffb300:#ff1744><bold>ULTRA-RARE BONUS!</bold></gradient> <gray>Your reward gained {enchantment} <white>{level}</white>.</gray>"
    );

    public static final Property<String> log_done = newProperty("logs.done", "Done ({time})!");
    public static final Property<String> log_config_migrated = newProperty("logs.config-migrated", "Successfully migrated {file}.");
    public static final Property<String> log_prize_error = newProperty("logs.prize-error", "An error occurred while loading prize {prize}.");
    public static final Property<String> log_locations_retry = newProperty("logs.locations-retry", "Failed to load {amount} locations and will retry in 10 seconds.");
    public static final Property<String> log_recovery_error = newProperty("logs.recovery-error", "Startup recovery completed with errors: {error}");
    public static final Property<String> log_hologram_enabled = newProperty("logs.hologram-enabled", "{plugin} support has been enabled.");
    public static final Property<String> log_center_world_missing = newProperty("logs.center-world-missing", "The envoy center world could not be found. The event was cancelled. Center: {center}");
    public static final Property<String> log_visuals_failed = newProperty("logs.visuals-failed", "Could not finish envoy visuals for session {session}.");
    public static final Property<String> log_hologram_cleanup_failed = newProperty("logs.hologram-cleanup-failed", "Could not remove an envoy hologram during cleanup.");
    public static final Property<String> log_task_cancel_failed = newProperty("logs.task-cancel-failed", "Could not cancel envoy {task} task.");
    public static final Property<String> log_generation_partial = newProperty("logs.generation-partial", "Generated {generated}/{requested} envoy locations after {attempts} attempts.");
    public static final Property<String> log_generation_area_limited = newProperty("logs.generation-area-limited", "The crate amount exceeds the configured area. Spawning {amount} crates instead.");
    public static final Property<String> log_tier_capacity_limited = newProperty("logs.tier-capacity-limited", "Tier limits only allowed {assigned}/{requested} crates to be assigned.");
    public static final Property<String> log_armor_set_invalid = newProperty("logs.armor-set-invalid", "Armor set in tier {tier} was disabled: {reason}.");
    public static final Property<String> log_rare_enchantment_invalid = newProperty("logs.rare-enchantment-invalid", "Rare enchantment in tier {tier} was disabled: {reason}.");
    public static final Property<String> log_armor_breaker_unavailable = newProperty("logs.armor-breaker-unavailable", "Armor Breaker is missing from the server registry. Install the new jar and perform a full server restart.");
    public static final Property<String> log_armor_breaker_name_invalid = newProperty("logs.armor-breaker-name-invalid", "Could not parse the Armor Breaker display name; using a safe fallback.");
    public static final Property<String> log_chunk_load_failed = newProperty("logs.chunk-load-failed", "Could not load {type} envoy chunk at {x},{z}.");
    public static final Property<String> log_chunk_request_failed = newProperty("logs.chunk-request-failed", "Could not request {type} envoy chunk at {x},{z}.");
    public static final Property<String> log_no_tiers = newProperty("logs.no-tiers", "No tiers were found in the tiers folder. Delete the folder to regenerate the examples.");
    public static final Property<String> log_no_valid_locations = newProperty("logs.no-valid-locations", "Could not generate any valid envoy locations.");
    public static final Property<String> log_signal_cancel_failed = newProperty("logs.signal-cancel-failed", "Could not cancel an envoy signal flare.");
    public static final Property<String> log_location_deserialize_failed = newProperty("logs.location-deserialize-failed", "Could not deserialize persisted envoy location {location}.");
    public static final Property<String> log_center_repair_attempt = newProperty("logs.center-repair-attempt", "Attempting to repair the envoy center location.");
    public static final Property<String> log_center_repair_failed = newProperty("logs.center-repair-failed", "The envoy center could not be repaired. Another attempt will be made at the next event.");
    public static final Property<String> log_center_repair_success = newProperty("logs.center-repair-success", "The envoy center was repaired and the event will continue.");
    public static final Property<String> log_locations_repair_attempt = newProperty("logs.locations-repair-attempt", "Attempting to repair {amount} failed locations.");
    public static final Property<String> log_locations_repair_success = newProperty("logs.locations-repair-success", "Successfully repaired {amount} locations.");
    public static final Property<String> log_locations_repair_failed = newProperty("logs.locations-repair-failed", "Could not repair {amount} locations; they will not be retried.");
    public static final Property<String> log_scheduler_failed = newProperty("logs.scheduler-failed", "Scheduled task failed: {context}");
    public static final Property<String> log_entity_retired = newProperty("logs.entity-retired", "Entity retired before task ran: {context}");
    public static final Property<String> log_start_begin_failed = newProperty("logs.start-begin-failed", "Could not begin envoy event.");
    public static final Property<String> log_start_generation_failed = newProperty("logs.start-generation-failed", "Could not generate envoy locations.");
    public static final Property<String> log_start_spawn_failed = newProperty("logs.start-spawn-failed", "Could not spawn envoy crates.");
    public static final Property<String> log_start_activation_failed = newProperty("logs.start-activation-failed", "Could not activate envoy session.");
    public static final Property<String> log_start_processing_failed = newProperty("logs.start-processing-failed", "Could not process generated envoy locations.");
    public static final Property<List<String>> log_hologram_missing = newListProperty("logs.hologram-missing", List.of(
            "No supported hologram plugin was found. If you use CMI, enable its hologram module in modules.yml.",
            "After enabling the CMI module, run /crazyenvoys reload; otherwise restart the server."
    ));
    public static final Property<String> log_center_debug = newProperty("logs.center-debug", "Center diagnostics: saved={saved}, location={location}, world-loaded={world_loaded}");
    public static final Property<String> log_locale_directory_failed = newProperty("logs.locale-directory-failed", "Could not create locale directory at {path}.");
    public static final Property<String> log_locale_not_found = newProperty("logs.locale-not-found", "Locale {locale} was not found. Falling back to en-US.");
    public static final Property<String> log_locale_migration_failed = newProperty("logs.locale-migration-failed", "Could not migrate {source} to {target}.");
    public static final Property<String> log_locale_invalid = newProperty("logs.locale-invalid", "Invalid locale name {locale}. Falling back to en-US.");
    public static final Property<String> log_locale_prepare_failed = newProperty("logs.locale-prepare-failed", "Could not prepare locale {locale}.");
    public static final Property<String> log_tier_templates_failed = newProperty("logs.tier-templates-failed", "Could not install localized tier templates.");
    public static final Property<String> log_config_comments_failed = newProperty("logs.config-comments-failed", "Could not update localized config comments.");

    @Comment({
            "A list of available placeholders: {type}, {time}, {succeeded_amount}, {failed_amount}"
    })
    public static final Property<List<String>> successfully_migrated_users = newListProperty("command.migrate.success-users", List.of(
            "<bold><gold>━━━━━━━━━━━━━━━━━━━ Migration Stats ━━━━━━━━━━━━━━━━━━━</gold></bold>",
            "<dark_gray>»</dark_gray> <green>Successful Conversions: ",
            " ⤷ {succeeded_amount}</green>",
            "<dark_gray>»</dark_gray> <red>Failed Conversions: ",
            " ⤷ {failed_amount}</red>",
            "",
            "<red>Conversion Time: <yellow>{time}",
            "<red>Conversion Type: <yellow>{type}",
            "",
            "<bold><gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</gold></bold>"
    ));

    @Comment({
            "A list of available placeholders: {type}, {files}",
            "",
            "{files} will output multiple envoys if migrating from another plugin"
    })
    public static final Property<List<String>> successfully_migrated = newListProperty("command.migrate.success", List.of(
            "<bold><gold>━━━━━━━━━━━━━━━━━━━ Migration Stats ━━━━━━━━━━━━━━━━━━━</gold></bold>",
            "<dark_gray>»</dark_gray> <green>Successful Conversions: ",
            " ⤷ {succeeded_amount}</green>",
            "<dark_gray>»</dark_gray> <red>Failed Conversions: ",
            " ⤷ {failed_amount}</red>",
            "",
            "<red>Conversion Time: <yellow>{time}",
            "<red>Conversion Type: <yellow>{type}",
            "",
            "<red>Converted Files:",
            "{files}",
            "",
            "<bold><gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</gold></bold>"
    ));

    public static final Property<List<String>> help = newListProperty("misc.help", List.of(
            "<gold>/crazyenvoys help <gray>- Shows the envoy help menu.",
            "<gold>/crazyenvoys reload <gray>- Reloads all the config files.",
            "<gold>/crazyenvoys time <gray>- Shows the time till the envoy starts or ends.",
            "<gold>/crazyenvoys drops [page] <gray>- Shows all current crate locations.",
            "<gold>/crazyenvoys ignore <gray>- Shuts up the envoy collecting message.",
            "<gold>/crazyenvoys flare [amount] [player] <gray>- Give a player a flare to call an envoy event.",
            "<gold>/crazyenvoys edit <gray>- Edit the crate locations with bedrock.",
            "<gold>/crazyenvoys start <gray>- Force starts the envoy.",
            "<gold>/crazyenvoys stop <gray>- Force stops the envoy.",
            "<gold>/crazyenvoys center <gray>- Set the center of the random crate drops."
    ));
}
