package com.atlaskv.core.rpc;

/**
 * Result returned in response to an {@link AppendEntriesArgs} RPC.
 *
 * @param term currentTerm, for leader to update itself
 * @param success true if follower contained entry matching prevLogIndex and prevLogTerm
 * @param matchIndex highest index known to be replicated on follower
 */
public record AppendEntriesReply(long term, boolean success, long matchIndex) {

    public AppendEntriesReply {
        if (term < 0) {
            throw new IllegalArgumentException("Term must be non-negative (>= 0), got: " + term);
        }
        if (matchIndex < 0) {
            throw new IllegalArgumentException("MatchIndex must be non-negative (>= 0), got: " + matchIndex);
        }
    }
}
