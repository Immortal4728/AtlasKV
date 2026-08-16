package com.atlaskv.test;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.rpc.InstallSnapshotArgs;
import com.atlaskv.core.rpc.InstallSnapshotReply;
import com.atlaskv.core.storage.SnapshotMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotIntegrationTest {

    private RaftTestHarness harness;

    @BeforeEach
    void setUp() {
        harness = RaftTestHarness.builder()
                .snapshotThresholdEntries(3) // Auto-snapshot after 3 committed entries
                .build();
    }

    @AfterEach
    void tearDown() {
        harness.close();
    }

    @Test
    @DisplayName("Test automatic snapshot creation on threshold")
    void testAutomaticSnapshotCreationOnThreshold() {
        harness.electLeader();

        CompletableFuture<byte[]> f1 = harness.submitCommand("cmd1");
        harness.ackReplication(NodeId.of("node-2"), 2);
        f1.join();

        CompletableFuture<byte[]> f2 = harness.submitCommand("cmd2");
        harness.ackReplication(NodeId.of("node-2"), 3);
        f2.join();

        CompletableFuture<byte[]> f3 = harness.submitCommand("cmd3");
        harness.ackReplication(NodeId.of("node-2"), 4);
        f3.join();

        // 3 entries applied (1 NOOP + 2 commands) >= threshold (3) -> Auto snapshot triggered at index 3!
        Optional<SnapshotMetadata> metaOpt = harness.node().snapshotStorage().getLatestSnapshotMetadata();
        assertTrue(metaOpt.isPresent());
        assertEquals(3L, metaOpt.get().lastIncludedIndex());
        assertEquals(1L, metaOpt.get().lastIncludedTerm());

        // Verify log is compacted up to index 3
        assertEquals(4L, harness.logStorage().getFirstLogIndex());
    }

    @Test
    @DisplayName("Test leader sends InstallSnapshot to lagging follower")
    void testLeaderSendsInstallSnapshotToLaggingFollower() {
        harness.electLeader();

        // Submit and commit 6 commands (indices 2 to 7)
        for (int i = 1; i <= 6; i++) {
            CompletableFuture<byte[]> f = harness.submitCommand("cmd" + i);
            harness.ackReplication(NodeId.of("node-2"), i + 1);
            f.join();
        }

        harness.transport().clearHistory();

        // Heartbeat timeout to trigger replication to node-3 (which is still at nextIndex=1)
        harness.handleEvent(new com.atlaskv.core.event.RaftEvent.HeartbeatTimeoutEvent());

        // node-3 nextIndex (1) < leader firstLogIndex -> Leader must send InstallSnapshot to node-3
        StubTransport.SentInstallSnapshot sentSnap = harness.transport().lastInstallSnapshotTo(NodeId.of("node-3"));
        assertNotNull(sentSnap);
        assertEquals(6L, sentSnap.args().lastIncludedIndex());
        assertEquals(1L, sentSnap.args().lastIncludedTerm());

        // node-3 replies success -> leader updates matchIndex & nextIndex for node-3
        harness.ackInstallSnapshot(NodeId.of("node-3"));
    }

    @Test
    void testFollowerAppliesInstallSnapshot() {
        InstallSnapshotArgs args = new InstallSnapshotArgs(
                1L,
                NodeId.of("node-2"),
                10L,
                1L,
                0,
                new byte[]{0, 0, 0, 1, 0, 0, 0, 4, 't', 'e', 's', 't'},
                true
        );

        InstallSnapshotReply reply = harness.receiveInstallSnapshot(args);
        assertTrue(reply.success());
        assertEquals(1L, reply.term());
        assertEquals(10L, harness.node().commitIndex());
        assertEquals(10L, harness.node().lastApplied());
        assertEquals(11L, harness.logStorage().getFirstLogIndex());

        assertEquals(1, harness.stateMachine().appliedCount());
        assertArrayEquals("test".getBytes(), harness.stateMachine().appliedCommands().get(0));
    }
}
