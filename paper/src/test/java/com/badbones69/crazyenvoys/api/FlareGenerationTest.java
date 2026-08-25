package com.badbones69.crazyenvoys.api;

import com.badbones69.crazyenvoys.api.objects.EventSession;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlareGenerationTest {

    @Test
    void flareDropCountAlwaysStaysWithinConfiguredBounds() {
        final Set<Integer> results = new HashSet<>();

        for (int seed = 0; seed < 1_000; seed++) {
            final int selected = CrazyManager.selectDropCount(3, 6, new Random(seed));
            assertTrue(selected >= 3 && selected <= 6);
            results.add(selected);
        }

        assertEquals(Set.of(3, 4, 5, 6), results);
    }

    @Test
    void malformedDropBoundsAreNormalizedSafely() {
        assertEquals(0, CrazyManager.selectDropCount(-5, -1, new Random(1)));
        assertEquals(8, CrazyManager.selectDropCount(8, 3, new Random(1)));
        assertEquals(4, CrazyManager.selectDropCount(4, 4, new Random(1)));
    }

    @Test
    void spawnCountNeverExceedsAvailableRadiusArea() {
        assertEquals(6, CrazyManager.limitSpawnsToArea(6, 20, 250));
        assertEquals(3, CrazyManager.limitSpawnsToArea(10, 0, 1));
        assertEquals(0, CrazyManager.limitSpawnsToArea(6, 20, 20));
        assertEquals(0, CrazyManager.limitSpawnsToArea(-1, 20, 250));
    }

    @Test
    void onlyGlobalSessionsAffectTheAutomaticSchedule() {
        final EventSession global = new EventSession(1, null, EventSession.StartMode.GLOBAL, null);
        final EventSession.SpawnOrigin origin = new EventSession.SpawnOrigin(UUID.randomUUID(), 1200, -450);
        final EventSession flare = new EventSession(2, "player", EventSession.StartMode.FLARE, origin);

        assertTrue(global.affectsGlobalSchedule());
        assertFalse(flare.affectsGlobalSchedule());
        assertEquals(origin, flare.spawnOrigin());
        assertThrows(IllegalArgumentException.class, () ->
                new EventSession(3, "player", EventSession.StartMode.FLARE, null)
        );
    }
}
