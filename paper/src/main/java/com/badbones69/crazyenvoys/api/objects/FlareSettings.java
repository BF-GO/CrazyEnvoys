package com.badbones69.crazyenvoys.api.objects;

import ch.jalu.configme.SettingsManager;
import com.badbones69.crazyenvoys.Methods;
import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.api.enums.PersistentKeys;
import com.badbones69.crazyenvoys.config.types.ConfigKeys;
import com.badbones69.crazyenvoys.util.ItemUtil;
import com.ryderbelserion.fusion.paper.builders.items.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.badbones69.crazyenvoys.config.ConfigManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FlareSettings {
    
    private ItemBuilder builder;
    
    public void load() {
        final SettingsManager config = ConfigManager.getConfig();

        String name = config.getProperty(ConfigKeys.envoys_flare_item_name);
        if (name.isBlank() || name.equals(ConfigKeys.envoys_flare_item_name.getDefaultValue())) {
            name = Messages.flare_item_name.getString();
        }

        List<String> lore = config.getProperty(ConfigKeys.envoys_flare_item_lore);
        if (lore.isEmpty() || lore.equals(ConfigKeys.envoys_flare_item_lore.getDefaultValue())) {
            lore = Messages.flare_item_lore.getList();
        }

        this.builder = ItemBuilder.from(config.getProperty(ConfigKeys.envoys_flare_item_type).toLowerCase())
                .withDisplayName(name)
                .withDisplayLore(lore);

        ItemUtil.addGlow(this.builder, String.valueOf(config.getProperty(ConfigKeys.envoys_flare_item_glowing)));
    }
    
    public ItemStack getFlare(@NotNull final Player player) {
        return getFlare(player, 1);
    }
    
    public ItemStack getFlare(@NotNull final Player player, int amount) {
        return this.builder.setPersistentBoolean(PersistentKeys.envoy_flare.getNamespacedKey(), true).setAmount(amount).asItemStack(player);
    }
    
    public boolean isFlare(ItemStack item) {
        return item.getPersistentDataContainer().has(PersistentKeys.envoy_flare.getNamespacedKey());
    }
    
    public void giveFlare(Player player) {
        giveFlare(player, 1);
    }
    
    public void giveFlare(Player player, int amount) {
        if (player.getInventory().isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), getFlare(player, amount));
        } else {
            Methods.addItem(player, getFlare(player, amount));
        }
    }
    
    public void takeFlare(Player player) {
        player.getInventory().removeItem(getFlare(player));
    }
}
