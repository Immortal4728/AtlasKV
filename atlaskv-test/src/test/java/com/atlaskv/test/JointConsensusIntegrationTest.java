package com.atlaskv.test;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.RaftRole;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.core.storage.SnapshotMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JointConsensusIntegrationTest {

    private RaftTestHarness harness;

    @BeforeEach
    void setUp() {
        harness = RaftTestHarness.threeNodeCluster();
    }

    @AfterEach
    void tearDown() {
        harness.close();
    }

    @Test
    @DisplayName("1. Add member transitions through joint consensus to new configuration")
    void testAddMember() {
        harness.electLeader();

        CompletableFuture<Void> future = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.ADD, NodeId.of("node-4"), future));

        assertTrue(harness.node().currentMembership().isJoint());
        assertEquals(Set.of(NodeId.of("node-1"), NodeId.of("node-2"), NodeId.of("node-3")), harness.node().currentMembership().oldMembers());
        assertEquals(Set.of(NodeId.of("node-1"), NodeId.of("node-2"), NodeId.of("node-3"), NodeId.of("node-4")), harness.node().currentMembership().newMembers());

        // Ack from old majority (node-2) and new majority (node-4)
        harness.ackReplication(NodeId.of("node-2"), harness.logStorage().getLastLogIndex());
        harness.ackReplication(NodeId.of("node-4"), harness.logStorage().getLastLogIndex());

        // Transition auto-commits C_new
        harness.ackReplication(NodeId.of("node-2"), harness.logStorage().getLastLogIndex());
        harness.ackReplication(NodeId.of("node-4"), harness.logStorage().getLastLogIndex());

        future.join();
        assertFalse(harness.node().currentMembership().isJoint());
        assertTrue(harness.node().currentMembership().oldMembers().contains(NodeId.of("node-4")));
    }

    @Test
    @DisplayName("2. Remove member transitions through joint consensus")
    void testRemoveMember() {
        harness.electLeader();

        CompletableFuture<Void> future = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.REMOVE, NodeId.of("node-3"), future));

        assertTrue(harness.node().currentMembership().isJoint());

        // Ack replication to commit joint entry and then single C_new entry
        harness.ackReplication(NodeId.of("node-2"), harness.logStorage().getLastLogIndex());
        harness.ackReplication(NodeId.of("node-2"), harness.logStorage().getLastLogIndex());

        future.join();
        assertFalse(harness.node().currentMembership().isJoint());
        assertFalse(harness.node().currentMembership().oldMembers().contains(NodeId.of("node-3")));
    }

    @Test
    @DisplayName("3. Restart during configuration change recovers joint state from WAL log scan")
    void testRestartDuringConfigurationChange() {
        harness.electLeader();
        CompletableFuture<Void> future = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.ADD, NodeId.of("node-4"), future));

        assertTrue(harness.node().currentMembership().isJoint());

        // Create new node instance sharing the same log storage
        com.atlaskv.core.RaftNode restarted = new com.atlaskv.core.RaftNode(
                harness.config(),
                new com.atlaskv.test.SimulatedClock(),
                harness.logStorage(),
                new com.atlaskv.core.storage.InMemoryPersistentStateStore(),
                harness.transport(),
                harness.stateMachine()
        );
        assertTrue(restarted.currentMembership().isJoint());
        assertEquals(Set.of(NodeId.of("node-1"), NodeId.of("node-2"), NodeId.of("node-3"), NodeId.of("node-4")), restarted.currentMembership().newMembers());
        restarted.close();
    }

    @Test
    @DisplayName("4. Snapshot recovery preserves active cluster membership")
    void testSnapshotRecoveryWithMembershipState() {
        harness.electLeader();
        CompletableFuture<Void> future = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.ADD, NodeId.of("node-4"), future));
        harness.ackReplication(NodeId.of("node-2"), harness.logStorage().getLastLogIndex());
        harness.ackReplication(NodeId.of("node-4"), harness.logStorage().getLastLogIndex());
        harness.ackReplication(NodeId.of("node-2"), harness.logStorage().getLastLogIndex());
        harness.ackReplication(NodeId.of("node-4"), harness.logStorage().getLastLogIndex());

        SnapshotMetadata meta = harness.takeSnapshot();
        assertNotNull(meta.membership());
        assertTrue(meta.membership().oldMembers().contains(NodeId.of("node-4")));
    }

    @Test
    @DisplayName("5. Leader failover during joint consensus preserves safe state")
    void testLeaderFailoverDuringConfigurationChange() {
        harness.electLeader();
        CompletableFuture<Void> future = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.ADD, NodeId.of("node-4"), future));

        // Step down leader by receiving AppendEntries with higher term
        harness.receiveAppendEntries(new com.atlaskv.core.rpc.AppendEntriesArgs(
                2L, NodeId.of("node-2"), 0L, 0L, java.util.List.of(), 0L));

        assertEquals(RaftRole.FOLLOWER, harness.node().role());
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("6. Invalid membership change requests fail exceptionally")
    void testInvalidMembershipChangeRequests() {
        harness.electLeader();

        CompletableFuture<Void> futureRemoveNonExistent = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.REMOVE, NodeId.of("node-99"), futureRemoveNonExistent));
        assertTrue(futureRemoveNonExistent.isCompletedExceptionally());
    }

    @Test
    @DisplayName("7. Duplicate member addition fails with IllegalArgumentException")
    void testDuplicateMemberAddition() {
        harness.electLeader();

        CompletableFuture<Void> future = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.ADD, NodeId.of("node-2"), future));
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("8. Removing leader triggers joint consensus and leader steps down upon commit")
    void testRemoveLeader() {
        harness.electLeader();

        CompletableFuture<Void> future = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.REMOVE, NodeId.of("node-1"), future));

        assertTrue(harness.node().currentMembership().isJoint());
        harness.ackReplication(NodeId.of("node-2"), harness.logStorage().getLastLogIndex());
        harness.ackReplication(NodeId.of("node-3"), harness.logStorage().getLastLogIndex());

        harness.ackReplication(NodeId.of("node-2"), harness.logStorage().getLastLogIndex());
        harness.ackReplication(NodeId.of("node-3"), harness.logStorage().getLastLogIndex());

        future.join();
        assertEquals(RaftRole.FOLLOWER, harness.node().role());
    }

    @Test
    @DisplayName("9. Quorum preservation requires majorities from both Cold and Cnew in joint consensus")
    void testQuorumPreservation() {
        harness.electLeader();

        CompletableFuture<Void> future = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.ADD, NodeId.of("node-4"), future));

        long jointIndex = harness.logStorage().getLastLogIndex();

        // Ack only from node-2 (Cold majority present, Cnew majority missing node-4)
        harness.ackReplication(NodeId.of("node-2"), jointIndex);
        assertEquals(0L, harness.node().commitIndex()); // Joint entry not committed yet!

        // Ack from node-4 (satisfies Cnew majority) -> Joint entry commits!
        harness.ackReplication(NodeId.of("node-4"), jointIndex);
        assertTrue(harness.node().commitIndex() >= jointIndex);
    }

    @Test
    @DisplayName("10. Concurrent membership requests fail with IllegalStateException")
    void testConcurrentMembershipRequests() {
        harness.electLeader();

        CompletableFuture<Void> f1 = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.ADD, NodeId.of("node-4"), f1));

        CompletableFuture<Void> f2 = new CompletableFuture<>();
        harness.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.ADD, NodeId.of("node-5"), f2));

        assertTrue(f2.isCompletedExceptionally());
    }
}
