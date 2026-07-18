package com.atlaskv.server.config;

import com.atlaskv.core.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ClusterConfig} validation and construction.
 */
class ClusterConfigTest {

    @Nested
    @DisplayName("Valid configuration")
    class ValidConfig {

        @Test
        @DisplayName("Builds with all required fields")
        void buildsWithAllRequiredFields() {
            ClusterConfig config = ClusterConfig.builder()
                    .nodeId("node-1")
                    .listenAddress("localhost", 9001)
                    .addPeer("node-2", "localhost", 9002)
                    .addPeer("node-3", "localhost", 9003)
                    .storageDirectory("/tmp/atlaskv-test/node-1/storage")
                    .snapshotDirectory("/tmp/atlaskv-test/node-1/snapshots")
                    .build();

            assertThat(config.nodeId()).isEqualTo(NodeId.of("node-1"));
            assertThat(config.listenAddress().getPort()).isEqualTo(9001);
            assertThat(config.peerIds()).containsExactlyInAnyOrder(
                    NodeId.of("node-2"), NodeId.of("node-3"));
            assertThat(config.peerAddressStrings()).hasSize(2);
        }

        @Test
        @DisplayName("Applies default timeouts when not specified")
        void appliesDefaultTimeouts() {
            ClusterConfig config = minimalConfig().build();

            assertThat(config.minElectionTimeout()).isEqualTo(Duration.ofMillis(150));
            assertThat(config.maxElectionTimeout()).isEqualTo(Duration.ofMillis(300));
            assertThat(config.heartbeatInterval()).isEqualTo(Duration.ofMillis(50));
            assertThat(config.snapshotThresholdEntries()).isEqualTo(100L);
            assertThat(config.rpcTimeout()).isEqualTo(Duration.ofMillis(1000));
        }

        @Test
        @DisplayName("Overrides timeouts when specified")
        void overridesTimeouts() {
            ClusterConfig config = minimalConfig()
                    .electionTimeout(Duration.ofMillis(500), Duration.ofMillis(1000))
                    .heartbeatInterval(Duration.ofMillis(100))
                    .snapshotThresholdEntries(50L)
                    .rpcTimeout(Duration.ofMillis(2000))
                    .build();

            assertThat(config.minElectionTimeout()).isEqualTo(Duration.ofMillis(500));
            assertThat(config.maxElectionTimeout()).isEqualTo(Duration.ofMillis(1000));
            assertThat(config.heartbeatInterval()).isEqualTo(Duration.ofMillis(100));
            assertThat(config.snapshotThresholdEntries()).isEqualTo(50L);
            assertThat(config.rpcTimeout()).isEqualTo(Duration.ofMillis(2000));
        }

        @Test
        @DisplayName("Supports single-node cluster with no peers")
        void supportsSingleNodeCluster() {
            ClusterConfig config = ClusterConfig.builder()
                    .nodeId("solo-node")
                    .listenAddress("localhost", 9001)
                    .storageDirectory("/tmp/atlaskv-test/solo/storage")
                    .snapshotDirectory("/tmp/atlaskv-test/solo/snapshots")
                    .build();

            assertThat(config.peerIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation failures")
    class ValidationFailures {

        @Test
        @DisplayName("Rejects null nodeId")
        void rejectsNullNodeId() {
            assertThatThrownBy(() -> ClusterConfig.builder()
                    .listenAddress("localhost", 9001)
                    .storageDirectory("/tmp/storage")
                    .snapshotDirectory("/tmp/snapshots")
                    .build())
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("nodeId must not be null");
        }

        @Test
        @DisplayName("Rejects null listenAddress")
        void rejectsNullListenAddress() {
            assertThatThrownBy(() -> ClusterConfig.builder()
                    .nodeId("node-1")
                    .storageDirectory("/tmp/storage")
                    .snapshotDirectory("/tmp/snapshots")
                    .build())
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("listenAddress must not be null");
        }

        @Test
        @DisplayName("Rejects blank storageDirectory")
        void rejectsBlankStorageDirectory() {
            assertThatThrownBy(() -> ClusterConfig.builder()
                    .nodeId("node-1")
                    .listenAddress("localhost", 9001)
                    .storageDirectory("  ")
                    .snapshotDirectory("/tmp/snapshots")
                    .build())
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("storageDirectory must not be null or blank");
        }

        @Test
        @DisplayName("Rejects blank snapshotDirectory")
        void rejectsBlankSnapshotDirectory() {
            assertThatThrownBy(() -> ClusterConfig.builder()
                    .nodeId("node-1")
                    .listenAddress("localhost", 9001)
                    .storageDirectory("/tmp/storage")
                    .snapshotDirectory("")
                    .build())
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("snapshotDirectory must not be null or blank");
        }

        @Test
        @DisplayName("Rejects maxElectionTimeout less than minElectionTimeout")
        void rejectsInvertedElectionTimeout() {
            assertThatThrownBy(() -> minimalConfig()
                    .electionTimeout(Duration.ofMillis(500), Duration.ofMillis(100))
                    .build())
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("maxElectionTimeout must be >= minElectionTimeout");
        }

        @Test
        @DisplayName("Rejects heartbeatInterval greater than or equal to minElectionTimeout")
        void rejectsHeartbeatExceedsElection() {
            assertThatThrownBy(() -> minimalConfig()
                    .heartbeatInterval(Duration.ofMillis(200))
                    .build())
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("heartbeatInterval must be less than minElectionTimeout");
        }

        @Test
        @DisplayName("Rejects zero snapshotThresholdEntries")
        void rejectsZeroSnapshotThreshold() {
            assertThatThrownBy(() -> minimalConfig()
                    .snapshotThresholdEntries(0)
                    .build())
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("snapshotThresholdEntries must be > 0");
        }

        @Test
        @DisplayName("Rejects self in peer list")
        void rejectsSelfInPeerList() {
            assertThatThrownBy(() -> ClusterConfig.builder()
                    .nodeId("node-1")
                    .listenAddress("localhost", 9001)
                    .addPeer("node-1", "localhost", 9001)
                    .storageDirectory("/tmp/storage")
                    .snapshotDirectory("/tmp/snapshots")
                    .build())
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("must not contain the node's own ID");
        }

        @Test
        @DisplayName("Collects multiple violations in a single exception")
        void collectsMultipleViolations() {
            assertThatThrownBy(() -> ClusterConfig.builder()
                    .electionTimeout(Duration.ofMillis(-1), Duration.ofMillis(100))
                    .build())
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("nodeId must not be null")
                    .hasMessageContaining("minElectionTimeout must be positive");
        }
    }

    private static ClusterConfig.Builder minimalConfig() {
        return ClusterConfig.builder()
                .nodeId("node-1")
                .listenAddress("localhost", 9001)
                .storageDirectory("/tmp/atlaskv-test/node-1/storage")
                .snapshotDirectory("/tmp/atlaskv-test/node-1/snapshots");
    }
}
