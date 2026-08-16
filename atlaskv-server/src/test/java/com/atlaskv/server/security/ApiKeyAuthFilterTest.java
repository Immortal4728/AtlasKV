package com.atlaskv.server.security;

import com.atlaskv.server.config.AtlasKvProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ApiKeyAuthFilterTest {

    private AtlasKvProperties properties;
    private ObjectMapper objectMapper;
    private DefaultAuthenticationService authService;
    private ApiKeyAuthFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        properties = new AtlasKvProperties();
        objectMapper = new ObjectMapper();
        authService = new DefaultAuthenticationService(properties);
        filterChain = mock(FilterChain.class);
    }

    private void initFilter() {
        authService.init();
        filter = new ApiKeyAuthFilter(properties, authService, objectMapper);
    }

    @Test
    void authDisabled_allowsRequestAndSetsLocalDevPrincipal() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(false);
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);

        // Verify local dev principal was set on the request
        Optional<AuthenticatedPrincipal> principal = SecurityContext.getPrincipal(request);
        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo("local-dev");
        assertThat(principal.get().username()).isEqualTo("Local Developer");
        assertThat(principal.get().role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void authEnabled_noCredentials_returns401() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized");
        assertThat(response.getContentAsString()).doesNotContain("secret-token-123");
    }

    @Test
    void authEnabled_invalidCredentials_returns403() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        request.addHeader("Authorization", "Bearer wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Forbidden");
        assertThat(response.getContentAsString()).doesNotContain("secret-token-123");
        assertThat(response.getContentAsString()).doesNotContain("wrong-token");
    }

    @Test
    void authEnabled_validBearerToken_allowsRequestWithAdminPrincipal() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        request.addHeader("Authorization", "Bearer secret-token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        Optional<AuthenticatedPrincipal> principal = SecurityContext.getPrincipal(request);
        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo("admin");
        assertThat(principal.get().role()).isEqualTo(UserRole.ADMIN);
        assertThat(principal.get().isAdmin()).isTrue();
    }

    @Test
    void authEnabled_validApiKeyToken_allowsRequest() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        request.addHeader("Authorization", "ApiKey secret-token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authEnabled_validXApiKeyHeader_allowsRequest() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        request.addHeader("X-API-Key", "secret-token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authEnabled_validQueryApiKey_allowsRequest() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/watch/my-key");
        request.setParameter("apiKey", "secret-token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authEnabled_validQueryToken_allowsRequest() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/watch/my-key");
        request.setParameter("token", "secret-token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authEnabled_healthEndpoint_allowsWithoutCredentials() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authEnabled_corsPreflight_allowsWithoutCredentials() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/kv/my-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authEnabled_unconfiguredServerKeys_returns500() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        request.addHeader("Authorization", "Bearer something");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("Configuration Error");
    }

    @Test
    void authEnabled_userApiKey_setsUserPrincipal() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("admin-secret");
        initFilter();

        // Register a USER
        authService.registerUser(new User("user-1", "Alice", UserRole.USER, true));
        authService.registerApiKey(new ApiKey("user-key-1", "user-secret-abc",
                "user-1", System.currentTimeMillis(), true));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        request.addHeader("Authorization", "Bearer user-secret-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        Optional<AuthenticatedPrincipal> principal = SecurityContext.getPrincipal(request);
        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo("user-1");
        assertThat(principal.get().username()).isEqualTo("Alice");
        assertThat(principal.get().role()).isEqualTo(UserRole.USER);
        assertThat(principal.get().isUser()).isTrue();
    }

    @Test
    void authEnabled_revokedApiKey_returns403() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("admin-secret");
        initFilter();

        authService.revokeApiKey("admin-api-key");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        request.addHeader("Authorization", "Bearer admin-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void authEnabled_inactiveUser_returns403() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("admin-secret");
        initFilter();

        authService.deactivateUser("admin");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        request.addHeader("Authorization", "Bearer admin-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void errorResponses_neverContainSecrets() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("my-super-secret-token");
        initFilter();

        // 401 - no token
        MockHttpServletRequest request401 = new MockHttpServletRequest("GET", "/api/v1/kv/key");
        MockHttpServletResponse response401 = new MockHttpServletResponse();
        filter.doFilter(request401, response401, filterChain);
        assertThat(response401.getContentAsString()).doesNotContain("my-super-secret-token");

        // 403 - wrong token
        MockHttpServletRequest request403 = new MockHttpServletRequest("GET", "/api/v1/kv/key");
        request403.addHeader("Authorization", "Bearer attacker-token");
        MockHttpServletResponse response403 = new MockHttpServletResponse();
        filter.doFilter(request403, response403, filterChain);
        assertThat(response403.getContentAsString()).doesNotContain("my-super-secret-token");
        assertThat(response403.getContentAsString()).doesNotContain("attacker-token");
    }

    @Test
    void securityContext_clearedAfterRequest() throws ServletException, IOException {
        properties.getSecurity().setAuthEnabled(true);
        properties.getSecurity().setAuthToken("secret-token-123");
        initFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/kv/my-key");
        request.addHeader("Authorization", "Bearer secret-token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // ThreadLocal should be cleared after the filter completes
        assertThat(SecurityContext.getPrincipal()).isEmpty();
    }
}
