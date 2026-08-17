package com.atlaskv.server.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for lease creation.
 *
 * @param leaseId optional lease ID (auto-generated if null/blank)
 * @param ttl     TTL duration (e.g. 30s)
 */
public record LeaseRequest(
        String leaseId,
        @NotBlank(message = "TTL must not be blank")
        String ttl
) {
    public LeaseRequest(String ttl) {
        this(null, ttl);
    }
}
