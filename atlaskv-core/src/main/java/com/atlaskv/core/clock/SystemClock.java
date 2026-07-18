package com.atlaskv.core.clock;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Real system clock implementation delegating to {@link System#currentTimeMillis()}
 * and a backing {@link ScheduledExecutorService} for timer scheduling.
 */
public final class SystemClock implements Clock, AutoCloseable {

    private final ScheduledExecutorService scheduler;

    /**
     * Constructs a SystemClock with a default single-threaded daemon executor.
     */
    public SystemClock() {
        this(Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "atlaskv-system-clock");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /**
     * Constructs a SystemClock with a custom backing scheduler.
     *
     * @param scheduler the ScheduledExecutorService to use
     */
    public SystemClock(ScheduledExecutorService scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "Scheduler must not be null");
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public Cancellable scheduleOnce(Duration delay, Runnable task) {
        Objects.requireNonNull(delay, "Delay must not be null");
        Objects.requireNonNull(task, "Task must not be null");

        ScheduledFuture<?> future = scheduler.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
