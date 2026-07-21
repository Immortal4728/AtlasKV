package com.atlaskv.server.metrics;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe metrics recorder for Compare-And-Swap (CAS) operations.
 */
@Component
public class CasMetrics {

    private final AtomicLong attempts = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong totalLatencyNs = new AtomicLong();

    /**
     * Records a CAS attempt.
     */
    public void recordAttempt() {
        attempts.incrementAndGet();
    }

    /**
     * Records a successful CAS operation.
     */
    public void recordSuccess() {
        successes.incrementAndGet();
    }

    /**
     * Records a failed CAS operation.
     */
    public void recordFailure() {
        failures.incrementAndGet();
    }

    /**
     * Records the execution latency of a CAS operation.
     *
     * @param durationNs latency in nanoseconds
     */
    public void recordLatency(long durationNs) {
        totalLatencyNs.addAndGet(durationNs);
    }

    /**
     * Returns total CAS attempts.
     *
     * @return attempts count
     */
    public long totalAttempts() {
        return attempts.get();
    }

    /**
     * Returns successful CAS count.
     *
     * @return successes count
     */
    public long successes() {
        return successes.get();
    }

    /**
     * Returns failed CAS count.
     *
     * @return failures count
     */
    public long failures() {
        return failures.get();
    }

    /**
     * Returns the average latency of CAS operations in milliseconds.
     *
     * @return average latency in ms
     */
    public double averageLatencyMs() {
        long count = attempts.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalLatencyNs.get() / count / 1_000_000.0;
    }
}
