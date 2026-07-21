package com.atlaskv.core;

import com.atlaskv.core.clock.Cancellable;
import com.atlaskv.core.clock.Clock;
import com.atlaskv.core.config.RaftConfig;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.core.statemachine.StateMachine;
import com.atlaskv.core.storage.InMemoryLogStorage;
import com.atlaskv.core.transport.PeerTransport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class RaftNodeTest {

    private NodeId selfId;
    private NodeId peer1;
    private NodeId peer2;
    private TestClock clock;
    private InMemoryLogStorage logStorage;
    private TestTransport transport;
    private TestStateMachine stateMachine;
    private RaftNode raftNode;

    @BeforeEach
    void setUp() {
        selfId = NodeId.of("node-1");
        peer1 = NodeId.of("node-2");
        peer2 = NodeId.of("node-3");
        clock = new TestClock();

        RaftConfig config = new RaftConfig(
                selfId,
                Set.of(peer1, peer2),
                Duration.ofMillis(150),
                Duration.ofMillis(300),
                Duration.ofMillis(50)
        );

        logStorage = new InMemoryLogStorage();
        transport = new TestTransport();
        stateMachine = new TestStateMachine();
        raftNode = new RaftNode(config, clock, logStorage, transport, stateMachine);
    }

    @AfterEach
    void tearDown() {
        raftNode.close();
    }

    // ── Election Tests ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Election")
    class ElectionTests {

        @Test
        @DisplayName("Node starts in FOLLOWER role with term 0")
        void initialState() {
            assertThat(raftNode.role()).isEqualTo(RaftRole.FOLLOWER);
            assertThat(raftNode.currentTerm()).isEqualTo(0L);
            assertThat(raftNode.votedFor()).isNull();
        }

        @Test
        @DisplayName("Election timeout causes transition to CANDIDATE and sends RequestVote RPCs")
        void electionTimeoutTransitionsToCandidate() throws InterruptedException {
            raftNode.start();
            clock.advanceTime(Duration.ofMillis(350));
            Thread.sleep(100);

            assertThat(raftNode.role()).isEqualTo(RaftRole.CANDIDATE);
            assertThat(raftNode.currentTerm()).isEqualTo(1L);
            assertThat(raftNode.votedFor()).isEqualTo(selfId);
            assertThat(transport.sentRequestVotes).hasSize(2);
        }

        @Test
        @DisplayName("Candidate transitions to LEADER upon receiving majority votes")
        void candidateWinsElection() throws InterruptedException {
            raftNode.start();
            clock.advanceTime(Duration.ofMillis(350));
            Thread.sleep(100);

            assertThat(raftNode.role()).isEqualTo(RaftRole.CANDIDATE);

            transport.replyRequestVote(peer1, new RequestVoteReply(1L, true));
            Thread.sleep(100);

            assertThat(raftNode.role()).isEqualTo(RaftRole.LEADER);
            assertThat(raftNode.currentLeader()).isEqualTo(selfId);
            assertThat(transport.sentAppendEntries).isNotEmpty();
        }

        @Test
        @DisplayName("Inbound RequestVote with lower term is rejected")
        void rejectLowerTermRequestVote() {
            raftNode.start();

            CompletableFuture<AppendEntriesReply> aeFuture = new CompletableFuture<>();
            AppendEntriesArgs aeArgs = new AppendEntriesArgs(2L, peer1, 0L, 0L, List.of(), 0L);
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesEvent(aeArgs, aeFuture));
            aeFuture.join();
            assertThat(raftNode.currentTerm()).isEqualTo(2L);

            CompletableFuture<RequestVoteReply> rvFuture = new CompletableFuture<>();
            RequestVoteArgs rvArgs = new RequestVoteArgs(1L, peer2, 0L, 0L);
            raftNode.handleEvent(new RaftEvent.InboundRequestVoteEvent(rvArgs, rvFuture));

            RequestVoteReply reply = rvFuture.join();
            assertThat(reply.voteGranted()).isFalse();
            assertThat(reply.term()).isEqualTo(2L);
        }

        @Test
        @DisplayName("AppendEntries from valid leader resets election timer and confirms leader")
        void appendEntriesResetsElectionTimer() {
            raftNode.start();

            CompletableFuture<AppendEntriesReply> future = new CompletableFuture<>();
            AppendEntriesArgs args = new AppendEntriesArgs(1L, peer1, 0L, 0L, List.of(), 0L);
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesEvent(args, future));

            AppendEntriesReply reply = future.join();
            assertThat(reply.success()).isTrue();
            assertThat(raftNode.currentLeader()).isEqualTo(peer1);
            assertThat(raftNode.currentTerm()).isEqualTo(1L);
            assertThat(raftNode.role()).isEqualTo(RaftRole.FOLLOWER);
        }
    }

    // ── Log Replication Tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("Log Replication")
    class LogReplicationTests {

        @Test
        @DisplayName("Follower appends entries from leader with matching prevLog")
        void followerAppendsEntries() {
            raftNode.start();

            LogEntry entry1 = new LogEntry(1, 1, "set x 1".getBytes(StandardCharsets.UTF_8));
            CompletableFuture<AppendEntriesReply> future = new CompletableFuture<>();
            AppendEntriesArgs args = new AppendEntriesArgs(1L, peer1, 0L, 0L, List.of(entry1), 0L);
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesEvent(args, future));

            AppendEntriesReply reply = future.join();
            assertThat(reply.success()).isTrue();
            assertThat(reply.matchIndex()).isEqualTo(1L);
            assertThat(logStorage.getLastLogIndex()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Follower rejects AppendEntries when prevLogIndex doesn't match")
        void followerRejectsMismatchedPrevLog() {
            raftNode.start();

            CompletableFuture<AppendEntriesReply> future = new CompletableFuture<>();
            LogEntry entry2 = new LogEntry(2, 1, "set y 2".getBytes(StandardCharsets.UTF_8));
            AppendEntriesArgs args = new AppendEntriesArgs(1L, peer1, 1L, 1L, List.of(entry2), 0L);
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesEvent(args, future));

            AppendEntriesReply reply = future.join();
            assertThat(reply.success()).isFalse();
        }

        @Test
        @DisplayName("Follower truncates conflicting entries and appends new ones")
        void followerTruncatesConflictingEntries() {
            raftNode.start();

            logStorage.append(new LogEntry(1, 1, "old".getBytes(StandardCharsets.UTF_8)));

            LogEntry newEntry = new LogEntry(1, 2, "new".getBytes(StandardCharsets.UTF_8));
            CompletableFuture<AppendEntriesReply> future = new CompletableFuture<>();
            AppendEntriesArgs args = new AppendEntriesArgs(2L, peer1, 0L, 0L, List.of(newEntry), 0L);
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesEvent(args, future));

            AppendEntriesReply reply = future.join();
            assertThat(reply.success()).isTrue();
            assertThat(logStorage.getLastLogIndex()).isEqualTo(1L);
            assertThat(logStorage.getTermAt(1)).isEqualTo(2L);
        }

        @Test
        @DisplayName("Follower advances commitIndex and applies entries to state machine")
        void followerAdvancesCommitAndApplies() {
            raftNode.start();

            LogEntry entry1 = new LogEntry(1, 1, "cmd-1".getBytes(StandardCharsets.UTF_8));
            CompletableFuture<AppendEntriesReply> future1 = new CompletableFuture<>();
            AppendEntriesArgs args1 = new AppendEntriesArgs(1L, peer1, 0L, 0L, List.of(entry1), 1L);
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesEvent(args1, future1));

            future1.join();
            assertThat(raftNode.commitIndex()).isEqualTo(1L);
            assertThat(raftNode.lastApplied()).isEqualTo(1L);
            assertThat(stateMachine.appliedCommands).hasSize(1);
        }

        @Test
        @DisplayName("Multiple entries appended in a single AppendEntries RPC")
        void followerAppendsBatchEntries() {
            raftNode.start();

            LogEntry entry1 = new LogEntry(1, 1, "a".getBytes(StandardCharsets.UTF_8));
            LogEntry entry2 = new LogEntry(2, 1, "b".getBytes(StandardCharsets.UTF_8));
            LogEntry entry3 = new LogEntry(3, 1, "c".getBytes(StandardCharsets.UTF_8));

            CompletableFuture<AppendEntriesReply> future = new CompletableFuture<>();
            AppendEntriesArgs args = new AppendEntriesArgs(1L, peer1, 0L, 0L,
                    List.of(entry1, entry2, entry3), 3L);
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesEvent(args, future));

            AppendEntriesReply reply = future.join();
            assertThat(reply.success()).isTrue();
            assertThat(reply.matchIndex()).isEqualTo(3L);
            assertThat(raftNode.commitIndex()).isEqualTo(3L);
            assertThat(stateMachine.appliedCommands).hasSize(3);
        }
    }

    // ── Client Command Tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Client Commands")
    class ClientCommandTests {

        @Test
        @DisplayName("Non-leader rejects client commands")
        void nonLeaderRejectsCommands() throws InterruptedException {
            raftNode.start();

            CompletableFuture<byte[]> future = new CompletableFuture<>();
            raftNode.handleEvent(new RaftEvent.ClientCommandEvent(
                    "set x 1".getBytes(StandardCharsets.UTF_8), future));
            awaitCondition(future::isCompletedExceptionally);

            assertThat(future.isCompletedExceptionally()).isTrue();
        }

        @Test
        @DisplayName("Leader appends client command to log and replicates")
        void leaderAppendsAndReplicates() throws InterruptedException {
            becomeLeader();

            transport.sentAppendEntries.clear();
            CompletableFuture<byte[]> clientFuture = new CompletableFuture<>();
            raftNode.handleEvent(new RaftEvent.ClientCommandEvent(
                    "set x 42".getBytes(StandardCharsets.UTF_8), clientFuture));
            awaitCondition(() -> logStorage.getLastLogIndex() == 2L);

            assertThat(logStorage.getLastLogIndex()).isEqualTo(2L);
            assertThat(logStorage.getTermAt(2)).isEqualTo(1L);
            assertThat(transport.sentAppendEntries).isNotEmpty();
            assertThat(clientFuture.isDone()).isFalse();
        }

        @Test
        @DisplayName("Leader commits and applies command after majority acknowledgment")
        void leaderCommitsAfterMajority() throws InterruptedException {
            becomeLeader();

            CompletableFuture<byte[]> clientFuture = new CompletableFuture<>();
            raftNode.handleEvent(new RaftEvent.ClientCommandEvent(
                    "set x 42".getBytes(StandardCharsets.UTF_8), clientFuture));
            awaitCondition(() -> logStorage.getLastLogIndex() == 2L);

            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesReplyEvent(
                    peer1, new AppendEntriesReply(1L, true, 2L)));
            awaitCondition(clientFuture::isDone);

            assertThat(clientFuture.isDone()).isTrue();
            assertThat(clientFuture.join()).isNotNull();
            assertThat(raftNode.commitIndex()).isEqualTo(2L);
            assertThat(raftNode.lastApplied()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Leader retries replication on failure response")
        void leaderRetriesOnFailure() throws InterruptedException {
            becomeLeader();

            raftNode.handleEvent(new RaftEvent.ClientCommandEvent(
                    "set x 1".getBytes(StandardCharsets.UTF_8), new CompletableFuture<>()));
            awaitCondition(() -> logStorage.getLastLogIndex() == 2L);

            int countBefore = transport.sentAppendEntries.size();
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesReplyEvent(
                    peer1, new AppendEntriesReply(1L, false, 0L)));
            awaitCondition(() -> transport.sentAppendEntries.size() > countBefore);

            assertThat(transport.sentAppendEntries.size()).isGreaterThan(countBefore);
        }

        @Test
        @DisplayName("Pending commands fail when leader steps down")
        void pendingCommandsFailOnStepDown() throws InterruptedException {
            becomeLeader();
 
            CompletableFuture<byte[]> clientFuture = new CompletableFuture<>();
            raftNode.handleEvent(new RaftEvent.ClientCommandEvent(
                    "set x 1".getBytes(StandardCharsets.UTF_8), clientFuture));
            awaitCondition(() -> logStorage.getLastLogIndex() == 2L);
 
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesReplyEvent(
                    peer1, new AppendEntriesReply(5L, false, 0L)));
            awaitCondition(clientFuture::isCompletedExceptionally);
 
            assertThat(raftNode.role()).isEqualTo(RaftRole.FOLLOWER);
            assertThat(clientFuture.isCompletedExceptionally()).isTrue();
        }
    }

    // ── ReadIndex Tests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("ReadIndex Linearizable Reads")
    class ReadIndexTests {

        @Test
        @DisplayName("Non-leader rejects ReadIndex request")
        void nonLeaderRejectsReadIndex() throws InterruptedException {
            raftNode.start();

            CompletableFuture<Long> future = new CompletableFuture<>();
            raftNode.handleEvent(new RaftEvent.ClientReadIndexEvent(future));
            awaitCondition(future::isCompletedExceptionally);

            assertThat(future.isCompletedExceptionally()).isTrue();
        }

        @Test
        @DisplayName("Leader completes ReadIndex after majority heartbeat confirmation")
        void leaderCompletesReadIndexAfterQuorum() throws InterruptedException {
            becomeLeader();

            CompletableFuture<Long> readFuture = new CompletableFuture<>();
            raftNode.handleEvent(new RaftEvent.ClientReadIndexEvent(readFuture));
            awaitCondition(() -> !transport.sentAppendEntries.isEmpty());

            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesReplyEvent(
                    peer1, new AppendEntriesReply(1L, true, 1L)));
            awaitCondition(readFuture::isDone);

            assertThat(readFuture.isDone()).isTrue();
            assertThat(readFuture.join()).isNotNull();
        }
    }

    // ── Commit Advancement Tests ─────────────────────────────────────────────

    @Nested
    @DisplayName("Commit Advancement")
    class CommitAdvancementTests {

        @Test
        @DisplayName("Leader only commits entries from current term (§5.4.2)")
        void leaderOnlyCommitsCurrentTermEntries() throws InterruptedException {
            // Pre-populate log with an entry from a previous term (term 1, not current)
            logStorage.append(new LogEntry(1, 1, "old".getBytes(StandardCharsets.UTF_8)));

            // becomeLeader() brings node to term 1; entry at index 1 is from term 1.
            // After becoming leader, the node is at term 1 — so this entry IS current term.
            // To test §5.4.2 properly, we need the entry to be from a PRIOR term.
            // Use a pre-existing entry from term 1, then win election at term 2.
            becomeLeaderAtTerm(2);

            // peer1 says it has replicated index 1
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesReplyEvent(
                    peer1, new AppendEntriesReply(2L, true, 1L)));
            Thread.sleep(50);

            // commitIndex should NOT advance because entry 1 is from term 1, not current term 2
            assertThat(raftNode.commitIndex()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Leader commits when majority replicates current-term entry")
        void leaderCommitsWithMajority() throws InterruptedException {
            becomeLeader();

            raftNode.handleEvent(new RaftEvent.ClientCommandEvent(
                    "set k v".getBytes(StandardCharsets.UTF_8), new CompletableFuture<>()));
            awaitCondition(() -> logStorage.getLastLogIndex() == 2L);

            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesReplyEvent(
                    peer1, new AppendEntriesReply(1L, true, 2L)));
            awaitCondition(() -> raftNode.commitIndex() == 2L);

            assertThat(raftNode.commitIndex()).isEqualTo(2L);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void awaitCondition(java.util.function.Supplier<Boolean> condition) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!condition.get()) {
            if (System.currentTimeMillis() - start > 2000) {
                throw new AssertionError("Condition not met within timeout");
            }
            Thread.sleep(5);
        }
    }

    private void becomeLeader() throws InterruptedException {
        raftNode.start();
        clock.advanceTime(Duration.ofMillis(350));
        awaitCondition(() -> transport.pendingVotes.containsKey(peer1));

        transport.replyRequestVote(peer1, new RequestVoteReply(1L, true));
        awaitCondition(() -> raftNode.role() == RaftRole.LEADER);

        assertThat(raftNode.role()).isEqualTo(RaftRole.LEADER);
    }

    private void becomeLeaderAtTerm(long term) throws InterruptedException {
        raftNode.start();

        // First step up to the desired term via AppendEntries from a peer
        if (term > 1) {
            CompletableFuture<AppendEntriesReply> stepUpFuture = new CompletableFuture<>();
            raftNode.handleEvent(new RaftEvent.InboundAppendEntriesEvent(
                    new AppendEntriesArgs(term - 1, peer1, 0L, 0L, List.of(), 0L), stepUpFuture));
            stepUpFuture.join();
        }

        // Trigger election at the target term
        clock.advanceTime(Duration.ofMillis(350));
        awaitCondition(() -> transport.pendingVotes.containsKey(peer1));

        assertThat(raftNode.currentTerm()).isEqualTo(term);
        transport.replyRequestVote(peer1, new RequestVoteReply(term, true));
        awaitCondition(() -> raftNode.role() == RaftRole.LEADER);

        assertThat(raftNode.role()).isEqualTo(RaftRole.LEADER);
        assertThat(raftNode.currentTerm()).isEqualTo(term);
    }

    // ── Test Doubles ─────────────────────────────────────────────────────────

    private static final class TestTransport implements PeerTransport {

        final List<RequestVoteArgs> sentRequestVotes = new CopyOnWriteArrayList<>();
        final List<AppendEntriesArgs> sentAppendEntries = new CopyOnWriteArrayList<>();
        final Map<NodeId, CompletableFuture<RequestVoteReply>> pendingVotes = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<RequestVoteReply> sendRequestVote(NodeId target, RequestVoteArgs args) {
            sentRequestVotes.add(args);
            CompletableFuture<RequestVoteReply> future = new CompletableFuture<>();
            pendingVotes.put(target, future);
            return future;
        }

        @Override
        public CompletableFuture<AppendEntriesReply> sendAppendEntries(NodeId target, AppendEntriesArgs args) {
            sentAppendEntries.add(args);
            return new CompletableFuture<>();
        }

        @Override
        public CompletableFuture<com.atlaskv.core.rpc.InstallSnapshotReply> sendInstallSnapshot(NodeId target, com.atlaskv.core.rpc.InstallSnapshotArgs args) {
            return new CompletableFuture<>();
        }

        void replyRequestVote(NodeId peer, RequestVoteReply reply) {
            CompletableFuture<RequestVoteReply> future = pendingVotes.remove(peer);
            if (future != null) {
                future.complete(reply);
            }
        }

        @Override
        public void close() {
            // No resources to release
        }
    }

    private static final class TestStateMachine implements StateMachine {
        final List<byte[]> appliedCommands = new CopyOnWriteArrayList<>();

        @Override
        public byte[] apply(byte[] command) {
            appliedCommands.add(command);
            return command;
        }

        @Override
        public byte[] takeSnapshot() { return new byte[0]; }

        @Override
        public void restoreSnapshot(byte[] snapshot) { }
    }

    private static final class TestClock implements Clock {

        private long currentTime;
        private final List<TestTask> tasks = new CopyOnWriteArrayList<>();

        @Override
        public synchronized long currentTimeMillis() {
            return currentTime;
        }

        @Override
        public synchronized Cancellable scheduleOnce(Duration delay, Runnable task) {
            long trigger = currentTime + delay.toMillis();
            TestTask t = new TestTask(trigger, task);
            tasks.add(t);
            return () -> {
                t.cancelled = true;
                return true;
            };
        }

        synchronized void advanceTime(Duration duration) {
            currentTime += duration.toMillis();
            for (TestTask t : tasks) {
                if (!t.cancelled && !t.executed && currentTime >= t.triggerTime) {
                    t.executed = true;
                    t.task.run();
                }
            }
        }

        private static final class TestTask {
            final long triggerTime;
            final Runnable task;
            boolean cancelled;
            boolean executed;

            TestTask(long triggerTime, Runnable task) {
                this.triggerTime = triggerTime;
                this.task = task;
            }
        }
    }
}
