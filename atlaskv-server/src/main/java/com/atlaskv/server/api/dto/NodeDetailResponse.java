package com.atlaskv.server.api.dto;

import com.atlaskv.core.RaftRole;

/**
 * Response DTO for detailed node information in the cluster.
 *
 * @param id node identifier
 * @param host network hostname
 * @param port REST HTTP port
 * @param grpcPort internal gRPC consensus port
 * @param role Raft role (LEADER, FOLLOWER, CANDIDATE)
 * @param healthy operational status
 * @param term current Raft term
 * @param commitIndex highest committed log index
 * @param appliedIndex highest applied log index
 * @param matchIndex replicated log index on this peer
 * @param nextIndex next log index to replicate to this peer
 * @param isLeader whether this node is the active Raft leader
 * @param isLocal whether this is the local node handling the request
 * @param latencyMs replication latency in milliseconds
 * @param peers count of peers connected to this node
 */
public record NodeDetailResponse(
        String id,
        String host,
        int port,
        int grpcPort,
        RaftRole role,
        boolean healthy,
        long term,
        long commitIndex,
        long appliedIndex,
        long matchIndex,
        long nextIndex,
        boolean isLeader,
        boolean isLocal,
        double latencyMs,
        int peers
) {
}
