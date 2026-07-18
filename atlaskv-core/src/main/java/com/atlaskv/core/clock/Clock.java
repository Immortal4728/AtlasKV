package com.atlaskv.core.clock;

import java.time.Duration;

/**
 * Time abstraction for deterministic testing and timing control in the Raft consensus engine.
 */
public interface Clock {

    /**
     * Returns the current time in milliseconds since Epoch.
     *
     * @return current time in milliseconds
     */
    long currentTimeMillis();

    /**
     * Schedules a task to be executed once after the specified delay.
     *
     * @param delay duration after which task should run
     * @param task task to execute
     * @return Cancellable handle to cancel the scheduled task
     */
    Cancellable scheduleOnce(Duration delay, Runnable task);
}
