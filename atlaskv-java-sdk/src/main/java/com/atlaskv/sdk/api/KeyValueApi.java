package com.atlaskv.sdk.api;

import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.KeyValue;
import com.atlaskv.sdk.models.PrefixResult;
import com.atlaskv.sdk.util.JsonUtil;
import com.atlaskv.sdk.util.ValidationUtil;

import java.net.http.HttpRequest;
import java.util.concurrent.CompletableFuture;

/**
 * Key-Value API client. Provides operations for CRUD, CAS, TTL, Leases, and Prefix queries.
 */
public final class KeyValueApi {

    private final AtlasKVClient client;

    public KeyValueApi(AtlasKVClient client) {
        this.client = client;
    }

    /**
     * Stores a key-value pair.
     *
     * @param key   key to store
     * @param value value to store
     * @return updated key-value metadata
     */
    public KeyValue put(String key, String value) {
        ValidationUtil.validateKey(key);
        ValidationUtil.validateValue(value);

        KeyValueRequest requestBody = new KeyValueRequest(value, null, null);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/kv/" + key))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.writeValueAsString(requestBody)));

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), KeyValue.class));
    }

    /**
     * Stores a key-value pair asynchronously.
     */
    public CompletableFuture<KeyValue> putAsync(String key, String value) {
        return CompletableFuture.supplyAsync(() -> put(key, value), client.connectionPool().executorService());
    }

    /**
     * Stores a key-value pair with a TTL.
     *
     * @param key   key to store
     * @param value value to store
     * @param ttl   TTL duration string (e.g. "30s", "10m")
     * @return updated key-value metadata
     */
    public KeyValue putWithTTL(String key, String value, String ttl) {
        ValidationUtil.validateKey(key);
        ValidationUtil.validateValue(value);
        if (ttl == null || ttl.isBlank()) {
            throw new IllegalArgumentException("TTL must not be null or blank");
        }

        KeyValueRequest requestBody = new KeyValueRequest(value, ttl, null);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/kv/" + key))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.writeValueAsString(requestBody)));

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), KeyValue.class));
    }

    /**
     * Stores a key-value pair with a TTL asynchronously.
     */
    public CompletableFuture<KeyValue> putWithTTLAsync(String key, String value, String ttl) {
        return CompletableFuture.supplyAsync(() -> putWithTTL(key, value, ttl), client.connectionPool().executorService());
    }

    /**
     * Stores a key-value pair associated with a lease.
     *
     * @param key     key to store
     * @param value   value to store
     * @param leaseId ID of the lease
     * @return updated key-value metadata
     */
    public KeyValue putWithLease(String key, String value, String leaseId) {
        ValidationUtil.validateKey(key);
        ValidationUtil.validateValue(value);
        if (leaseId == null || leaseId.isBlank()) {
            throw new IllegalArgumentException("Lease ID must not be null or blank");
        }

        KeyValueRequest requestBody = new KeyValueRequest(value, null, leaseId);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/kv/" + key))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.writeValueAsString(requestBody)));

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), KeyValue.class));
    }

    /**
     * Stores a key-value pair associated with a lease asynchronously.
     */
    public CompletableFuture<KeyValue> putWithLeaseAsync(String key, String value, String leaseId) {
        return CompletableFuture.supplyAsync(() -> putWithLease(key, value, leaseId), client.connectionPool().executorService());
    }

    /**
     * Retrieves the key-value details for a key.
     *
     * @param key key to lookup
     * @return key-value details (exists = false if key not present)
     */
    public KeyValue get(String key) {
        ValidationUtil.validateKey(key);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/kv/" + key))
                .GET();

        return client.execute(builder, response -> {
            if (response.statusCode() == 404) {
                return new KeyValue(key, null, false, null, null, null);
            }
            return JsonUtil.readValue(response.body(), KeyValue.class);
        });
    }

    /**
     * Retrieves key-value details asynchronously.
     */
    public CompletableFuture<KeyValue> getAsync(String key) {
        return CompletableFuture.supplyAsync(() -> get(key), client.connectionPool().executorService());
    }

    /**
     * Deletes a key-value pair.
     *
     * @param key key to delete
     * @return true if deleted, false otherwise
     */
    public boolean delete(String key) {
        ValidationUtil.validateKey(key);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/kv/" + key))
                .DELETE();

        return client.execute(builder, response -> {
            KeyValue kv = JsonUtil.readValue(response.body(), KeyValue.class);
            return kv.exists();
        });
    }

    /**
     * Deletes a key-value pair asynchronously.
     */
    public CompletableFuture<Boolean> deleteAsync(String key) {
        return CompletableFuture.supplyAsync(() -> delete(key), client.connectionPool().executorService());
    }

    /**
     * Checks if a key exists in the store.
     *
     * @param key key to check
     * @return true if exists, false otherwise
     */
    public boolean exists(String key) {
        return get(key).exists();
    }

    /**
     * Checks if a key exists asynchronously.
     */
    public CompletableFuture<Boolean> existsAsync(String key) {
        return CompletableFuture.supplyAsync(() -> exists(key), client.connectionPool().executorService());
    }

    /**
     * Performs a Compare-And-Swap (CAS) update on a key.
     *
     * @param key             key to update
     * @param value           new value to set
     * @param expectedVersion expected current version in the store
     * @return updated key-value details
     * @throws com.atlaskv.sdk.exceptions.ConflictException if expected version does not match
     */
    public KeyValue casPut(String key, String value, long expectedVersion) {
        ValidationUtil.validateKey(key);
        ValidationUtil.validateValue(value);

        KeyValueRequest requestBody = new KeyValueRequest(value, null, null);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/kv/" + key + "?expectedVersion=" + expectedVersion))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(JsonUtil.writeValueAsString(requestBody)));

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), KeyValue.class));
    }

    /**
     * Performs CAS update asynchronously.
     */
    public CompletableFuture<KeyValue> casPutAsync(String key, String value, long expectedVersion) {
        return CompletableFuture.supplyAsync(() -> casPut(key, value, expectedVersion), client.connectionPool().executorService());
    }

    /**
     * Queries keys matching a prefix.
     *
     * @param prefix key prefix to scan
     * @return prefix query results
     */
    public PrefixResult prefix(String prefix) {
        return prefix(prefix, 0, 100);
    }

    /**
     * Queries keys matching a prefix with pagination.
     *
     * @param prefix key prefix to scan
     * @param offset pagination offset
     * @param limit  maximum results to return
     * @return prefix query results
     */
    public PrefixResult prefix(String prefix, int offset, int limit) {
        ValidationUtil.validatePrefix(prefix);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/kv/prefix/" + prefix + "?offset=" + offset + "&limit=" + limit))
                .GET();

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), PrefixResult.class));
    }

    /**
     * Scans keys matching prefix asynchronously.
     */
    public CompletableFuture<PrefixResult> prefixAsync(String prefix) {
        return CompletableFuture.supplyAsync(() -> prefix(prefix), client.connectionPool().executorService());
    }

    /**
     * Scans keys matching prefix with pagination asynchronously.
     */
    public CompletableFuture<PrefixResult> prefixAsync(String prefix, int offset, int limit) {
        return CompletableFuture.supplyAsync(() -> prefix(prefix, offset, limit), client.connectionPool().executorService());
    }

    /**
     * Internal request DTO matching the server API.
     *
     * @param value   the value
     * @param ttl     the TTL duration
     * @param leaseId the associated lease ID
     */
    private record KeyValueRequest(String value, String ttl, String leaseId) {}
}
