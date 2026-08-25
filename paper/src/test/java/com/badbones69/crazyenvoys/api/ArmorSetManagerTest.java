package com.badbones69.crazyenvoys.api;

import com.badbones69.crazyenvoys.api.objects.misc.ArmorSetPiece;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorSetManagerTest {

    @Test
    void completeMatchingSetIsRecognized() {
        assertEquals("doom", ArmorSetManager.findCompleteSetId(completeSet("doom"), Set.of("doom")));
    }

    @Test
    void missingMixedForgedAndUnknownSetsAreRejected() {
        final Map<ArmorSetPiece, ArmorSetManager.ArmorMarker> missing = completeSet("doom");
        missing.remove(ArmorSetPiece.FEET);
        assertNull(ArmorSetManager.findCompleteSetId(missing, Set.of("doom")));

        final Map<ArmorSetPiece, ArmorSetManager.ArmorMarker> mixed = completeSet("doom");
        mixed.put(ArmorSetPiece.FEET, marker("cursed", ArmorSetPiece.FEET));
        assertNull(ArmorSetManager.findCompleteSetId(mixed, Set.of("doom", "cursed")));

        final Map<ArmorSetPiece, ArmorSetManager.ArmorMarker> forgedSlot = completeSet("doom");
        forgedSlot.put(ArmorSetPiece.HEAD, marker("doom", ArmorSetPiece.CHEST));
        assertNull(ArmorSetManager.findCompleteSetId(forgedSlot, Set.of("doom")));

        assertNull(ArmorSetManager.findCompleteSetId(completeSet("unknown"), Set.of("doom")));
    }

    @Test
    void configuredPiecesOnlyMatchTheirArmorSlot() {
        assertTrue(ArmorSetPiece.HEAD.matches(Material.NETHERITE_HELMET));
        assertTrue(ArmorSetPiece.CHEST.matches(Material.DIAMOND_CHESTPLATE));
        assertTrue(ArmorSetPiece.LEGS.matches(Material.IRON_LEGGINGS));
        assertTrue(ArmorSetPiece.FEET.matches(Material.LEATHER_BOOTS));
        assertNull(ArmorSetPiece.fromConfig("helmet").orElse(null));
    }

    @Test
    void strongerAndLongerExternalEffectsArePreserved() {
        assertFalse(ArmorSetManager.shouldApplyEffect(1, 2, 20));
        assertFalse(ArmorSetManager.shouldApplyEffect(1, 0, 200));
        assertFalse(ArmorSetManager.shouldApplyEffect(1, 1, 80));
        assertTrue(ArmorSetManager.shouldApplyEffect(1, 0, 80));
        assertTrue(ArmorSetManager.shouldApplyEffect(1, 1, 60));
    }

    private static Map<ArmorSetPiece, ArmorSetManager.ArmorMarker> completeSet(final String id) {
        final Map<ArmorSetPiece, ArmorSetManager.ArmorMarker> result = new EnumMap<>(ArmorSetPiece.class);
        for (final ArmorSetPiece piece : ArmorSetPiece.values()) result.put(piece, marker(id, piece));
        return result;
    }

    private static ArmorSetManager.ArmorMarker marker(final String id, final ArmorSetPiece piece) {
        return new ArmorSetManager.ArmorMarker(id, piece.getConfigName());
    }
}
