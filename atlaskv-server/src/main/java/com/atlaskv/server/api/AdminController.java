package com.atlaskv.server.api;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.storage.SnapshotMetadata;
import com.atlaskv.server.api.dto.SnapshotResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.lifecycle.NodeState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for administrative operations: snapshot and shutdown.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration", description = "Administrative operations: snapshot, shutdown")
public class AdminController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    private final NodeLifecycleManager lifecycleManager;
    private final ConfigurableApplicationContext applicationContext;

    /**
     * Constructs the AdminController.
     *
     * @param lifecycleManager node lifecycle manager
     * @param applicationContext Spring application context for graceful shutdown
     */
    public AdminController(NodeLifecycleManager lifecycleManager,
                           ConfigurableApplicationContext applicationContext) {
        this.lifecycleManager = lifecycleManager;
        this.applicationContext = applicationContext;
    }

    /**
     * Triggers a manual snapshot of the current state machine state.
     *
     * @return snapshot response with metadata
     */
    @PostMapping("/snapshot")
    @Operation(summary = "Take a snapshot",
            description = "Triggers a manual snapshot of the Raft state machine")
    public ResponseEntity<SnapshotResponse> takeSnapshot() {
        RaftNode node = lifecycleManager.raftNode();
        if (node == null || lifecycleManager.state() != NodeState.RUNNING) {
            return ResponseEntity.badRequest().body(
                    new SnapshotResponse(false, 0, 0));
        }

        SnapshotMetadata meta = node.takeSnapshot();
        if (meta == null) {
            return ResponseEntity.ok(new SnapshotResponse(false, 0, 0));
        }

        LOG.info("Manual snapshot taken: lastIncludedIndex={}, lastIncludedTerm={}",
                meta.lastIncludedIndex(), meta.lastIncludedTerm());

        return ResponseEntity.ok(new SnapshotResponse(
                true, meta.lastIncludedIndex(), meta.lastIncludedTerm()));
    }

    /**
     * Initiates a graceful shutdown of the AtlasKV server.
     *
     * @return acknowledgment
     */
    @PostMapping("/shutdown")
    @Operation(summary = "Graceful shutdown",
            description = "Shuts down the Raft node and the Spring Boot application gracefully")
    public ResponseEntity<Map<String, String>> shutdown() {
        LOG.info("Shutdown requested via REST API");

        // Perform async shutdown so the response can be returned
        Thread.ofVirtual().name("shutdown-trigger").start(() -> {
            try {
                Thread.sleep(500); // Allow response to be sent
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            applicationContext.close();
        });

        return ResponseEntity.ok(Map.of(
                "status", "shutting_down",
                "message", "AtlasKV node is shutting down gracefully"));
    }
}
