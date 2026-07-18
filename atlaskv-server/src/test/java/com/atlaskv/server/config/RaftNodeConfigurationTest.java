package com.atlaskv.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RaftNodeConfigurationTest {

    private final RaftNodeConfiguration configuration = new RaftNodeConfiguration();

    @Test
    @DisplayName("Creates valid ClusterConfig from AtlasKvProperties")
    void createsValidClusterConfig() {
        AtlasKvProperties props = new AtlasKvProperties();
        props.getNode().setId("node1");
        props.getServer().setHost("127.0.0.1");
        props.getServer().setGrpcPort(50051);
        props.getRaft().setMinElectionTimeoutMs(300);
        props.getRaft().setMaxElectionTimeoutMs(600);
        props.getRaft().setHeartbeatIntervalMs(100);
        props.getRaft().setPeerAddresses(Map.of("node2", "127.0.0.1:50052"));

        ClusterConfig config = configuration.clusterConfig(props);

        assertThat(config.nodeId().value()).isEqualTo("node1");
        assertThat(config.listenAddress().getPort()).isEqualTo(50051);
        assertThat(config.peerIds()).extracting("value").contains("node2");
    }

    @Test
    @DisplayName("Throws ConfigValidationException when minElectionTimeout >= maxElectionTimeout")
    void throwsWhenMinElectionTimeoutGreaterThanMax() {
        AtlasKvProperties props = new AtlasKvProperties();
        props.getRaft().setMinElectionTimeoutMs(600);
        props.getRaft().setMaxElectionTimeoutMs(300);

        assertThatThrownBy(() -> configuration.clusterConfig(props))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must be strictly less than maxElectionTimeoutMs");
    }

    @Test
    @DisplayName("Throws ConfigValidationException when heartbeatInterval >= minElectionTimeout")
    void throwsWhenHeartbeatIntervalGreaterThanMinElection() {
        AtlasKvProperties props = new AtlasKvProperties();
        props.getRaft().setMinElectionTimeoutMs(300);
        props.getRaft().setMaxElectionTimeoutMs(600);
        props.getRaft().setHeartbeatIntervalMs(400);

        assertThatThrownBy(() -> configuration.clusterConfig(props))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("must be strictly less than minElectionTimeoutMs");
    }
}
