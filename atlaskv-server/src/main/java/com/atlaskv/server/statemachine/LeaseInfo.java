package com.atlaskv.server.statemachine;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents info for a single active lease.
 */
public final class LeaseInfo {
    private final String leaseId;
    private final long durationMs;
    private volatile long expiryTimeMs;
    private final Set<String> keys = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a LeaseInfo.
     *
     * @param leaseId      lease ID
     * @param durationMs   lease duration in milliseconds
     * @param expiryTimeMs absolute expiry timestamp
     */
    public LeaseInfo(String leaseId, long durationMs, long expiryTimeMs) {
        this.leaseId = leaseId;
        this.durationMs = durationMs;
        this.expiryTimeMs = expiryTimeMs;
    }

    public String leaseId() {
        return leaseId;
    }

    public long durationMs() {
        return durationMs;
    }

    public long expiryTimeMs() {
        return expiryTimeMs;
    }

    public Set<String> keys() {
        return keys;
    }

    /**
     * Renews the lease, resetting the expiry time.
     *
     * @param now current timestamp in milliseconds
     */
    public void renew(long now) {
        this.expiryTimeMs = now + durationMs;
    }
}
