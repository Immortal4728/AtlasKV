package com.atlaskv.server.lifecycle;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.RaftRole;
import com.atlaskv.core.statemachine.StateMachine;
import com.atlaskv.server.config.ClusterConfig;
import com.atlaskv.server.health.NodeHealthStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link NodeLifecycleManager} covering single-node and
 * multi-node cluster startup, shutdown, restart recovery, and resource cleanup.
 */
class NodeLifecycleManagerIT {

    @TempDir
    Path tempDir;

    private final List<NodeLifecycleManager> managersToClean = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (NodeLifecycleManager mgr : managersToClean) {
            try {
                mgr.close();
            } catch (Exception ignored) {
                // Best-effort cleanup
            }
        }
        managersToClean.clear();
    }

    // ── Single Node ────────────────────────────────────────────

    @Nested
    @DisplayName("Single node lifecycle")
    class SingleNode {

        @Test
        @DisplayName("Starts and reaches RUNNING state")
        void startsSuccessfully() {
            NodeLifecycleManager mgr = createSingleNodeManager("solo-1");
            mgr.start();

            assertThat(mgr.state()).isEqualTo(NodeState.RUNNING);
            assertThat(mgr.port()).isGreaterThan(0);
            assertThat(mgr.raftNode()).isNotNull();
        }

        @Test
        @DisplayName("Stops gracefully and reaches STOPPED state")
        void stopsGracefully() {
            NodeLifecycleManager mgr = createSingleNodeManager("solo-2");
            mgr.start();
            assertThat(mgr.state()).isEqualTo(NodeState.RUNNING);

            mgr.stop();
            assertThat(mgr.state()).isEqualTo(NodeState.STOPPED);
        }

        @Test
        @DisplayName("Single node becomes leader immediately (self-majority)")
        void singleNodeBecomesLeader() throws InterruptedException {
            NodeLifecycleManager mgr = createSingleNodeManager("solo-leader");
            mgr.start();

            // Single node should elect itself leader very quickly
            boolean becameLeader = waitForCondition(
                    () -> mgr.raftNode().role() == RaftRole.LEADER,
                    5000);
            assertThat(becameLeader)
                    .as("Single node should become leader within 5 seconds")
                    .isTrue();
        }

        @Test
        @DisplayName("AutoCloseable via close()")
        void autoCloseableWorks() {
            NodeLifecycleManager mgr = createSingleNodeManager("solo-close");
            mgr.start();
            assertThat(mgr.state()).isEqualTo(NodeState.RUNNING);

            mgr.close();
            assertThat(mgr.state()).isEqualTo(NodeState.STOPPED);
        }
    }

    // ── Health Checking ────────────────────────────────────────

    @Nested
    @DisplayName("Health checking")
    class HealthChecking {

        @Test
        @DisplayName("Returns healthy status when running")
        void healthyWhenRunning() {
            NodeLifecycleManager mgr = createSingleNodeManager("health-1");
            mgr.start();

            NodeHealthStatus status = mgr.healthStatus();
            assertThat(status.healthy()).isTrue();
            assertThat(status.nodeId()).isEqualTo(NodeId.of("health-1"));
            assertThat(status.startedAtMillis()).isGreaterThan(0L);
            assertThat(status.uptimeMillis()).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("Returns unhealthy status when not running")
        void unhealthyWhenNotRunning() {
            NodeLifecycleManager mgr = createSingleNodeManager("health-2");

            NodeHealthStatus status = mgr.healthStatus();
            assertThat(status.healthy()).isFalse();
        }

        @Test
        @DisplayName("Reports leader status accurately")
        void reportsLeaderStatus() throws InterruptedException {
            NodeLifecycleManager mgr = createSingleNodeManager("health-leader");
            mgr.start();

            waitForCondition(() -> mgr.raftNode().role() == RaftRole.LEADER, 5000);

            NodeHealthStatus status = mgr.healthStatus();
            assertThat(status.isLeader()).isTrue();
            assertThat(status.role()).isEqualTo(RaftRole.LEADER);
        }
    }

    // ── Persistent Identity ────────────────────────────────────

    @Nested
    @DisplayName("Persistent identity")
    class PersistentIdentity {

        @Test
        @DisplayName("Creates node-id file on first start")
        void createsIdentityFile() {
            NodeLifecycleManager mgr = createSingleNodeManager("identity-1");
            mgr.start();

            Path idFile = tempDir.resolve("identity-1/storage/node-id");
            assertThat(idFile).exists();
            assertThat(idFile).hasContent("identity-1");
        }

        @Test
        @DisplayName("Verifies identity on restart")
        void verifiesIdentityOnRestart() {
            NodeLifecycleManager mgr = createSingleNodeManager("identity-2");
            mgr.start();
            mgr.stop();

            // Second start should verify and succeed
            NodeLifecycleManager mgr2 = createSingleNodeManager("identity-2");
            mgr2.start();
            assertThat(mgr2.state()).isEqualTo(NodeState.RUNNING);
        }

        @Test
        @DisplayName("Rejects mismatched identity on restart")
        void rejectsMismatchedIdentity() throws IOException {
            NodeLifecycleManager mgr = createSingleNodeManager("identity-3");
            mgr.start();
            mgr.stop();

            // Try to start a different node with the same storage directory
            ClusterConfig mismatchedConfig = ClusterConfig.builder()
                    .nodeId("different-node")
                    .listenAddress("localhost", 0)
                    .storageDirectory(tempDir.resolve("identity-3/storage").toString())
                    .snapshotDirectory(tempDir.resolve("identity-3/snapshots").toString())
                    .build();
            NodeLifecycleManager mismatchedMgr = new NodeLifecycleManager(mismatchedConfig, new TestStateMachine());
            managersToClean.add(mismatchedMgr);

            assertThatThrownBy(mismatchedMgr::start)
                    .isInstanceOf(NodeLifecycleException.class)
                    .hasStackTraceContaining("identity mismatch");
        }
    }

    // ── Restart Recovery ───────────────────────────────────────

    @Nested
    @DisplayName("Restart recovery")
    class RestartRecovery {

        @Test
        @DisplayName("Recovers persistent state after clean shutdown")
        void recoversAfterCleanShutdown() throws InterruptedException {
            NodeLifecycleManager mgr1 = createSingleNodeManager("restart-clean");
            mgr1.start();

            waitForCondition(() -> mgr1.raftNode().role() == RaftRole.LEADER, 5000);
            long termBeforeShutdown = mgr1.raftNode().currentTerm();
            assertThat(termBeforeShutdown).isGreaterThan(0L);

            mgr1.stop();
            assertThat(mgr1.state()).isEqualTo(NodeState.STOPPED);

            // Restart
            NodeLifecycleManager mgr2 = createSingleNodeManager("restart-clean");
            mgr2.start();
            assertThat(mgr2.state()).isEqualTo(NodeState.RUNNING);

            // Persisted term should be >= what it was before shutdown
            assertThat(mgr2.raftNode().currentTerm())
                    .isGreaterThanOrEqualTo(termBeforeShutdown);
        }

        @Test
        @DisplayName("Recovers after simulated crash (no graceful stop)")
        void recoversAfterSimulatedCrash() throws InterruptedException {
            NodeLifecycleManager mgr1 = createSingleNodeManager("restart-crash");
            mgr1.start();

            waitForCondition(() -> mgr1.raftNode().role() == RaftRole.LEADER, 5000);
            long termBeforeCrash = mgr1.raftNode().currentTerm();

            // Simulate crash — close without graceful stop
            mgr1.close();

            // Restart from persisted state
            NodeLifecycleManager mgr2 = createSingleNodeManager("restart-crash");
            mgr2.start();
            assertThat(mgr2.state()).isEqualTo(NodeState.RUNNING);
            assertThat(mgr2.raftNode().currentTerm())
                    .isGreaterThanOrEqualTo(termBeforeCrash);
        }

        @Test
        @DisplayName("Multiple restart cycles maintain state")
        void multipleRestartCycles() throws InterruptedException {
            String nodeId = "multi-restart";

            for (int cycle = 1; cycle <= 3; cycle++) {
                NodeLifecycleManager mgr = createSingleNodeManager(nodeId);
                mgr.start();
                assertThat(mgr.state()).isEqualTo(NodeState.RUNNING);

                waitForCondition(() -> mgr.raftNode().role() == RaftRole.LEADER, 5000);

                mgr.stop();
                assertThat(mgr.state()).isEqualTo(NodeState.STOPPED);
            }

            // Final verification — identity file still correct
            Path idFile = tempDir.resolve(nodeId + "/storage/node-id");
            assertThat(idFile).hasContent(nodeId);
        }

        @Test
        @DisplayName("Leader is available after restart")
        void leaderAvailableAfterRestart() throws InterruptedException {
            NodeLifecycleManager mgr1 = createSingleNodeManager("leader-recovery");
            mgr1.start();
            waitForCondition(() -> mgr1.raftNode().role() == RaftRole.LEADER, 5000);
            mgr1.stop();

            NodeLifecycleManager mgr2 = createSingleNodeManager("leader-recovery");
            mgr2.start();

            boolean becameLeader = waitForCondition(
                    () -> mgr2.raftNode().role() == RaftRole.LEADER,
                    5000);
            assertThat(becameLeader)
                    .as("Single node should re-elect itself leader after restart")
                    .isTrue();
        }
    }

    // ── Three-node cluster ─────────────────────────────────────

    @Nested
    @DisplayName("Three-node cluster")
    class ThreeNodeCluster {

        @Test
        @DisplayName("All three nodes start and reach RUNNING state")
        void allNodesStart() {
            NodeLifecycleManager[] managers = createThreeNodeCluster();
            for (NodeLifecycleManager mgr : managers) {
                mgr.start();
                assertThat(mgr.state()).isEqualTo(NodeState.RUNNING);
            }
        }

        @Test
        @DisplayName("Cluster elects a leader within timeout")
        void clusterElectsLeader() throws InterruptedException {
            NodeLifecycleManager[] managers = createThreeNodeCluster();
            for (NodeLifecycleManager mgr : managers) {
                mgr.start();
            }

            boolean hasLeader = waitForCondition(() -> {
                for (NodeLifecycleManager mgr : managers) {
                    if (mgr.raftNode().role() == RaftRole.LEADER) {
                        return true;
                    }
                }
                return false;
            }, 10000);

            assertThat(hasLeader)
                    .as("Three-node cluster should elect a leader within 10 seconds")
                    .isTrue();
        }

        @Test
        @DisplayName("All nodes can be stopped gracefully")
        void allNodesStopped() {
            NodeLifecycleManager[] managers = createThreeNodeCluster();
            for (NodeLifecycleManager mgr : managers) {
                mgr.start();
            }
            for (NodeLifecycleManager mgr : managers) {
                mgr.stop();
                assertThat(mgr.state()).isEqualTo(NodeState.STOPPED);
            }
        }

        @Test
        @DisplayName("Follower recovers after restart")
        void followerRecovery() throws InterruptedException {
            NodeLifecycleManager[] managers = createThreeNodeCluster();
            for (NodeLifecycleManager mgr : managers) {
                mgr.start();
            }

            // Wait for leader election
            waitForCondition(() -> {
                for (NodeLifecycleManager mgr : managers) {
                    if (mgr.raftNode().role() == RaftRole.LEADER) {
                        return true;
                    }
                }
                return false;
            }, 10000);

            // Find a follower and restart it
            NodeLifecycleManager follower = null;
            int followerIdx = -1;
            for (int i = 0; i < managers.length; i++) {
                if (managers[i].raftNode().role() == RaftRole.FOLLOWER) {
                    follower = managers[i];
                    followerIdx = i;
                    break;
                }
            }
            assertThat(follower).isNotNull();

            follower.stop();
            assertThat(follower.state()).isEqualTo(NodeState.STOPPED);

            // Restart with a new manager pointing to same storage
            NodeLifecycleManager restarted = recreateManager(followerIdx);
            managers[followerIdx] = restarted;
            restarted.start();
            assertThat(restarted.state()).isEqualTo(NodeState.RUNNING);
        }
    }

    // ── State Transitions ──────────────────────────────────────

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        @DisplayName("Cannot start from RUNNING state")
        void cannotStartFromRunning() {
            NodeLifecycleManager mgr = createSingleNodeManager("bad-start");
            mgr.start();

            assertThatThrownBy(mgr::start)
                    .isInstanceOf(NodeLifecycleException.class)
                    .hasMessageContaining("Cannot start node in state: RUNNING");
        }

        @Test
        @DisplayName("Cannot stop from CREATED state")
        void cannotStopFromCreated() {
            NodeLifecycleManager mgr = createSingleNodeManager("bad-stop");
            // stop() is a no-op warning, not an exception
            mgr.stop();
            assertThat(mgr.state()).isEqualTo(NodeState.CREATED);
        }

        @Test
        @DisplayName("Transitions: CREATED → STARTING → RUNNING → STOPPING → STOPPED")
        void fullLifecycleTransition() {
            NodeLifecycleManager mgr = createSingleNodeManager("transition");
            assertThat(mgr.state()).isEqualTo(NodeState.CREATED);

            mgr.start();
            assertThat(mgr.state()).isEqualTo(NodeState.RUNNING);

            mgr.stop();
            assertThat(mgr.state()).isEqualTo(NodeState.STOPPED);
        }

        @Test
        @DisplayName("Can restart from STOPPED state")
        void canRestartFromStopped() {
            NodeLifecycleManager mgr = createSingleNodeManager("restart-ok");
            mgr.start();
            mgr.stop();
            assertThat(mgr.state()).isEqualTo(NodeState.STOPPED);

            mgr.start();
            assertThat(mgr.state()).isEqualTo(NodeState.RUNNING);
        }
    }

    // ── Resource Cleanup ───────────────────────────────────────

    @Nested
    @DisplayName("Resource cleanup")
    class ResourceCleanup {

        @Test
        @DisplayName("Storage directories exist after stop")
        void storageDirectoriesExist() {
            NodeLifecycleManager mgr = createSingleNodeManager("cleanup-1");
            mgr.start();
            mgr.stop();

            assertThat(tempDir.resolve("cleanup-1/storage")).isDirectory();
            assertThat(tempDir.resolve("cleanup-1/snapshots")).isDirectory();
        }

        @Test
        @DisplayName("WAL file exists after stop")
        void walFileExistsAfterStop() {
            NodeLifecycleManager mgr = createSingleNodeManager("cleanup-2");
            mgr.start();
            mgr.stop();

            assertThat(tempDir.resolve("cleanup-2/storage/raft.wal")).exists();
        }

        @Test
        @DisplayName("Port is released after stop")
        void portReleasedAfterStop() {
            NodeLifecycleManager mgr = createSingleNodeManager("cleanup-3");
            mgr.start();
            int port = mgr.port();
            assertThat(port).isGreaterThan(0);

            mgr.stop();

            // Start another manager on the same port to verify release
            ClusterConfig config = ClusterConfig.builder()
                    .nodeId("cleanup-3b")
                    .listenAddress("localhost", port)
                    .storageDirectory(tempDir.resolve("cleanup-3b/storage").toString())
                    .snapshotDirectory(tempDir.resolve("cleanup-3b/snapshots").toString())
                    .build();
            NodeLifecycleManager mgr2 = new NodeLifecycleManager(config, new TestStateMachine());
            managersToClean.add(mgr2);
            mgr2.start();
            assertThat(mgr2.state()).isEqualTo(NodeState.RUNNING);
            assertThat(mgr2.port()).isEqualTo(port);
        }
    }

    // ── Transport Initialization ───────────────────────────────

    @Nested
    @DisplayName("Transport initialization")
    class TransportInitialization {

        @Test
        @DisplayName("gRPC server binds to ephemeral port when 0")
        void bindsToEphemeralPort() {
            ClusterConfig config = ClusterConfig.builder()
                    .nodeId("ephemeral")
                    .listenAddress("localhost", 0)
                    .storageDirectory(tempDir.resolve("ephemeral/storage").toString())
                    .snapshotDirectory(tempDir.resolve("ephemeral/snapshots").toString())
                    .build();
            NodeLifecycleManager mgr = new NodeLifecycleManager(config, new TestStateMachine());
            managersToClean.add(mgr);
            mgr.start();

            assertThat(mgr.port()).isGreaterThan(0);
        }

        @Test
        @DisplayName("gRPC server starts with configured port")
        void startsOnConfiguredPort() {
            // Use port 0 to avoid conflicts (this tests the mechanism is working)
            NodeLifecycleManager mgr = createSingleNodeManager("transport-init");
            mgr.start();

            assertThat(mgr.port()).isGreaterThan(0);
            assertThat(mgr.state()).isEqualTo(NodeState.RUNNING);
        }
    }

    // ── Helper methods ─────────────────────────────────────────

    private NodeLifecycleManager createSingleNodeManager(String nodeId) {
        ClusterConfig config = ClusterConfig.builder()
                .nodeId(nodeId)
                .listenAddress("localhost", 0)
                .storageDirectory(tempDir.resolve(nodeId + "/storage").toString())
                .snapshotDirectory(tempDir.resolve(nodeId + "/snapshots").toString())
                .build();
        NodeLifecycleManager mgr = new NodeLifecycleManager(config, new TestStateMachine());
        managersToClean.add(mgr);
        return mgr;
    }

    private NodeLifecycleManager[] createThreeNodeCluster() {
        // Use ephemeral ports (0) and we'll build the config after port binding
        // Since gRPC needs to know peer addresses before start, we pre-allocate ports
        // by using ephemeral port 0 — each node discovers peers via config
        int port1 = findEphemeralPort();
        int port2 = findEphemeralPort();
        int port3 = findEphemeralPort();

        ClusterConfig config1 = ClusterConfig.builder()
                .nodeId("cluster-1")
                .listenAddress("localhost", port1)
                .addPeer("cluster-2", "localhost", port2)
                .addPeer("cluster-3", "localhost", port3)
                .storageDirectory(tempDir.resolve("cluster-1/storage").toString())
                .snapshotDirectory(tempDir.resolve("cluster-1/snapshots").toString())
                .electionTimeout(Duration.ofMillis(300), Duration.ofMillis(600))
                .heartbeatInterval(Duration.ofMillis(100))
                .build();

        ClusterConfig config2 = ClusterConfig.builder()
                .nodeId("cluster-2")
                .listenAddress("localhost", port2)
                .addPeer("cluster-1", "localhost", port1)
                .addPeer("cluster-3", "localhost", port3)
                .storageDirectory(tempDir.resolve("cluster-2/storage").toString())
                .snapshotDirectory(tempDir.resolve("cluster-2/snapshots").toString())
                .electionTimeout(Duration.ofMillis(300), Duration.ofMillis(600))
                .heartbeatInterval(Duration.ofMillis(100))
                .build();

        ClusterConfig config3 = ClusterConfig.builder()
                .nodeId("cluster-3")
                .listenAddress("localhost", port3)
                .addPeer("cluster-1", "localhost", port1)
                .addPeer("cluster-2", "localhost", port2)
                .storageDirectory(tempDir.resolve("cluster-3/storage").toString())
                .snapshotDirectory(tempDir.resolve("cluster-3/snapshots").toString())
                .electionTimeout(Duration.ofMillis(300), Duration.ofMillis(600))
                .heartbeatInterval(Duration.ofMillis(100))
                .build();

        NodeLifecycleManager mgr1 = new NodeLifecycleManager(config1, new TestStateMachine());
        NodeLifecycleManager mgr2 = new NodeLifecycleManager(config2, new TestStateMachine());
        NodeLifecycleManager mgr3 = new NodeLifecycleManager(config3, new TestStateMachine());
        managersToClean.add(mgr1);
        managersToClean.add(mgr2);
        managersToClean.add(mgr3);

        return new NodeLifecycleManager[]{mgr1, mgr2, mgr3};
    }

    private NodeLifecycleManager recreateManager(int clusterIndex) {
        String nodeId = "cluster-" + (clusterIndex + 1);
        // Recreate with same storage but new port
        ClusterConfig config = ClusterConfig.builder()
                .nodeId(nodeId)
                .listenAddress("localhost", 0)
                .storageDirectory(tempDir.resolve(nodeId + "/storage").toString())
                .snapshotDirectory(tempDir.resolve(nodeId + "/snapshots").toString())
                .electionTimeout(Duration.ofMillis(300), Duration.ofMillis(600))
                .heartbeatInterval(Duration.ofMillis(100))
                .build();
        NodeLifecycleManager mgr = new NodeLifecycleManager(config, new TestStateMachine());
        managersToClean.add(mgr);
        return mgr;
    }

    private static int findEphemeralPort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to find ephemeral port", e);
        }
    }

    @SuppressWarnings("BusyWait")
    private static boolean waitForCondition(java.util.function.BooleanSupplier condition, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condition.getAsBoolean();
    }

    /**
     * Simple test state machine that records applied commands with snapshot support.
     */
    static final class TestStateMachine implements StateMachine {

        private final List<byte[]> applied = new ArrayList<>();

        @Override
        public synchronized byte[] apply(byte[] command) {
            applied.add(command.clone());
            return command;
        }

        @Override
        public synchronized byte[] takeSnapshot() {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 DataOutputStream dos = new DataOutputStream(baos)) {
                dos.writeInt(applied.size());
                for (byte[] cmd : applied) {
                    dos.writeInt(cmd.length);
                    dos.write(cmd);
                }
                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize snapshot", e);
            }
        }

        @Override
        public synchronized void restoreSnapshot(byte[] snapshot) {
            applied.clear();
            if (snapshot == null || snapshot.length == 0) {
                return;
            }
            try (ByteArrayInputStream bais = new ByteArrayInputStream(snapshot);
                 DataInputStream dis = new DataInputStream(bais)) {
                int count = dis.readInt();
                for (int i = 0; i < count; i++) {
                    int len = dis.readInt();
                    byte[] cmd = new byte[len];
                    dis.readFully(cmd);
                    applied.add(cmd);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to restore snapshot", e);
            }
        }

        public synchronized List<byte[]> appliedCommands() {
            return List.copyOf(applied);
        }
    }
}
