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
 */
public record KeyValueResponse(
        String key,
        String value,
        boolean found,
        Long version,
        Long createdAt,
        Long updatedAt
) {
    /**
     * Backward-compatible constructor.
     *
     * @param key   the key
     * @param value the value
     * @param found true if found
     */
    public KeyValueResponse(String key, String value, boolean found) {
        this(key, value, found, null, null, null);
    }
}
