package com.atlaskv.server.config;

import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Spring configuration that wires AtlasKV properties into the lifecycle manager.
 * Translates YAML configuration into the domain's immutable {@link ClusterConfig}.
 */
@Configuration
@EnableConfigurationProperties(AtlasKvProperties.class)
public class RaftNodeConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RaftNodeConfiguration.class);

    /**
     * Creates the immutable ClusterConfig from Spring-bound properties.
     *
     * @param props YAML-bound configuration properties
     * @return validated ClusterConfig
     */
    @Bean
    public ClusterConfig clusterConfig(AtlasKvProperties props) {
        AtlasKvProperties.NodeProperties node = props.getNode();
        AtlasKvProperties.RaftProperties raft = props.getRaft();
        AtlasKvProperties.ServerProperties server = props.getServer();

        if (raft.getMinElectionTimeoutMs() >= raft.getMaxElectionTimeoutMs()) {
            throw new ConfigValidationException(
                    "minElectionTimeoutMs (" + raft.getMinElectionTimeoutMs()
                            + ") must be strictly less than maxElectionTimeoutMs ("
                            + raft.getMaxElectionTimeoutMs() + ")");
        }
        if (raft.getHeartbeatIntervalMs() >= raft.getMinElectionTimeoutMs()) {
            throw new ConfigValidationException(
                    "heartbeatIntervalMs (" + raft.getHeartbeatIntervalMs()
                            + ") must be strictly less than minElectionTimeoutMs ("
                            + raft.getMinElectionTimeoutMs() + ")");
        }

        Path dataDir = Path.of(raft.getDataDir());
        String storageDir = dataDir.resolve("storage").toString();
        String snapshotDir = dataDir.resolve("snapshots").toString();

        ClusterConfig.Builder builder = ClusterConfig.builder()
                .nodeId(node.getId())
                .listenAddress(server.getHost(), server.getGrpcPort())
                .storageDirectory(storageDir)
                .snapshotDirectory(snapshotDir)
                .electionTimeout(
                        Duration.ofMillis(raft.getMinElectionTimeoutMs()),
                        Duration.ofMillis(raft.getMaxElectionTimeoutMs()))
                .heartbeatInterval(Duration.ofMillis(raft.getHeartbeatIntervalMs()))
                .snapshotThresholdEntries(raft.getSnapshotThreshold())
                .rpcTimeout(Duration.ofMillis(raft.getRpcTimeoutMs()));

        // Add peers from named map (peerId -> host:port)
        for (Map.Entry<String, String> entry : raft.getPeerAddresses().entrySet()) {
            InetSocketAddress addr = parseAddress(entry.getValue());
            builder.addPeer(entry.getKey(), addr.getHostString(), addr.getPort());
        }

        // Add peers from simple list (auto-generated peer IDs)
        int peerIndex = 1;
        for (String peerAddr : raft.getPeers()) {
            String peerId = "peer-" + peerIndex++;
            // Skip if this address matches this node's own address
            InetSocketAddress addr = parseAddress(peerAddr);
            builder.addPeer(peerId, addr.getHostString(), addr.getPort());
        }

        ClusterConfig config = builder.build();
        LOG.info("ClusterConfig created: nodeId={}, peers={}, grpcPort={}",
                config.nodeId(), config.peerIds(), server.getGrpcPort());
        return config;
    }

    /**
     * Creates the key-value state machine bean.
     *
     * @return key-value state machine
     */
    @Bean
    public KeyValueStateMachine keyValueStateMachine() {
        return new KeyValueStateMachine();
    }

    /**
     * Creates and starts the node lifecycle manager.
     *
     * @param config validated cluster configuration
     * @param stateMachine key-value state machine
     * @return started lifecycle manager
     */
    @Bean
    public NodeLifecycleManager nodeLifecycleManager(
            ClusterConfig config, KeyValueStateMachine stateMachine) {
        LOG.info("Configured NodeLifecycleManager for node {}", config.nodeId());
        return new NodeLifecycleManager(config, stateMachine);
    }

    private static InetSocketAddress parseAddress(String hostPort) {
        int colonIdx = hostPort.lastIndexOf(':');
        if (colonIdx <= 0) {
            throw new ConfigValidationException(
                    "Invalid peer address format '" + hostPort + "', expected host:port");
        }
        String host = hostPort.substring(0, colonIdx);
        int port = Integer.parseInt(hostPort.substring(colonIdx + 1));
        return new InetSocketAddress(host, port);
    }
}
