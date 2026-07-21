package com.atlaskv.server.metrics;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe metrics recorder for lease operations.
 */
@Component
public class LeaseMetrics {

    private final AtomicLong activeLeases = new AtomicLong();
    private final AtomicLong expiredLeases = new AtomicLong();
    private final AtomicLong renewals = new AtomicLong();
    private final AtomicLong totalLeaseDurationMs = new AtomicLong();
    private final AtomicLong totalLeasesCreated = new AtomicLong();

    /**
     * Records the creation of a new lease with its duration.
     *
     * @param durationMs lease duration in milliseconds
     */
    public void recordLeaseCreated(long durationMs) {
        activeLeases.incrementAndGet();
        totalLeasesCreated.incrementAndGet();
        totalLeaseDurationMs.addAndGet(durationMs);
    }

    /**
     * Records the expiration of a lease.
     */
    public void recordLeaseExpired() {
        expiredLeases.incrementAndGet();
        activeLeases.updateAndGet(val -> Math.max(0, val - 1));
    }

    /**
     * Records the manual revocation of a lease.
     */
    public void recordLeaseRevoked() {
        activeLeases.updateAndGet(val -> Math.max(0, val - 1));
    }

    /**
     * Records a lease renewal.
     */
    public void recordRenewal() {
        renewals.incrementAndGet();
    }

    /**
     * Returns the current number of active leases.
     *
     * @return active leases
     */
    public long activeLeases() {
        return activeLeases.get();
    }

    /**
     * Returns the total count of expired leases.
     *
     * @return expired leases
     */
    public long expiredLeases() {
        return expiredLeases.get();
    }

    /**
     * Returns the total count of renewals.
     *
     * @return renewals count
     */
    public long renewals() {
        return renewals.get();
    }

    /**
     * Returns the average duration of leases in milliseconds.
     *
     * @return average duration
     */
    public double averageLeaseDurationMs() {
        long count = totalLeasesCreated.get();
        return count > 0 ? (double) totalLeaseDurationMs.get() / count : 0.0;
    }
}
