package com.atlaskv.server.statemachine;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents info and lifecycle state for a distributed lease.
 */
public final class LeaseInfo {
    private final String leaseId;
    private final long durationMs;
    private final long createdAtMs;
    private volatile long expiryTimeMs;
    private volatile long lastActionTimeMs;
    private volatile LeaseStatus status;
    private final Set<String> keys = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a LeaseInfo with active status.
     *
     * @param leaseId      lease ID
     * @param durationMs   lease duration in milliseconds
     * @param expiryTimeMs absolute expiry timestamp
     */
    public LeaseInfo(String leaseId, long durationMs, long expiryTimeMs) {
        this(leaseId, durationMs, expiryTimeMs, System.currentTimeMillis(), expiryTimeMs, LeaseStatus.ACTIVE);
    }

    /**
     * Constructs a LeaseInfo with full lifecycle attributes.
     *
     * @param leaseId          lease ID
     * @param durationMs       lease duration in milliseconds
     * @param expiryTimeMs     absolute expiry timestamp
     * @param createdAtMs      creation timestamp in milliseconds
     * @param lastActionTimeMs last action timestamp in milliseconds
     * @param status           current lease status
     */
    public LeaseInfo(String leaseId, long durationMs, long expiryTimeMs,
                     long createdAtMs, long lastActionTimeMs, LeaseStatus status) {
        this.leaseId = leaseId;
        this.durationMs = durationMs;
        this.expiryTimeMs = expiryTimeMs;
        this.createdAtMs = createdAtMs;
        this.lastActionTimeMs = lastActionTimeMs;
        this.status = status != null ? status : LeaseStatus.ACTIVE;
    }

    public String leaseId() {
        return leaseId;
    }

    public long durationMs() {
        return durationMs;
    }

    public long createdAtMs() {
        return createdAtMs;
    }

    public long expiryTimeMs() {
        return expiryTimeMs;
    }

    public long lastActionTimeMs() {
        return lastActionTimeMs;
    }

    public LeaseStatus status() {
        return status;
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
        this.lastActionTimeMs = now;
        this.status = LeaseStatus.ACTIVE;
    }

    /**
     * Marks the lease as expired.
     *
     * @param now expiration timestamp in milliseconds
     */
    public void markExpired(long now) {
        this.status = LeaseStatus.EXPIRED;
        this.lastActionTimeMs = now;
    }

    /**
     * Marks the lease as manually revoked.
     *
     * @param now revocation timestamp in milliseconds
     */
    public void markRevoked(long now) {
        this.status = LeaseStatus.REVOKED;
        this.lastActionTimeMs = now;
    }
}
