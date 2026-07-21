package com.atlaskv.server.api.dto;

/**
 * Response DTO when a Compare-And-Swap (CAS) version mismatch occurs.
 *
 * @param expectedVersion the version the client expected
 * @param currentVersion  the actual current version of the key
 * @param reason          explanation of the conflict
 */
public record CasConflictResponse(
        long expectedVersion,
        long currentVersion,
        String reason
) {}
