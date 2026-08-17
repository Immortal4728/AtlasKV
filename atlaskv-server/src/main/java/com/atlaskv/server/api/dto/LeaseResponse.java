package com.atlaskv.server.api.dto;

import java.util.Set;

/**
 * Response DTO for lease operations and history audit.
 *
 * @param leaseId          lease ID
 * @param durationMs       lease duration in milliseconds
 * @param expiryTimeMs     absolute expiry timestamp
 * @param keys             keys currently or previously associated with the lease
 * @param status           lease lifecycle status (ACTIVE, EXPIRED, REVOKED)
 * @param createdAtMs      timestamp when the lease was created
 * @param lastActionTimeMs timestamp when the lease was last acted upon (renewed, expired, revoked)
 */
public record LeaseResponse(
        String leaseId,
        long durationMs,
        long expiryTimeMs,
        Set<String> keys,
        String status,
        Long createdAtMs,
        Long lastActionTimeMs
) {
    /**
     * Backwards-compatible constructor for 4-argument responses.
     */
    public LeaseResponse(String leaseId, long durationMs, long expiryTimeMs, Set<String> keys) {
        this(leaseId, durationMs, expiryTimeMs, keys, "ACTIVE", expiryTimeMs - durationMs, expiryTimeMs);
    }
}
