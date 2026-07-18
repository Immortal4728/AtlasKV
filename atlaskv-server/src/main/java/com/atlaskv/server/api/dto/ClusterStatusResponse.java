package com.atlaskv.server.api.dto;

import com.atlaskv.core.RaftRole;

/**
 * Response DTO for cluster status endpoint.
 *
 * @param nodeId identity of this node
 * @param role current Raft role
 * @param currentTerm current Raft term
 * @param commitIndex highest committed log index
 * @param lastApplied highest applied log index
 * @param currentLeader current leader node id (null if unknown)
 * @param healthy true if node is operational
 * @param uptimeMs uptime in milliseconds
 * @param nodeState lifecycle state of the node
 * @param grpcPort gRPC server listening port
 * @param peerCount number of configured peers
 */
public record ClusterStatusResponse(
        String nodeId,
        RaftRole role,
        long currentTerm,
        long commitIndex,
        long lastApplied,
        String currentLeader,
        boolean healthy,
        long uptimeMs,
        String nodeState,
        int grpcPort,
        int peerCount
) {
}
