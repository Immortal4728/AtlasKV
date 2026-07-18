package com.atlaskv.core.config;

import com.atlaskv.core.NodeId;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the cluster membership configuration state in Raft Joint Consensus.
 *
 * <p>Supports both simple configuration (C_old) and joint configuration (C_old,new).
 * Provides thread-safe, immutable quorum evaluation logic for elections and log commit.
 */
public final class ClusterMembership {

    private final Set<NodeId> oldMembers;
    private final Set<NodeId> newMembers;

    private ClusterMembership(Set<NodeId> oldMembers, Set<NodeId> newMembers) {
        Objects.requireNonNull(oldMembers, "oldMembers must not be null");
        if (oldMembers.isEmpty()) {
            throw new IllegalArgumentException("oldMembers must not be empty");
        }
        this.oldMembers = Set.copyOf(oldMembers);
        this.newMembers = newMembers != null ? Set.copyOf(newMembers) : Collections.emptySet();
    }

    /**
     * Creates a simple cluster membership configuration (C_old).
     *
     * @param members set of member node IDs
     * @return single membership instance
     */
    public static ClusterMembership ofSingle(Set<NodeId> members) {
        return new ClusterMembership(members, null);
    }

    /**
     * Creates a joint consensus cluster membership configuration (C_old,new).
     *
     * @param oldMembers set of member node IDs in C_old
     * @param newMembers set of member node IDs in C_new
     * @return joint membership instance
     */
    public static ClusterMembership ofJoint(Set<NodeId> oldMembers, Set<NodeId> newMembers) {
        Objects.requireNonNull(newMembers, "newMembers must not be null");
        if (newMembers.isEmpty()) {
            throw new IllegalArgumentException("newMembers must not be empty in joint consensus");
        }
        return new ClusterMembership(oldMembers, newMembers);
    }

    /**
     * Indicates whether joint consensus is currently active.
     *
     * @return true if in joint consensus (C_old,new)
     */
    public boolean isJoint() {
        return !newMembers.isEmpty() && !newMembers.equals(oldMembers);
    }

    /**
     * Returns the set of member node IDs in the old configuration (C_old).
     *
     * @return unmodifiable set of old member IDs
     */
    public Set<NodeId> oldMembers() {
        return oldMembers;
    }

    /**
     * Returns the set of member node IDs in the new configuration (C_new).
     *
     * @return unmodifiable set of new member IDs, or empty set if not joint
     */
    public Set<NodeId> newMembers() {
        return newMembers;
    }

    /**
     * Returns all active member node IDs across old and new configurations (C_old U C_new).
     *
     * @return set of all active node IDs
     */
    public Set<NodeId> activeMembers() {
        if (!isJoint()) {
            return oldMembers;
        }
        Set<NodeId> combined = new HashSet<>(oldMembers);
        combined.addAll(newMembers);
        return Collections.unmodifiableSet(combined);
    }

    /**
     * Returns all active peer node IDs excluding self.
     *
     * @param selfId identity of this node
     * @return set of active peer node IDs
     */
    public Set<NodeId> activePeers(NodeId selfId) {
        return activeMembers().stream()
                .filter(id -> !id.equals(selfId))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Evaluates whether a set of active voter responses satisfies the quorum requirements.
     * In joint consensus (C_old,new), majorities from BOTH C_old and C_new are required.
     *
     * @param activeVoters set of nodes that granted votes or acknowledged entries
     * @return true if quorum is reached
     */
    public boolean isQuorum(Set<NodeId> activeVoters) {
        Objects.requireNonNull(activeVoters, "activeVoters must not be null");

        long oldVotes = oldMembers.stream().filter(activeVoters::contains).count();
        boolean oldQuorum = oldVotes >= (oldMembers.size() / 2) + 1;

        if (!isJoint()) {
            return oldQuorum;
        }

        long newVotes = newMembers.stream().filter(activeVoters::contains).count();
        boolean newQuorum = newVotes >= (newMembers.size() / 2) + 1;

        return oldQuorum && newQuorum;
    }

    /**
     * Converts a joint configuration (C_old,new) to the final new configuration (C_new).
     *
     * @return single membership instance for C_new
     */
    public ClusterMembership toSingle() {
        if (!isJoint()) {
            return this;
        }
        return ofSingle(newMembers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClusterMembership that = (ClusterMembership) o;
        return Objects.equals(oldMembers, that.oldMembers)
                && Objects.equals(newMembers, that.newMembers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldMembers, newMembers);
    }

    @Override
    public String toString() {
        if (isJoint()) {
            return "ClusterMembership[C_old=" + oldMembers + ", C_new=" + newMembers + "]";
        }
        return "ClusterMembership[C_single=" + oldMembers + "]";
    }
}
