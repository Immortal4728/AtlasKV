package com.atlaskv.test;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.NodeId;
import com.atlaskv.core.RaftRole;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class LogReplicationIntegrationTest {

    private RaftTestHarness harness;
    private NodeId peer1;

    @BeforeEach
    void setUp() {
        harness = RaftTestHarness.threeNodeCluster();
        peer1 = NodeId.of("node-2");
    }

    @AfterEach
    void tearDown() {
        harness.close();
    }

    @Test
    @DisplayName("Leader appends command and sends AppendEntries to peers")
    void leaderAppendsAndReplicates() {
        harness.electLeader();

        CompletableFuture<byte[]> future = harness.submitCommand("set key value");
        assertThat(future.isDone()).isFalse();

        assertThat(harness.logStorage().getLastLogIndex()).isEqualTo(2L);
        assertThat(harness.logStorage().getTermAt(2)).isEqualTo(1L);

        StubTransport.SentAppendEntries lastAppendPeer1 = harness.transport().lastAppendEntriesTo(peer1);
        assertThat(lastAppendPeer1).isNotNull();
        assertThat(lastAppendPeer1.args().entries()).hasSize(2);
        assertThat(new String(lastAppendPeer1.args().entries().get(1).command(), StandardCharsets.UTF_8))
                .isEqualTo("set key value");
    }

    @Test
    @DisplayName("Leader commits and completes future when majority acknowledges")
    void leaderCommitsOnMajorityAck() {
        harness.electLeader();

        CompletableFuture<byte[]> future = harness.submitCommand("set key value");
        assertThat(future.isDone()).isFalse();

        // Acknowledge from peer1
        harness.ackReplication(peer1, 2L);

        assertThat(future.isDone()).isTrue();
        assertThat(future.join()).isEqualTo("set key value".getBytes(StandardCharsets.UTF_8));
        assertThat(harness.node().commitIndex()).isEqualTo(2L);
        assertThat(harness.node().lastApplied()).isEqualTo(2L);
        assertThat(harness.stateMachine().appliedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Leader retries with lower nextIndex when follower rejects AppendEntries")
    void leaderBacktracksOnRejection() {
        harness.electLeader();

        harness.submitCommand("command-1");

        // Peer 1 rejects (because its log is behind/inconsistent)
        harness.nackReplication(peer1);

        // Leader should attempt to send AppendEntries with lower prevLogIndex
        StubTransport.SentAppendEntries retrySend = harness.transport().lastAppendEntriesTo(peer1);
        assertThat(retrySend).isNotNull();
        assertThat(retrySend.args().prevLogIndex()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Follower rejects AppendEntries if prevLogTerm does not match")
    void followerRejectsMismatchedTerm() {
        // Pre-populate follower log with entry at index 1, term 1
        harness.logStorage().append(new LogEntry(1, 1, "old".getBytes(StandardCharsets.UTF_8)));

        // Leader sends entry with prevLogIndex=1 but prevLogTerm=2 (mismatch)
        AppendEntriesArgs args = new AppendEntriesArgs(
                2L, peer1, 1L, 2L,
                List.of(new LogEntry(2, 2, "new".getBytes(StandardCharsets.UTF_8))),
                0L
        );

        AppendEntriesReply reply = harness.receiveAppendEntries(args);
        assertThat(reply.success()).isFalse();
        assertThat(harness.logStorage().getLastLogIndex()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Follower overwrites conflicting uncommitted entries")
    void followerOverwritesConflicts() {
        // Pre-populate follower log with 3 entries from term 1
        harness.logStorage().append(new LogEntry(1, 1, "cmd1".getBytes(StandardCharsets.UTF_8)));
        harness.logStorage().append(new LogEntry(2, 1, "cmd2".getBytes(StandardCharsets.UTF_8)));
        harness.logStorage().append(new LogEntry(3, 1, "cmd3".getBytes(StandardCharsets.UTF_8)));

        // New leader sends AppendEntries starting at index 2 with term 2
        AppendEntriesArgs args = new AppendEntriesArgs(
                2L, peer1, 1L, 1L,
                List.of(
                        new LogEntry(2, 2, "newCmd2".getBytes(StandardCharsets.UTF_8)),
                        new LogEntry(3, 2, "newCmd3".getBytes(StandardCharsets.UTF_8))
                ),
                2L
        );

        AppendEntriesReply reply = harness.receiveAppendEntries(args);
        assertThat(reply.success()).isTrue();
        assertThat(harness.logStorage().getLastLogIndex()).isEqualTo(3L);
        assertThat(harness.logStorage().getTermAt(2)).isEqualTo(2L);
        assertThat(harness.logStorage().getTermAt(3)).isEqualTo(2L);
        assertThat(harness.node().commitIndex()).isEqualTo(2L);
        assertThat(harness.node().lastApplied()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Idempotent AppendEntries does not corrupt log when duplicate entries are received")
    void followerHandlesDuplicateAppendEntries() {
        LogEntry entry = new LogEntry(1, 1, "cmd1".getBytes(StandardCharsets.UTF_8));
        AppendEntriesArgs args = new AppendEntriesArgs(1L, peer1, 0L, 0L, List.of(entry), 1L);

        // Receive twice
        AppendEntriesReply reply1 = harness.receiveAppendEntries(args);
        AppendEntriesReply reply2 = harness.receiveAppendEntries(args);

        assertThat(reply1.success()).isTrue();
        assertThat(reply2.success()).isTrue();
        assertThat(harness.logStorage().getLastLogIndex()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Leader step-down cancels all pending client command futures")
    void pendingFuturesFailOnStepDown() {
        harness.electLeader();

        CompletableFuture<byte[]> f1 = harness.submitCommand("cmd1");
        CompletableFuture<byte[]> f2 = harness.submitCommand("cmd2");

        // Leader sees higher term in RPC reply
        harness.handleEvent(new com.atlaskv.core.event.RaftEvent.InboundAppendEntriesReplyEvent(
                peer1, new AppendEntriesReply(5L, false, 0L)));

        assertThat(harness.node().role()).isEqualTo(RaftRole.FOLLOWER);
        assertThat(f1.isCompletedExceptionally()).isTrue();
        assertThat(f2.isCompletedExceptionally()).isTrue();
    }
}
