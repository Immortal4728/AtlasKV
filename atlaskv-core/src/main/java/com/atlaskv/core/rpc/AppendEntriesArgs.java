package com.atlaskv.core.rpc;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.NodeId;
import java.util.List;
import java.util.Objects;

/**
 * Invoked by leader to replicate log entries and send heartbeats (Raft Section 5.2, 5.3).
 *
 * @param term leader's term
 * @param leaderId so follower can redirect clients
 * @param prevLogIndex index of log entry immediately preceding new ones
 * @param prevLogTerm term of prevLogIndex entry
 * @param entries log entries to store (empty for heartbeat)
 * @param leaderCommit leader's commitIndex
 */
public record AppendEntriesArgs(
        long term,
        NodeId leaderId,
        long prevLogIndex,
        long prevLogTerm,
        List<LogEntry> entries,
        long leaderCommit
) {

    public AppendEntriesArgs {
        if (term < 0) {
            throw new IllegalArgumentException("Term must be non-negative (>= 0), got: " + term);
        }
        Objects.requireNonNull(leaderId, "LeaderId must not be null");
        if (prevLogIndex < 0) {
            throw new IllegalArgumentException("PrevLogIndex must be non-negative (>= 0), got: " + prevLogIndex);
        }
        if (prevLogTerm < 0) {
            throw new IllegalArgumentException("PrevLogTerm must be non-negative (>= 0), got: " + prevLogTerm);
        }
        Objects.requireNonNull(entries, "Entries list must not be null");
        entries = List.copyOf(entries);
        if (leaderCommit < 0) {
            throw new IllegalArgumentException("LeaderCommit must be non-negative (>= 0), got: " + leaderCommit);
        }
    }
}
