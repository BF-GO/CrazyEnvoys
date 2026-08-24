package com.badbones69.crazyenvoys.scheduler;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.ryderbelserion.fusion.paper.builders.folia.FoliaScheduler;
import com.ryderbelserion.fusion.paper.builders.folia.Scheduler;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class SchedulerAdapter {

    private final CrazyEnvoys plugin;

    public SchedulerAdapter(@NotNull final CrazyEnvoys plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> runGlobal(@NotNull final String context, @NotNull final Runnable action) {
        return supplyGlobal(context, () -> {
            action.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supplyGlobal(@NotNull final String context, @NotNull final Supplier<T> action) {
        final CompletableFuture<T> future = new CompletableFuture<>();

        try {
            new FoliaScheduler(this.plugin, Scheduler.global_scheduler) {
                @Override
                public void run() {
                    complete(context, action, future);
                }
            }.runNow();
        } catch (Throwable throwable) {
            fail(context, throwable, future);
        }

        return future;
    }

    public CompletableFuture<Void> runRegion(@NotNull final Location location, @NotNull final String context, @NotNull final Runnable action) {
        return supplyRegion(location, context, () -> {
            action.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supplyRegion(@NotNull final Location location, @NotNull final String context, @NotNull final Supplier<T> action) {
        final CompletableFuture<T> future = new CompletableFuture<>();

        if (!location.isWorldLoaded()) {
            fail(context, new IllegalStateException("Cannot schedule region task for an unloaded world"), future);
            return future;
        }

        try {
            new FoliaScheduler(this.plugin, location) {
                @Override
                public void run() {
                    complete(context, action, future);
                }
            }.runNow();
        } catch (Throwable throwable) {
            fail(context, throwable, future);
        }

        return future;
    }

    public CompletableFuture<Void> runEntity(@NotNull final Entity entity, @NotNull final String context, @NotNull final Runnable action) {
        return supplyEntity(entity, context, () -> {
            action.run();
            return null;
        });
    }

    public CompletableFuture<Void> runEntityDelayed(@NotNull final Entity entity, final long delayTicks,
                                                     @NotNull final String context, @NotNull final Runnable action) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        final Runnable retired = () -> retire(context, future);

        try {
            new FoliaScheduler(this.plugin, retired, entity) {
                @Override
                public void run() {
                    complete(context, () -> {
                        action.run();
                        return null;
                    }, future);
                }
            }.runDelayed(delayTicks);
        } catch (Throwable throwable) {
            fail(context, throwable, future);
        }

        return future;
    }

    public <T> CompletableFuture<T> supplyEntity(@NotNull final Entity entity, @NotNull final String context, @NotNull final Supplier<T> action) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        final Runnable retired = () -> retire(context, future);

        try {
            new FoliaScheduler(this.plugin, retired, entity) {
                @Override
                public void run() {
                    complete(context, action, future);
                }
            }.runNow();
        } catch (Throwable throwable) {
            fail(context, throwable, future);
        }

        return future;
    }

    public CompletableFuture<Void> runAsync(@NotNull final String context, @NotNull final Runnable action) {
        return supplyAsync(context, () -> {
            action.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supplyAsync(@NotNull final String context, @NotNull final Supplier<T> action) {
        final CompletableFuture<T> future = new CompletableFuture<>();

        try {
            new FoliaScheduler(this.plugin, Scheduler.async_scheduler) {
                @Override
                public void run() {
                    complete(context, action, future);
                }
            }.runNow();
        } catch (Throwable throwable) {
            fail(context, throwable, future);
        }

        return future;
    }

    public CompletableFuture<Void> runForSender(@NotNull final CommandSender sender, @NotNull final String context, @NotNull final Runnable action) {
        if (sender instanceof Player player) return runEntity(player, context, action);
        if (sender instanceof Entity entity) return runEntity(entity, context, action);
        if (sender instanceof BlockCommandSender blockSender) return runRegion(blockSender.getBlock().getLocation(), context, action);

        return runGlobal(context, action);
    }

    private <T> void complete(@NotNull final String context, @NotNull final Supplier<T> action, @NotNull final CompletableFuture<T> future) {
        if (future.isDone()) return;

        try {
            future.complete(action.get());
        } catch (Throwable throwable) {
            fail(context, throwable, future);
        }
    }

    private <T> void fail(@NotNull final String context, @NotNull final Throwable throwable, @NotNull final CompletableFuture<T> future) {
        this.plugin.getLogger().log(Level.SEVERE, "Scheduled task failed: " + context, throwable);
        future.completeExceptionally(throwable);
    }

    private <T> void retire(@NotNull final String context, @NotNull final CompletableFuture<T> future) {
        final CancellationException exception = new CancellationException("Entity retired before task ran: " + context);
        this.plugin.getLogger().log(Level.FINE, exception.getMessage());
        future.completeExceptionally(exception);
    }
}
