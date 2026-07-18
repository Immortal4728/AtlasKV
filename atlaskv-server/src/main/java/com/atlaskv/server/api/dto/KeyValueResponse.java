package com.atlaskv.server.api.dto;

/**
 * Response DTO for key-value read/write operations.
 *
 * @param key the key
 * @param value the value (null for deleted/not-found)
 * @param found true if the key was found
 */
public record KeyValueResponse(
        String key,
        String value,
        boolean found
) {
}
