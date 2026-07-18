package com.atlaskv.core.storage;

import com.atlaskv.core.config.ClusterMembership;

/**
 * Immutable metadata associated with a Raft snapshot.
 *
 * @param lastIncludedIndex 1-based log index of the last entry included in snapshot
 * @param lastIncludedTerm term of the entry at lastIncludedIndex
 * @param membership cluster membership configuration at the time of the snapshot
 */
public record SnapshotMetadata(
        long lastIncludedIndex,
        long lastIncludedTerm,
        ClusterMembership membership
) {

    public SnapshotMetadata(long lastIncludedIndex, long lastIncludedTerm) {
        this(lastIncludedIndex, lastIncludedTerm, null);
    }

    public SnapshotMetadata {
        if (lastIncludedIndex < 0) {
            throw new IllegalArgumentException("lastIncludedIndex must be >= 0, got: " + lastIncludedIndex);
        }
        if (lastIncludedTerm < 0) {
            throw new IllegalArgumentException("lastIncludedTerm must be >= 0, got: " + lastIncludedTerm);
        }
    }
}
