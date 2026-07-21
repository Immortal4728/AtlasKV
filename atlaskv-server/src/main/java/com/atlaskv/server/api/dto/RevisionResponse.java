package com.atlaskv.server.api.dto;

/**
 * Response DTO representing a historical key revision.
 */
public record RevisionResponse(
        long revisionNumber,
        String value,
        long timestamp,
        String operation,
        String nodeId,
        String leaseId,
        String ttl
) {}
