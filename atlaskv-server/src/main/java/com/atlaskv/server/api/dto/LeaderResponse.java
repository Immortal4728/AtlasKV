package com.atlaskv.server.api.dto;

/**
 * Response DTO for leader discovery endpoint.
 *
 * @param leaderId current leader node id (null if unknown)
 * @param isThisNodeLeader true if this node is the current leader
 * @param currentTerm current Raft term
 */
public record LeaderResponse(
        String leaderId,
        boolean isThisNodeLeader,
        long currentTerm
) {
}
