package com.atlaskv.core;

import com.atlaskv.core.clock.Cancellable;
import com.atlaskv.core.clock.Clock;
import com.atlaskv.core.config.RaftConfig;
import com.atlaskv.core.statemachine.StateMachine;
import com.atlaskv.core.storage.InMemoryLogStorage;
import com.atlaskv.core.storage.InMemoryPersistentStateStore;
import com.atlaskv.core.storage.InMemorySnapshotStorage;
import com.atlaskv.core.transport.PeerTransport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test proving that committed WAL entries are replayed into the state
 * machine when a RaftNode is reconstructed with the same log storage (simulating
 * a process restart).
 *
 * <p>Bug: Prior to the fix, RaftNode's constructor never replayed committed
 * entries, leaving commitIndex and lastApplied at 0. The state machine started
 * empty after restart, causing all previously created keys to disappear.
 */
@DisplayName("RaftNode WAL Recovery")
class RaftNodeWalRecoveryTest {

    private static final NodeId SELF = NodeId.of("node-1");
    private static final NodeId PEER1 = NodeId.of("node-2");
    private static final NodeId PEER2 = NodeId.of("node-3");

    @Test
    @DisplayName("committed entries are replayed into state machine after restart")
    void committedEntriesReplayedOnRestart() {
        // ── Phase 1: Normal operation ─────────────────────────────────
        InMemoryLogStorage logStorage = new InMemoryLogStorage();
        InMemoryPersistentStateStore stateStore = new InMemoryPersistentStateStore();
        InMemorySnapshotStorage snapshotStorage = new InMemorySnapshotStorage();
        RecordingStateMachine sm1 = new RecordingStateMachine();
        NoOpClock clock1 = new NoOpClock();
        NoOpTransport transport1 = new NoOpTransport();

        RaftConfig config = new RaftConfig(SELF, Set.of(PEER1, PEER2),
                Duration.ofMillis(150), Duration.ofMillis(300), Duration.ofMillis(50));

        RaftNode node1 = new RaftNode(config, clock1, logStorage,
                stateStore, snapshotStorage, transport1, sm1);

        // Simulate leader writing 3 entries into the log
        byte[] cmd1 = "PUT key1 value1".getBytes(StandardCharsets.UTF_8);
        byte[] cmd2 = "PUT key2 value2".getBytes(StandardCharsets.UTF_8);
        byte[] cmd3 = "PUT key3 value3".getBytes(StandardCharsets.UTF_8);

        logStorage.append(new LogEntry(1, 1, cmd1));
        logStorage.append(new LogEntry(2, 1, cmd2));
        logStorage.append(new LogEntry(3, 1, cmd3));

        // Simulate the entries being committed and applied
        node1.setCommitIndex(3);
        RaftNodeEngine.applyCommitted(node1);

        // Verify state machine received all 3 commands
        assertThat(sm1.appliedCommands).hasSize(3);
        assertThat(new String(sm1.appliedCommands.get(0), StandardCharsets.UTF_8)).isEqualTo("PUT key1 value1");
        assertThat(new String(sm1.appliedCommands.get(1), StandardCharsets.UTF_8)).isEqualTo("PUT key2 value2");
        assertThat(new String(sm1.appliedCommands.get(2), StandardCharsets.UTF_8)).isEqualTo("PUT key3 value3");
        assertThat(node1.lastApplied()).isEqualTo(3);
        assertThat(node1.commitIndex()).isEqualTo(3);

        // ── Phase 2: Simulate restart ─────────────────────────────────
        // Close the old node (but keep the same log storage — simulates WAL on disk)
        node1.close();

        // Create a FRESH state machine (simulates process restart — in-memory state is gone)
        RecordingStateMachine sm2 = new RecordingStateMachine();
        NoOpClock clock2 = new NoOpClock();
        NoOpTransport transport2 = new NoOpTransport();

        // Reconstruct RaftNode with the SAME log storage (entries are still there)
        RaftNode node2 = new RaftNode(config, clock2, logStorage,
                stateStore, snapshotStorage, transport2, sm2);

        // ── Phase 3: Verify recovery ──────────────────────────────────
        // The state machine should have all 3 entries replayed
        assertThat(sm2.appliedCommands)
                .as("State machine should have all 3 entries replayed after restart")
                .hasSize(3);
        assertThat(new String(sm2.appliedCommands.get(0), StandardCharsets.UTF_8)).isEqualTo("PUT key1 value1");
        assertThat(new String(sm2.appliedCommands.get(1), StandardCharsets.UTF_8)).isEqualTo("PUT key2 value2");
        assertThat(new String(sm2.appliedCommands.get(2), StandardCharsets.UTF_8)).isEqualTo("PUT key3 value3");

        // commitIndex and lastApplied should be at 3
        assertThat(node2.commitIndex()).isEqualTo(3);
        assertThat(node2.lastApplied()).isEqualTo(3);

        node2.close();
    }

    @Test
    @DisplayName("empty log results in empty state machine after restart")
    void emptyLogProducesEmptyStateMachine() {
        InMemoryLogStorage logStorage = new InMemoryLogStorage();
        InMemoryPersistentStateStore stateStore = new InMemoryPersistentStateStore();
        InMemorySnapshotStorage snapshotStorage = new InMemorySnapshotStorage();
        RecordingStateMachine sm = new RecordingStateMachine();
        NoOpClock clock = new NoOpClock();
        NoOpTransport transport = new NoOpTransport();

        RaftConfig config = new RaftConfig(SELF, Set.of(PEER1, PEER2),
                Duration.ofMillis(150), Duration.ofMillis(300), Duration.ofMillis(50));

        RaftNode node = new RaftNode(config, clock, logStorage,
                stateStore, snapshotStorage, transport, sm);

        assertThat(sm.appliedCommands).isEmpty();
        assertThat(node.commitIndex()).isEqualTo(0);
        assertThat(node.lastApplied()).isEqualTo(0);

        node.close();
    }

    @Test
    @DisplayName("recovery with snapshot + subsequent WAL entries replays correctly")
    void snapshotPlusWalEntriesReplayedOnRestart() {
        // ── Phase 1: Create node, commit entries, take snapshot, commit more ──
        InMemoryLogStorage logStorage = new InMemoryLogStorage();
        InMemoryPersistentStateStore stateStore = new InMemoryPersistentStateStore();
        InMemorySnapshotStorage snapshotStorage = new InMemorySnapshotStorage();
        RecordingStateMachine sm1 = new RecordingStateMachine();
        NoOpClock clock1 = new NoOpClock();
        NoOpTransport transport1 = new NoOpTransport();

        RaftConfig config = new RaftConfig(SELF, Set.of(PEER1, PEER2),
                Duration.ofMillis(150), Duration.ofMillis(300), Duration.ofMillis(50));

        RaftNode node1 = new RaftNode(config, clock1, logStorage,
                stateStore, snapshotStorage, transport1, sm1);

        // Write 2 entries and commit them
        logStorage.append(new LogEntry(1, 1, "PUT a 1".getBytes(StandardCharsets.UTF_8)));
        logStorage.append(new LogEntry(2, 1, "PUT b 2".getBytes(StandardCharsets.UTF_8)));
        node1.setCommitIndex(2);
        RaftNodeEngine.applyCommitted(node1);

        // Take a snapshot at index 2
        node1.takeSnapshot();

        // Write 2 more entries after the snapshot
        logStorage.append(new LogEntry(3, 1, "PUT c 3".getBytes(StandardCharsets.UTF_8)));
        logStorage.append(new LogEntry(4, 1, "PUT d 4".getBytes(StandardCharsets.UTF_8)));
        node1.setCommitIndex(4);
        RaftNodeEngine.applyCommitted(node1);

        assertThat(sm1.appliedCommands).hasSize(4);

        node1.close();

        // ── Phase 2: Restart with fresh state machine ─────────────────
        RecordingStateMachine sm2 = new RecordingStateMachine();

        RaftNode node2 = new RaftNode(config, new NoOpClock(), logStorage,
                stateStore, snapshotStorage, new NoOpTransport(), sm2);

        // The snapshot restores entries a and b, and WAL replay adds c and d
        // sm2 should have received the restoreSnapshot call (with a+b data) then apply(c), apply(d)
        assertThat(node2.commitIndex()).isEqualTo(4);
        assertThat(node2.lastApplied()).isEqualTo(4);

        // The state machine was restored from snapshot, then had entries 3 and 4 applied
        // appliedCommands only tracks apply() calls (not restoreSnapshot), so should have 2
        assertThat(sm2.appliedCommands).hasSize(2);
        assertThat(new String(sm2.appliedCommands.get(0), StandardCharsets.UTF_8)).isEqualTo("PUT c 3");
        assertThat(new String(sm2.appliedCommands.get(1), StandardCharsets.UTF_8)).isEqualTo("PUT d 4");

        node2.close();
    }

    // ── Test Helpers ──────────────────────────────────────────────────

    private static final class RecordingStateMachine implements StateMachine {
        final List<byte[]> appliedCommands = new CopyOnWriteArrayList<>();
        private byte[] snapshotData;

        @Override
        public byte[] apply(byte[] command) {
            appliedCommands.add(command);
            return command;
        }

        @Override
        public byte[] takeSnapshot() {
            // Simple snapshot: just the count of applied commands
            return snapshotData != null ? snapshotData : new byte[0];
        }

        @Override
        public void restoreSnapshot(byte[] snapshot) {
            // Don't track restoreSnapshot in appliedCommands
            snapshotData = snapshot;
        }
    }

    private static final class NoOpClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return 0;
        }

        @Override
        public Cancellable scheduleOnce(Duration delay, Runnable task) {
            return () -> true;
        }
    }

    private static final class NoOpTransport implements PeerTransport {
        @Override
        public CompletableFuture<com.atlaskv.core.rpc.RequestVoteReply> sendRequestVote(
                NodeId target, com.atlaskv.core.rpc.RequestVoteArgs args) {
            return new CompletableFuture<>();
        }

        @Override
        public CompletableFuture<com.atlaskv.core.rpc.AppendEntriesReply> sendAppendEntries(
                NodeId target, com.atlaskv.core.rpc.AppendEntriesArgs args) {
            return new CompletableFuture<>();
        }

        @Override
        public CompletableFuture<com.atlaskv.core.rpc.InstallSnapshotReply> sendInstallSnapshot(
                NodeId target, com.atlaskv.core.rpc.InstallSnapshotArgs args) {
            return new CompletableFuture<>();
        }

        @Override
        public void close() {
            // no-op
        }
    }
}
