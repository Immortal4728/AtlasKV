package com.atlaskv.server.api;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.RaftRole;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
import com.atlaskv.server.statemachine.KeyMetadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.atlaskv.server.metrics.ReadMetrics;
import com.atlaskv.server.metrics.HistoryMetrics;
import com.atlaskv.server.api.dto.RevisionResponse;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.concurrent.ExecutionException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST controller for key-value CRUD operations.
 * Writes are routed through the Raft consensus engine; reads use Raft ReadIndex for linearizability.
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
    private final com.atlaskv.server.metrics.CasMetrics casMetrics;
    private final HistoryMetrics historyMetrics;

    /**
     * Constructs the KeyValueController.
     *
     * @param lifecycleManager node lifecycle manager
     * @param stateMachine key-value state machine
     */
    public KeyValueController(NodeLifecycleManager lifecycleManager,
                              KeyValueStateMachine stateMachine) {
        this(lifecycleManager, stateMachine, new ReadMetrics(), new com.atlaskv.server.metrics.CasMetrics(), new HistoryMetrics());
    }

    /**
     * Constructs the KeyValueController with metrics.
     *
     * @param lifecycleManager node lifecycle manager
     * @param stateMachine key-value state machine
     * @param readMetrics read latency metrics recorder
     * @param casMetrics CAS metrics recorder
     * @param historyMetrics history metrics recorder
     */
    @Autowired
    public KeyValueController(NodeLifecycleManager lifecycleManager,
                              KeyValueStateMachine stateMachine,
                              ReadMetrics readMetrics,
                              com.atlaskv.server.metrics.CasMetrics casMetrics,
                              HistoryMetrics historyMetrics) {
        this.lifecycleManager = lifecycleManager;
        this.stateMachine = stateMachine;
        this.readMetrics = readMetrics;
        this.casMetrics = casMetrics;
        this.historyMetrics = historyMetrics;
    }

    /**
     * Reads a value by key. Performs linearizable ReadIndex read by default.
     *
     * @param key the key to look up
     * @param linearizable whether to require linearizable ReadIndex read (default: true)
     * @return key-value response
     */
    @GetMapping("/{key}/**")
    @Operation(summary = "Read a value by key or retrieve revision history",
            description = "If the URI ends with /history, returns the revision history. Otherwise, returns the key value.")
    public ResponseEntity<?> handleGet(
            @PathVariable @NotBlank String key,
            @RequestParam(name = "linearizable", defaultValue = "true") boolean linearizable,
            HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.endsWith("/history")) {
            return getHistory(linearizable, request);
        } else {
            return get(linearizable, request);
        }
    }

    private ResponseEntity<KeyValueResponse> get(
            boolean linearizable,
            HttpServletRequest request) {
        String fullKey = extractKey(request);
        if (!linearizable) {
            Optional<String> value = stateMachine.get(fullKey);
            KeyMetadata meta = stateMachine.metadata().get(fullKey);
            KeyValueResponse response = new KeyValueResponse(
                    fullKey, value.orElse(null), value.isPresent(),
                    meta != null ? meta.version() : null,
                    meta != null ? meta.createdAt() : null,
                    meta != null ? meta.updatedAt() : null);

            return value.isPresent()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        RaftNode node = requireLeader();
        long startTime = System.currentTimeMillis();

        CompletableFuture<Long> readIndexFuture = new CompletableFuture<>();
        node.handleEvent(new RaftEvent.ClientReadIndexEvent(readIndexFuture));

        try {
            readIndexFuture.get(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long latency = System.currentTimeMillis() - startTime;
            readMetrics.recordReadSuccess(latency);

            Optional<String> value = stateMachine.get(fullKey);
            KeyMetadata meta = stateMachine.metadata().get(fullKey);
            KeyValueResponse response = new KeyValueResponse(
                    fullKey, value.orElse(null), value.isPresent(),
                    meta != null ? meta.version() : null,
                    meta != null ? meta.createdAt() : null,
                    meta != null ? meta.updatedAt() : null);

            return value.isPresent()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (TimeoutException e) {
            readMetrics.recordReadFailure();
            throw new CommandTimeoutException("ReadIndex timed out after "
                    + READ_TIMEOUT_SECONDS + " seconds");
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

    /**
     * Writes a key-value pair through Raft consensus.
     *
     * @param key the key to write
     * @param request request body containing the value
     * @return key-value response
     */
    @PostMapping("/{key}/**")
    @Operation(summary = "Write a key-value pair or rollback to a revision",
            description = "If the URI contains /rollback/{revision}, performs rollback. Otherwise, performs standard PUT.")
    public ResponseEntity<?> handlePost(
            @PathVariable @NotBlank String key,
            @RequestBody(required = false) @Valid KeyValueRequest requestBody,
            HttpServletRequest request) {
        String uri = request.getRequestURI();
        int rollbackIdx = uri.indexOf("/rollback/");
        if (rollbackIdx != -1) {
            try {
                String revStr = uri.substring(rollbackIdx + "/rollback/".length());
                long revision = Long.parseLong(revStr.trim());
                return rollback(revision, request);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } else {
            if (requestBody == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            return put(requestBody, request);
        }
    }

    private ResponseEntity<KeyValueResponse> put(
            KeyValueRequest request,
            HttpServletRequest requestContext) {
        String fullKey = extractKey(requestContext);
        RaftNode node = requireLeader();
        String nodeId = lifecycleManager.healthStatus().nodeId().value();

        String command;
        if (request.ttl() != null || request.leaseId() != null) {
            String ttlParam = request.ttl() != null ? request.ttl() : "NULL";
            String leaseParam = request.leaseId() != null ? request.leaseId() : "NULL";
            command = "PUT_TTL_HIST " + nodeId + " " + fullKey + " " + ttlParam + " " + leaseParam + " " + request.value();
        } else {
            command = "PUT_HIST " + nodeId + " " + fullKey + " " + request.value();
        }

        byte[] result = submitCommand(node, command);

        String resultStr = new String(result, StandardCharsets.UTF_8);
        boolean success = resultStr.startsWith("OK");
        if (success) {
            historyMetrics.recordWrite();
        }
        KeyMetadata meta = success ? stateMachine.metadata().get(fullKey) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new KeyValueResponse(fullKey, request.value(), success,
                         meta != null ? meta.version() : null,
                         meta != null ? meta.createdAt() : null,
                         meta != null ? meta.updatedAt() : null));
    }

    /**
     * Writes a key-value pair with Compare-And-Swap (CAS).
     *
     * @param key            the key to write
     * @param request        request body containing the value
     * @param headerVersion  expected version from If-Version header
     * @param paramVersion   expected version from expectedVersion query parameter
     * @return key-value response or CAS conflict details
     */
    @org.springframework.web.bind.annotation.PutMapping("/{key}/**")
    @Operation(summary = "Write a key-value pair with Compare-And-Swap (CAS)",
            description = "Updates the value if the current version matches the expected version. Must be sent to the leader")
    public ResponseEntity<?> putCas(
            @PathVariable @NotBlank String key,
            @RequestBody @Valid KeyValueRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "If-Version", required = false) Long headerVersion,
            @RequestParam(value = "expectedVersion", required = false) Long paramVersion,
            HttpServletRequest requestContext) {
        String fullKey = extractKey(requestContext);
        Long expectedVersion = headerVersion != null ? headerVersion : paramVersion;
        if (expectedVersion == null) {
            return put(request, requestContext);
        }

        RaftNode node = requireLeader();
        String nodeId = lifecycleManager.healthStatus().nodeId().value();

        casMetrics.recordAttempt();
        long start = System.nanoTime();

        String command = "CAS_PUT_HIST " + nodeId + " " + fullKey + " " + expectedVersion + " " + request.value();
        byte[] result = submitCommand(node, command);
        String resultStr = new String(result, StandardCharsets.UTF_8);

        long latencyNs = System.nanoTime() - start;
        casMetrics.recordLatency(latencyNs);

        if (resultStr.startsWith("CONFLICT:")) {
            casMetrics.recordFailure();
            long currentVersion = parseConflictCurrentVersion(resultStr);
            com.atlaskv.server.api.dto.CasConflictResponse conflict = new com.atlaskv.server.api.dto.CasConflictResponse(
                    expectedVersion,
                    currentVersion,
                    "Version mismatch: expected " + expectedVersion + " but current is " + currentVersion
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflict);
        }

        if (resultStr.startsWith("OK")) {
            casMetrics.recordSuccess();
            historyMetrics.recordWrite();
            KeyMetadata meta = stateMachine.metadata().get(fullKey);
            KeyValueResponse response = new KeyValueResponse(
                    fullKey, request.value(), true,
                    meta != null ? meta.version() : null,
                    meta != null ? meta.createdAt() : null,
                    meta != null ? meta.updatedAt() : null
            );
            return ResponseEntity.ok(response);
        }

        casMetrics.recordFailure();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new KeyValueResponse(fullKey, request.value(), false));
    }

    private long parseConflictCurrentVersion(String resultStr) {
        try {
            int currentIdx = resultStr.indexOf("current=");
            if (currentIdx != -1) {
                return Long.parseLong(resultStr.substring(currentIdx + 8).trim());
            }
        } catch (Exception e) {
            // fallback
        }
        return 0;
    }

    /**
     * Deletes a key-value pair through Raft consensus.
     *
     * @param key the key to delete
     * @return key-value response
     */
    @DeleteMapping("/{key}/**")
    @Operation(summary = "Delete a key-value pair",
            description = "Removes the key through Raft consensus. Must be sent to the leader")
    public ResponseEntity<KeyValueResponse> delete(
            @PathVariable @NotBlank String key,
            HttpServletRequest request) {
        String fullKey = extractKey(request);
        RaftNode node = requireLeader();
        String nodeId = lifecycleManager.healthStatus().nodeId().value();

        String command = "DELETE_HIST " + nodeId + " " + fullKey;
        byte[] result = submitCommand(node, command);

        String resultStr = new String(result, StandardCharsets.UTF_8);
        boolean deleted = resultStr.startsWith("DELETED");
        if (deleted) {
            historyMetrics.recordWrite();
        }
        return ResponseEntity.ok(new KeyValueResponse(fullKey, null, deleted));
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

    private ResponseEntity<java.util.List<RevisionResponse>> getHistory(
            boolean linearizable,
            HttpServletRequest request) {
        String fullKey = extractKey(request);
        historyMetrics.recordRead();
        if (!linearizable) {
            return retrieveHistory(fullKey);
        }

        RaftNode node = requireLeader();
        long startTime = System.currentTimeMillis();

        CompletableFuture<Long> readIndexFuture = new CompletableFuture<>();
        node.handleEvent(new RaftEvent.ClientReadIndexEvent(readIndexFuture));

        try {
            readIndexFuture.get(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long latency = System.currentTimeMillis() - startTime;
            readMetrics.recordReadSuccess(latency);
            return retrieveHistory(fullKey);
        } catch (TimeoutException e) {
            readMetrics.recordReadFailure();
            throw new CommandTimeoutException("ReadIndex timed out after "
                    + READ_TIMEOUT_SECONDS + " seconds");
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

    private ResponseEntity<java.util.List<RevisionResponse>> retrieveHistory(String key) {
        java.util.List<com.atlaskv.server.statemachine.KeyRevision> revisions = stateMachine.history().get(key);
        if (revisions == null || revisions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        java.util.List<RevisionResponse> responses = new java.util.ArrayList<>();
        for (var rev : revisions) {
            responses.add(new RevisionResponse(
                    rev.revisionNumber(),
                    rev.value(),
                    rev.timestamp(),
                    rev.operation(),
                    rev.nodeId(),
                    rev.leaseId(),
                    rev.ttl()
            ));
        }
        return ResponseEntity.ok(responses);
    }

    private ResponseEntity<KeyValueResponse> rollback(
            long revision,
            HttpServletRequest request) {
        String fullKey = extractKey(request);
        RaftNode node = requireLeader();
        String nodeId = lifecycleManager.healthStatus().nodeId().value();

        String command = "ROLLBACK " + nodeId + " " + fullKey + " " + revision;
        byte[] result = submitCommand(node, command);

        String resultStr = new String(result, StandardCharsets.UTF_8);
        if (resultStr.startsWith("ERROR:")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KeyValueResponse(fullKey, null, false));
        }

        historyMetrics.recordRollback();
        historyMetrics.recordWrite();

        Optional<String> value = stateMachine.get(fullKey);
        KeyMetadata meta = stateMachine.metadata().get(fullKey);

        KeyValueResponse response = new KeyValueResponse(
                fullKey, value.orElse(null), true,
                meta != null ? meta.version() : null,
                meta != null ? meta.createdAt() : null,
                meta != null ? meta.updatedAt() : null
        );

        return ResponseEntity.ok(response);
    }

    private byte[] submitCommand(RaftNode node, String command) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        node.handleEvent(new RaftEvent.ClientCommandEvent(
                command.getBytes(StandardCharsets.UTF_8), future));

        try {
            return future.get(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new CommandTimeoutException("Command timed out after "
                    + WRITE_TIMEOUT_SECONDS + " seconds");
        } catch (java.util.concurrent.ExecutionException e) {
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
