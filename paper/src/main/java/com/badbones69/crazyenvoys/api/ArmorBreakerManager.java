package com.badbones69.crazyenvoys.api;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.badbones69.crazyenvoys.api.enums.Messages;
import com.badbones69.crazyenvoys.api.enums.PersistentKeys;
import com.badbones69.crazyenvoys.api.objects.misc.Prize;
import com.badbones69.crazyenvoys.api.objects.misc.RareEnchantmentDefinition;
import com.badbones69.crazyenvoys.bootstrap.ArmorBreakerBootstrap;
import com.ryderbelserion.fusion.core.api.enums.Level;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.random.RandomGenerator;

public final class ArmorBreakerManager implements Listener {

    private static final NamespacedKey ENCHANTMENT_KEY = NamespacedKey.fromString(ArmorBreakerBootstrap.ENCHANTMENT_ID);

    private final Enchantment enchantment;

    public ArmorBreakerManager(@NotNull final CrazyEnvoys plugin) {
        this.enchantment = ENCHANTMENT_KEY == null ? null : Registry.ENCHANTMENT.get(ENCHANTMENT_KEY);

        if (this.enchantment == null) {
            plugin.getFusion().log(Level.ERROR, Messages.log_armor_breaker_unavailable.getString());
        }
    }

    public MaterializedPrize materialize(
            @NotNull final Prize prize,
            @NotNull final Player player,
            @NotNull final RareEnchantmentDefinition definition,
            @NotNull final RandomGenerator random
    ) {
        final List<ItemStack> items = prize.getItems(player);
        if (this.enchantment == null || items.stream().noneMatch(item -> isEligible(item.getType()))) {
            return new MaterializedPrize(items, false, 0);
        }
        if (!definition.roll(random)) return new MaterializedPrize(items, false, 0);

        boolean applied = false;
        for (final ItemStack item : items) {
            if (!isEligible(item.getType())) continue;
            item.addUnsafeEnchantment(this.enchantment, definition.level());
            applied = true;
        }

        return new MaterializedPrize(items, applied, applied ? definition.level() : 0);
    }

    public int getLevel(@Nullable final ItemStack item) {
        if (this.enchantment == null || item == null || item.isEmpty()) return 0;
        return Math.min(RareEnchantmentDefinition.MAX_LEVEL, item.getEnchantmentLevel(this.enchantment));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(@NotNull final EntityShootBowEvent event) {
        final ItemStack bow = event.getBow();
        final Entity projectile = event.getProjectile();
        markProjectile(projectile, getLevel(bow));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(@NotNull final PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof Trident)) return;
        markProjectile(event.getProjectile(), getLevel(event.getItemStack()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onDamage(@NotNull final EntityDamageByEntityEvent event) {
        final int level = resolveAttackLevel(event);
        if (level <= 0) return;

        adjustModifier(event, EntityDamageEvent.DamageModifier.ARMOR, level);
        adjustModifier(event, EntityDamageEvent.DamageModifier.MAGIC, level);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvil(@NotNull final PrepareAnvilEvent event) {
        if (this.enchantment == null || event.getResult() == null || event.getResult().isEmpty()) return;

        final ItemStack first = event.getInventory().getFirstItem();
        final ItemStack second = event.getInventory().getSecondItem();
        final int inputLevel = Math.max(getLevel(first), getLevel(second));
        if (inputLevel <= 0 || !isEligible(event.getResult().getType())) return;

        final ItemStack result = event.getResult().clone();
        final int resultLevel = getLevel(result);
        if (resultLevel == inputLevel) return;

        result.removeEnchantment(this.enchantment);
        result.addUnsafeEnchantment(this.enchantment, inputLevel);
        event.setResult(result);
    }

    private void markProjectile(@NotNull final Entity projectile, final int level) {
        if (level <= 0) return;
        projectile.getPersistentDataContainer().set(
                PersistentKeys.armor_breaker_projectile.getNamespacedKey(),
                PersistentDataType.INTEGER,
                level
        );
    }

    private int resolveAttackLevel(@NotNull final EntityDamageByEntityEvent event) {
        final Entity direct = event.getDamageSource().getDirectEntity();
        int projectileLevel = getProjectileLevel(direct == null ? event.getDamager() : direct);
        if (projectileLevel <= 0 && direct != null && direct != event.getDamager()) {
            projectileLevel = getProjectileLevel(event.getDamager());
        }
        if (projectileLevel > 0) return projectileLevel;

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return 0;
        }

        if (!(event.getDamager() instanceof LivingEntity attacker)) return 0;
        final EntityEquipment equipment = attacker.getEquipment();
        return equipment == null ? 0 : getLevel(equipment.getItemInMainHand());
    }

    private int getProjectileLevel(@NotNull final Entity entity) {
        final Integer level = entity.getPersistentDataContainer().get(
                PersistentKeys.armor_breaker_projectile.getNamespacedKey(),
                PersistentDataType.INTEGER
        );
        return level == null ? 0 : Math.clamp(level, 0, RareEnchantmentDefinition.MAX_LEVEL);
    }

    @SuppressWarnings("deprecation")
    private static void adjustModifier(
            @NotNull final EntityDamageEvent event,
            @NotNull final EntityDamageEvent.DamageModifier modifier,
            final int level
    ) {
        if (!event.isApplicable(modifier)) return;
        final double current = event.getDamage(modifier);
        if (current >= 0.0D) return;
        event.setDamage(modifier, adjustedModifier(current, level));
    }

    static double penetration(final int level) {
        return switch (Math.clamp(level, 0, RareEnchantmentDefinition.MAX_LEVEL)) {
            case 1 -> 0.15D;
            case 2 -> 0.30D;
            case 3 -> 0.45D;
            default -> 0.0D;
        };
    }

    static double adjustedModifier(final double modifier, final int level) {
        return modifier < 0.0D ? modifier * (1.0D - penetration(level)) : modifier;
    }

    public static boolean isEligible(@NotNull final Material material) {
        if (material.isLegacy()) return false;
        final String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || material == Material.SHEARS
                || material == Material.MACE
                || material == Material.TRIDENT
                || material == Material.BOW
                || material == Material.CROSSBOW;
    }

    public static String romanLevel(final int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(level);
        };
    }

    public record MaterializedPrize(@NotNull List<ItemStack> items, boolean awarded, int level) {
        public MaterializedPrize {
            items = List.copyOf(items);
        }
    }
}
