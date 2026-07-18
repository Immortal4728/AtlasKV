package com.atlaskv.core.rpc;

/**
 * Reply for the InstallSnapshot RPC (Raft §7).
 *
 * @param term currentTerm, for leader to update itself
 * @param success true if follower accepted the snapshot
 */
public record InstallSnapshotReply(long term, boolean success) {

    public InstallSnapshotReply {
        if (term < 0) {
            throw new IllegalArgumentException("Term must be >= 0, got: " + term);
        }
    }
}
