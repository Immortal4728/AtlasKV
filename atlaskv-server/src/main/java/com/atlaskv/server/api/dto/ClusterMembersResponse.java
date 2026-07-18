package com.atlaskv.server.api.dto;

import java.util.List;

/**
 * Response DTO for cluster membership endpoint.
 *
 * @param members active cluster members
 * @param jointConsensusActive true if a membership transition is currently in progress
 * @param oldMembers previous configuration member list during joint consensus
 * @param newMembers proposed configuration member list during joint consensus
 * @param leaderId current leader node ID
 */
public record ClusterMembersResponse(
        List<String> members,
        boolean jointConsensusActive,
        List<String> oldMembers,
        List<String> newMembers,
        String leaderId
) {
}
