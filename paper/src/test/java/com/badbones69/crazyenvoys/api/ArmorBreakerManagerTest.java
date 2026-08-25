package com.badbones69.crazyenvoys.api;

import com.badbones69.crazyenvoys.api.objects.misc.RareEnchantmentDefinition;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorBreakerManagerTest {

    @Test
    void levelsRestoreExactlyFifteenThirtyAndFortyFivePercent() {
        assertEquals(0.15D, ArmorBreakerManager.penetration(1));
        assertEquals(0.30D, ArmorBreakerManager.penetration(2));
        assertEquals(0.45D, ArmorBreakerManager.penetration(3));

        assertEquals(-8.5D, ArmorBreakerManager.adjustedModifier(-10.0D, 1));
        assertEquals(-7.0D, ArmorBreakerManager.adjustedModifier(-10.0D, 2));
        assertEquals(-5.5D, ArmorBreakerManager.adjustedModifier(-10.0D, 3));

        final double armor = -8.0D;
        final double protection = -4.0D;
        for (int level = 1; level <= 3; level++) {
            final double restored = ArmorBreakerManager.adjustedModifier(armor, level) - armor
                    + ArmorBreakerManager.adjustedModifier(protection, level) - protection;
            assertEquals(12.0D * ArmorBreakerManager.penetration(level), restored, 1.0E-9D);
        }
    }

    @Test
    void nakedAndNonReductionModifiersAreUnchanged() {
        assertEquals(0.0D, ArmorBreakerManager.adjustedModifier(0.0D, 3));
        assertEquals(4.0D, ArmorBreakerManager.adjustedModifier(4.0D, 3));
        assertEquals(-10.0D, ArmorBreakerManager.adjustedModifier(-10.0D, 0));
    }

    @Test
    void allConfiguredWeaponAndToolFamiliesAreEligible() {
        for (final Material material : new Material[]{
                Material.IRON_SWORD, Material.NETHERITE_AXE, Material.DIAMOND_PICKAXE,
                Material.COPPER_SHOVEL, Material.GOLDEN_HOE, Material.SHEARS,
                Material.MACE, Material.TRIDENT, Material.BOW, Material.CROSSBOW
        }) {
            assertTrue(ArmorBreakerManager.isEligible(material), material.name());
        }

        assertFalse(ArmorBreakerManager.isEligible(Material.NETHERITE_CHESTPLATE));
        assertFalse(ArmorBreakerManager.isEligible(Material.SHIELD));
        assertFalse(ArmorBreakerManager.isEligible(Material.ENCHANTED_BOOK));
    }

    @Test
    void chanceBoundariesAndLevelsAreValidated() {
        final RareEnchantmentDefinition definition = new RareEnchantmentDefinition(true, 2.0D, 1);
        assertTrue(definition.succeeds(0.0D));
        assertTrue(definition.succeeds(1.999999D));
        assertFalse(definition.succeeds(2.0D));
        assertFalse(definition.succeeds(100.0D));

        final RareEnchantmentDefinition disabled = new RareEnchantmentDefinition(false, 99.0D, 3);
        assertFalse(disabled.enabled());
        assertEquals(0.0D, disabled.chance());
        assertEquals(0, disabled.level());

        assertThrows(IllegalArgumentException.class, () -> new RareEnchantmentDefinition(true, -0.1D, 1));
        assertThrows(IllegalArgumentException.class, () -> new RareEnchantmentDefinition(true, 100.1D, 1));
        assertThrows(IllegalArgumentException.class, () -> new RareEnchantmentDefinition(true, 1.0D, 0));
        assertThrows(IllegalArgumentException.class, () -> new RareEnchantmentDefinition(true, 1.0D, 4));
    }
}
