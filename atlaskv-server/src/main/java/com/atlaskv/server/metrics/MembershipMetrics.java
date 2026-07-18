package com.atlaskv.server.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe metrics recorder for Raft cluster membership changes.
 */
@Component
public class MembershipMetrics {

    private final AtomicLong totalMembershipChanges = new AtomicLong();
    private final AtomicLong totalMembershipChangeLatencyMs = new AtomicLong();
    private final AtomicLong lastMembershipChangeLatencyMs = new AtomicLong();

    /**
     * Records a completed cluster membership change.
     *
     * @param latencyMs execution latency in milliseconds
     */
    public void recordMembershipChangeSuccess(long latencyMs) {
        totalMembershipChanges.incrementAndGet();
        totalMembershipChangeLatencyMs.addAndGet(latencyMs);
        lastMembershipChangeLatencyMs.set(latencyMs);
    }

    /**
     * Returns total membership changes processed.
     *
     * @return total membership change count
     */
    public long totalMembershipChanges() {
        return totalMembershipChanges.get();
    }

    /**
     * Returns average membership change latency in milliseconds.
     *
     * @return average latency or 0.0 if no changes recorded
     */
    public double averageMembershipChangeLatencyMs() {
        long count = totalMembershipChanges.get();
        return count > 0 ? (double) totalMembershipChangeLatencyMs.get() / count : 0.0;
    }

    /**
     * Returns latency of the most recent membership change.
     *
     * @return last change latency in milliseconds
     */
    public long lastMembershipChangeLatencyMs() {
        return lastMembershipChangeLatencyMs.get();
    }
}
