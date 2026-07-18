package com.atlaskv.server.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * Spring-managed configuration properties bound from {@code application.yml}.
 * Validated eagerly at startup via Jakarta Validation annotations.
 */
@ConfigurationProperties(prefix = "atlaskv")
@Validated
public final class AtlasKvProperties {

    @NotNull
    private NodeProperties node = new NodeProperties();

    @NotNull
    private RaftProperties raft = new RaftProperties();

    @NotNull
    private ServerProperties server = new ServerProperties();

    /**
     * Returns node identity properties.
     *
     * @return node properties
     */
    public NodeProperties getNode() {
        return node;
    }

    /**
     * Sets node identity properties.
     *
     * @param node node properties
     */
    public void setNode(NodeProperties node) {
        this.node = node;
    }

    /**
     * Returns Raft algorithm properties.
     *
     * @return raft properties
     */
    public RaftProperties getRaft() {
        return raft;
    }

    /**
     * Sets Raft algorithm properties.
     *
     * @param raft raft properties
     */
    public void setRaft(RaftProperties raft) {
        this.raft = raft;
    }

    /**
     * Returns server transport properties.
     *
     * @return server properties
     */
    public ServerProperties getServer() {
        return server;
    }

    /**
     * Sets server transport properties.
     *
     * @param server server properties
     */
    public void setServer(ServerProperties server) {
        this.server = server;
    }

    /**
     * Node identity configuration.
     */
    public static final class NodeProperties {

        @NotBlank
        private String id = "node1";

        /**
         * Returns the node identifier.
         *
         * @return node id
         */
        public String getId() {
            return id;
        }

        /**
         * Sets the node identifier.
         *
         * @param id node id
         */
        public void setId(String id) {
            this.id = id;
        }
    }

    /**
     * Raft algorithm tuning and storage configuration.
     */
    public static final class RaftProperties {

        @NotBlank
        private String dataDir = "./data";

        private List<String> peers = List.of();

        private Map<String, String> peerAddresses = Map.of();

        @Min(50)
        private long minElectionTimeoutMs = 500;

        @Min(50)
        private long maxElectionTimeoutMs = 1000;

        @Min(10)
        private long heartbeatIntervalMs = 150;

        @Min(1)
        private long snapshotThreshold = 100;

        @Min(100)
        private long rpcTimeoutMs = 2000;

        /**
         * Returns the data directory path.
         *
         * @return data directory
         */
        public String getDataDir() {
            return dataDir;
        }

        /**
         * Sets the data directory path.
         *
         * @param dataDir data directory
         */
        public void setDataDir(String dataDir) {
            this.dataDir = dataDir;
        }

        /**
         * Returns the list of peer addresses (host:port format).
         *
         * @return peer address list
         */
        public List<String> getPeers() {
            return peers;
        }

        /**
         * Sets the list of peer addresses.
         *
         * @param peers peer address list
         */
        public void setPeers(List<String> peers) {
            this.peers = peers;
        }

        /**
         * Returns the named peer addresses map (peerId to host:port).
         *
         * @return peer address map
         */
        public Map<String, String> getPeerAddresses() {
            return peerAddresses;
        }

        /**
         * Sets the named peer addresses map.
         *
         * @param peerAddresses peer address map
         */
        public void setPeerAddresses(Map<String, String> peerAddresses) {
            this.peerAddresses = peerAddresses;
        }

        /**
         * Returns the minimum election timeout in milliseconds.
         *
         * @return min election timeout ms
         */
        public long getMinElectionTimeoutMs() {
            return minElectionTimeoutMs;
        }

        /**
         * Sets the minimum election timeout in milliseconds.
         *
         * @param minElectionTimeoutMs min election timeout ms
         */
        public void setMinElectionTimeoutMs(long minElectionTimeoutMs) {
            this.minElectionTimeoutMs = minElectionTimeoutMs;
        }

        /**
         * Returns the maximum election timeout in milliseconds.
         *
         * @return max election timeout ms
         */
        public long getMaxElectionTimeoutMs() {
            return maxElectionTimeoutMs;
        }

        /**
         * Sets the maximum election timeout in milliseconds.
         *
         * @param maxElectionTimeoutMs max election timeout ms
         */
        public void setMaxElectionTimeoutMs(long maxElectionTimeoutMs) {
            this.maxElectionTimeoutMs = maxElectionTimeoutMs;
        }

        /**
         * Returns the heartbeat interval in milliseconds.
         *
         * @return heartbeat interval ms
         */
        public long getHeartbeatIntervalMs() {
            return heartbeatIntervalMs;
        }

        /**
         * Sets the heartbeat interval in milliseconds.
         *
         * @param heartbeatIntervalMs heartbeat interval ms
         */
        public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
            this.heartbeatIntervalMs = heartbeatIntervalMs;
        }

        /**
         * Returns the snapshot threshold in number of committed entries.
         *
         * @return snapshot threshold
         */
        public long getSnapshotThreshold() {
            return snapshotThreshold;
        }

        /**
         * Sets the snapshot threshold.
         *
         * @param snapshotThreshold snapshot threshold
         */
        public void setSnapshotThreshold(long snapshotThreshold) {
            this.snapshotThreshold = snapshotThreshold;
        }

        /**
         * Returns the RPC timeout in milliseconds.
         *
         * @return rpc timeout ms
         */
        public long getRpcTimeoutMs() {
            return rpcTimeoutMs;
        }

        /**
         * Sets the RPC timeout in milliseconds.
         *
         * @param rpcTimeoutMs rpc timeout ms
         */
        public void setRpcTimeoutMs(long rpcTimeoutMs) {
            this.rpcTimeoutMs = rpcTimeoutMs;
        }
    }

    /**
     * Server transport configuration.
     */
    public static final class ServerProperties {

        @NotBlank
        private String host = "0.0.0.0";

        @Min(0)
        private int grpcPort = 50051;

        /**
         * Returns the gRPC server host.
         *
         * @return server host
         */
        public String getHost() {
            return host;
        }

        /**
         * Sets the gRPC server host.
         *
         * @param host server host
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * Returns the gRPC server port.
         *
         * @return grpc port
         */
        public int getGrpcPort() {
            return grpcPort;
        }

        /**
         * Sets the gRPC server port.
         *
         * @param grpcPort grpc port
         */
        public void setGrpcPort(int grpcPort) {
            this.grpcPort = grpcPort;
        }
    }
}
