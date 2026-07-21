package com.atlaskv.sdk.models;

import java.util.Set;

/**
 * Immutable representation of an active distributed lease.
 *
 * @param leaseId      unique lease ID
 * @param durationMs   duration of the lease in milliseconds
 * @param expiryTimeMs absolute expiry timestamp in milliseconds
 * @param keys         set of keys currently associated with this lease
 */
public record Lease(
        String leaseId,
        long durationMs,
        long expiryTimeMs,
        Set<String> keys
) {}
