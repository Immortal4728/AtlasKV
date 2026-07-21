package com.atlaskv.server.lifecycle;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.clock.SystemClock;
import com.atlaskv.core.config.RaftConfig;
import com.atlaskv.core.statemachine.StateMachine;
import com.atlaskv.server.config.ClusterConfig;
import com.atlaskv.server.health.NodeHealthStatus;
import com.atlaskv.storage.metadata.FilePersistentStateStore;
import com.atlaskv.storage.snapshot.FileSnapshotStorage;
import com.atlaskv.storage.wal.WalLogStorage;
import com.atlaskv.transport.grpc.GrpcPeerTransport;
import com.atlaskv.transport.grpc.RaftGrpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the full lifecycle of a Raft node: startup, operation, shutdown, and restart.
 *
 * <p>Startup sequence:
 * <ol>
 *   <li>Persist / verify node identity</li>
 *   <li>Open WAL log storage (replays entries)</li>
 *   <li>Open persistent state store (loads term/vote)</li>
 *   <li>Open snapshot storage (loads latest snapshot)</li>
 *   <li>Create RaftNode (restores state machine from snapshot)</li>
 *   <li>Initialize gRPC transport (server + client channels)</li>
 *   <li>Start RaftNode event loop and timers</li>
 * </ol>
 *
 * <p>Shutdown sequence:
 * <ol>
 *   <li>Stop RaftNode (cancels timers, closes event loop)</li>
 *   <li>Stop gRPC server</li>
 *   <li>Close gRPC transport (client channels)</li>
 *   <li>Close log storage (flushes WAL)</li>
 *   <li>Close persistent state store</li>
 *   <li>Close snapshot storage</li>
 *   <li>Close clock scheduler</li>
 * </ol>
 *
 * <p>Thread-safe via atomic state transitions.
 */
public final class NodeLifecycleManager implements org.springframework.context.SmartLifecycle, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NodeLifecycleManager.class);
    private static final String NODE_IDENTITY_FILE = "node-id";
    private static final String WAL_FILE = "raft.wal";
    private static final String METADATA_FILE = "raft.meta";

    private final ClusterConfig config;
    private final StateMachine stateMachine;
    private final AtomicReference<NodeState> state = new AtomicReference<>(NodeState.CREATED);

    private volatile RaftNode raftNode;
    private volatile RaftGrpcServer grpcServer;
    private volatile GrpcPeerTransport grpcTransport;
    private volatile WalLogStorage walLogStorage;
    private volatile FilePersistentStateStore persistentStateStore;
    private volatile FileSnapshotStorage snapshotStorage;
    private volatile SystemClock clock;
    private volatile long startedAtMillis;

    /**
     * Constructs a NodeLifecycleManager for the given config and state machine.
     *
     * @param config cluster configuration
     * @param stateMachine application state machine
     */
    public NodeLifecycleManager(ClusterConfig config, StateMachine stateMachine) {
        this.config = Objects.requireNonNull(config, "Config must not be null");
        this.stateMachine = Objects.requireNonNull(stateMachine, "StateMachine must not be null");
    }

    /**
     * Starts the Raft node through the full bootstrap sequence.
     *
     * @throws NodeLifecycleException if startup fails
     */
    public void start() {
        if (!state.compareAndSet(NodeState.CREATED, NodeState.STARTING)
                && !state.compareAndSet(NodeState.STOPPED, NodeState.STARTING)) {
            throw new NodeLifecycleException(
                    "Cannot start node in state: " + state.get());
        }

        LOG.info("Starting AtlasKV node [{}]...", config.nodeId());

        try {
            ensureDirectories();
            persistNodeIdentity();
            initStorage();
            initTransport();
            initRaftNode();
            startTransport();
            startRaftNode();

            startedAtMillis = System.currentTimeMillis();
            state.set(NodeState.RUNNING);
            LOG.info("AtlasKV node [{}] is now RUNNING on port {}",
                    config.nodeId(), grpcServer.port());

        } catch (Exception e) {
            state.set(NodeState.FAILED);
            LOG.error("Failed to start AtlasKV node [{}]", config.nodeId(), e);
            cleanupOnFailure();
            throw new NodeLifecycleException(
                    "Failed to start node " + config.nodeId(), e);
        }
    }

    /**
     * Stops the Raft node gracefully, releasing all resources.
     */
    public void stop() {
        if (!state.compareAndSet(NodeState.RUNNING, NodeState.STOPPING)) {
            LOG.warn("Cannot stop node in state: {}", state.get());
            return;
        }

        LOG.info("Stopping AtlasKV node [{}]", config.nodeId());

        try {
            stopRaftNode();
            stopTransport();
            closeStorage();
            closeClock();
        } catch (Exception e) {
            LOG.error("Error during shutdown of node [{}]", config.nodeId(), e);
        } finally {
            state.set(NodeState.STOPPED);
            LOG.info("AtlasKV node [{}] is now STOPPED", config.nodeId());
        }
    }

    /**
     * Returns the current lifecycle state of the node.
     *
     * @return current node state
     */
    public NodeState state() {
        return state.get();
    }

    /**
     * Returns the cluster configuration.
     *
     * @return cluster configuration
     */
    public ClusterConfig config() {
        return config;
    }

    /**
     * Returns the managed RaftNode, or null if not started.
     *
     * @return the RaftNode instance
     */
    public RaftNode raftNode() {
        return raftNode;
    }

    /**
     * Returns the gRPC server port, or -1 if not started.
     *
     * @return listening port
     */
    public int port() {
        RaftGrpcServer srv = grpcServer;
        return srv != null ? srv.port() : -1;
    }

    /**
     * Returns a snapshot of the node's current health status.
     *
     * @return health status
     */
    public NodeHealthStatus healthStatus() {
        RaftNode node = this.raftNode;
        if (node == null || state.get() != NodeState.RUNNING) {
            return new NodeHealthStatus(
                    config.nodeId(),
                    com.atlaskv.core.RaftRole.FOLLOWER,
                    0L, 0L, 0L, null, false, 0L);
        }

        return new NodeHealthStatus(
                config.nodeId(),
                node.role(),
                node.currentTerm(),
                node.commitIndex(),
                node.lastApplied(),
                node.currentLeader(),
                true,
                startedAtMillis);
    }

    /**
     * Registers a peer socket address dynamically with the transport.
     *
     * @param nodeId node ID
     * @param address host:port string
     */
    public void registerPeer(com.atlaskv.core.NodeId nodeId, String address) {
        GrpcPeerTransport transport = this.grpcTransport;
        if (transport != null && address != null && !address.isBlank()) {
            transport.registerPeer(nodeId, address);
        }
    }

    /**
     * Unregisters a peer dynamically from the transport.
     *
     * @param nodeId node ID
     */
    public void unregisterPeer(com.atlaskv.core.NodeId nodeId) {
        GrpcPeerTransport transport = this.grpcTransport;
        if (transport != null) {
            transport.unregisterPeer(nodeId);
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return state.get() == NodeState.RUNNING;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1000;
    }

    @Override
    public void close() {
        if (state.get() == NodeState.RUNNING) {
            stop();
        } else if (state.get() == NodeState.FAILED) {
            cleanupOnFailure();
            state.set(NodeState.STOPPED);
        }
    }

    // ── Startup Steps ──────────────────────────────────────────

    private void ensureDirectories() throws IOException {
        Files.createDirectories(Path.of(config.storageDirectory()));
        Files.createDirectories(Path.of(config.snapshotDirectory()));
    }

    private void persistNodeIdentity() throws IOException {
        Path identityFile = Path.of(config.storageDirectory(), NODE_IDENTITY_FILE);
        if (Files.exists(identityFile)) {
            String storedId = Files.readString(identityFile, StandardCharsets.UTF_8).trim();
            if (!storedId.equals(config.nodeId().value())) {
                throw new NodeLifecycleException(
                        "Node identity mismatch: config has '" + config.nodeId().value()
                                + "' but storage has '" + storedId + "'. "
                                + "Storage directory may belong to a different node.");
            }
            LOG.info("Verified persistent node identity: {}", storedId);
        } else {
            Files.writeString(identityFile, config.nodeId().value(), StandardCharsets.UTF_8);
            LOG.info("Persisted node identity: {}", config.nodeId());
        }
    }

    private void initStorage() {
        Path storagePath = Path.of(config.storageDirectory());

        this.walLogStorage = new WalLogStorage(storagePath.resolve(WAL_FILE));
        LOG.info("WAL log storage initialized: lastIndex={}", walLogStorage.getLastLogIndex());

        this.persistentStateStore = new FilePersistentStateStore(storagePath.resolve(METADATA_FILE));
        LOG.info("Persistent state store initialized: term={}, votedFor={}",
                persistentStateStore.loadState().currentTerm(),
                persistentStateStore.loadState().votedFor());

        this.snapshotStorage = new FileSnapshotStorage(Path.of(config.snapshotDirectory()));
        LOG.info("Snapshot storage initialized: hasSnapshot={}",
                snapshotStorage.getLatestSnapshotMetadata().isPresent());
    }

    private void initTransport() {
        this.grpcTransport = new GrpcPeerTransport(
                config.peerAddressStrings(),
                config.rpcTimeout());
        LOG.info("gRPC transport initialized with {} peer(s)", config.peerAddresses().size());
    }

    private void initRaftNode() {
        this.stateMachine.restoreSnapshot(null);
        this.clock = new SystemClock();

        RaftConfig raftConfig = new RaftConfig(
                config.nodeId(),
                config.peerIds(),
                config.minElectionTimeout(),
                config.maxElectionTimeout(),
                config.heartbeatInterval(),
                config.snapshotThresholdEntries());

        this.raftNode = new RaftNode(
                raftConfig,
                clock,
                walLogStorage,
                persistentStateStore,
                snapshotStorage,
                grpcTransport,
                stateMachine);
        LOG.info("RaftNode initialized");
    }

    private void startTransport() throws IOException {
        this.grpcServer = new RaftGrpcServer(
                config.listenAddress().getPort(),
                event -> raftNode.handleEvent(event));
        grpcServer.start();
        LOG.info("gRPC server started on port {}", grpcServer.port());
    }

    private void startRaftNode() {
        raftNode.start();
        LOG.info("RaftNode event loop and timers started");
    }

    // ── Shutdown Steps ─────────────────────────────────────────

    private void stopRaftNode() {
        if (raftNode != null) {
            try {
                raftNode.close();
                LOG.info("RaftNode closed");
            } catch (Exception e) {
                LOG.warn("Error closing RaftNode", e);
            }
            raftNode = null;
        }
    }

    private void stopTransport() {
        if (grpcServer != null) {
            try {
                grpcServer.close();
                LOG.info("gRPC server stopped");
            } catch (Exception e) {
                LOG.warn("Error closing gRPC server", e);
            }
            grpcServer = null;
        }
        if (grpcTransport != null) {
            try {
                grpcTransport.close();
                LOG.info("gRPC transport closed");
            } catch (Exception e) {
                LOG.warn("Error closing gRPC transport", e);
            }
            grpcTransport = null;
        }
    }

    private void closeStorage() {
        if (walLogStorage != null) {
            try {
                walLogStorage.close();
                LOG.info("WAL log storage closed");
            } catch (Exception e) {
                LOG.warn("Error closing WAL log storage", e);
            }
            walLogStorage = null;
        }
        if (persistentStateStore != null) {
            try {
                persistentStateStore.close();
                LOG.info("Persistent state store closed");
            } catch (Exception e) {
                LOG.warn("Error closing persistent state store", e);
            }
            persistentStateStore = null;
        }
        if (snapshotStorage != null) {
            try {
                snapshotStorage.close();
                LOG.info("Snapshot storage closed");
            } catch (Exception e) {
                LOG.warn("Error closing snapshot storage", e);
            }
            snapshotStorage = null;
        }
    }

    private void closeClock() {
        if (clock != null) {
            try {
                clock.close();
                LOG.info("System clock closed");
            } catch (Exception e) {
                LOG.warn("Error closing system clock", e);
            }
            clock = null;
        }
    }

    private void cleanupOnFailure() {
        try {
            stopRaftNode();
        } catch (Exception ignored) {
            // Best effort cleanup
        }
        try {
            stopTransport();
        } catch (Exception ignored) {
            // Best effort cleanup
        }
        try {
            closeStorage();
        } catch (Exception ignored) {
            // Best effort cleanup
        }
        try {
            closeClock();
        } catch (Exception ignored) {
            // Best effort cleanup
        }
    }
}
