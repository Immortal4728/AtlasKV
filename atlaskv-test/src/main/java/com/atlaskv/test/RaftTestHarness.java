package com.atlaskv.test;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.RaftNode;
import com.atlaskv.core.RaftRole;
import com.atlaskv.core.config.RaftConfig;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.core.storage.InMemoryLogStorage;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Deterministic test harness for driving a single RaftNode through its event handler
 * without background threads. All events are dispatched synchronously via
 * {@link RaftNode#handleEvent(RaftEvent)}.
 *
 * <p>The harness does NOT start the EventLoop. Tests drive the node by calling
 * {@link #handleEvent(RaftEvent)} directly, which gives full control over ordering.
 */
public final class RaftTestHarness implements AutoCloseable {

    private final RaftNode node;
    private final InMemoryLogStorage logStorage;
    private final com.atlaskv.core.storage.SnapshotStorage snapshotStorage;
    private final StubTransport transport;
    private final StubStateMachine stateMachine;
    private final RaftConfig config;

    private RaftTestHarness(Builder builder) {
        this.config = builder.config;
        this.logStorage = builder.logStorage;
        this.snapshotStorage = builder.snapshotStorage;
        this.transport = builder.transport;
        this.stateMachine = builder.stateMachine;
        this.node = new RaftNode(
                config,
                builder.clock,
                logStorage,
                new com.atlaskv.core.storage.InMemoryPersistentStateStore(),
                snapshotStorage,
                transport,
                stateMachine
        );
    }

    public RaftNode node() {
        return node;
    }

    public InMemoryLogStorage logStorage() {
        return logStorage;
    }

    public StubTransport transport() {
        return transport;
    }

    public StubStateMachine stateMachine() {
        return stateMachine;
    }

    public RaftConfig config() {
        return config;
    }

    public void handleEvent(RaftEvent event) {
        node.handleEvent(event);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Transitions the node to LEADER state by simulating an election timeout
     * and granting a majority vote. Dispatches events synchronously.
     */
    public void electLeader() {
        handleEvent(new RaftEvent.ElectionTimeoutEvent());
        if (node.role() != RaftRole.LEADER) {
            NodeId firstPeer = config.peers().iterator().next();
            transport.replyRequestVote(firstPeer, new RequestVoteReply(node.currentTerm(), true));
            handleEvent(new RaftEvent.InboundRequestVoteReplyEvent(
                    firstPeer, new RequestVoteReply(node.currentTerm(), true)));
        }
    }

    /**
     * Transitions the node to LEADER state at the specified term.
     *
     * @param targetTerm the term at which to become leader
     */
    public void electLeaderAtTerm(long targetTerm) {
        long currentTerm = node.currentTerm();
        if (targetTerm > currentTerm + 1) {
            CompletableFuture<AppendEntriesReply> stepUpFuture = new CompletableFuture<>();
            NodeId peer = config.peers().iterator().next();
            handleEvent(new RaftEvent.InboundAppendEntriesEvent(
                    new AppendEntriesArgs(targetTerm - 1, peer, 0L, 0L, List.of(), 0L),
                    stepUpFuture));
        }
        handleEvent(new RaftEvent.ElectionTimeoutEvent());
        if (node.role() != RaftRole.LEADER) {
            NodeId firstPeer = config.peers().iterator().next();
            handleEvent(new RaftEvent.InboundRequestVoteReplyEvent(
                    firstPeer, new RequestVoteReply(node.currentTerm(), true)));
        }
    }

    /**
     * Submits a client command and returns the future that will be completed
     * when the entry is committed and applied.
     *
     * @param command command string
     * @return future that completes with the state machine result
     */
    public CompletableFuture<byte[]> submitCommand(String command) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        handleEvent(new RaftEvent.ClientCommandEvent(
                command.getBytes(StandardCharsets.UTF_8), future));
        return future;
    }

    /**
     * Simulates a follower receiving AppendEntries from a leader.
     *
     * @param args the AppendEntries arguments
     * @return the reply
     */
    /**
     * Simulates a follower receiving AppendEntries from a leader.
     *
     * @param args the AppendEntries arguments
     * @return the reply
     */
    public AppendEntriesReply receiveAppendEntries(AppendEntriesArgs args) {
        CompletableFuture<AppendEntriesReply> future = new CompletableFuture<>();
        handleEvent(new RaftEvent.InboundAppendEntriesEvent(args, future));
        return future.join();
    }

    /**
     * Simulates a follower receiving InstallSnapshot from a leader.
     *
     * @param args the InstallSnapshot arguments
     * @return the reply
     */
    public com.atlaskv.core.rpc.InstallSnapshotReply receiveInstallSnapshot(com.atlaskv.core.rpc.InstallSnapshotArgs args) {
        CompletableFuture<com.atlaskv.core.rpc.InstallSnapshotReply> future = new CompletableFuture<>();
        handleEvent(new RaftEvent.InboundInstallSnapshotEvent(args, future));
        return future.join();
    }

    /**
     * Acknowledges replication from a peer at the given match index.
     *
     * @param peer the peer that acknowledged
     * @param matchIndex the highest replicated index
     */
    public void ackReplication(NodeId peer, long matchIndex) {
        handleEvent(new RaftEvent.InboundAppendEntriesReplyEvent(
                peer, new AppendEntriesReply(node.currentTerm(), true, matchIndex)));
    }

    /**
     * Reports a replication failure from a peer.
     *
     * @param peer the peer that failed
     */
    public void nackReplication(NodeId peer) {
        handleEvent(new RaftEvent.InboundAppendEntriesReplyEvent(
                peer, new AppendEntriesReply(node.currentTerm(), false, 0L)));
    }

    /**
     * Acknowledges an InstallSnapshot RPC from a peer.
     *
     * @param peer the peer that acknowledged
     */
    public void ackInstallSnapshot(NodeId peer) {
        handleEvent(new RaftEvent.InboundInstallSnapshotReplyEvent(
                peer, new com.atlaskv.core.rpc.InstallSnapshotReply(node.currentTerm(), true)));
    }

    /**
     * Reports an InstallSnapshot failure from a peer.
     *
     * @param peer the peer that failed
     */
    public void nackInstallSnapshot(NodeId peer) {
        handleEvent(new RaftEvent.InboundInstallSnapshotReplyEvent(
                peer, new com.atlaskv.core.rpc.InstallSnapshotReply(node.currentTerm(), false)));
    }

    /**
     * Triggers a snapshot manually on the node.
     *
     * @return metadata of the created snapshot
     */
    public com.atlaskv.core.storage.SnapshotMetadata takeSnapshot() {
        return node.takeSnapshot();
    }

    @Override
    public void close() {
        node.close();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a harness with a standard 3-node cluster configuration.
     *
     * @return configured harness
     */
    public static RaftTestHarness threeNodeCluster() {
        return builder()
                .selfId("node-1")
                .peers("node-2", "node-3")
                .build();
    }

    public static final class Builder {
        private NodeId selfId = NodeId.of("node-1");
        private Set<NodeId> peers = Set.of(NodeId.of("node-2"), NodeId.of("node-3"));
        private Duration minElectionTimeout = Duration.ofMillis(150);
        private Duration maxElectionTimeout = Duration.ofMillis(300);
        private Duration heartbeatInterval = Duration.ofMillis(50);
        private long snapshotThresholdEntries = RaftConfig.DEFAULT_SNAPSHOT_THRESHOLD;
        private SimulatedClock clock = new SimulatedClock();
        private InMemoryLogStorage logStorage = new InMemoryLogStorage();
        private com.atlaskv.core.storage.SnapshotStorage snapshotStorage = new com.atlaskv.core.storage.InMemorySnapshotStorage();
        private StubTransport transport = new StubTransport();
        private StubStateMachine stateMachine = new StubStateMachine();
        private RaftConfig config;

        public Builder selfId(String id) {
            this.selfId = NodeId.of(id);
            return this;
        }

        public Builder peers(String... peerIds) {
            Set<NodeId> peerSet = new java.util.HashSet<>();
            for (String id : peerIds) {
                peerSet.add(NodeId.of(id));
            }
            this.peers = Set.copyOf(peerSet);
            return this;
        }

        public Builder snapshotThresholdEntries(long threshold) {
            this.snapshotThresholdEntries = threshold;
            return this;
        }

        public Builder clock(SimulatedClock clock) {
            this.clock = clock;
            return this;
        }

        public Builder logStorage(InMemoryLogStorage logStorage) {
            this.logStorage = logStorage;
            return this;
        }

        public Builder snapshotStorage(com.atlaskv.core.storage.SnapshotStorage snapshotStorage) {
            this.snapshotStorage = snapshotStorage;
            return this;
        }

        public Builder transport(StubTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder stateMachine(StubStateMachine stateMachine) {
            this.stateMachine = stateMachine;
            return this;
        }

        public RaftTestHarness build() {
            this.config = new RaftConfig(selfId, peers, minElectionTimeout, maxElectionTimeout, heartbeatInterval, snapshotThresholdEntries);
            return new RaftTestHarness(this);
        }
    }
}
