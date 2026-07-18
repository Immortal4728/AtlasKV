package com.atlaskv.core.config;

import com.atlaskv.core.NodeId;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable configuration parameters for a Raft node.
 *
 * @param selfId identity of this node
 * @param peers set of peer node identities (excluding self)
 * @param minElectionTimeout minimum election timeout duration
 * @param maxElectionTimeout maximum election timeout duration
 * @param heartbeatInterval heartbeat broadcast interval for leaders
 * @param snapshotThresholdEntries number of committed log entries triggering an automatic snapshot
 */
public record RaftConfig(
        NodeId selfId,
        Set<NodeId> peers,
        Duration minElectionTimeout,
        Duration maxElectionTimeout,
        Duration heartbeatInterval,
        long snapshotThresholdEntries
) {

    public static final long DEFAULT_SNAPSHOT_THRESHOLD = 100L;

    public RaftConfig(
            NodeId selfId,
            Set<NodeId> peers,
            Duration minElectionTimeout,
            Duration maxElectionTimeout,
            Duration heartbeatInterval
    ) {
        this(selfId, peers, minElectionTimeout, maxElectionTimeout, heartbeatInterval, DEFAULT_SNAPSHOT_THRESHOLD);
    }

    public RaftConfig {
        Objects.requireNonNull(selfId, "SelfId must not be null");
        Objects.requireNonNull(peers, "Peers set must not be null");
        if (peers.contains(selfId)) {
            throw new IllegalArgumentException("Peers set must not contain selfId: " + selfId);
        }
        peers = Set.copyOf(peers);
        Objects.requireNonNull(minElectionTimeout, "MinElectionTimeout must not be null");
        Objects.requireNonNull(maxElectionTimeout, "MaxElectionTimeout must not be null");
        Objects.requireNonNull(heartbeatInterval, "HeartbeatInterval must not be null");

        if (minElectionTimeout.isNegative() || minElectionTimeout.isZero()) {
            throw new IllegalArgumentException("MinElectionTimeout must be positive, got: " + minElectionTimeout);
        }
        if (maxElectionTimeout.compareTo(minElectionTimeout) < 0) {
            throw new IllegalArgumentException("MaxElectionTimeout must be >= MinElectionTimeout");
        }
        if (heartbeatInterval.isNegative() || heartbeatInterval.isZero()) {
            throw new IllegalArgumentException("HeartbeatInterval must be positive, got: " + heartbeatInterval);
        }
        if (snapshotThresholdEntries <= 0) {
            throw new IllegalArgumentException("SnapshotThresholdEntries must be > 0, got: " + snapshotThresholdEntries);
        }
    }

    public int clusterSize() {
        return peers.size() + 1;
    }

    public int majorityQuorum() {
        return (clusterSize() / 2) + 1;
    }
}
