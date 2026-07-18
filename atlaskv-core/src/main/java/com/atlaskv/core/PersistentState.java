package com.atlaskv.core;

/**
 * Immutable value object representing Raft persistent state.
 *
 * @param currentTerm latest term node has seen
 * @param votedFor candidateId that received vote in current term (or null if none)
 */
public record PersistentState(long currentTerm, NodeId votedFor) {

    public PersistentState {
        if (currentTerm < 0) {
            throw new IllegalArgumentException("Current term must be non-negative (>= 0), got: " + currentTerm);
        }
    }

    public static PersistentState initial() {
        return new PersistentState(0L, null);
    }

    public PersistentState withTerm(long newTerm) {
        return new PersistentState(newTerm, null);
    }

    public PersistentState withVote(NodeId candidateId) {
        return new PersistentState(this.currentTerm, candidateId);
    }
}
