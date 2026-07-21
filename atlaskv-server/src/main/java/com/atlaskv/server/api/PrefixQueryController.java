package com.atlaskv.server.api;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.RaftRole;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.server.api.dto.PrefixEntry;
import com.atlaskv.server.api.dto.PrefixQueryResponse;
import com.atlaskv.server.api.dto.RevisionResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.metrics.PrefixMetrics;
import com.atlaskv.server.statemachine.KeyMetadata;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * REST controller for prefix-based key-value queries.
 * Supports strongly consistent (linearizable) reads via Raft ReadIndex.
 */
@RestController
@RequestMapping("/api/v1/kv")
@Tag(name = "Prefix Queries",
        description = "Prefix-based key-value scanning with pagination and sorting")
public class PrefixQueryController {

    private static final long READ_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    private final NodeLifecycleManager lifecycleManager;
    private final KeyValueStateMachine stateMachine;
    private final PrefixMetrics prefixMetrics;

    @Autowired
    public PrefixQueryController(
            NodeLifecycleManager lifecycleManager,
            KeyValueStateMachine stateMachine,
            PrefixMetrics prefixMetrics) {
        this.lifecycleManager = lifecycleManager;
        this.stateMachine = stateMachine;
        this.prefixMetrics = prefixMetrics;
    }

    /**
     * Scans all keys matching the given prefix.
     *
     * @param prefix          the key prefix to match
     * @param limit           maximum number of entries to return
     * @param offset          pagination offset
     * @param sort            sort order (asc or desc)
     * @param includeMetadata whether to include version/timestamps
     * @param linearizable    whether to perform linearizable read
     * @return paginated prefix query response
     */
    @GetMapping("/prefix/{prefix}/**")
    @Operation(summary = "Query keys by prefix",
            description = "Returns all key-value pairs matching the prefix. "
                    + "Supports pagination, sorting, and optional metadata")
    public ResponseEntity<PrefixQueryResponse> queryByPrefix(
            @PathVariable
            @Parameter(description = "Key prefix to match")
            String prefix,
            @RequestParam(defaultValue = "100")
            @Parameter(description = "Maximum entries to return (1-1000)")
            int limit,
            @RequestParam(defaultValue = "0")
            @Parameter(description = "Pagination offset")
            int offset,
            @RequestParam(defaultValue = "asc")
            @Parameter(description = "Sort order: asc or desc")
            String sort,
            @RequestParam(defaultValue = "true")
            @Parameter(description = "Include version/timestamp metadata")
            boolean includeMetadata,
            @RequestParam(defaultValue = "false")
            @Parameter(description = "Include historical revisions")
            boolean includeHistory,
            @RequestParam(name = "linearizable", defaultValue = "true")
            @Parameter(description = "Linearizable ReadIndex read")
            boolean linearizable,
            jakarta.servlet.http.HttpServletRequest request) {

        // Reconstruct the full prefix from the path
        String fullPrefix = extractFullPrefix(request);

        // Clamp limit
        int clampedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        int clampedOffset = Math.max(0, offset);

        long startNs = System.nanoTime();

        if (linearizable) {
            waitForReadIndex();
        }

        // Perform prefix scan
        List<Map.Entry<String, String>> allMatches =
                stateMachine.getByPrefix(fullPrefix);

        // Sort
        if ("desc".equalsIgnoreCase(sort)) {
            allMatches.sort(
                    Map.Entry.<String, String>comparingByKey()
                            .reversed());
        }

        int totalCount = allMatches.size();

        // Paginate
        int fromIndex = Math.min(clampedOffset, totalCount);
        int toIndex = Math.min(fromIndex + clampedLimit, totalCount);
        List<Map.Entry<String, String>> page =
                allMatches.subList(fromIndex, toIndex);

        // Build entries
        long now = System.currentTimeMillis();
        List<PrefixEntry> entries = new ArrayList<>(page.size());
        for (Map.Entry<String, String> e : page) {
            String key = e.getKey();
            String value = e.getValue();

            Long version = null;
            Long createdAt = null;
            Long updatedAt = null;
            Long ttlRemaining = null;
            String leaseId = null;
            List<RevisionResponse> histList = null;

            if (includeMetadata) {
                KeyMetadata meta = stateMachine.metadata().get(key);
                if (meta != null) {
                    version = meta.version();
                    createdAt = meta.createdAt();
                    updatedAt = meta.updatedAt();
                }
                Long expiry = stateMachine.keyTtls().get(key);
                if (expiry != null) {
                    long remaining = expiry - now;
                    ttlRemaining = remaining > 0 ? remaining : 0;
                }
                leaseId = stateMachine.keyToLease().get(key);
            }

            if (includeHistory) {
                List<com.atlaskv.server.statemachine.KeyRevision> revs = stateMachine.history().get(key);
                if (revs != null) {
                    histList = new ArrayList<>();
                    for (com.atlaskv.server.statemachine.KeyRevision rev : revs) {
                        histList.add(new RevisionResponse(
                                rev.revisionNumber(),
                                rev.value(),
                                rev.timestamp(),
                                rev.operation(),
                                rev.nodeId(),
                                rev.leaseId(),
                                rev.ttl()
                        ));
                    }
                }
            }

            entries.add(new PrefixEntry(key, value,
                    version, createdAt, updatedAt,
                    ttlRemaining, leaseId, histList));
        }

        long latencyNs = System.nanoTime() - startNs;
        prefixMetrics.recordQuery(latencyNs, entries.size());

        PrefixQueryResponse response = new PrefixQueryResponse(
                fullPrefix, entries, totalCount,
                clampedOffset, clampedLimit);
        return ResponseEntity.ok(response);
    }

    /**
     * Extracts the full prefix from the request URI.
     * Handles slashes in the prefix path (e.g. config/database/).
     */
    private String extractFullPrefix(
            jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        String marker = "/api/v1/kv/prefix/";
        int idx = uri.indexOf(marker);
        if (idx != -1) {
            return uri.substring(idx + marker.length());
        }
        return "";
    }

    private void waitForReadIndex() {
        RaftNode node = requireLeader();
        CompletableFuture<Long> readIndexFuture = new CompletableFuture<>();
        node.handleEvent(
                new RaftEvent.ClientReadIndexEvent(readIndexFuture));

        try {
            readIndexFuture.get(
                    READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new CommandTimeoutException(
                    "ReadIndex timed out after "
                            + READ_TIMEOUT_SECONDS + " seconds");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalStateException) {
                throw new NotLeaderException(
                        e.getCause().getMessage());
            }
            throw new CommandTimeoutException(
                    "ReadIndex failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandTimeoutException(
                    "ReadIndex interrupted");
        }
    }

    private RaftNode requireLeader() {
        RaftNode node = lifecycleManager.raftNode();
        if (node == null) {
            throw new NotLeaderException("Node is not running");
        }
        if (node.role() != RaftRole.LEADER) {
            com.atlaskv.core.NodeId leaderNodeId =
                    node.currentLeader();
            String leaderId = leaderNodeId != null
                    ? leaderNodeId.value() : null;
            java.net.InetSocketAddress leaderSocketAddr = null;
            if (leaderNodeId != null
                    && lifecycleManager.config() != null) {
                leaderSocketAddr = lifecycleManager.config()
                        .peerAddresses().get(leaderNodeId);
            }
            String leaderAddress = NotLeaderException.resolveLeaderAddress(leaderNodeId, leaderSocketAddr);
            throw new NotLeaderException(
                    "This node is not the leader. Current leader: "
                            + (leaderId != null
                            ? leaderId : "unknown"),
                    leaderId, leaderAddress);
        }
        return node;
    }
}
