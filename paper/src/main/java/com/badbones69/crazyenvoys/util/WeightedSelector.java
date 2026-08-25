package com.badbones69.crazyenvoys.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public final class WeightedSelector {

    private WeightedSelector() {}

    public record Entry<T>(@NotNull T value, int weight, int maximumSelections) {}

    public static <T> @NotNull Optional<T> selectOneOrRandom(
            @NotNull final List<Entry<T>> entries,
            @NotNull final RandomGenerator random
    ) {
        final List<T> selected = select(entries, 1, random);
        if (!selected.isEmpty()) return Optional.of(selected.getFirst());
        if (entries.isEmpty()) return Optional.empty();

        return Optional.of(entries.get(random.nextInt(entries.size())).value());
    }

    public static <T> @NotNull List<T> select(
            @NotNull final List<Entry<T>> entries,
            final int requested,
            @NotNull final RandomGenerator random
    ) {
        if (requested <= 0 || entries.isEmpty()) return List.of();

        final int[] selectedCounts = new int[entries.size()];
        final List<T> selected = new ArrayList<>(requested);

        while (selected.size() < requested) {
            long totalWeight = 0L;

            for (int index = 0; index < entries.size(); index++) {
                final Entry<T> entry = entries.get(index);
                if (entry.weight() <= 0) continue;
                if (entry.maximumSelections() > 0 && selectedCounts[index] >= entry.maximumSelections()) continue;

                totalWeight = Long.MAX_VALUE - totalWeight < entry.weight()
                        ? Long.MAX_VALUE
                        : totalWeight + entry.weight();
            }

            if (totalWeight <= 0L) break;

            long roll = random.nextLong(totalWeight);

            for (int index = 0; index < entries.size(); index++) {
                final Entry<T> entry = entries.get(index);
                if (entry.weight() <= 0) continue;
                if (entry.maximumSelections() > 0 && selectedCounts[index] >= entry.maximumSelections()) continue;

                if (roll < entry.weight()) {
                    selected.add(entry.value());
                    selectedCounts[index]++;
                    break;
                }

                roll -= entry.weight();
            }
        }

        return List.copyOf(selected);
    }
}
