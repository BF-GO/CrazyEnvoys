package com.badbones69.crazyenvoys.api.objects.misc;

import com.ryderbelserion.fusion.paper.builders.items.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Optional;

public class Prize {
    
    private final String prizeID;
    private int chance;
    private boolean dropItems;
    private volatile List<String> messages;
    private volatile List<String> commands;
    private volatile List<ItemBuilder> builders;
    private String displayName;
    private ArmorSetPiece armorSetPiece;
    
    public Prize(String prizeID) {
        this.prizeID = prizeID;
        this.chance = 100;
        this.dropItems = false;
        this.displayName = "";
        this.messages = List.of();
        this.commands = List.of();
        this.builders = List.of();
    }

    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Get the prizeID of the prize.
     *
     * @return The prizeID of the prize.
     */
    public String getPrizeID() {
        return this.prizeID;
    }
    
    /**
     * Get the relative weight of the prize being selected.
     *
     * @return The chance as an integer.
     */
    public int getChance() {
        return this.chance;
    }
    
    /**
     * Set the relative selection weight of the prize.
     *
     * @param chance The new non-negative selection weight.
     */
    public Prize setChance(int chance) {
        this.chance = chance;

        return this;
    }

    public Prize setDisplayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    public Optional<ArmorSetPiece> getArmorSetPiece() {
        return Optional.ofNullable(this.armorSetPiece);
    }

    public Prize setArmorSetPiece(final ArmorSetPiece armorSetPiece) {
        this.armorSetPiece = armorSetPiece;

        return this;
    }
    
    /**
     * Check if the items from Items: drop to the floor or go into the player's inventory.
     *
     * @return True if drops to the ground and false if it goes into their inventory.
     */
    public boolean getDropItems() {
        return this.dropItems;
    }
    
    /**
     * Make the items from the Items: option either drop on the ground or go into their inventory.
     *
     * @param dropItems The option to drop items on the floor or into their inventory.
     */
    public Prize setDropItems(boolean dropItems) {
        this.dropItems = dropItems;

        return this;
    }
    
    /**
     * Get the messages that are sent to the player when winning.
     *
     * @return The messages that are sent to the player.
     */
    public List<String> getMessages() {
        return List.copyOf(this.messages);
    }
    
    /**
     * Set the messages that the player gets.
     *
     * @param messages The new messages the player gets. This will auto color code the messages.
     */
    public Prize setMessages(@NotNull final List<String> messages) {
        this.messages = List.copyOf(messages);

        return this;
    }
    
    /**
     * Get the list of commands the prize runs.
     */
    public List<String> getCommands() {
        return List.copyOf(this.commands);
    }
    
    /**
     * Set the list of commands the prize will run.
     *
     * @param commands List of commands to be run.
     */
    public Prize setCommands(@NotNull final List<String> commands) {
        this.commands = List.copyOf(commands);

        return this;
    }
    
    /**
     * Get the items that the prize will give.
     *
     * @return The items that are won in the prize.
     */
    public List<ItemStack> getItems() {
        return this.builders.stream()
                .map(ItemBuilder::asItemStack)
                .map(ItemStack::clone)
                .toList();
    }

    /**
     * Materialize this prize for a player without exposing an ItemBuilder cached stack.
     *
     * @param player player used to resolve item placeholders
     * @return independent item copies
     */
    public List<ItemStack> getItems(@NotNull final Player player) {
        return this.builders.stream()
                .map(builder -> builder.asItemStack(player))
                .map(ItemStack::clone)
                .toList();
    }

    public List<ItemBuilder> getItemBuilders() {
        return List.copyOf(this.builders);
    }
    
    public Prize setItemBuilders(@NotNull final List<ItemBuilder> itemBuilders) {
        this.builders = List.copyOf(itemBuilders);

        return this;
    }
}
