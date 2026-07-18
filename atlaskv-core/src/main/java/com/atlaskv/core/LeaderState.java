package com.atlaskv.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Volatile leader state tracking nextIndex and matchIndex for each peer (Raft Section 5.3).
 *
 * <p>Reinitialized after each election. nextIndex starts at leader's last log index + 1,
 * matchIndex starts at 0.
 */
public final class LeaderState {

    private final Map<NodeId, Long> nextIndex;
    private final Map<NodeId, Long> matchIndex;

    /**
     * Initializes leader state for the given peers.
     *
     * @param peers set of peer NodeIds (must not include self)
     * @param lastLogIndex leader's last log index at time of election
     */
    public LeaderState(Set<NodeId> peers, long lastLogIndex) {
        Objects.requireNonNull(peers, "Peers must not be null");
        this.nextIndex = new HashMap<>(peers.size());
        this.matchIndex = new HashMap<>(peers.size());
        for (NodeId peer : peers) {
            nextIndex.put(peer, lastLogIndex + 1);
            matchIndex.put(peer, 0L);
        }
    }

    public long getNextIndex(NodeId peer) {
        return nextIndex.getOrDefault(peer, 1L);
    }

    public void setNextIndex(NodeId peer, long index) {
        nextIndex.put(peer, index);
    }

    public void decrementNextIndex(NodeId peer) {
        long current = getNextIndex(peer);
        if (current > 1) {
            nextIndex.put(peer, current - 1);
        }
    }

    public long getMatchIndex(NodeId peer) {
        return matchIndex.getOrDefault(peer, 0L);
    }

    public void setMatchIndex(NodeId peer, long index) {
        matchIndex.put(peer, index);
    }

    /**
     * Dynamically updates the tracking maps for a new set of peers.
     *
     * @param peers updated set of peer NodeIds
     * @param lastLogIndex current last log index for initializing new peers
     */
    public void updatePeers(Set<NodeId> peers, long lastLogIndex) {
        Objects.requireNonNull(peers, "Peers must not be null");
        for (NodeId peer : peers) {
            nextIndex.putIfAbsent(peer, lastLogIndex + 1);
            matchIndex.putIfAbsent(peer, 0L);
        }
    }
}
