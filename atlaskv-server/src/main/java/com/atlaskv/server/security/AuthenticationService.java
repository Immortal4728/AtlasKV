package com.atlaskv.server.security;

import java.util.Optional;

/**
 * Service responsible for authenticating API requests and managing
 * the identity store (users and API keys).
 */
public interface AuthenticationService {

    /**
     * Authenticates a request by matching the provided secret token against
     * registered API keys and resolving the associated user.
     *
     * @param secret the secret token extracted from the request header
     * @return the authenticated principal if credentials are valid, user is active,
     *         and the API key is not revoked; empty otherwise
     */
    Optional<AuthenticatedPrincipal> authenticate(String secret);

    /**
     * Registers a user in the identity store.
     *
     * @param user the user to register
     */
    void registerUser(User user);

    /**
     * Registers an API key in the identity store.
     *
     * @param apiKey the API key to register
     */
    void registerApiKey(ApiKey apiKey);

    /**
     * Returns a user by their unique identifier.
     *
     * @param userId the user ID
     * @return optional containing the user if found
     */
    Optional<User> getUser(String userId);

    /**
     * Returns an API key by its identifier.
     *
     * @param keyId the API key identifier
     * @return optional containing the API key if found
     */
    Optional<ApiKey> getApiKey(String keyId);

    /**
     * Revokes an API key, preventing further authentication with it.
     *
     * @param keyId the API key identifier to revoke
     */
    void revokeApiKey(String keyId);

    /**
     * Deactivates a user, preventing authentication with any of their API keys.
     *
     * @param userId the user ID to deactivate
     */
    void deactivateUser(String userId);

    /**
     * Returns whether any API keys have been configured in the identity store.
     *
     * @return true if at least one API key exists
     */
    boolean hasConfiguredKeys();
}
