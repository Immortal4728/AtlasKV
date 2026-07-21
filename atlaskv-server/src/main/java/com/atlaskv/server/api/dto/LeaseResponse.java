package com.atlaskv.server.api.dto;

import java.util.Set;

/**
 * Response DTO for lease operations.
 *
 * @param leaseId      lease ID
 * @param durationMs   lease duration in milliseconds
 * @param expiryTimeMs absolute expiry timestamp
 * @param keys         keys currently associated with the lease
 */
public record LeaseResponse(
        String leaseId,
        long durationMs,
        long expiryTimeMs,
        Set<String> keys
) {}
