package com.badbones69.crazyenvoys.listeners.timer;

import com.ryderbelserion.fusion.paper.builders.folia.FoliaScheduler;
import com.ryderbelserion.fusion.paper.builders.folia.Scheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple countdown timer using the Runnable interface in seconds!
 *
 * @author ExpDev
 */
public class CountdownTimer extends FoliaScheduler {

    // Seconds and shiz
    private final int seconds;
    private final AtomicInteger secondsLeft;

    public CountdownTimer(final JavaPlugin plugin, final int seconds) {
        super(plugin, Scheduler.global_scheduler);

        this.seconds = seconds;
        this.secondsLeft = new AtomicInteger(seconds);
    }

    /**
     * Runs the timer once, decrements seconds etc...
     * Really wish we could make it protected/private, so you couldn't access it.
     */
    @Override
    public void run() {
        // Is the timer up?
        if (this.secondsLeft.get() < 1) {
            cancelSafely();

            return;
        }

        // Decrement the seconds left.
        this.secondsLeft.decrementAndGet();
    }

    /**
     * Gets the total seconds this timer was set to run for.
     *
     * @return Total seconds timer should run.
     */
    public int getTotalSeconds() {
        return this.seconds;
    }

    /**
     * Gets the seconds left this timer should run.
     *
     * @return Seconds left timer should run.
     */
    public int getSecondsLeft() {
        return this.secondsLeft.get();
    }

    /**
     * Schedules this instance to "run" every second.
     */
    public void scheduleTimer() {
        runAtFixedRate(0L, 20L);
    }

    public void cancelSafely() {
        if (getTask() == null) return;

        try {
            cancel();
        } catch (RuntimeException ignored) {
        }
    }
}
