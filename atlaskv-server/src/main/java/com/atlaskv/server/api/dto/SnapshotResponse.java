package com.atlaskv.server.api.dto;

/**
 * Response DTO for snapshot operations.
 *
 * @param success true if snapshot was taken successfully
 * @param lastIncludedIndex last log index included in snapshot
 * @param lastIncludedTerm term at lastIncludedIndex
 */
public record SnapshotResponse(
        boolean success,
        long lastIncludedIndex,
        long lastIncludedTerm
) {
}
