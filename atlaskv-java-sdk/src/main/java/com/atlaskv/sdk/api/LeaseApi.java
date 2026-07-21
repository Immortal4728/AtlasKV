package com.atlaskv.sdk.api;

import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.Lease;
import com.atlaskv.sdk.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Lease API client. Allows clients to acquire, renew, list, or revoke distributed leases.
 */
public final class LeaseApi {

    private final AtlasKVClient client;

    public LeaseApi(AtlasKVClient client) {
        this.client = client;
    }

    /**
     * Creates a new distributed lease with an auto-generated lease ID.
     *
     * @param ttl TTL duration string (e.g. "30s", "1m")
     * @return details of the created lease
     */
    public Lease createLease(String ttl) {
        return createLease(null, ttl);
    }

    /**
     * Creates a new distributed lease.
     *
     * @param leaseId custom lease ID (null or blank to auto-generate)
     * @param ttl     TTL duration string (e.g. "30s", "1m")
     * @return details of the created lease
     */
    public Lease createLease(String leaseId, String ttl) {
        if (ttl == null || ttl.isBlank()) {
            throw new IllegalArgumentException("TTL must not be null or blank");
        }

        LeaseRequest requestBody = new LeaseRequest(leaseId, ttl);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/lease"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.writeValueAsString(requestBody)));

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), Lease.class));
    }

    /**
     * Creates a new distributed lease asynchronously.
     */
    public CompletableFuture<Lease> createLeaseAsync(String ttl) {
        return CompletableFuture.supplyAsync(() -> createLease(ttl), client.connectionPool().executorService());
    }

    /**
     * Creates a new distributed lease asynchronously.
     */
    public CompletableFuture<Lease> createLeaseAsync(String leaseId, String ttl) {
        return CompletableFuture.supplyAsync(() -> createLease(leaseId, ttl), client.connectionPool().executorService());
    }

    /**
     * Renews an active lease, extending its expiration deadline.
     *
     * @param leaseId the lease ID to renew
     */
    public void renewLease(String leaseId) {
        if (leaseId == null || leaseId.isBlank()) {
            throw new IllegalArgumentException("Lease ID must not be null or blank");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/lease/" + leaseId + "/renew"))
                .POST(HttpRequest.BodyPublishers.noBody());

        client.execute(builder, response -> null);
    }

    /**
     * Renews an active lease asynchronously.
     */
    public CompletableFuture<Void> renewLeaseAsync(String leaseId) {
        return CompletableFuture.runAsync(() -> renewLease(leaseId), client.connectionPool().executorService());
    }

    /**
     * Revokes an active lease, expiring all associated keys immediately.
     *
     * @param leaseId the lease ID to revoke
     */
    public void revokeLease(String leaseId) {
        if (leaseId == null || leaseId.isBlank()) {
            throw new IllegalArgumentException("Lease ID must not be null or blank");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/lease/" + leaseId))
                .DELETE();

        client.execute(builder, response -> null);
    }

    /**
     * Revokes an active lease asynchronously.
     */
    public CompletableFuture<Void> revokeLeaseAsync(String leaseId) {
        return CompletableFuture.runAsync(() -> revokeLease(leaseId), client.connectionPool().executorService());
    }

    /**
     * Lists all active leases in the cluster.
     *
     * @return list of active leases
     */
    public List<Lease> listLeases() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/lease"))
                .GET();

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), new TypeReference<List<Lease>>() {}));
    }

    /**
     * Lists active leases asynchronously.
     */
    public CompletableFuture<List<Lease>> listLeasesAsync() {
        return CompletableFuture.supplyAsync(this::listLeases, client.connectionPool().executorService());
    }

    /**
     * Internal request DTO matching the server API.
     *
     * @param leaseId the lease ID
     * @param ttl     the TTL duration
     */
    private record LeaseRequest(String leaseId, String ttl) {}
}
