package com.atlaskv.server.api;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.RaftNode;
import com.atlaskv.core.config.ClusterMembership;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.server.api.dto.AddMemberRequest;
import com.atlaskv.server.api.dto.ClusterMembersResponse;
import com.atlaskv.server.api.dto.ClusterStatusResponse;
import com.atlaskv.server.api.dto.LeaderResponse;
import com.atlaskv.server.api.dto.MetricsResponse;
import com.atlaskv.server.config.ClusterConfig;
import com.atlaskv.server.health.NodeHealthStatus;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.metrics.HistoryMetrics;
import com.atlaskv.server.metrics.MembershipMetrics;
import com.atlaskv.server.metrics.PrefixMetrics;
import com.atlaskv.server.metrics.ReadMetrics;
import com.atlaskv.server.statemachine.KeyValueStateMachine;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * REST controller exposing cluster status, leader discovery, metrics, and membership management APIs.
 */
@RestController
@RequestMapping("/api/v1/cluster")
@Tag(name = "Cluster Management", description = "Cluster status, leader discovery, membership, and metrics APIs")
public class ClusterController {

    private final NodeLifecycleManager lifecycleManager;
    private final ClusterConfig clusterConfig;
    private final KeyValueStateMachine stateMachine;
    private final ReadMetrics readMetrics;
    private final MembershipMetrics membershipMetrics;
    private final com.atlaskv.server.metrics.CasMetrics casMetrics;
    private final PrefixMetrics prefixMetrics;
    private final HistoryMetrics historyMetrics;

    public ClusterController(NodeLifecycleManager lifecycleManager,
                             ClusterConfig clusterConfig,
                             KeyValueStateMachine stateMachine) {
        this(lifecycleManager, clusterConfig, stateMachine, new ReadMetrics(),
                new MembershipMetrics(), new com.atlaskv.server.metrics.CasMetrics(),
                new PrefixMetrics(), new HistoryMetrics());
    }

    @Autowired
    public ClusterController(NodeLifecycleManager lifecycleManager,
                             ClusterConfig clusterConfig,
                             KeyValueStateMachine stateMachine,
                             ReadMetrics readMetrics,
                             MembershipMetrics membershipMetrics,
                             com.atlaskv.server.metrics.CasMetrics casMetrics,
                             PrefixMetrics prefixMetrics,
                             HistoryMetrics historyMetrics) {
        this.lifecycleManager = lifecycleManager;
        this.clusterConfig = clusterConfig;
        this.stateMachine = stateMachine;
        this.readMetrics = readMetrics;
        this.membershipMetrics = membershipMetrics;
        this.casMetrics = casMetrics;
        this.prefixMetrics = prefixMetrics;
        this.historyMetrics = historyMetrics;
    }

    @GetMapping("/status")
    @Operation(summary = "Get cluster status",
            description = "Returns current node role, term, commit index, health, and uptime")
    public ResponseEntity<ClusterStatusResponse> getStatus() {
        NodeHealthStatus health = lifecycleManager.healthStatus();
        ClusterStatusResponse response = new ClusterStatusResponse(
                health.nodeId().value(),
                health.role(),
                health.currentTerm(),
                health.commitIndex(),
                health.lastApplied(),
                health.currentLeader() != null ? health.currentLeader().value() : null,
                health.healthy(),
                health.healthy() ? health.uptimeMillis() : 0L,
                lifecycleManager.state().name(),
                lifecycleManager.port(),
                clusterConfig.peerIds().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leader")
    @Operation(summary = "Discover current leader",
            description = "Returns the current leader node ID and whether this node is the leader")
    public ResponseEntity<LeaderResponse> getLeader() {
        NodeHealthStatus health = lifecycleManager.healthStatus();
        NodeId leader = health.currentLeader();
        LeaderResponse response = new LeaderResponse(
                leader != null ? leader.value() : null,
                health.isLeader(),
                health.currentTerm());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/members")
    @Operation(summary = "Get cluster membership", description = "Returns active member nodes and joint consensus status")
    public ResponseEntity<ClusterMembersResponse> getMembers() {
        RaftNode node = lifecycleManager.raftNode();
        if (node == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        ClusterMembership mem = node.currentMembership();
        List<String> activeMembers = mem.oldMembers().stream().map(NodeId::value).toList();
        List<String> oldMembers = mem.isJoint() ? mem.oldMembers().stream().map(NodeId::value).toList() : List.of();
        List<String> newMembers = mem.isJoint() ? mem.newMembers().stream().map(NodeId::value).toList() : List.of();
        String leaderId = node.currentLeader() != null ? node.currentLeader().value() : null;

        return ResponseEntity.ok(new ClusterMembersResponse(
                activeMembers, mem.isJoint(), oldMembers, newMembers, leaderId));
    }

    @PostMapping("/members")
    @Operation(summary = "Add a cluster member", description = "Executes Joint Consensus protocol to add a node")
    public ResponseEntity<?> addMember(@RequestBody AddMemberRequest request) {
        if (request == null || request.nodeId() == null || request.nodeId().isBlank()) {
            return ResponseEntity.badRequest().body("NodeId must not be blank");
        }

        RaftNode node = lifecycleManager.raftNode();
        if (node == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Raft node not running");
        }

        NodeId targetNode = NodeId.of(request.nodeId().trim());
        if (request.address() != null && !request.address().isBlank()) {
            lifecycleManager.registerPeer(targetNode, request.address().trim());
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        long start = System.currentTimeMillis();
        node.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.ADD, targetNode, future));

        try {
            future.get(10, TimeUnit.SECONDS);
            membershipMetrics.recordMembershipChangeSuccess(System.currentTimeMillis() - start);
            return getMembers();
        } catch (InterruptedException | ExecutionException | TimeoutException | RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof IllegalStateException && cause.getMessage() != null
                    && cause.getMessage().contains("in progress")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(cause.getMessage());
            }
            if (cause instanceof IllegalStateException && cause.getMessage() != null
                    && cause.getMessage().contains("Not leader")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cause.getMessage());
            }
            return ResponseEntity.badRequest().body(cause.getMessage());
        }
    }

    @DeleteMapping("/members/{nodeId}")
    @Operation(summary = "Remove a cluster member", description = "Executes Joint Consensus protocol to remove a node")
    public ResponseEntity<?> removeMember(@PathVariable String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return ResponseEntity.badRequest().body("NodeId must not be blank");
        }

        RaftNode node = lifecycleManager.raftNode();
        if (node == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Raft node not running");
        }

        NodeId targetNode = NodeId.of(nodeId.trim());
        CompletableFuture<Void> future = new CompletableFuture<>();
        long start = System.currentTimeMillis();
        node.handleEvent(new RaftEvent.ClientMembershipChangeEvent(
                RaftEvent.MemberChangeType.REMOVE, targetNode, future));

        try {
            future.get(10, TimeUnit.SECONDS);
            lifecycleManager.unregisterPeer(targetNode);
            membershipMetrics.recordMembershipChangeSuccess(System.currentTimeMillis() - start);
            return getMembers();
        } catch (InterruptedException | ExecutionException | TimeoutException | RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof IllegalStateException && cause.getMessage() != null
                    && cause.getMessage().contains("in progress")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(cause.getMessage());
            }
            if (cause instanceof IllegalStateException && cause.getMessage() != null
                    && cause.getMessage().contains("Not leader")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cause.getMessage());
            }
            return ResponseEntity.badRequest().body(cause.getMessage());
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get cluster metrics",
            description = "Returns log length, snapshot metadata, KV store size, uptime, read latency, and membership metrics")
    public ResponseEntity<MetricsResponse> getMetrics() {
        NodeHealthStatus health = lifecycleManager.healthStatus();
        RaftNode node = lifecycleManager.raftNode();

        long snapshotLastIndex = 0;
        long snapshotLastTerm = 0;
        long logLength = 0;

        if (node != null) {
            var meta = node.snapshotStorage().getLatestSnapshotMetadata();
            if (meta.isPresent()) {
                snapshotLastIndex = meta.get().lastIncludedIndex();
                snapshotLastTerm = meta.get().lastIncludedTerm();
            }
            logLength = health.commitIndex();
        }

        double averageHistorySize = 0.0;
        if (!stateMachine.history().isEmpty()) {
            long totalHistoryRevisions = 0;
            for (var entry : stateMachine.history().values()) {
                totalHistoryRevisions += entry.size();
            }
            averageHistorySize = (double) totalHistoryRevisions / stateMachine.history().size();
        }

        MetricsResponse response = new MetricsResponse(
                health.nodeId().value(),
                health.currentTerm(),
                health.commitIndex(),
                health.lastApplied(),
                logLength,
                snapshotLastIndex,
                snapshotLastTerm,
                stateMachine.size(),
                health.healthy() ? health.uptimeMillis() : 0L,
                readMetrics.totalReadRequests(),
                readMetrics.successfulReadRequests(),
                readMetrics.averageReadLatencyMs(),
                membershipMetrics.totalMembershipChanges(),
                membershipMetrics.averageMembershipChangeLatencyMs(),
                casMetrics.totalAttempts(),
                casMetrics.successes(),
                casMetrics.failures(),
                casMetrics.averageLatencyMs(),
                prefixMetrics.queryCount(),
                prefixMetrics.averageLatencyMs(),
                prefixMetrics.averageResultSize(),
                historyMetrics.historyReads(),
                historyMetrics.historyWrites(),
                historyMetrics.rollbackCount(),
                averageHistorySize);

        return ResponseEntity.ok(response);
    }
}
