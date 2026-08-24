package com.badbones69.crazyenvoys.commands.types.admin;

import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.api.events.EnvoyEndEvent;
import com.badbones69.crazyenvoys.commands.types.EnvoyCommand;
import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.annotations.Command;
import dev.triumphteam.cmd.core.annotations.Syntax;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

public class ReloadCommand extends EnvoyCommand {

    @Command("reload")
    @Permission(value = "envoy.reload", def = PermissionDefault.OP)
    @Syntax("/envoys reload")
    public void execute(final CommandSender sender) {
        if (this.crazyManager.isEnvoyBusy()) {
            EnvoyEndEvent event = new EnvoyEndEvent(EnvoyEndEvent.EnvoyEndReason.RELOAD);

            this.pluginManager.callEvent(event);

        }

        this.crazyManager.endEnvoyEventAsync().handle((unused, throwable) -> null)
                .thenCompose(unused -> this.crazyManager.getScheduler().runGlobal("reload CrazyEnvoys files", () -> {
                    this.fusion.reload();
                    this.fileManager.refresh(false);
                }))
                .thenCompose(unused -> this.crazyManager.reloadAsync())
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) return;
                    this.crazyManager.getScheduler().runForSender(sender, "confirm CrazyEnvoys reload", () -> Messages.reloaded.sendMessage(sender));
                });
    }
}
