package com.atlaskv.sdk.models;

/**
 * Immutable cluster status model.
 *
 * @param nodeId        identity of the queried node
 * @param role          current Raft role (LEADER, FOLLOWER, CANDIDATE)
 * @param currentTerm   current Raft consensus term
 * @param commitIndex   highest committed log index
 * @param lastApplied   highest applied log index
 * @param currentLeader current leader node ID (null if unknown)
 * @param healthy       true if the node is operational
 * @param uptimeMs      uptime in milliseconds
 * @param nodeState     lifecycle state of the node (e.g. STARTED, STOPPING)
 * @param grpcPort      gRPC server listening port
 * @param peerCount     number of configured peers in the cluster
 */
public record ClusterStatus(
        String nodeId,
        String role,
        long currentTerm,
        long commitIndex,
        long lastApplied,
        String currentLeader,
        boolean healthy,
        long uptimeMs,
        String nodeState,
        int grpcPort,
        int peerCount
) {}
