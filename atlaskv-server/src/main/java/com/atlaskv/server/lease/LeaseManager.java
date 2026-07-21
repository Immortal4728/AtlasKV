package com.atlaskv.server.lease;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.NotLeaderException;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.metrics.LeaseMetrics;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
import com.atlaskv.server.statemachine.LeaseInfo;
import com.atlaskv.server.statemachine.DurationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe Lease Manager with replicated consensus operations and expiration scheduler.
 */
@Component
public final class LeaseManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(LeaseManager.class);
    private static final long WRITE_TIMEOUT_SECONDS = 5;

    private final KeyValueStateMachine stateMachine;
    private final NodeLifecycleManager lifecycleManager;
    private final LeaseMetrics metrics;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "lease-manager-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Constructs a LeaseManager.
     *
     * @param stateMachine     the database state machine
     * @param lifecycleManager the node lifecycle manager
     * @param metrics          the lease metrics collector
     */
    @Autowired
    public LeaseManager(KeyValueStateMachine stateMachine,
                        NodeLifecycleManager lifecycleManager,
                        LeaseMetrics metrics) {
        this.stateMachine = stateMachine;
        this.lifecycleManager = lifecycleManager;
        this.metrics = metrics;
        this.scheduler.scheduleAtFixedRate(this::checkExpirations, 500, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Replicates a lease creation command via Raft.
     *
     * @param leaseId     lease ID
     * @param durationStr human-readable TTL duration (e.g. 30s)
     */
    public void createLease(String leaseId, String durationStr) {
        long durationMs = DurationParser.parseDurationMs(durationStr);
        String command = "LEASE_CREATE " + leaseId + " " + durationMs;
        submitCommand(command);
        metrics.recordLeaseCreated(durationMs);
        LOG.info("Replicated creation of lease [{}] with duration [{}] ms", leaseId, durationMs);
    }

    /**
     * Replicates a lease renewal command via Raft.
     *
     * @param leaseId lease ID
     */
    public void renewLease(String leaseId) {
        String command = "LEASE_RENEW " + leaseId;
        submitCommand(command);
        metrics.recordRenewal();
        LOG.info("Replicated renewal of lease [{}]", leaseId);
    }

    /**
     * Replicates a lease revocation command via Raft.
     *
     * @param leaseId lease ID
     */
    public void revokeLease(String leaseId) {
        String command = "LEASE_REVOKE " + leaseId;
        submitCommand(command);
        metrics.recordLeaseRevoked();
        LOG.info("Replicated revocation of lease [{}]", leaseId);
    }

    /**
     * Returns active leases registered in the state machine.
     *
     * @return collection of leases
     */
    public Collection<LeaseInfo> listLeases() {
        return stateMachine.leases().values();
    }

    private void checkExpirations() {
        RaftNode node = lifecycleManager.raftNode();
        if (node == null || node.role() != RaftRole.LEADER) {
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Scan and expire Key TTLs
        for (Map.Entry<String, Long> entry : stateMachine.keyTtls().entrySet()) {
            if (now > entry.getValue()) {
                String key = entry.getKey();
                LOG.info("Leader detected expired key [{}], submitting EXPIRE command", key);
                try {
                    submitCommand("EXPIRE " + key);
                } catch (Exception e) {
                    LOG.error("Failed to submit EXPIRE for key: {}", key, e);
                }
            }
        }

        // 2. Scan and revoke expired leases
        for (LeaseInfo lease : stateMachine.leases().values()) {
            if (now > lease.expiryTimeMs()) {
                String leaseId = lease.leaseId();
                LOG.info("Leader detected expired lease [{}], submitting LEASE_REVOKE command", leaseId);
                try {
                    submitCommand("LEASE_REVOKE " + leaseId);
                    metrics.recordLeaseExpired();
                } catch (Exception e) {
                    LOG.error("Failed to submit LEASE_REVOKE for leaseId: {}", leaseId, e);
                }
            }
        }
    }

    private void submitCommand(String command) {
        RaftNode node = lifecycleManager.raftNode();
        if (node == null) {
            throw new NotLeaderException("Node is not running");
        }
        if (node.role() != RaftRole.LEADER) {
            // Find current leader metadata to aid redirection
            com.atlaskv.core.NodeId leaderNodeId = node.currentLeader();
            String leaderId = leaderNodeId != null ? leaderNodeId.value() : null;
            java.net.InetSocketAddress leaderSocketAddr = null;
            if (leaderNodeId != null && lifecycleManager.config() != null) {
                leaderSocketAddr = lifecycleManager.config().peerAddresses().get(leaderNodeId);
            }
            String leaderAddress = NotLeaderException.resolveLeaderAddress(leaderNodeId, leaderSocketAddr);
            throw new NotLeaderException(
                    "This node is not the leader. Current leader: " + (leaderId != null ? leaderId : "unknown"),
                    leaderId, leaderAddress);
        }

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        node.handleEvent(new com.atlaskv.core.event.RaftEvent.ClientCommandEvent(
                command.getBytes(StandardCharsets.UTF_8), future));

        try {
            future.get(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("Command timed out after "
                    + WRITE_TIMEOUT_SECONDS + " seconds");
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof IllegalStateException) {
                throw new NotLeaderException(e.getCause().getMessage());
            }
            throw new IllegalStateException("Command failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Command interrupted");
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }
}
