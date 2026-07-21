package com.atlaskv.sdk.models;

/**
 * Immutable representations of all cluster and state machine metrics.
 *
 * @param nodeId                         identity of this node
 * @param currentTerm                    current term
 * @param commitIndex                    highest committed log index
 * @param lastApplied                    highest applied log index
 * @param logLength                      total log entries
 * @param snapshotLastIndex              last index in snapshot
 * @param snapshotLastTerm               last term in snapshot
 * @param kvStoreSize                    number of active keys
 * @param uptimeMs                       uptime in milliseconds
 * @param totalReadRequests              total read index requests
 * @param successfulReadRequests          successful read index requests
 * @param averageReadLatencyMs           average read latency
 * @param membershipChangeCount          total configuration changes
 * @param averageMembershipChangeLatencyMs average membership change latency
 * @param totalCasAttempts               total CAS operations
 * @param successfulCasRequests          successful CAS operations
 * @param failedCasRequests              failed CAS operations
 * @param averageCasLatencyMs            average CAS latency
 * @param prefixQueryCount               total prefix query operations
 * @param averagePrefixLatencyMs         average prefix query latency
 * @param averagePrefixResultSize        average prefix result size
 * @param historyReads                   total history read queries
 * @param historyWrites                  total history write queries
 * @param rollbackCount                  total rollback operations
 * @param averageHistorySize             average revision history size
 */
public record Metrics(
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
        double averageHistorySize
) {}
