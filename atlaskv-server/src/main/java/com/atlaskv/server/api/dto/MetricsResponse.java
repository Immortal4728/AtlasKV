package com.atlaskv.server.api.dto;

/**
 * Response DTO for cluster metrics endpoint.
 *
 * @param nodeId identity of this node
 * @param currentTerm current Raft term
 * @param commitIndex highest committed log index
 * @param lastApplied highest applied log index
 * @param logLength total log entries
 * @param snapshotLastIndex last snapshot included index
 * @param snapshotLastTerm last snapshot included term
 * @param kvStoreSize number of keys in the KV store
 * @param uptimeMs uptime in milliseconds
 * @param totalReadRequests total ReadIndex read requests
 * @param successfulReadRequests successful ReadIndex read requests
 * @param averageReadLatencyMs average read latency in milliseconds
 * @param membershipChangeCount total membership changes
 * @param averageMembershipChangeLatencyMs average membership change latency in milliseconds
 */
public record MetricsResponse(
        String nodeId,
        long currentTerm,
        long commitIndex,
        long lastApplied,
        long logLength,
        long snapshotLastIndex,
        long snapshotLastTerm,
        int kvStoreSize,
        long uptimeMs,
        long totalReadRequests,
        long successfulReadRequests,
        double averageReadLatencyMs,
        long membershipChangeCount,
        double averageMembershipChangeLatencyMs
) {
    public MetricsResponse(String nodeId, long currentTerm, long commitIndex, long lastApplied,
                           long logLength, long snapshotLastIndex, long snapshotLastTerm,
                           int kvStoreSize, long uptimeMs) {
        this(nodeId, currentTerm, commitIndex, lastApplied, logLength, snapshotLastIndex,
                snapshotLastTerm, kvStoreSize, uptimeMs, 0L, 0L, 0.0, 0L, 0.0);
    }

    public MetricsResponse(String nodeId, long currentTerm, long commitIndex, long lastApplied,
                           long logLength, long snapshotLastIndex, long snapshotLastTerm,
                           int kvStoreSize, long uptimeMs, long totalReadRequests,
                           long successfulReadRequests, double averageReadLatencyMs) {
        this(nodeId, currentTerm, commitIndex, lastApplied, logLength, snapshotLastIndex,
                snapshotLastTerm, kvStoreSize, uptimeMs, totalReadRequests, successfulReadRequests,
                averageReadLatencyMs, 0L, 0.0);
    }
}
