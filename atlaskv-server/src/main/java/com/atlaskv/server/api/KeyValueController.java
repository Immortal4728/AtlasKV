package com.atlaskv.server.api;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.RaftRole;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.server.api.dto.CasConflictResponse;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.api.dto.RevisionResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.metrics.CasMetrics;
import com.atlaskv.server.metrics.HistoryMetrics;
import com.atlaskv.server.metrics.ReadMetrics;
import com.atlaskv.server.security.NamespaceResolver;
import com.atlaskv.server.statemachine.KeyMetadata;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
import com.atlaskv.server.statemachine.LeaseInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * REST controller for key-value CRUD operations.
 * Writes route through Raft consensus; reads use Raft ReadIndex for linearizability.
 * Operations are transparently scoped to the caller's logical namespace.
 */
@RestController
@RequestMapping("/api/v1/kv")
@Tag(name = "Key-Value Store", description = "CRUD operations on the distributed key-value store")
public class KeyValueController {

    private static final long WRITE_TIMEOUT_SECONDS = 5;
    private static final long READ_TIMEOUT_SECONDS = 5;

    private final NodeLifecycleManager lifecycleManager;
    private final KeyValueStateMachine stateMachine;
    private final ReadMetrics readMetrics;
    private final CasMetrics casMetrics;
    private final HistoryMetrics historyMetrics;

    /**
     * Constructs the KeyValueController with defaults.
     */
    public KeyValueController(NodeLifecycleManager lifecycleManager, KeyValueStateMachine stateMachine) {
        this(lifecycleManager, stateMachine, new ReadMetrics(), new CasMetrics(), new HistoryMetrics());
    }

    /**
     * Constructs the KeyValueController with metrics.
     */
    @Autowired
    public KeyValueController(NodeLifecycleManager lifecycleManager,
                              KeyValueStateMachine stateMachine,
                              ReadMetrics readMetrics,
                              CasMetrics casMetrics,
                              HistoryMetrics historyMetrics) {
        this.lifecycleManager = lifecycleManager;
        this.stateMachine = stateMachine;
        this.readMetrics = readMetrics;
        this.casMetrics = casMetrics;
        this.historyMetrics = historyMetrics;
    }

    /**
     * Handles GET requests for key lookup or history.
     */
    @GetMapping("/{key}/**")
    @Operation(summary = "Read a value by key or retrieve revision history")
    public ResponseEntity<?> handleGet(
            @PathVariable @NotBlank String key,
            @RequestParam(name = "linearizable", defaultValue = "true") boolean linearizable,
            HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.endsWith("/history")) {
            return getHistory(linearizable, request);
        }
        return get(linearizable, request);
    }

    private record KeyTtlInfo(Long ttlRemaining, String clientLeaseId, boolean isExpired) {}

    private KeyTtlInfo resolveKeyTtl(String storageKey, String namespace) {
        long now = System.currentTimeMillis();
        Long ttlRemaining = null;
        boolean expired = false;

        Long expiry = stateMachine.keyTtls().get(storageKey);
        if (expiry != null) {
            long remaining = expiry - now;
            if (remaining <= 0) {
                expired = true;
            } else {
                ttlRemaining = remaining;
            }
        }

        String storageLeaseId = stateMachine.keyToLease().get(storageKey);
        String clientLeaseId = null;
        if (storageLeaseId != null) {
            clientLeaseId = NamespaceResolver.toClientLeaseId(storageLeaseId, namespace);
            LeaseInfo lease = stateMachine.leases().get(storageLeaseId);
            if (lease == null || lease.status() != com.atlaskv.server.statemachine.LeaseStatus.ACTIVE) {
                expired = true;
            } else {
                long leaseRemaining = lease.expiryTimeMs() - now;
                if (leaseRemaining <= 0) {
                    expired = true;
                } else {
                    ttlRemaining = (ttlRemaining == null) ? leaseRemaining : Math.min(ttlRemaining, leaseRemaining);
                }
            }
        }

        return new KeyTtlInfo(ttlRemaining, clientLeaseId, expired);
    }

    private ResponseEntity<KeyValueResponse> get(boolean linearizable, HttpServletRequest request) {
        String clientKey = extractKey(request);
        String namespace = NamespaceResolver.resolveNamespace(request);
        String storageKey = NamespaceResolver.toStorageKey(clientKey, namespace);

        if (linearizable) {
            try {
                waitForReadIndex();
            } catch (NotLeaderException ignored) {
                // Follower nodes serve local state machine state when not leader
            }
        }

        Optional<String> value = stateMachine.get(storageKey);
        KeyTtlInfo ttlInfo = resolveKeyTtl(storageKey, namespace);

        if (ttlInfo.isExpired()) {
            value = Optional.empty();
        }

        KeyMetadata meta = value.isPresent() ? stateMachine.metadata().get(storageKey) : null;
        KeyValueResponse response = new KeyValueResponse(
                clientKey, value.orElse(null), value.isPresent(),
                meta != null ? meta.version() : null,
                meta != null ? meta.createdAt() : null,
                meta != null ? meta.updatedAt() : null,
                value.isPresent() ? ttlInfo.ttlRemaining() : null,
                value.isPresent() ? ttlInfo.clientLeaseId() : null);

        return value.isPresent()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles POST requests for writing key-value or rollback.
     */
    @PostMapping("/{key}/**")
    @Operation(summary = "Write a key-value pair or rollback to a revision")
    public ResponseEntity<?> handlePost(
            @PathVariable @NotBlank String key,
            @RequestBody(required = false) KeyValueRequest requestBody,
            HttpServletRequest request) {
        String uri = request.getRequestURI();
        int rollbackIdx = uri.indexOf("/rollback/");
        if (rollbackIdx != -1) {
            try {
                String revStr = uri.substring(rollbackIdx + "/rollback/".length());
                return rollback(Long.parseLong(revStr.trim()), request);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
        if (requestBody == null || requestBody.value() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return put(requestBody, request);
    }

    private ResponseEntity<KeyValueResponse> put(KeyValueRequest request, HttpServletRequest requestContext) {
        String clientKey = extractKey(requestContext);
        String namespace = NamespaceResolver.resolveNamespace(requestContext);
        String storageKey = NamespaceResolver.toStorageKey(clientKey, namespace);

        RaftNode node = requireLeader();
        String nodeId = lifecycleManager.healthStatus().nodeId().value();

        String command;
        if (request.ttl() != null || request.leaseId() != null) {
            String ttlParam = request.ttl() != null ? request.ttl() : "NULL";
            String storageLeaseId = request.leaseId() != null
                    ? NamespaceResolver.toStorageLeaseId(request.leaseId(), namespace)
                    : "NULL";
            command = "PUT_TTL_HIST " + nodeId + " " + storageKey + " " + ttlParam + " " + storageLeaseId + " " + request.value();
        } else {
            command = "PUT_HIST " + nodeId + " " + storageKey + " " + request.value();
        }

        byte[] result = submitCommand(node, command);
        String resultStr = new String(result, StandardCharsets.UTF_8);
        if (resultStr.startsWith("ERROR:")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, resultStr.substring(6).trim());
        }
        boolean success = resultStr.startsWith("OK");
        if (success) {
            historyMetrics.recordWrite();
        }
        KeyMetadata meta = success ? stateMachine.metadata().get(storageKey) : null;
        KeyTtlInfo ttlInfo = success ? resolveKeyTtl(storageKey, namespace) : new KeyTtlInfo(null, null, false);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new KeyValueResponse(clientKey, request.value(), success,
                        meta != null ? meta.version() : null,
                        meta != null ? meta.createdAt() : null,
                        meta != null ? meta.updatedAt() : null,
                        ttlInfo.ttlRemaining(),
                        ttlInfo.clientLeaseId()));
    }

    /**
     * Writes a key-value pair with Compare-And-Swap (CAS).
     */
    @PutMapping("/{key}/**")
    @Operation(summary = "Write a key-value pair with Compare-And-Swap (CAS)")
    public ResponseEntity<?> putCas(
            @PathVariable @NotBlank String key,
            @RequestBody @Valid KeyValueRequest request,
            @RequestHeader(value = "If-Version", required = false) Long headerVersion,
            @RequestParam(value = "expectedVersion", required = false) Long paramVersion,
            HttpServletRequest requestContext) {
        Long expectedVersion = headerVersion != null ? headerVersion : paramVersion;
        if (expectedVersion == null) {
            return put(request, requestContext);
        }

        String clientKey = extractKey(requestContext);
        String namespace = NamespaceResolver.resolveNamespace(requestContext);
        String storageKey = NamespaceResolver.toStorageKey(clientKey, namespace);

        RaftNode node = requireLeader();
        String nodeId = lifecycleManager.healthStatus().nodeId().value();

        casMetrics.recordAttempt();
        long start = System.nanoTime();

        String command = "CAS_PUT_HIST " + nodeId + " " + storageKey + " " + expectedVersion + " " + request.value();
        byte[] result = submitCommand(node, command);
        String resultStr = new String(result, StandardCharsets.UTF_8);
        casMetrics.recordLatency(System.nanoTime() - start);

        if (resultStr.startsWith("CONFLICT:")) {
            casMetrics.recordFailure();
            long currentVersion = parseConflictCurrentVersion(resultStr);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new CasConflictResponse(
                    expectedVersion, currentVersion,
                    "Version mismatch: expected " + expectedVersion + " but current is " + currentVersion));
        }

        if (resultStr.startsWith("OK")) {
            casMetrics.recordSuccess();
            historyMetrics.recordWrite();
            KeyMetadata meta = stateMachine.metadata().get(storageKey);
            KeyTtlInfo ttlInfo = resolveKeyTtl(storageKey, namespace);
            return ResponseEntity.ok(new KeyValueResponse(
                    clientKey, request.value(), true,
                    meta != null ? meta.version() : null,
                    meta != null ? meta.createdAt() : null,
                    meta != null ? meta.updatedAt() : null,
                    ttlInfo.ttlRemaining(),
                    ttlInfo.clientLeaseId()));
        }

        casMetrics.recordFailure();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new KeyValueResponse(clientKey, request.value(), false));
    }

    private long parseConflictCurrentVersion(String resultStr) {
        try {
            int currentIdx = resultStr.indexOf("current=");
            if (currentIdx != -1) {
                return Long.parseLong(resultStr.substring(currentIdx + 8).trim());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Deletes a key-value pair through Raft consensus.
     */
    @DeleteMapping("/{key}/**")
    @Operation(summary = "Delete a key-value pair")
    public ResponseEntity<KeyValueResponse> delete(
            @PathVariable @NotBlank String key,
            HttpServletRequest request) {
        String clientKey = extractKey(request);
        String namespace = NamespaceResolver.resolveNamespace(request);
        String storageKey = NamespaceResolver.toStorageKey(clientKey, namespace);

        RaftNode node = requireLeader();
        String nodeId = lifecycleManager.healthStatus().nodeId().value();

        String command = "DELETE_HIST " + nodeId + " " + storageKey;
        byte[] result = submitCommand(node, command);
        boolean deleted = new String(result, StandardCharsets.UTF_8).startsWith("DELETED");
        if (deleted) {
            historyMetrics.recordWrite();
        }
        return ResponseEntity.ok(new KeyValueResponse(clientKey, null, deleted));
    }

    private ResponseEntity<List<RevisionResponse>> getHistory(boolean linearizable, HttpServletRequest request) {
        String clientKey = extractKey(request);
        String namespace = NamespaceResolver.resolveNamespace(request);
        String storageKey = NamespaceResolver.toStorageKey(clientKey, namespace);

        historyMetrics.recordRead();
        if (linearizable) {
            waitForReadIndex();
        }

        List<com.atlaskv.server.statemachine.KeyRevision> revisions = stateMachine.history().get(storageKey);
        if (revisions == null || revisions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<RevisionResponse> responses = new ArrayList<>();
        for (var rev : revisions) {
            String clientLeaseId = (rev.leaseId() != null && !"NULL".equalsIgnoreCase(rev.leaseId()))
                    ? NamespaceResolver.toClientLeaseId(rev.leaseId(), namespace)
                    : null;
            String ttlStr = (rev.ttl() != null && !"NULL".equalsIgnoreCase(rev.ttl()))
                    ? rev.ttl()
                    : null;
            responses.add(new RevisionResponse(
                    rev.revisionNumber(), rev.value(), rev.timestamp(),
                    rev.operation(), rev.nodeId(), clientLeaseId, ttlStr));
        }
        return ResponseEntity.ok(responses);
    }

    private ResponseEntity<KeyValueResponse> rollback(long revision, HttpServletRequest request) {
        String clientKey = extractKey(request);
        String namespace = NamespaceResolver.resolveNamespace(request);
        String storageKey = NamespaceResolver.toStorageKey(clientKey, namespace);

        RaftNode node = requireLeader();
        String nodeId = lifecycleManager.healthStatus().nodeId().value();

        String command = "ROLLBACK " + nodeId + " " + storageKey + " " + revision;
        byte[] result = submitCommand(node, command);
        if (new String(result, StandardCharsets.UTF_8).startsWith("ERROR:")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KeyValueResponse(clientKey, null, false));
        }

        historyMetrics.recordRollback();
        historyMetrics.recordWrite();
        Optional<String> value = stateMachine.get(storageKey);
        KeyMetadata meta = stateMachine.metadata().get(storageKey);
        KeyTtlInfo ttlInfo = resolveKeyTtl(storageKey, namespace);

        return ResponseEntity.ok(new KeyValueResponse(
                clientKey, value.orElse(null), true,
                meta != null ? meta.version() : null,
                meta != null ? meta.createdAt() : null,
                meta != null ? meta.updatedAt() : null,
                ttlInfo.ttlRemaining(),
                ttlInfo.clientLeaseId()));
    }

    private void waitForReadIndex() {
        RaftNode node = requireLeader();
        long startTime = System.currentTimeMillis();
        CompletableFuture<Long> future = new CompletableFuture<>();
        node.handleEvent(new RaftEvent.ClientReadIndexEvent(future));

        try {
            future.get(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            readMetrics.recordReadSuccess(System.currentTimeMillis() - startTime);
        } catch (TimeoutException e) {
            readMetrics.recordReadFailure();
            throw new CommandTimeoutException("ReadIndex timed out after " + READ_TIMEOUT_SECONDS + " seconds");
        } catch (ExecutionException e) {
            readMetrics.recordReadFailure();
            if (e.getCause() instanceof IllegalStateException) {
                throw new NotLeaderException(e.getCause().getMessage());
            }
            throw new CommandTimeoutException("ReadIndex failed: " + e.getMessage());
        } catch (InterruptedException e) {
            readMetrics.recordReadFailure();
            Thread.currentThread().interrupt();
            throw new CommandTimeoutException("ReadIndex interrupted");
        }
    }

    private RaftNode requireLeader() {
        RaftNode node = lifecycleManager.raftNode();
        if (node == null) {
            throw new NotLeaderException("Node is not running");
        }
        if (node.role() != RaftRole.LEADER) {
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
        return node;
    }

    private byte[] submitCommand(RaftNode node, String command) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        node.handleEvent(new RaftEvent.ClientCommandEvent(command.getBytes(StandardCharsets.UTF_8), future));
        try {
            return future.get(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new CommandTimeoutException("Command timed out after " + WRITE_TIMEOUT_SECONDS + " seconds");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalStateException) {
                throw new NotLeaderException(e.getCause().getMessage());
            }
            throw new CommandTimeoutException("Command failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandTimeoutException("Command interrupted");
        }
    }

    private String extractKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/api/v1/kv/";
        int idx = uri.indexOf(prefix);
        if (idx == -1) {
            return "";
        }
        String key = uri.substring(idx + prefix.length());
        int queryIdx = key.indexOf('?');
        if (queryIdx != -1) {
            key = key.substring(0, queryIdx);
        }
        if (key.endsWith("/history")) {
            key = key.substring(0, key.length() - "/history".length());
        }
        int rollbackIdx = key.indexOf("/rollback/");
        if (rollbackIdx != -1) {
            key = key.substring(0, rollbackIdx);
        }
        return key;
    }
}
