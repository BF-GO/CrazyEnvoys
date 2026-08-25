package com.badbones69.crazyenvoys.commands.types.player;

import com.badbones69.crazyenvoys.Methods;
import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.commands.types.EnvoyCommand;
import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.annotations.ArgName;
import dev.triumphteam.cmd.core.annotations.Command;
import dev.triumphteam.cmd.core.annotations.Optional;
import dev.triumphteam.cmd.core.annotations.Suggestion;
import dev.triumphteam.cmd.core.annotations.Syntax;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropCommand extends EnvoyCommand {

    @Command(value = "drop", alias = {"drops"})
    @Permission(value = "envoy.drops", def = PermissionDefault.TRUE)
    @Syntax("/envoys drops [page]")
    public void execute(final CommandSender sender, @ArgName("page") @Optional @Suggestion("drops") final Integer page) {
        final List<String> locs = new ArrayList<>();
        final boolean active = this.crazyManager.isEnvoyActive();
        final List<Block> blocks = new ArrayList<>(active
                ? this.crazyManager.getCurrentDropLocations()
                : this.locationSettings.getSpawnLocations());

        if (blocks.isEmpty()) {
            Messages.not_started.sendMessage(sender);
            return;
        }

        blocks.sort(Comparator
                .comparing((Block block) -> block.getWorld().getName())
                .thenComparingInt(Block::getX)
                .thenComparingInt(Block::getZ)
                .thenComparingInt(Block::getY));

        int amount = 1;

        final Map<String, String> placeholders = new HashMap<>();

        for (Block block : blocks) {
            placeholders.put("{id}", String.valueOf(amount));
            placeholders.put("{world}", Messages.displayWorldName(block.getWorld()));
            placeholders.put("{x}", String.valueOf(block.getX()));
            placeholders.put("{y}", String.valueOf(block.getY()));
            placeholders.put("{z}", String.valueOf(block.getZ()));

            locs.add(Messages.drops_format.getMessage(sender, placeholders));

            amount++;

            placeholders.clear();
        }

        if (active) {
            Messages.drops_available.sendMessage(sender);
        } else {
            Messages.drops_possibilities.sendMessage(sender);
        }

        for (String dropLocation : Methods.getPage(locs, page == null ? 1 : page)) {
            sender.sendMessage(this.fusion.asComponent(sender, dropLocation));
        }

        if (locs.size() > 10) Messages.drops_page.sendMessage(sender);
    }
}
