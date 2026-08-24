package com.badbones69.crazyenvoys.commands.types.admin;

import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.api.events.EnvoyStartEvent;
import com.badbones69.crazyenvoys.commands.types.EnvoyCommand;
import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.annotations.Command;
import dev.triumphteam.cmd.core.annotations.Syntax;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

public class StartCommand extends EnvoyCommand {

    @Command(value = "start", alias = {"begin"})
    @Permission(value = "envoy.start", def = PermissionDefault.OP)
    @Syntax("/envoys start")
    public void start(final CommandSender sender) {
        if (this.crazyManager.isEnvoyBusy()) {
            Messages.already_started.sendMessage(sender);

            return;
        }

        EnvoyStartEvent event;

        Player starter = null;

        if (sender instanceof Player player) {
            event = new EnvoyStartEvent(EnvoyStartEvent.EnvoyStartReason.FORCED_START_PLAYER, player);

            starter = player;
        } else {
            event = new EnvoyStartEvent(EnvoyStartEvent.EnvoyStartReason.FORCED_START_CONSOLE);
        }

        this.pluginManager.callEvent(event);

        if (event.isCancelled()) return;

        this.crazyManager.startEnvoyEventAsync(starter).whenComplete((started, throwable) -> {
            if (throwable != null || !Boolean.TRUE.equals(started)) return;

            this.crazyManager.getScheduler().runForSender(sender, "confirm forced envoy start", () -> Messages.force_start.sendMessage(sender));
        });
    }
}
