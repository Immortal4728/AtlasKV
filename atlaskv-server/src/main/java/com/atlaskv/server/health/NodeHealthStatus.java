package com.atlaskv.server.health;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.RaftRole;

import java.util.Objects;

/**
 * Immutable snapshot of a Raft node's health status at a point in time.
 *
 * @param nodeId identity of this node
 * @param role current Raft role (FOLLOWER, CANDIDATE, LEADER)
 * @param currentTerm current Raft term
 * @param commitIndex highest committed log index
 * @param lastApplied highest applied log index
 * @param currentLeader current known leader (null if unknown)
 * @param healthy true if node is operational
 * @param startedAtMillis epoch millis when node was started
 */
public record NodeHealthStatus(
        NodeId nodeId,
        RaftRole role,
        long currentTerm,
        long commitIndex,
        long lastApplied,
        NodeId currentLeader,
        boolean healthy,
        long startedAtMillis
) {
    public NodeHealthStatus {
        Objects.requireNonNull(nodeId, "NodeId must not be null");
        Objects.requireNonNull(role, "Role must not be null");
    }

    /**
     * Returns the uptime in milliseconds since the node started.
     *
     * @return uptime in milliseconds
     */
    public long uptimeMillis() {
        return System.currentTimeMillis() - startedAtMillis;
    }

    /**
     * Returns true if this node is currently the leader.
     *
     * @return true if leader
     */
    public boolean isLeader() {
        return role == RaftRole.LEADER;
    }
}
