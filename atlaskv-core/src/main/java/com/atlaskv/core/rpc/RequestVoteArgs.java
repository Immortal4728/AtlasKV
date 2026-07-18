package com.atlaskv.core.rpc;

import com.atlaskv.core.NodeId;
import java.util.Objects;

/**
 * Invoked by candidates to gather votes in an election (Raft Section 5.2, 5.4).
 *
 * @param term candidate's term
 * @param candidateId candidate requesting vote
 * @param lastLogIndex index of candidate's last log entry
 * @param lastLogTerm term of candidate's last log entry
 */
public record RequestVoteArgs(long term, NodeId candidateId, long lastLogIndex, long lastLogTerm) {

    public RequestVoteArgs {
        if (term < 0) {
            throw new IllegalArgumentException("Term must be non-negative (>= 0), got: " + term);
        }
        Objects.requireNonNull(candidateId, "CandidateId must not be null");
        if (lastLogIndex < 0) {
            throw new IllegalArgumentException("LastLogIndex must be non-negative (>= 0), got: " + lastLogIndex);
        }
        if (lastLogTerm < 0) {
            throw new IllegalArgumentException("LastLogTerm must be non-negative (>= 0), got: " + lastLogTerm);
        }
    }
}
