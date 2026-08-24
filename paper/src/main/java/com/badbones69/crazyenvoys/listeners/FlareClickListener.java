package com.badbones69.crazyenvoys.listeners;

import ch.jalu.configme.SettingsManager;
import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.Methods;
import com.badbones69.crazyenvoys.api.CrazyManager;
import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.api.events.EnvoyStartEvent;
import com.badbones69.crazyenvoys.api.objects.FlareSettings;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import com.badbones69.crazyenvoys.config.ConfigManager;
import com.badbones69.crazyenvoys.config.types.ConfigKeys;
import java.util.Map;

public class FlareClickListener implements Listener {

    private @NotNull final CrazyEnvoys plugin = CrazyEnvoys.get();
    private @NotNull final SettingsManager config = ConfigManager.getConfig();

    private @NotNull final Server server = this.plugin.getServer();

    private @NotNull final PluginManager pluginManager = this.server.getPluginManager();

    private @NotNull final CrazyManager crazyManager = this.plugin.getCrazyManager();

    private @NotNull final FlareSettings flareSettings = this.plugin.getFlareSettings();

    @EventHandler
    public void onFlareInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack flare = Methods.getItemInHand(player);

            if (this.flareSettings.isFlare(flare)) {
                event.setCancelled(true);

                if (!player.hasPermission("envoy.flare.use")) {
                    Messages.cant_use_flares.sendMessage(player);

                    return;
                }

                if (this.crazyManager.isEnvoyBusy()) {
                    Messages.already_started.sendMessage(player);

                    return;
                }

                boolean toggle = false;

                if (this.pluginManager.isPluginEnabled("WorldEdit") && this.pluginManager.isPluginEnabled("WorldGuard")) {
                    if (this.config.getProperty(ConfigKeys.envoys_flare_world_guard_toggle)) {
                        for (String region : this.config.getProperty(ConfigKeys.envoys_flare_world_guard_regions)) {
                            if (this.crazyManager.getWorldGuardPluginSupport().inRegion(region, player.getLocation())) toggle = true;
                        }
                    } else {
                        toggle = true;
                    }
                } else {
                    toggle = true;
                }

                if (!toggle) {
                    Messages.not_in_world_guard_region.sendMessage(player);

                    return;
                }

                this.crazyManager.getScheduler().supplyGlobal("count online players for flare", () -> this.server.getOnlinePlayers().size())
                        .thenAccept(online -> this.crazyManager.getScheduler().runEntity(player, "finish flare use", () -> useFlare(player, online)));
            }
        }
    }

    private void useFlare(final Player player, final int online) {
        if (this.crazyManager.isEnvoyBusy() || !this.flareSettings.isFlare(Methods.getItemInHand(player))) {
            if (this.crazyManager.isEnvoyBusy()) Messages.already_started.sendMessage(player);
            return;
        }

        if (this.config.getProperty(ConfigKeys.envoys_flare_minimum_players_toggle)
                && online < this.config.getProperty(ConfigKeys.envoys_flare_minimum_players_amount)) {
            Messages.not_enough_players.sendMessage(player, Map.of("{amount}", String.valueOf(online)));
            return;
        }

        final EnvoyStartEvent envoyStartEvent = new EnvoyStartEvent(EnvoyStartEvent.EnvoyStartReason.FLARE);
        this.pluginManager.callEvent(envoyStartEvent);
        if (envoyStartEvent.isCancelled()) return;

        this.crazyManager.startEnvoyEventAsync(player).whenComplete((started, throwable) -> {
            if (throwable != null || !Boolean.TRUE.equals(started)) return;

            this.crazyManager.getScheduler().runEntity(player, "consume used envoy flare", () -> {
                Messages.used_flare.sendMessage(player);
                this.flareSettings.takeFlare(player);
            });
        });
    }
}
