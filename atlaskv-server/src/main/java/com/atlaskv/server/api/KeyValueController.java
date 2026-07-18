package com.atlaskv.server.api;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.RaftRole;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
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
import org.springframework.web.bind.annotation.RequestParam;
import java.util.concurrent.ExecutionException;

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

    /**
     * Constructs the KeyValueController.
     *
     * @param lifecycleManager node lifecycle manager
     * @param stateMachine key-value state machine
     */
    public KeyValueController(NodeLifecycleManager lifecycleManager,
                              KeyValueStateMachine stateMachine) {
        this(lifecycleManager, stateMachine, new ReadMetrics());
    }

    /**
     * Constructs the KeyValueController with metrics.
     *
     * @param lifecycleManager node lifecycle manager
     * @param stateMachine key-value state machine
     * @param readMetrics read latency metrics recorder
     */
    @Autowired
    public KeyValueController(NodeLifecycleManager lifecycleManager,
                              KeyValueStateMachine stateMachine,
                              ReadMetrics readMetrics) {
        this.lifecycleManager = lifecycleManager;
        this.stateMachine = stateMachine;
        this.readMetrics = readMetrics;
    }

    /**
     * Reads a value by key. Performs linearizable ReadIndex read by default.
     *
     * @param key the key to look up
     * @param linearizable whether to require linearizable ReadIndex read (default: true)
     * @return key-value response
     */
    @GetMapping("/{key}")
    @Operation(summary = "Read a value by key",
            description = "Returns the value associated with the key. By default performs linearizable ReadIndex read")
    public ResponseEntity<KeyValueResponse> get(
            @PathVariable @NotBlank String key,
            @RequestParam(name = "linearizable", defaultValue = "true") boolean linearizable) {
        if (!linearizable) {
            Optional<String> value = stateMachine.get(key);
            KeyValueResponse response = new KeyValueResponse(
                    key, value.orElse(null), value.isPresent());

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

            Optional<String> value = stateMachine.get(key);
            KeyValueResponse response = new KeyValueResponse(
                    key, value.orElse(null), value.isPresent());

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
    @PostMapping("/{key}")
    @Operation(summary = "Write a key-value pair",
            description = "Stores the value through Raft consensus. Must be sent to the leader")
    public ResponseEntity<KeyValueResponse> put(
            @PathVariable @NotBlank String key,
            @RequestBody @Valid KeyValueRequest request) {
        RaftNode node = requireLeader();

        String command = "PUT " + key + " " + request.value();
        byte[] result = submitCommand(node, command);

        String resultStr = new String(result, StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new KeyValueResponse(key, request.value(),
                        resultStr.startsWith("OK")));
    }

    /**
     * Deletes a key-value pair through Raft consensus.
     *
     * @param key the key to delete
     * @return key-value response
     */
    @DeleteMapping("/{key}")
    @Operation(summary = "Delete a key-value pair",
            description = "Removes the key through Raft consensus. Must be sent to the leader")
    public ResponseEntity<KeyValueResponse> delete(
            @PathVariable @NotBlank String key) {
        RaftNode node = requireLeader();

        String command = "DELETE " + key;
        byte[] result = submitCommand(node, command);

        String resultStr = new String(result, StandardCharsets.UTF_8);
        boolean deleted = resultStr.startsWith("DELETED");
        return ResponseEntity.ok(new KeyValueResponse(key, null, deleted));
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
            String leaderAddress = leaderSocketAddr != null
                    ? leaderSocketAddr.getHostString() + ":" + leaderSocketAddr.getPort()
                    : null;
            throw new NotLeaderException(
                    "This node is not the leader. Current leader: " + (leaderId != null ? leaderId : "unknown"),
                    leaderId, leaderAddress);
        }
        return node;
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
}
