package com.atlaskv.core.config;

import com.atlaskv.core.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RaftConfigTest {

    @Test
    @DisplayName("Valid RaftConfig calculates cluster size and majority quorum correctly")
    void testValidConfig() {
        NodeId self = NodeId.of("node-1");
        Set<NodeId> peers = Set.of(NodeId.of("node-2"), NodeId.of("node-3"));

        RaftConfig config = new RaftConfig(
                self,
                peers,
                Duration.ofMillis(150),
                Duration.ofMillis(300),
                Duration.ofMillis(50)
        );

        assertThat(config.selfId()).isEqualTo(self);
        assertThat(config.peers()).isEqualTo(peers);
        assertThat(config.clusterSize()).isEqualTo(3);
        assertThat(config.majorityQuorum()).isEqualTo(2);
    }

    @Test
    @DisplayName("Peers containing selfId throws IllegalArgumentException")
    void testPeersContainsSelf() {
        NodeId self = NodeId.of("node-1");
        Set<NodeId> peers = Set.of(self, NodeId.of("node-2"));

        assertThatThrownBy(() -> new RaftConfig(
                self,
                peers,
                Duration.ofMillis(150),
                Duration.ofMillis(300),
                Duration.ofMillis(50)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain selfId");
    }

    @Test
    @DisplayName("Invalid timeout ranges throw IllegalArgumentException")
    void testInvalidTimeouts() {
        NodeId self = NodeId.of("node-1");
        Set<NodeId> peers = Set.of(NodeId.of("node-2"));

        assertThatThrownBy(() -> new RaftConfig(
                self,
                peers,
                Duration.ofMillis(300),
                Duration.ofMillis(150),
                Duration.ofMillis(50)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
