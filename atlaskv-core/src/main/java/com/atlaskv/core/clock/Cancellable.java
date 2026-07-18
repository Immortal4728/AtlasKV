package com.atlaskv.core.clock;

/**
 * Handle allowing cancellation of a scheduled task.
 */
@FunctionalInterface
public interface Cancellable {

    /**
     * Cancels the scheduled task.
     *
     * @return true if task was successfully cancelled, false if already executed or cancelled
     */
    boolean cancel();
}
