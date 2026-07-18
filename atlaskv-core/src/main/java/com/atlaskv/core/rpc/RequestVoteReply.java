package com.atlaskv.core.rpc;

/**
 * Result returned in response to a {@link RequestVoteArgs} RPC.
 *
 * @param term currentTerm, for candidate to update itself
 * @param voteGranted true means candidate received vote
 */
public record RequestVoteReply(long term, boolean voteGranted) {

    public RequestVoteReply {
        if (term < 0) {
            throw new IllegalArgumentException("Term must be non-negative (>= 0), got: " + term);
        }
    }
}
