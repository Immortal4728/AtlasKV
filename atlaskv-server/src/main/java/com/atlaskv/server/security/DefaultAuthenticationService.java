package com.atlaskv.server.security;

import com.atlaskv.server.config.AtlasKvProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link AuthenticationService}.
 * Uses constant-time comparison for secret validation to prevent timing attacks.
 *
 * <p>On startup, if an {@code AUTH_TOKEN} is configured in properties, this service
 * automatically registers a default Administrator user and API key.</p>
 */
@Service
public class DefaultAuthenticationService implements AuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthenticationService.class);

    private static final String DEFAULT_ADMIN_USER_ID = "admin";
    private static final String DEFAULT_ADMIN_KEY_ID = "admin-api-key";

    private final AtlasKvProperties properties;
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, ApiKey> apiKeys = new ConcurrentHashMap<>();

    /**
     * Constructs the authentication service.
     *
     * @param properties server configuration properties
     */
    public DefaultAuthenticationService(AtlasKvProperties properties) {
        this.properties = properties;
    }

    /**
     * Initializes the default administrator identity from configuration properties.
     * Called after dependency injection is complete.
     */
    @PostConstruct
    void init() {
        AtlasKvProperties.SecurityProperties sec = properties.getSecurity();
        if (sec == null) {
            return;
        }
        String token = sec.getAuthToken();
        if (token == null || token.isBlank()) {
            return;
        }

        String adminUsername = sec.getAdminUsername();
        if (adminUsername == null || adminUsername.isBlank()) {
            adminUsername = "Administrator";
        }

        User adminUser = new User(DEFAULT_ADMIN_USER_ID, adminUsername, UserRole.ADMIN, true);
        registerUser(adminUser);

        ApiKey adminKey = new ApiKey(
                DEFAULT_ADMIN_KEY_ID,
                token,
                DEFAULT_ADMIN_USER_ID,
                System.currentTimeMillis(),
                true
        );
        registerApiKey(adminKey);

        LOG.info("Initialized default administrator identity: userId={}, username={}",
                DEFAULT_ADMIN_USER_ID, adminUsername);
    }

    @Override
    public Optional<AuthenticatedPrincipal> authenticate(String secret) {
        if (secret == null || secret.isBlank()) {
            return Optional.empty();
        }

        byte[] providedBytes = secret.getBytes(StandardCharsets.UTF_8);

        for (ApiKey key : apiKeys.values()) {
            if (!key.active()) {
                continue;
            }
            byte[] expectedBytes = key.secret().getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(expectedBytes, providedBytes)) {
                return resolveUser(key.userId());
            }
        }

        return Optional.empty();
    }

    @Override
    public void registerUser(User user) {
        users.put(user.id(), user);
    }

    @Override
    public void registerApiKey(ApiKey apiKey) {
        apiKeys.put(apiKey.id(), apiKey);
    }

    @Override
    public Optional<User> getUser(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public Optional<ApiKey> getApiKey(String keyId) {
        return Optional.ofNullable(apiKeys.get(keyId));
    }

    @Override
    public void revokeApiKey(String keyId) {
        apiKeys.computeIfPresent(keyId, (id, existing) ->
                new ApiKey(existing.id(), existing.secret(), existing.userId(),
                        existing.createdAt(), false));
    }

    @Override
    public void deactivateUser(String userId) {
        users.computeIfPresent(userId, (id, existing) ->
                new User(existing.id(), existing.username(), existing.role(), false));
    }

    @Override
    public boolean hasConfiguredKeys() {
        return !apiKeys.isEmpty();
    }

    private Optional<AuthenticatedPrincipal> resolveUser(String userId) {
        User user = users.get(userId);
        if (user == null || !user.active()) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedPrincipal(user.id(), user.username(), user.role()));
    }
}
