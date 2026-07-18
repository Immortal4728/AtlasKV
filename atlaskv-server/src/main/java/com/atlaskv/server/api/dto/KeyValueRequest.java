package com.atlaskv.server.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for key-value write operations.
 *
 * @param value the value to store
 */
public record KeyValueRequest(
        @NotNull(message = "Value must not be null")
        String value
) {
}
