package com.atlaskv.sdk.api;

import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.ClusterStatus;
import com.atlaskv.sdk.models.Metrics;
import com.atlaskv.sdk.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Cluster API client. Exposes cluster state, leader node discovery, and server metrics.
 */
public final class ClusterApi {

    private final AtlasKVClient client;

    public ClusterApi(AtlasKVClient client) {
        this.client = client;
    }

    /**
     * Retrieves the status of the cluster node.
     *
     * @return cluster status
     */
    public ClusterStatus status() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/cluster/status"))
                .GET();

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), ClusterStatus.class));
    }

    /**
     * Retrieves the status of the cluster node asynchronously.
     */
    public CompletableFuture<ClusterStatus> statusAsync() {
        return CompletableFuture.supplyAsync(this::status, client.connectionPool().executorService());
    }

    /**
     * Retrieves the ID of the current leader node.
     *
     * @return leader node ID (or null if unknown)
     */
    public String leader() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/cluster/leader"))
                .GET();

        return client.execute(builder, response -> {
            Map<String, Object> body = JsonUtil.readValue(response.body(), new TypeReference<>() {});
            return (String) body.get("leaderId");
        });
    }

    /**
     * Retrieves the ID of the current leader node asynchronously.
     */
    public CompletableFuture<String> leaderAsync() {
        return CompletableFuture.supplyAsync(this::leader, client.connectionPool().executorService());
    }

    /**
     * Lists the active members of the cluster.
     *
     * @return list of active member node IDs
     */
    public List<String> members() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/cluster/members"))
                .GET();

        return client.execute(builder, response -> {
            Map<String, Object> body = JsonUtil.readValue(response.body(), new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<String> members = (List<String>) body.get("members");
            return members != null ? members : List.of();
        });
    }

    /**
     * Lists the active members of the cluster asynchronously.
     */
    public CompletableFuture<List<String>> membersAsync() {
        return CompletableFuture.supplyAsync(this::members, client.connectionPool().executorService());
    }

    /**
     * Retrieves internal performance metrics from the cluster.
     *
     * @return cluster metrics
     */
    public Metrics metrics() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/cluster/metrics"))
                .GET();

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), Metrics.class));
    }

    /**
     * Retrieves internal performance metrics asynchronously.
     */
    public CompletableFuture<Metrics> metricsAsync() {
        return CompletableFuture.supplyAsync(this::metrics, client.connectionPool().executorService());
    }
}
