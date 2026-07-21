package com.atlaskv.server.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for key-value write operations.
 *
 * @param value   the value to store
 * @param ttl     optional duration string (e.g. 30s)
 * @param leaseId optional lease ID to associate
 */
public record KeyValueRequest(
        @NotNull(message = "Value must not be null")
        String value,
        String ttl,
        String leaseId
) {
    /**
     * Helper constructor for backward compatibility.
     *
     * @param value the value to store
     */
    public KeyValueRequest(String value) {
        this(value, null, null);
    }
}
