package com.atlaskv.sdk.client;

import com.atlaskv.sdk.connection.Authentication;
import com.atlaskv.sdk.connection.RetryPolicy;
import com.atlaskv.sdk.util.ValidationUtil;

import java.time.Duration;

/**
 * Builder class for configuring and creating instances of {@link AtlasKVClient}.
 */
public final class AtlasKVClientBuilder {

    private String host = "localhost";
    private int port = 8080;
    private Duration timeout = Duration.ofSeconds(5);
    private RetryPolicy retryPolicy = RetryPolicy.defaultPolicy();
    private Authentication authentication = Authentication.none();

    /**
     * Package-private constructor to enforce builder usage via {@link AtlasKVClient#builder()}.
     */
    AtlasKVClientBuilder() {}

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
        return builder();
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
        return builder();
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
        return builder();
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
        return builder();
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
        return builder();
    }

    /**
     * Builds and returns a new {@link AtlasKVClient} instance configured with the current builder options.
     *
     * @return configured client instance
     */
    public AtlasKVClient build() {
        return new AtlasKVClient(host, port, timeout, retryPolicy, authentication);
    }

    private AtlasKVClientBuilder builder() {
        return this;
    }
}
