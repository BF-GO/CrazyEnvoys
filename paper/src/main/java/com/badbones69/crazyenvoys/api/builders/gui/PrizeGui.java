package com.badbones69.crazyenvoys.api.builders.gui;

import com.badbones69.crazyenvoys.api.builders.gui.api.types.StaticInventory;
import com.badbones69.crazyenvoys.api.enums.PersistentKeys;
import com.badbones69.crazyenvoys.api.objects.misc.Prize;
import com.badbones69.crazyenvoys.api.objects.misc.Tier;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PrizeGui extends StaticInventory {

    private final Prize prize;
    private final List<ItemStack> items;

    public PrizeGui(@NotNull final Player player, @NotNull final Tier tier, @NotNull final Prize prize, @NotNull final String title, final int size) {
        this(player, tier, prize, prize.getItems(player), title, size);
    }

    public PrizeGui(
            @NotNull final Player player,
            @NotNull final Tier tier,
            @NotNull final Prize prize,
            @NotNull final List<ItemStack> items,
            @NotNull final String title,
            final int size
    ) {
        super(player, tier, title, size);

        this.prize = prize;
        this.items = items.stream().map(ItemStack::clone).toList();
    }

    @Override
    public void build() {
        final String id = this.prize.getPrizeID();

        this.items.forEach(source -> {
            final ItemStack guiItem = source.clone();
            guiItem.editPersistentDataContainer(container -> container.set(
                    PersistentKeys.prize_item.getNamespacedKey(),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    id
            ));

            this.gui.addSlotAction(guiItem, event -> {
                final ItemStack itemStack = event.getCurrentItem();

                if (itemStack == null || itemStack.isEmpty()) return;

                final PersistentDataContainerView container = itemStack.getPersistentDataContainer();

                if (!container.has(PersistentKeys.prize_item.getNamespacedKey())) return;

                final PlayerInventory inventory = this.player.getInventory();

                final ItemStack reward = itemStack.clone();
                reward.editPersistentDataContainer(box -> box.remove(PersistentKeys.prize_item.getNamespacedKey()));

                event.setCurrentItem(null);

                inventory.addItem(reward).values().forEach(leftover ->
                        this.player.getWorld().dropItem(this.player.getLocation(), leftover)
                );
            });
        });

        this.gui.open(this.player);
    }
}
