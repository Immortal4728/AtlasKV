package com.atlaskv.sdk.api;

import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.KeyValue;
import com.atlaskv.sdk.models.Revision;
import com.atlaskv.sdk.util.JsonUtil;
import com.atlaskv.sdk.util.ValidationUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * History API client. Exposes version history, rollback, and revision lookup capabilities.
 */
public final class HistoryApi {

    private final AtlasKVClient client;

    public HistoryApi(AtlasKVClient client) {
        this.client = client;
    }

    /**
     * Retrieves the revision history for a key.
     *
     * @param key the key to query
     * @return list of revisions
     */
    public List<Revision> history(String key) {
        ValidationUtil.validateKey(key);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/kv/" + key + "/history"))
                .GET();

        return client.execute(builder, response -> {
            if (response.statusCode() == 404) {
                return List.of();
            }
            return JsonUtil.readValue(response.body(), new TypeReference<List<Revision>>() {});
        });
    }

    /**
     * Retrieves revision history asynchronously.
     */
    public CompletableFuture<List<Revision>> historyAsync(String key) {
        return CompletableFuture.supplyAsync(() -> history(key), client.connectionPool().executorService());
    }

    /**
     * Retrieves a specific revision for a key.
     *
     * @param key      the key to query
     * @param revision the target revision number
     * @return the revision metadata if found, empty otherwise
     */
    public Optional<Revision> revision(String key, long revision) {
        if (revision < 0) {
            throw new IllegalArgumentException("Revision number must be non-negative");
        }
        return history(key).stream()
                .filter(r -> r.revisionNumber() == revision)
                .findFirst();
    }

    /**
     * Retrieves specific revision asynchronously.
     */
    public CompletableFuture<Optional<Revision>> revisionAsync(String key, long revision) {
        return CompletableFuture.supplyAsync(() -> revision(key, revision), client.connectionPool().executorService());
    }

    /**
     * Rolls back a key to a specific revision.
     *
     * @param key      the key to rollback
     * @param revision the target revision number to rollback to
     * @return updated key-value details
     */
    public KeyValue rollback(String key, long revision) {
        ValidationUtil.validateKey(key);
        if (revision < 0) {
            throw new IllegalArgumentException("Revision number must be non-negative");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(client.resolveUri("/api/v1/kv/" + key + "/rollback/" + revision))
                .POST(HttpRequest.BodyPublishers.noBody());

        return client.execute(builder, response -> JsonUtil.readValue(response.body(), KeyValue.class));
    }

    /**
     * Performs rollback operation asynchronously.
     */
    public CompletableFuture<KeyValue> rollbackAsync(String key, long revision) {
        return CompletableFuture.supplyAsync(() -> rollback(key, revision), client.connectionPool().executorService());
    }
}
