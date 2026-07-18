package com.atlaskv.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Manages pending Raft ReadIndex requests and quorum confirmations for linearizable reads.
 */
public final class PendingReadIndexManager {

    private final List<PendingReadRequest> pendingReads = new ArrayList<>();
    private final Set<NodeId> heartbeatAcks = new HashSet<>();

    private static final class PendingReadRequest {
        private final long readIndex;
        private final CompletableFuture<Long> future;
        private final long term;
        private boolean leaderConfirmed;

        private PendingReadRequest(long readIndex, CompletableFuture<Long> future,
                                   long term, boolean leaderConfirmed) {
            this.readIndex = readIndex;
            this.future = future;
            this.term = term;
            this.leaderConfirmed = leaderConfirmed;
        }
    }

    /**
     * Registers a new ReadIndex request.
     *
     * @param readIndex leader commitIndex at time of read
     * @param future response future
     * @param currentTerm current leader term
     * @param singleNode true if single-node cluster
     */
    public void register(long readIndex, CompletableFuture<Long> future,
                         long currentTerm, boolean singleNode) {
        pendingReads.add(new PendingReadRequest(readIndex, future, currentTerm, singleNode));
    }

    /**
     * Resets heartbeat confirmations for a new heartbeat batch.
     *
     * @param selfId local node ID
     */
    public void resetHeartbeatAcks(NodeId selfId) {
        heartbeatAcks.clear();
        heartbeatAcks.add(selfId);
    }

    /**
     * Records a heartbeat response from a peer for the current term using ClusterMembership quorum.
     *
     * @param peer peer node ID
     * @param currentTerm current term
     * @param membership cluster membership configuration
     */
    public void recordHeartbeatAck(NodeId peer, long currentTerm, com.atlaskv.core.config.ClusterMembership membership) {
        heartbeatAcks.add(peer);
        if (membership != null && membership.isQuorum(heartbeatAcks)) {
            for (PendingReadRequest req : pendingReads) {
                if (req.term == currentTerm) {
                    req.leaderConfirmed = true;
                }
            }
        }
    }

    /**
     * Records a heartbeat response from a peer for the current term.
     *
     * @param peer peer node ID
     * @param currentTerm current term
     * @param majorityQuorum minimum nodes needed for quorum
     */
    public void recordHeartbeatAck(NodeId peer, long currentTerm, int majorityQuorum) {
        heartbeatAcks.add(peer);
        if (heartbeatAcks.size() >= majorityQuorum) {
            for (PendingReadRequest req : pendingReads) {
                if (req.term == currentTerm) {
                    req.leaderConfirmed = true;
                }
            }
        }
    }

    /**
     * Attempts to process pending reads whose leader is confirmed and lastApplied >= readIndex.
     *
     * @param currentTerm current term
     * @param lastApplied highest applied index
     */
    public void tryProcessPendingReads(long currentTerm, long lastApplied) {
        Iterator<PendingReadRequest> iterator = pendingReads.iterator();
        while (iterator.hasNext()) {
            PendingReadRequest req = iterator.next();
            if (req.term != currentTerm) {
                req.future.completeExceptionally(new IllegalStateException("Leadership term changed"));
                iterator.remove();
            } else if (req.leaderConfirmed && lastApplied >= req.readIndex) {
                req.future.complete(req.readIndex);
                iterator.remove();
            }
        }
    }

    /**
     * Fails all pending reads (e.g. on step-down or node closure).
     *
     * @param cause exception cause
     */
    public void failAll(Throwable cause) {
        for (PendingReadRequest req : pendingReads) {
            req.future.completeExceptionally(cause);
        }
        pendingReads.clear();
        heartbeatAcks.clear();
    }
}
