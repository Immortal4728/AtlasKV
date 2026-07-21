package com.atlaskv.server.api.dto;

/**
 * Single entry in a prefix query result.
 *
 * @param key          the full key
 * @param value        the value
 * @param version      current version of the key
 * @param createdAt    timestamp when the key was created
 * @param updatedAt    timestamp when the key was last modified
 * @param ttlRemaining remaining TTL in milliseconds, null if no TTL
 * @param leaseId      lease ID if bound, null otherwise
 */
public record PrefixEntry(
        String key,
        String value,
        Long version,
        Long createdAt,
        Long updatedAt,
        Long ttlRemaining,
        String leaseId,
        java.util.List<RevisionResponse> history
) {
    public PrefixEntry(String key, String value, Long version, Long createdAt, Long updatedAt,
                       Long ttlRemaining, String leaseId) {
        this(key, value, version, createdAt, updatedAt, ttlRemaining, leaseId, null);
    }
}
