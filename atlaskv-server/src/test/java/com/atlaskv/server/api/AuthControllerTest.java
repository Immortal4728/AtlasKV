package com.atlaskv.server.api;

import com.atlaskv.server.api.dto.AuthInfoResponse;
import com.atlaskv.server.config.AtlasKvProperties;
import com.atlaskv.server.security.AuthenticatedPrincipal;
import com.atlaskv.server.security.SecurityContext;
import com.atlaskv.server.security.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {

    private AtlasKvProperties properties;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        properties = new AtlasKvProperties();
        controller = new AuthController(properties);
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    void getAuthInfo_authDisabled_returnsDevModeAdmin() {
        properties.getSecurity().setAuthEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest();

        // When auth is disabled, filter sets local dev principal
        AuthenticatedPrincipal devPrincipal = new AuthenticatedPrincipal("local-dev", "Local Developer", UserRole.ADMIN);
        SecurityContext.setPrincipal(request, devPrincipal);

        ResponseEntity<AuthInfoResponse> response = controller.getAuthInfo(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        AuthInfoResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.authenticated()).isFalse();
        assertThat(body.userId()).isEqualTo("local-dev");
        assertThat(body.role()).isEqualTo("ADMIN");
        assertThat(body.namespace()).isEmpty();
    }

    @Test
    void getAuthInfo_userPrincipal_returnsScopedUserNamespace() {
        properties.getSecurity().setAuthEnabled(true);
        MockHttpServletRequest request = new MockHttpServletRequest();

        AuthenticatedPrincipal userPrincipal = new AuthenticatedPrincipal("user-alice", "Alice", UserRole.USER);
        SecurityContext.setPrincipal(request, userPrincipal);

        ResponseEntity<AuthInfoResponse> response = controller.getAuthInfo(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        AuthInfoResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.authenticated()).isTrue();
        assertThat(body.userId()).isEqualTo("user-alice");
        assertThat(body.username()).isEqualTo("Alice");
        assertThat(body.role()).isEqualTo("USER");
        assertThat(body.namespace()).isEqualTo("user-alice");
    }

    @Test
    void getAuthInfo_adminPrincipal_defaultGlobalNamespace() {
        properties.getSecurity().setAuthEnabled(true);
        MockHttpServletRequest request = new MockHttpServletRequest();

        AuthenticatedPrincipal adminPrincipal = new AuthenticatedPrincipal("admin", "Administrator", UserRole.ADMIN);
        SecurityContext.setPrincipal(request, adminPrincipal);

        ResponseEntity<AuthInfoResponse> response = controller.getAuthInfo(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        AuthInfoResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.authenticated()).isTrue();
        assertThat(body.userId()).isEqualTo("admin");
        assertThat(body.role()).isEqualTo("ADMIN");
        assertThat(body.namespace()).isEmpty();
    }

    @Test
    void getAuthInfo_adminPrincipal_withTargetNamespaceHeader() {
        properties.getSecurity().setAuthEnabled(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Namespace", "user-bob");

        AuthenticatedPrincipal adminPrincipal = new AuthenticatedPrincipal("admin", "Administrator", UserRole.ADMIN);
        SecurityContext.setPrincipal(request, adminPrincipal);

        ResponseEntity<AuthInfoResponse> response = controller.getAuthInfo(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        AuthInfoResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.authenticated()).isTrue();
        assertThat(body.userId()).isEqualTo("admin");
        assertThat(body.role()).isEqualTo("ADMIN");
        assertThat(body.namespace()).isEqualTo("user-bob");
    }
}
