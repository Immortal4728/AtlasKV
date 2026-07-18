package com.atlaskv.server.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe metrics recorder for Raft ReadIndex linearizable read operations.
 */
@Component
public class ReadMetrics {

    private final AtomicLong totalReadRequests = new AtomicLong();
    private final AtomicLong successfulReadRequests = new AtomicLong();
    private final AtomicLong totalReadLatencyMs = new AtomicLong();
    private final AtomicLong lastReadLatencyMs = new AtomicLong();

    /**
     * Records a completed linearizable read.
     *
     * @param latencyMs execution latency in milliseconds
     */
    public void recordReadSuccess(long latencyMs) {
        totalReadRequests.incrementAndGet();
        successfulReadRequests.incrementAndGet();
        totalReadLatencyMs.addAndGet(latencyMs);
        lastReadLatencyMs.set(latencyMs);
    }

    /**
     * Records a failed read request.
     */
    public void recordReadFailure() {
        totalReadRequests.incrementAndGet();
    }

    /**
     * Returns total read requests processed.
     *
     * @return total requests
     */
    public long totalReadRequests() {
        return totalReadRequests.get();
    }

    /**
     * Returns successful read requests count.
     *
     * @return successful requests
     */
    public long successfulReadRequests() {
        return successfulReadRequests.get();
    }

    /**
     * Returns average read latency in milliseconds.
     *
     * @return average latency or 0.0 if no reads recorded
     */
    public double averageReadLatencyMs() {
        long successCount = successfulReadRequests.get();
        return successCount > 0 ? (double) totalReadLatencyMs.get() / successCount : 0.0;
    }

    /**
     * Returns the latency of the most recent read operation.
     *
     * @return last read latency in milliseconds
     */
    public long lastReadLatencyMs() {
        return lastReadLatencyMs.get();
    }
}
