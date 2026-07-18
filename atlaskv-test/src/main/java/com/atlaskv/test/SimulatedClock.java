package com.atlaskv.test;

import com.atlaskv.core.clock.Cancellable;
import com.atlaskv.core.clock.Clock;

import java.time.Duration;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Deterministic in-memory Clock implementation for testing.
 * Time only advances when explicitly triggered via {@link #advanceTime(Duration)} or {@link #advanceTo(long)}.
 */
public final class SimulatedClock implements Clock {

    private long currentTime;
    private long sequenceCounter = 0L;
    private final PriorityQueue<ScheduledTask> taskQueue = new PriorityQueue<>();

    /**
     * Constructs a SimulatedClock starting at time 0.
     */
    public SimulatedClock() {
        this(0L);
    }

    /**
     * Constructs a SimulatedClock starting at the specified epoch millis.
     *
     * @param initialTimeMillis initial epoch milliseconds
     */
    public SimulatedClock(long initialTimeMillis) {
        if (initialTimeMillis < 0) {
            throw new IllegalArgumentException("Initial time must be non-negative, got: " + initialTimeMillis);
        }
        this.currentTime = initialTimeMillis;
    }

    @Override
    public synchronized long currentTimeMillis() {
        return currentTime;
    }

    @Override
    public synchronized Cancellable scheduleOnce(Duration delay, Runnable task) {
        Objects.requireNonNull(delay, "Delay must not be null");
        Objects.requireNonNull(task, "Task must not be null");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("Delay must not be negative, got: " + delay);
        }

        long triggerTime = currentTime + delay.toMillis();
        ScheduledTask scheduledTask = new ScheduledTask(triggerTime, sequenceCounter++, task);
        taskQueue.add(scheduledTask);

        return () -> {
            synchronized (SimulatedClock.this) {
                if (scheduledTask.cancelled || scheduledTask.executed) {
                    return false;
                }
                scheduledTask.cancelled = true;
                return taskQueue.remove(scheduledTask);
            }
        };
    }

    /**
     * Advances simulated time by the given duration and executes ready tasks in order.
     *
     * @param duration time to advance
     */
    public synchronized void advanceTime(Duration duration) {
        Objects.requireNonNull(duration, "Duration must not be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Advance duration must not be negative, got: " + duration);
        }
        advanceTo(currentTime + duration.toMillis());
    }

    /**
     * Advances simulated time to a target timestamp and executes ready tasks in order.
     *
     * @param targetTimeMillis target epoch timestamp in milliseconds
     */
    public synchronized void advanceTo(long targetTimeMillis) {
        if (targetTimeMillis < currentTime) {
            throw new IllegalArgumentException("Target time " + targetTimeMillis
                    + " is in the past (current: " + currentTime + ")");
        }

        while (!taskQueue.isEmpty() && taskQueue.peek().triggerTime <= targetTimeMillis) {
            ScheduledTask task = taskQueue.poll();
            if (task.cancelled) {
                continue;
            }
            this.currentTime = task.triggerTime;
            task.executed = true;
            task.runnable.run();
        }
        this.currentTime = targetTimeMillis;
    }

    private static final class ScheduledTask implements Comparable<ScheduledTask> {
        private final long triggerTime;
        private final long sequence;
        private final Runnable runnable;
        private boolean cancelled = false;
        private boolean executed = false;

        ScheduledTask(long triggerTime, long sequence, Runnable runnable) {
            this.triggerTime = triggerTime;
            this.sequence = sequence;
            this.runnable = runnable;
        }

        @Override
        public int compareTo(ScheduledTask o) {
            int timeCompare = Long.compare(this.triggerTime, o.triggerTime);
            if (timeCompare != 0) {
                return timeCompare;
            }
            return Long.compare(this.sequence, o.sequence);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ScheduledTask that = (ScheduledTask) o;
            return triggerTime == that.triggerTime && sequence == that.sequence;
        }

        @Override
        public int hashCode() {
            return Objects.hash(triggerTime, sequence);
        }
    }
}
