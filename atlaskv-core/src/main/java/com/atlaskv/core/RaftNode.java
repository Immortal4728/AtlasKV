package com.atlaskv.core;

import com.atlaskv.core.clock.Clock;
import com.atlaskv.core.config.ClusterMembership;
import com.atlaskv.core.config.RaftConfig;
import com.atlaskv.core.event.EventLoop;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.core.event.RaftEventHandler;
import com.atlaskv.core.statemachine.StateMachine;
import com.atlaskv.core.storage.InMemoryPersistentStateStore;
import com.atlaskv.core.storage.InMemorySnapshotStorage;
import com.atlaskv.core.storage.LogStorage;
import com.atlaskv.core.storage.PersistentStateStore;
import com.atlaskv.core.storage.Snapshot;
import com.atlaskv.core.storage.SnapshotMetadata;
import com.atlaskv.core.storage.SnapshotStorage;
import com.atlaskv.core.storage.StorageException;
import com.atlaskv.core.transport.PeerTransport;

import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Pure Java Raft consensus node supporting Joint Consensus cluster membership changes.
 */
public final class RaftNode implements RaftEventHandler, AutoCloseable {

    private final RaftConfig config;
    private final LogStorage logStorage;
    private final PersistentStateStore persistentStateStore;
    private final SnapshotStorage snapshotStorage;
    private final PeerTransport transport;
    private final StateMachine stateMachine;
    private final EventLoop eventLoop;
    private final RaftTimerManager timerManager;
    private final PendingCommands pendingCommands = new PendingCommands();

    private RaftRole role = RaftRole.FOLLOWER;
    private PersistentState persistentState;
    private long commitIndex;
    private long lastApplied;
    private NodeId currentLeader;

    private ClusterMembership currentMembership;
    private CompletableFuture<Void> pendingMembershipFuture;
    private long pendingMembershipTargetIndex = -1;

    private final Set<NodeId> votesReceived = new HashSet<>();
    private final PendingReadIndexManager pendingReadIndexManager = new PendingReadIndexManager();
    private LeaderState leaderState;

    public RaftNode(RaftConfig config, Clock clock, LogStorage logStorage,
                    PeerTransport transport, StateMachine stateMachine) {
        this(config, clock, logStorage, new InMemoryPersistentStateStore(),
                new InMemorySnapshotStorage(), transport, stateMachine);
    }

    public RaftNode(RaftConfig config, Clock clock, LogStorage logStorage,
                    PersistentStateStore persistentStateStore, PeerTransport transport,
                    StateMachine stateMachine) {
        this(config, clock, logStorage, persistentStateStore, new InMemorySnapshotStorage(), transport, stateMachine);
    }

    public RaftNode(RaftConfig config, Clock clock, LogStorage logStorage,
                    PersistentStateStore persistentStateStore, SnapshotStorage snapshotStorage,
                    PeerTransport transport, StateMachine stateMachine) {
        this.config = Objects.requireNonNull(config, "Config must not be null");
        this.logStorage = Objects.requireNonNull(logStorage, "LogStorage must not be null");
        this.persistentStateStore = Objects.requireNonNull(persistentStateStore, "PersistentStateStore must not be null");
        this.snapshotStorage = Objects.requireNonNull(snapshotStorage, "SnapshotStorage must not be null");
        this.transport = Objects.requireNonNull(transport, "Transport must not be null");
        this.stateMachine = Objects.requireNonNull(stateMachine, "StateMachine must not be null");
        this.eventLoop = new EventLoop(this);
        this.timerManager = new RaftTimerManager(clock, config, new Random(), eventLoop::submit);
        this.persistentState = persistentStateStore.loadState();

        Set<NodeId> initialMembers = new HashSet<>(config.peers());
        initialMembers.add(config.selfId());
        this.currentMembership = ClusterMembership.ofSingle(initialMembers);

        this.snapshotStorage.loadLatestSnapshot().ifPresent(s -> {
            if (s.metadata().membership() != null) {
                this.currentMembership = s.metadata().membership();
            }
            this.stateMachine.restoreSnapshot(s.data());
            this.commitIndex = s.metadata().lastIncludedIndex();
            this.lastApplied = s.metadata().lastIncludedIndex();
            this.logStorage.compactUpTo(s.metadata().lastIncludedIndex(), s.metadata().lastIncludedTerm());
        });
        scanLogForMembership();
    }

    void scanLogForMembership() {
        ClusterMembership scanned = RaftRpcHelper.rescanLogMembership(logStorage);
        if (scanned != null) {
            this.currentMembership = scanned;
        }
    }

    public synchronized void start() {
        eventLoop.start();
        timerManager.resetElectionTimer();
    }

    @Override
    public void handleEvent(RaftEvent event) {
        Objects.requireNonNull(event, "Event must not be null");
        switch (event) {
            case RaftEvent.ElectionTimeoutEvent ignored -> RaftNodeEngine.onElectionTimeout(this);
            case RaftEvent.HeartbeatTimeoutEvent ignored -> RaftNodeEngine.onHeartbeatTimeout(this);
            case RaftEvent.InboundRequestVoteEvent e -> RaftNodeEngine.onRequestVote(this, e.args(), e.responseFuture());
            case RaftEvent.InboundRequestVoteReplyEvent e -> RaftNodeEngine.onRequestVoteReply(this, e.fromNode(), e.reply());
            case RaftEvent.InboundAppendEntriesEvent e -> RaftNodeEngine.onAppendEntries(this, e.args(), e.responseFuture());
            case RaftEvent.InboundAppendEntriesReplyEvent e -> RaftNodeEngine.onAppendEntriesReply(this, e.fromNode(), e.reply());
            case RaftEvent.InboundInstallSnapshotEvent e -> RaftNodeEngine.onInstallSnapshot(this, e.args(), e.responseFuture());
            case RaftEvent.InboundInstallSnapshotReplyEvent e -> RaftNodeEngine.onInstallSnapshotReply(this, e.fromNode(), e.reply());
            case RaftEvent.ClientCommandEvent e -> RaftNodeEngine.onClientCommand(this, e.command(), e.responseFuture());
            case RaftEvent.ClientReadIndexEvent e -> RaftNodeEngine.onClientReadIndex(this, e.responseFuture());
            case RaftEvent.ClientMembershipChangeEvent e -> RaftNodeEngine.onClientMembershipChange(this, e.type(), e.targetNode(), e.responseFuture());
        }
    }

    public synchronized RaftRole role() {
        return role;
    }

    public synchronized long currentTerm() {
        return persistentState.currentTerm();
    }

    public synchronized NodeId votedFor() {
        return persistentState.votedFor();
    }

    public synchronized NodeId currentLeader() {
        return currentLeader;
    }

    public synchronized PersistentState persistentState() {
        return persistentState;
    }

    public synchronized long commitIndex() {
        return commitIndex;
    }

    public synchronized long lastApplied() {
        return lastApplied;
    }

    public synchronized ClusterMembership currentMembership() {
        return currentMembership;
    }

    public StateMachine stateMachine() {
        return stateMachine;
    }

    public SnapshotStorage snapshotStorage() {
        return snapshotStorage;
    }

    public LogStorage logStorage() {
        return logStorage;
    }

    public RaftConfig config() {
        return config;
    }

    public PeerTransport transport() {
        return transport;
    }

    public EventLoop eventLoop() {
        return eventLoop;
    }

    public RaftTimerManager timerManager() {
        return timerManager;
    }

    public PendingCommands pendingCommands() {
        return pendingCommands;
    }

    public PendingReadIndexManager pendingReadIndexManager() {
        return pendingReadIndexManager;
    }

    public LeaderState leaderState() {
        return leaderState;
    }

    public Set<NodeId> votesReceived() {
        return votesReceived;
    }

    CompletableFuture<Void> pendingMembershipFuture() {
        return pendingMembershipFuture;
    }

    long pendingMembershipTargetIndex() {
        return pendingMembershipTargetIndex;
    }

    synchronized void setRole(RaftRole role) {
        this.role = role;
    }

    synchronized void setCurrentLeader(NodeId leader) {
        this.currentLeader = leader;
    }

    synchronized void setLeaderState(LeaderState leaderState) {
        this.leaderState = leaderState;
    }

    synchronized void setCurrentMembership(ClusterMembership mem) {
        this.currentMembership = mem;
    }

    synchronized void setCommitIndex(long index) {
        this.commitIndex = index;
    }

    synchronized void setLastApplied(long index) {
        this.lastApplied = index;
    }

    synchronized void setPendingMembershipFuture(CompletableFuture<Void> future) {
        this.pendingMembershipFuture = future;
    }

    synchronized void setPendingMembershipTargetIndex(long index) {
        this.pendingMembershipTargetIndex = index;
    }

    synchronized void clearPendingMembershipFuture() {
        this.pendingMembershipFuture = null;
        this.pendingMembershipTargetIndex = -1;
    }

    synchronized void updatePersistentState(PersistentState newState) {
        this.persistentState = newState;
        this.persistentStateStore.saveState(newState);
    }

    synchronized void failPendingMembershipFuture(Throwable cause) {
        if (pendingMembershipFuture != null) {
            pendingMembershipFuture.completeExceptionally(cause);
            clearPendingMembershipFuture();
        }
    }

    public synchronized SnapshotMetadata takeSnapshot() {
        SnapshotMetadata meta = RaftSnapshotManager.takeSnapshot(lastApplied, logStorage, stateMachine, snapshotStorage);
        SnapshotMetadata updatedMeta = new SnapshotMetadata(meta.lastIncludedIndex(), meta.lastIncludedTerm(), currentMembership);
        snapshotStorage.loadLatestSnapshot().ifPresent(s ->
                snapshotStorage.saveSnapshot(new Snapshot(updatedMeta, s.data())));
        return updatedMeta;
    }

    @Override
    public synchronized void close() {
        timerManager.cancelElectionTimer();
        timerManager.cancelHeartbeatTimer();
        eventLoop.close();
        pendingReadIndexManager.failAll(new IllegalStateException("RaftNode closed"));
        failPendingMembershipFuture(new IllegalStateException("RaftNode closed"));
        try {
            persistentStateStore.close();
            snapshotStorage.close();
        } catch (Exception e) {
            throw new StorageException("Failed to close storage resources", e);
        }
    }
}
