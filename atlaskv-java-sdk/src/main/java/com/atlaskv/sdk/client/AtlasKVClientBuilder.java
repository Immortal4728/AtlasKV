package com.atlaskv.sdk.client;

import com.atlaskv.sdk.connection.Authentication;
import com.atlaskv.sdk.connection.RetryPolicy;
import com.atlaskv.sdk.util.ValidationUtil;

import java.net.URI;
import java.time.Duration;

/**
 * Builder class for configuring and creating instances of {@link AtlasKVClient}.
 */
public final class AtlasKVClientBuilder {

    private String host = "localhost";
    private int port = 8080;
    private URI baseUri = null;
    private Duration timeout = Duration.ofSeconds(5);
    private RetryPolicy retryPolicy = RetryPolicy.defaultPolicy();
    private Authentication authentication = Authentication.none();

    /**
     * Package-private constructor to enforce builder usage via {@link AtlasKVClient#builder()}.
     */
    AtlasKVClientBuilder() {}

    /**
     * Sets the remote endpoint URI of the AtlasKV server (e.g. "https://atlaskv.example.com" or "http://localhost:8081").
     *
     * @param endpoint endpoint URL
     * @return builder instance
     */
    public AtlasKVClientBuilder endpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Endpoint must not be null or blank");
        }
        String normalized = endpoint.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        this.baseUri = URI.create(normalized);
        return this;
    }

    /**
     * Sets the API key for authenticating requests against the AtlasKV server.
     *
     * @param apiKey API key secret
     * @return builder instance
     */
    public AtlasKVClientBuilder apiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be null or blank");
        }
        this.authentication = Authentication.bearer(apiKey);
        return this;
    }

    /**
     * Sets the host of the AtlasKV server.
     *
     * @param host server host
     * @return builder instance
     */
    public AtlasKVClientBuilder host(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host must not be null or blank");
        }
        this.host = host;
        this.baseUri = null;
        return this;
    }

    /**
     * Sets the port of the AtlasKV server.
     *
     * @param port server port
     * @return builder instance
     */
    public AtlasKVClientBuilder port(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.port = port;
        this.baseUri = null;
        return this;
    }

    /**
     * Sets the request timeout duration.
     *
     * @param timeout request timeout
     * @return builder instance
     */
    public AtlasKVClientBuilder timeout(Duration timeout) {
        ValidationUtil.validateTimeout(timeout);
        this.timeout = timeout;
        return this;
    }

    /**
     * Sets the retry policy.
     *
     * @param retryPolicy retry policy configuration
     * @return builder instance
     */
    public AtlasKVClientBuilder retryPolicy(RetryPolicy retryPolicy) {
        if (retryPolicy == null) {
            throw new IllegalArgumentException("RetryPolicy must not be null");
        }
        this.retryPolicy = retryPolicy;
        return this;
    }

    /**
     * Sets the authentication provider.
     *
     * @param authentication authentication details
     * @return builder instance
     */
    public AtlasKVClientBuilder authentication(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication must not be null");
        }
        this.authentication = authentication;
        return this;
    }

    /**
     * Builds and returns a new {@link AtlasKVClient} instance configured with the current builder options.
     *
     * @return configured client instance
     */
    public AtlasKVClient build() {
        URI effectiveUri = baseUri != null ? baseUri : URI.create("http://" + host + ":" + port);
        return new AtlasKVClient(effectiveUri, timeout, retryPolicy, authentication);
    }
}
