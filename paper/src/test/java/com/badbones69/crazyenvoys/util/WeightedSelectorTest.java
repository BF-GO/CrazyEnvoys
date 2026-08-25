package com.badbones69.crazyenvoys.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedSelectorTest {

    private static final List<WeightedSelector.Entry<String>> TIERS = List.of(
            new WeightedSelector.Entry<>("Basic", 58, 0),
            new WeightedSelector.Entry<>("Lucky", 25, 5),
            new WeightedSelector.Entry<>("Titan", 10, 3),
            new WeightedSelector.Entry<>("Cursed", 5, 2),
            new WeightedSelector.Entry<>("Doom", 2, 1)
    );

    @Test
    void fillsEveryConfiguredEventWithoutExceedingTierCaps() {
        for (int amount = 7; amount <= 20; amount++) {
            for (int seed = 0; seed < 100; seed++) {
                final List<String> selected = WeightedSelector.select(TIERS, amount, new Random(seed));
                final Map<String, Long> counts = selected.stream().collect(Collectors.groupingBy(
                        Function.identity(), Collectors.counting()
                ));

                assertEquals(amount, selected.size());
                assertTrue(counts.getOrDefault("Lucky", 0L) <= 5L);
                assertTrue(counts.getOrDefault("Titan", 0L) <= 3L);
                assertTrue(counts.getOrDefault("Cursed", 0L) <= 2L);
                assertTrue(counts.getOrDefault("Doom", 0L) <= 1L);
            }
        }
    }

    @Test
    void ignoresZeroWeightsAndStopsAtFiniteCapacity() {
        final List<String> selected = WeightedSelector.select(List.of(
                new WeightedSelector.Entry<>("disabled", 0, 0),
                new WeightedSelector.Entry<>("first", 1, 2),
                new WeightedSelector.Entry<>("second", 1, 1)
        ), 10, new Random(4));

        assertEquals(3, selected.size());
        assertFalse(selected.contains("disabled"));
    }

    @Test
    void nonEmptyPrizePoolAlwaysReturnsOnePrize() {
        final List<WeightedSelector.Entry<String>> disabledWeights = List.of(
                new WeightedSelector.Entry<>("first", 0, 1),
                new WeightedSelector.Entry<>("second", -1, 1)
        );

        assertTrue(WeightedSelector.selectOneOrRandom(disabledWeights, new Random(9)).isPresent());
        assertTrue(WeightedSelector.selectOneOrRandom(List.of(), new Random(9)).isEmpty());
        assertEquals("only", WeightedSelector.selectOneOrRandom(List.of(
                new WeightedSelector.Entry<>("only", 100, 1)
        ), new Random(9)).orElseThrow());
    }
}
