package com.atlaskv.server.api.dto;

/**
 * Response DTO for cluster metrics endpoint.
 *
 * @param nodeId                         identity of this node
 * @param currentTerm                    current Raft term
 * @param commitIndex                    highest committed log index
 * @param lastApplied                    highest applied log index
 * @param logLength                      total log entries
 * @param snapshotLastIndex              last snapshot included index
 * @param snapshotLastTerm               last snapshot included term
 * @param kvStoreSize                    number of keys in the KV store
 * @param uptimeMs                       uptime in milliseconds
 * @param totalReadRequests              total ReadIndex read requests
 * @param successfulReadRequests          successful ReadIndex read requests
 * @param averageReadLatencyMs           average read latency in milliseconds
 * @param membershipChangeCount          total membership changes
 * @param averageMembershipChangeLatencyMs average membership change latency in milliseconds
 * @param totalCasAttempts               total CAS attempts
 * @param successfulCasRequests          successful CAS requests
 * @param failedCasRequests              failed CAS requests
 * @param averageCasLatencyMs            average CAS latency in milliseconds
 * @param prefixQueryCount               total prefix queries
 * @param averagePrefixLatencyMs         average prefix query latency
 * @param averagePrefixResultSize        average prefix query result size
 * @param historyReads                   total key history read operations
 * @param historyWrites                  total key history write revisions created
 * @param rollbackCount                  total history rollbacks executed
 * @param averageHistorySize             average revision count per historical key
 * @param activeWatchers                 number of active SSE watcher subscriptions
 * @param totalEventsDelivered           cumulative watch events delivered
 * @param totalWatchConnections          total watcher connection attempts
 * @param activeLeases                   current active leases
 * @param expiredLeases                  cumulative expired leases
 * @param leaseRenewals                  cumulative lease renewals
 * @param averageLeaseDurationMs         average lease TTL in milliseconds
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
        double averageMembershipChangeLatencyMs,
        long totalCasAttempts,
        long successfulCasRequests,
        long failedCasRequests,
        double averageCasLatencyMs,
        long prefixQueryCount,
        double averagePrefixLatencyMs,
        double averagePrefixResultSize,
        long historyReads,
        long historyWrites,
        long rollbackCount,
        double averageHistorySize,
        long activeWatchers,
        long totalEventsDelivered,
        long totalWatchConnections,
        long activeLeases,
        long expiredLeases,
        long leaseRenewals,
        double averageLeaseDurationMs
) {
    /**
     * Helper constructor with basic arguments.
     */
    public MetricsResponse(String nodeId, long currentTerm, long commitIndex, long lastApplied,
                           long logLength, long snapshotLastIndex, long snapshotLastTerm,
                           int kvStoreSize, long uptimeMs) {
        this(nodeId, currentTerm, commitIndex, lastApplied, logLength, snapshotLastIndex,
                snapshotLastTerm, kvStoreSize, uptimeMs, 0L, 0L, 0.0, 0L, 0.0,
                0L, 0L, 0L, 0.0, 0L, 0.0, 0.0, 0L, 0L, 0L, 0.0,
                0L, 0L, 0L, 0L, 0L, 0L, 0.0);
    }

    /**
     * Helper constructor with read metrics.
     */
    public MetricsResponse(String nodeId, long currentTerm, long commitIndex, long lastApplied,
                           long logLength, long snapshotLastIndex, long snapshotLastTerm,
                           int kvStoreSize, long uptimeMs, long totalReadRequests,
                           long successfulReadRequests, double averageReadLatencyMs) {
        this(nodeId, currentTerm, commitIndex, lastApplied, logLength, snapshotLastIndex,
                snapshotLastTerm, kvStoreSize, uptimeMs, totalReadRequests, successfulReadRequests,
                averageReadLatencyMs, 0L, 0.0, 0L, 0L, 0L, 0.0, 0L, 0.0, 0.0, 0L, 0L, 0L, 0.0,
                0L, 0L, 0L, 0L, 0L, 0L, 0.0);
    }

    /**
     * 25-parameter backwards compatibility constructor.
     */
    public MetricsResponse(
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
            double averageMembershipChangeLatencyMs,
            long totalCasAttempts,
            long successfulCasRequests,
            long failedCasRequests,
            double averageCasLatencyMs,
            long prefixQueryCount,
            double averagePrefixLatencyMs,
            double averagePrefixResultSize,
            long historyReads,
            long historyWrites,
            long rollbackCount,
            double averageHistorySize) {
        this(nodeId, currentTerm, commitIndex, lastApplied, logLength, snapshotLastIndex,
                snapshotLastTerm, kvStoreSize, uptimeMs, totalReadRequests, successfulReadRequests,
                averageReadLatencyMs, membershipChangeCount, averageMembershipChangeLatencyMs,
                totalCasAttempts, successfulCasRequests, failedCasRequests, averageCasLatencyMs,
                prefixQueryCount, averagePrefixLatencyMs, averagePrefixResultSize,
                historyReads, historyWrites, rollbackCount, averageHistorySize,
                0L, 0L, 0L, 0L, 0L, 0L, 0.0);
    }
}
