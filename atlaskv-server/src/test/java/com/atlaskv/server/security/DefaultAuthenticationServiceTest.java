package com.atlaskv.server.security;

import com.atlaskv.server.config.AtlasKvProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAuthenticationServiceTest {

    private AtlasKvProperties properties;
    private DefaultAuthenticationService service;

    @BeforeEach
    void setUp() {
        properties = new AtlasKvProperties();
    }

    @Test
    void init_withAuthToken_createsAdminUserAndKey() {
        properties.getSecurity().setAuthToken("admin-secret-123");
        service = new DefaultAuthenticationService(properties);
        service.init();

        assertThat(service.hasConfiguredKeys()).isTrue();

        Optional<User> user = service.getUser("admin");
        assertThat(user).isPresent();
        assertThat(user.get().username()).isEqualTo("Administrator");
        assertThat(user.get().role()).isEqualTo(UserRole.ADMIN);
        assertThat(user.get().active()).isTrue();

        Optional<ApiKey> key = service.getApiKey("admin-api-key");
        assertThat(key).isPresent();
        assertThat(key.get().userId()).isEqualTo("admin");
        assertThat(key.get().active()).isTrue();
    }

    @Test
    void init_withCustomAdminUsername_usesCustomName() {
        properties.getSecurity().setAuthToken("admin-secret-123");
        properties.getSecurity().setAdminUsername("CustomAdmin");
        service = new DefaultAuthenticationService(properties);
        service.init();

        Optional<User> user = service.getUser("admin");
        assertThat(user).isPresent();
        assertThat(user.get().username()).isEqualTo("CustomAdmin");
    }

    @Test
    void init_withoutAuthToken_createsNoKeys() {
        service = new DefaultAuthenticationService(properties);
        service.init();

        assertThat(service.hasConfiguredKeys()).isFalse();
    }

    @Test
    void authenticate_validAdminToken_returnsAdminPrincipal() {
        properties.getSecurity().setAuthToken("admin-secret-123");
        service = new DefaultAuthenticationService(properties);
        service.init();

        Optional<AuthenticatedPrincipal> principal = service.authenticate("admin-secret-123");

        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo("admin");
        assertThat(principal.get().username()).isEqualTo("Administrator");
        assertThat(principal.get().role()).isEqualTo(UserRole.ADMIN);
        assertThat(principal.get().isAdmin()).isTrue();
    }

    @Test
    void authenticate_validUserToken_returnsUserPrincipal() {
        properties.getSecurity().setAuthToken("admin-secret");
        service = new DefaultAuthenticationService(properties);
        service.init();

        // Register a USER and their API key
        User regularUser = new User("user-1", "Alice", UserRole.USER, true);
        service.registerUser(regularUser);
        ApiKey userKey = new ApiKey("user-key-1", "user-secret-abc",
                "user-1", System.currentTimeMillis(), true);
        service.registerApiKey(userKey);

        Optional<AuthenticatedPrincipal> principal = service.authenticate("user-secret-abc");

        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo("user-1");
        assertThat(principal.get().username()).isEqualTo("Alice");
        assertThat(principal.get().role()).isEqualTo(UserRole.USER);
        assertThat(principal.get().isUser()).isTrue();
        assertThat(principal.get().isAdmin()).isFalse();
    }

    @Test
    void authenticate_invalidToken_returnsEmpty() {
        properties.getSecurity().setAuthToken("admin-secret-123");
        service = new DefaultAuthenticationService(properties);
        service.init();

        Optional<AuthenticatedPrincipal> principal = service.authenticate("wrong-token");

        assertThat(principal).isEmpty();
    }

    @Test
    void authenticate_nullToken_returnsEmpty() {
        properties.getSecurity().setAuthToken("admin-secret-123");
        service = new DefaultAuthenticationService(properties);
        service.init();

        assertThat(service.authenticate(null)).isEmpty();
    }

    @Test
    void authenticate_blankToken_returnsEmpty() {
        properties.getSecurity().setAuthToken("admin-secret-123");
        service = new DefaultAuthenticationService(properties);
        service.init();

        assertThat(service.authenticate("   ")).isEmpty();
    }

    @Test
    void authenticate_revokedApiKey_returnsEmpty() {
        properties.getSecurity().setAuthToken("admin-secret-123");
        service = new DefaultAuthenticationService(properties);
        service.init();

        service.revokeApiKey("admin-api-key");

        Optional<AuthenticatedPrincipal> principal = service.authenticate("admin-secret-123");
        assertThat(principal).isEmpty();
    }

    @Test
    void authenticate_inactiveUser_returnsEmpty() {
        properties.getSecurity().setAuthToken("admin-secret-123");
        service = new DefaultAuthenticationService(properties);
        service.init();

        service.deactivateUser("admin");

        Optional<AuthenticatedPrincipal> principal = service.authenticate("admin-secret-123");
        assertThat(principal).isEmpty();
    }

    @Test
    void revokeApiKey_marksKeyInactive() {
        properties.getSecurity().setAuthToken("admin-secret");
        service = new DefaultAuthenticationService(properties);
        service.init();

        service.revokeApiKey("admin-api-key");

        Optional<ApiKey> key = service.getApiKey("admin-api-key");
        assertThat(key).isPresent();
        assertThat(key.get().active()).isFalse();
    }

    @Test
    void deactivateUser_marksUserInactive() {
        properties.getSecurity().setAuthToken("admin-secret");
        service = new DefaultAuthenticationService(properties);
        service.init();

        service.deactivateUser("admin");

        Optional<User> user = service.getUser("admin");
        assertThat(user).isPresent();
        assertThat(user.get().active()).isFalse();
    }

    @Test
    void revokeApiKey_nonExistentKey_noError() {
        service = new DefaultAuthenticationService(properties);
        service.init();

        // Should not throw
        service.revokeApiKey("non-existent-key");
    }

    @Test
    void deactivateUser_nonExistentUser_noError() {
        service = new DefaultAuthenticationService(properties);
        service.init();

        // Should not throw
        service.deactivateUser("non-existent-user");
    }

    @Test
    void hasConfiguredKeys_afterRegistration_returnsTrue() {
        service = new DefaultAuthenticationService(properties);
        service.init();

        assertThat(service.hasConfiguredKeys()).isFalse();

        service.registerUser(new User("u1", "User1", UserRole.USER, true));
        service.registerApiKey(new ApiKey("k1", "secret", "u1", System.currentTimeMillis(), true));

        assertThat(service.hasConfiguredKeys()).isTrue();
    }
}
