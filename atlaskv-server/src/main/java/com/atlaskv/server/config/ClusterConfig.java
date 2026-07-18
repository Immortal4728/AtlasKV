package com.atlaskv.server.config;

import com.atlaskv.core.NodeId;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable cluster configuration model for an AtlasKV Raft node.
 * Captures node identity, peer addresses, storage paths, and tuning parameters.
 *
 * <p>Validated eagerly at construction time. Thread-safe and framework-independent.
 */
public final class ClusterConfig {

    private final NodeId nodeId;
    private final InetSocketAddress listenAddress;
    private final Map<NodeId, InetSocketAddress> peerAddresses;
    private final String storageDirectory;
    private final String snapshotDirectory;
    private final Duration minElectionTimeout;
    private final Duration maxElectionTimeout;
    private final Duration heartbeatInterval;
    private final long snapshotThresholdEntries;
    private final Duration rpcTimeout;

    private ClusterConfig(Builder builder) {
        this.nodeId = builder.nodeId;
        this.listenAddress = builder.listenAddress;
        this.peerAddresses = Collections.unmodifiableMap(new LinkedHashMap<>(builder.peerAddresses));
        this.storageDirectory = builder.storageDirectory;
        this.snapshotDirectory = builder.snapshotDirectory;
        this.minElectionTimeout = builder.minElectionTimeout;
        this.maxElectionTimeout = builder.maxElectionTimeout;
        this.heartbeatInterval = builder.heartbeatInterval;
        this.snapshotThresholdEntries = builder.snapshotThresholdEntries;
        this.rpcTimeout = builder.rpcTimeout;
    }

    public NodeId nodeId() {
        return nodeId;
    }

    public InetSocketAddress listenAddress() {
        return listenAddress;
    }

    public Map<NodeId, InetSocketAddress> peerAddresses() {
        return peerAddresses;
    }

    /**
     * Returns peer NodeIds as an unmodifiable set (excluding self).
     *
     * @return set of peer NodeIds
     */
    public Set<NodeId> peerIds() {
        return peerAddresses.keySet();
    }

    public String storageDirectory() {
        return storageDirectory;
    }

    public String snapshotDirectory() {
        return snapshotDirectory;
    }

    public Duration minElectionTimeout() {
        return minElectionTimeout;
    }

    public Duration maxElectionTimeout() {
        return maxElectionTimeout;
    }

    public Duration heartbeatInterval() {
        return heartbeatInterval;
    }

    public long snapshotThresholdEntries() {
        return snapshotThresholdEntries;
    }

    public Duration rpcTimeout() {
        return rpcTimeout;
    }

    /**
     * Converts peer addresses to the {@code host:port} string mapping used by GrpcPeerTransport.
     *
     * @return map of NodeId to "host:port" strings
     */
    public Map<NodeId, String> peerAddressStrings() {
        return peerAddresses.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getHostString() + ":" + e.getValue().getPort()));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ClusterConfig} with validation on build.
     */
    public static final class Builder {

        private static final Duration DEFAULT_MIN_ELECTION = Duration.ofMillis(150);
        private static final Duration DEFAULT_MAX_ELECTION = Duration.ofMillis(300);
        private static final Duration DEFAULT_HEARTBEAT = Duration.ofMillis(50);
        private static final long DEFAULT_SNAPSHOT_THRESHOLD = 100L;
        private static final Duration DEFAULT_RPC_TIMEOUT = Duration.ofMillis(1000);

        private NodeId nodeId;
        private InetSocketAddress listenAddress;
        private final Map<NodeId, InetSocketAddress> peerAddresses = new LinkedHashMap<>();
        private String storageDirectory;
        private String snapshotDirectory;
        private Duration minElectionTimeout = DEFAULT_MIN_ELECTION;
        private Duration maxElectionTimeout = DEFAULT_MAX_ELECTION;
        private Duration heartbeatInterval = DEFAULT_HEARTBEAT;
        private long snapshotThresholdEntries = DEFAULT_SNAPSHOT_THRESHOLD;
        private Duration rpcTimeout = DEFAULT_RPC_TIMEOUT;

        /**
         * Sets the identity of this node.
         *
         * @param nodeId unique node identifier
         * @return this builder
         */
        public Builder nodeId(NodeId nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        /**
         * Sets the identity of this node from a string.
         *
         * @param nodeId unique node identifier string
         * @return this builder
         */
        public Builder nodeId(String nodeId) {
            this.nodeId = NodeId.of(nodeId);
            return this;
        }

        /**
         * Sets the address this node listens on for incoming RPCs.
         *
         * @param host hostname or IP
         * @param port port number
         * @return this builder
         */
        public Builder listenAddress(String host, int port) {
            this.listenAddress = new InetSocketAddress(host, port);
            return this;
        }

        /**
         * Adds a peer node with its network address.
         *
         * @param peerId peer node identifier
         * @param host peer hostname or IP
         * @param port peer port number
         * @return this builder
         */
        public Builder addPeer(String peerId, String host, int port) {
            this.peerAddresses.put(NodeId.of(peerId), new InetSocketAddress(host, port));
            return this;
        }

        /**
         * Adds a peer node with its network address.
         *
         * @param peerId peer NodeId
         * @param address peer socket address
         * @return this builder
         */
        public Builder addPeer(NodeId peerId, InetSocketAddress address) {
            this.peerAddresses.put(peerId, address);
            return this;
        }

        /**
         * Sets the storage directory for WAL and metadata files.
         *
         * @param storageDirectory absolute path to storage directory
         * @return this builder
         */
        public Builder storageDirectory(String storageDirectory) {
            this.storageDirectory = storageDirectory;
            return this;
        }

        /**
         * Sets the snapshot directory for snapshot files.
         *
         * @param snapshotDirectory absolute path to snapshot directory
         * @return this builder
         */
        public Builder snapshotDirectory(String snapshotDirectory) {
            this.snapshotDirectory = snapshotDirectory;
            return this;
        }

        /**
         * Sets election timeout range.
         *
         * @param min minimum election timeout
         * @param max maximum election timeout
         * @return this builder
         */
        public Builder electionTimeout(Duration min, Duration max) {
            this.minElectionTimeout = min;
            this.maxElectionTimeout = max;
            return this;
        }

        /**
         * Sets heartbeat interval for the leader.
         *
         * @param heartbeatInterval heartbeat interval duration
         * @return this builder
         */
        public Builder heartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
            return this;
        }

        /**
         * Sets number of committed entries before automatic snapshot.
         *
         * @param threshold snapshot threshold
         * @return this builder
         */
        public Builder snapshotThresholdEntries(long threshold) {
            this.snapshotThresholdEntries = threshold;
            return this;
        }

        /**
         * Sets the RPC timeout for peer communication.
         *
         * @param rpcTimeout RPC timeout duration
         * @return this builder
         */
        public Builder rpcTimeout(Duration rpcTimeout) {
            this.rpcTimeout = rpcTimeout;
            return this;
        }

        /**
         * Validates and builds the immutable ClusterConfig.
         *
         * @return validated ClusterConfig
         * @throws ConfigValidationException if validation fails
         */
        public ClusterConfig build() {
            ConfigValidator.validate(this);
            return new ClusterConfig(this);
        }

        // package-private for validator access
        NodeId getNodeId() {
            return nodeId;
        }

        InetSocketAddress getListenAddress() {
            return listenAddress;
        }

        Map<NodeId, InetSocketAddress> getPeerAddresses() {
            return peerAddresses;
        }

        String getStorageDirectory() {
            return storageDirectory;
        }

        String getSnapshotDirectory() {
            return snapshotDirectory;
        }

        Duration getMinElectionTimeout() {
            return minElectionTimeout;
        }

        Duration getMaxElectionTimeout() {
            return maxElectionTimeout;
        }

        Duration getHeartbeatInterval() {
            return heartbeatInterval;
        }

        long getSnapshotThresholdEntries() {
            return snapshotThresholdEntries;
        }

        Duration getRpcTimeout() {
            return rpcTimeout;
        }
    }
}
