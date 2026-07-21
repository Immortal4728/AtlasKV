package com.atlaskv.sdk.connection;

import java.net.http.HttpRequest;
import java.util.Base64;

/**
 * Interface representing request authentication.
 * Allows custom interceptors to inject headers/tokens into outgoing HTTP requests.
 */
public interface Authentication {

    /**
     * Applies the authentication credentials to the outgoing request builder.
     *
     * @param builder the HTTP request builder
     */
    void apply(HttpRequest.Builder builder);

    /**
     * Creates a no-op authentication instance.
     *
     * @return no-op authentication
     */
    static Authentication none() {
        return builder -> { };
    }

    /**
     * Creates a Bearer token authentication instance.
     *
     * @param token bearer token
     * @return bearer token authentication
     */
    static Authentication bearer(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or blank");
        }
        return builder -> builder.header("Authorization", "Bearer " + token);
    }

    /**
     * Creates a Basic HTTP authentication instance.
     *
     * @param username username
     * @param password password
     * @return basic auth authentication
     */
    static Authentication basic(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password must not be null");
        }
        String encoded = Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return builder -> builder.header("Authorization", "Basic " + encoded);
    }
}
