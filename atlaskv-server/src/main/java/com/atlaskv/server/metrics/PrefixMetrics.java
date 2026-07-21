package com.atlaskv.server.metrics;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe metrics recorder for prefix query operations.
 */
@Component
public class PrefixMetrics {

    private final AtomicLong queryCount = new AtomicLong();
    private final AtomicLong totalLatencyNs = new AtomicLong();
    private final AtomicLong totalResultSize = new AtomicLong();

    /**
     * Records a completed prefix query.
     *
     * @param latencyNs  execution latency in nanoseconds
     * @param resultSize number of entries returned
     */
    public void recordQuery(long latencyNs, int resultSize) {
        queryCount.incrementAndGet();
        totalLatencyNs.addAndGet(latencyNs);
        totalResultSize.addAndGet(resultSize);
    }

    /**
     * Returns total prefix queries processed.
     *
     * @return query count
     */
    public long queryCount() {
        return queryCount.get();
    }

    /**
     * Returns average query latency in milliseconds.
     *
     * @return average latency in ms
     */
    public double averageLatencyMs() {
        long count = queryCount.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalLatencyNs.get() / count / 1_000_000.0;
    }

    /**
     * Returns the average result size per query.
     *
     * @return average result size
     */
    public double averageResultSize() {
        long count = queryCount.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalResultSize.get() / count;
    }
}
