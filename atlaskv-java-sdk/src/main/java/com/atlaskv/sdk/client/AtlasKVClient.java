package com.atlaskv.sdk.client;

import com.atlaskv.sdk.api.ClusterApi;
import com.atlaskv.sdk.api.HistoryApi;
import com.atlaskv.sdk.api.KeyValueApi;
import com.atlaskv.sdk.api.LeaseApi;
import com.atlaskv.sdk.api.WatchApi;
import com.atlaskv.sdk.connection.Authentication;
import com.atlaskv.sdk.connection.ConnectionPool;
import com.atlaskv.sdk.connection.RetryPolicy;
import com.atlaskv.sdk.exceptions.AtlasKVException;
import com.atlaskv.sdk.exceptions.ConflictException;
import com.atlaskv.sdk.exceptions.NotLeaderException;
import com.atlaskv.sdk.exceptions.TimeoutException;
import com.atlaskv.sdk.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main coordinator client for interacting with AtlasKV cluster nodes.
 */
public final class AtlasKVClient implements AutoCloseable {

    private final Duration timeout;
    private final RetryPolicy retryPolicy;
    private final Authentication authentication;
    private final ConnectionPool connectionPool;

    private final KeyValueApi keyValueApi;
    private final WatchApi watchApi;
    private final LeaseApi leaseApi;
    private final HistoryApi historyApi;
    private final ClusterApi clusterApi;

    private volatile URI activeBaseUri;

    /**
     * Constructs an AtlasKVClient instance.
     */
    AtlasKVClient(String host, int port, Duration timeout, RetryPolicy retryPolicy, Authentication authentication) {
        this.timeout = timeout;
        this.retryPolicy = retryPolicy;
        this.authentication = authentication;
        this.connectionPool = new ConnectionPool(timeout);
        this.activeBaseUri = URI.create("http://" + host + ":" + port);

        this.keyValueApi = new KeyValueApi(this);
        this.watchApi = new WatchApi(this);
        this.leaseApi = new LeaseApi(this);
        this.historyApi = new HistoryApi(this);
        this.clusterApi = new ClusterApi(this);
    }

    /**
     * Returns a builder to configure and construct an AtlasKVClient.
     *
     * @return client builder
     */
    public static AtlasKVClientBuilder builder() {
        return new AtlasKVClientBuilder();
    }

    public KeyValueApi keyValue() {
        return keyValueApi;
    }

    public WatchApi watch() {
        return watchApi;
    }

    public LeaseApi lease() {
        return leaseApi;
    }

    public HistoryApi history() {
        return historyApi;
    }

    public ClusterApi cluster() {
        return clusterApi;
    }

    public Duration timeout() {
        return timeout;
    }

    public URI activeBaseUri() {
        return activeBaseUri;
    }

    public ConnectionPool connectionPool() {
        return connectionPool;
    }

    /**
     * Resolves a relative path against the current active base URI.
     *
     * @param path the request path (must start with /)
     * @return absolute URI
     */
    public URI resolveUri(String path) {
        return activeBaseUri.resolve(path);
    }

    /**
     * Executes an HTTP request with automatic retry, backoff, and leader redirection.
     */
    public <T> T execute(HttpRequest.Builder requestBuilder, ResponseParser<T> parser) {
        int attempt = 0;
        long delay = retryPolicy.getInitialDelay().toMillis();
        HttpRequest originalRequest = requestBuilder.build();

        while (true) {
            URI targetUri = activeBaseUri.resolve(originalRequest.uri().getPath() 
                    + (originalRequest.uri().getRawQuery() != null ? "?" + originalRequest.uri().getRawQuery() : ""));
            
            HttpRequest.Builder builderWithUri = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .timeout(timeout);
            
            // Re-copy headers and method
            originalRequest.headers().map().forEach((name, values) -> {
                for (String val : values) {
                    builderWithUri.header(name, val);
                }
            });
            
            // Set method and body publisher
            HttpRequest.BodyPublisher bodyPublisher = originalRequest.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody());
            builderWithUri.method(originalRequest.method(), bodyPublisher);

            // Apply authentication
            authentication.apply(builderWithUri);

            HttpRequest request = builderWithUri.build();

            try {
                HttpResponse<String> response = connectionPool.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status >= 200 && status < 300 || status == 404) {
                    return parser.parse(response);
                }

                // Handle error status codes
                handleErrorStatus(status, response.body());

            } catch (NotLeaderException e) {
                // If leader redirect info is available, redirect
                if (e.getLeaderAddress() != null && attempt < retryPolicy.getMaxRetries()) {
                    this.activeBaseUri = URI.create("http://" + e.getLeaderAddress());
                    attempt++;
                    // Retry immediately on the new leader node
                    continue;
                }
                
                throw e;
            } catch (ConflictException e) {
                // CAS conflicts should fail immediately to let client application handle it
                throw e;
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new AtlasKVException("Request interrupted", e);
                }

                boolean isSafe = retryPolicy.isSafeToRetry(request.method());
                if (!isSafe || attempt >= retryPolicy.getMaxRetries()) {
                    if (e instanceof java.io.IOException) {
                        throw new TimeoutException("Request timed out or network error", e);
                    }
                    throw new AtlasKVException("Request execution failed: " + e.getMessage(), e);
                }
            }

            // Exponential backoff sleep
            attempt++;
            try {
                Thread.sleep(delay);
                delay = Math.min((long) (delay * retryPolicy.getMultiplier()), retryPolicy.getMaxDelay().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AtlasKVException("Retry backoff sleep interrupted", e);
            }
        }
    }

    /**
     * Executes an operation asynchronously via the connection pool's thread pool executor.
     */
    public <T> CompletableFuture<T> executeAsync(HttpRequest.Builder requestBuilder, ResponseParser<T> parser) {
        return CompletableFuture.supplyAsync(() -> execute(requestBuilder, parser), connectionPool.executorService());
    }

    private void handleErrorStatus(int status, String body) {
        if (status == 404) {
            // Let the caller handle 404 where appropriate, otherwise throw
            return;
        }

        if (status == 409) {
            try {
                Map<String, Object> details = JsonUtil.readValue(body, new TypeReference<>() {});
                Number expectedNum = (Number) details.get("expectedVersion");
                Number currentNum = (Number) details.get("currentVersion");
                long expected = expectedNum != null ? expectedNum.longValue() : -1;
                long current = currentNum != null ? currentNum.longValue() : -1;
                String msg = (String) details.getOrDefault("message", "Version mismatch");
                throw new ConflictException(msg, status, expected, current);
            } catch (com.atlaskv.sdk.exceptions.SerializationException | ClassCastException e) {
                throw new ConflictException("CAS Conflict occurred", status, -1, -1);
            }
        }

        if (status == 503) {
            try {
                Map<String, Object> details = JsonUtil.readValue(body, new TypeReference<>() {});
                String msg = (String) details.getOrDefault("detail", "Node is not running or not leader");
                String leaderId = (String) details.get("leaderId");
                String leaderAddress = (String) details.get("leaderAddress");
                throw new NotLeaderException(msg, status, leaderId, leaderAddress);
            } catch (com.atlaskv.sdk.exceptions.SerializationException | ClassCastException e) {
                throw new NotLeaderException("Node is not the cluster leader", status, null, null);
            }
        }

        // Generic error
        String detailMsg = body;
        try {
            Map<String, Object> details = JsonUtil.readValue(body, new TypeReference<>() {});
            detailMsg = (String) details.getOrDefault("detail", body);
            if (detailMsg == null) {
                detailMsg = body;
            }
        } catch (com.atlaskv.sdk.exceptions.SerializationException | ClassCastException ignored) {
        }
        throw new AtlasKVException("Server error (HTTP " + status + "): " + detailMsg, status, null);
    }

    @Override
    public void close() {
        connectionPool.shutdown();
    }

    /**
     * Functional interface for parsing HTTP responses.
     *
     * @param <T> the parsed response type
     */
    @FunctionalInterface
    public interface ResponseParser<T> {
        T parse(HttpResponse<String> response) throws Exception;
    }
}
