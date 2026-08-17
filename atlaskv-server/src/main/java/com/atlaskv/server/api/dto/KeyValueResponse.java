package com.atlaskv.server.api.dto;

/**
 * Response DTO for key-value read/write operations.
 *
 * @param key the key
 * @param value the value (null for deleted/not-found)
 * @param found true if the key was found
 * @param version current version of the key
 * @param createdAt timestamp when the key was created
 * @param updatedAt timestamp when the key was last modified
 * @param ttlRemaining remaining time to live in milliseconds (if TTL/lease attached)
 * @param leaseId associated lease ID (if lease attached)
 */
public record KeyValueResponse(
        String key,
        String value,
        boolean found,
        Long version,
        Long createdAt,
        Long updatedAt,
        Long ttlRemaining,
        String leaseId
) {
    /**
     * Backward-compatible 3-arg constructor.
     *
     * @param key   the key
     * @param value the value
     * @param found true if found
     */
    public KeyValueResponse(String key, String value, boolean found) {
        this(key, value, found, null, null, null, null, null);
    }

    /**
     * Backward-compatible 6-arg constructor.
     *
     * @param key       the key
     * @param value     the value
     * @param found     true if found
     * @param version   the version
     * @param createdAt creation timestamp
     * @param updatedAt update timestamp
     */
    public KeyValueResponse(String key, String value, boolean found, Long version, Long createdAt, Long updatedAt) {
        this(key, value, found, version, createdAt, updatedAt, null, null);
    }
}
