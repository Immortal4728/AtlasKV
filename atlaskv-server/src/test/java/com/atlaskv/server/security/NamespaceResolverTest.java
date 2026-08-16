package com.atlaskv.server.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class NamespaceResolverTest {

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    @DisplayName("USER role always resolves to principal.userId(), ignoring client X-Namespace and params")
    void userRoleResolvesToUserIdIgnoringHeaders() {
        AuthenticatedPrincipal userPrincipal = new AuthenticatedPrincipal("user-alice", "Alice", UserRole.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Namespace", "user-bob");
        request.setParameter("namespace", "user-charlie");
        SecurityContext.setPrincipal(request, userPrincipal);

        String ns = NamespaceResolver.resolveNamespace(request);
        assertThat(ns).isEqualTo("user-alice");
    }

    @Test
    @DisplayName("ADMIN role with X-Namespace header resolves to specified target namespace")
    void adminWithHeaderResolvesToTargetNamespace() {
        AuthenticatedPrincipal adminPrincipal = new AuthenticatedPrincipal("admin-1", "Admin", UserRole.ADMIN);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Namespace", "user-alice");
        SecurityContext.setPrincipal(request, adminPrincipal);

        String ns = NamespaceResolver.resolveNamespace(request);
        assertThat(ns).isEqualTo("user-alice");
    }

    @Test
    @DisplayName("ADMIN role with namespace query param resolves to specified target namespace")
    void adminWithQueryParamResolvesToTargetNamespace() {
        AuthenticatedPrincipal adminPrincipal = new AuthenticatedPrincipal("admin-1", "Admin", UserRole.ADMIN);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("namespace", "user-bob");
        SecurityContext.setPrincipal(request, adminPrincipal);

        String ns = NamespaceResolver.resolveNamespace(request);
        assertThat(ns).isEqualTo("user-bob");
    }

    @Test
    @DisplayName("ADMIN role without namespace header/param resolves to root empty string")
    void adminWithoutHeaderResolvesToRoot() {
        AuthenticatedPrincipal adminPrincipal = new AuthenticatedPrincipal("admin-1", "Admin", UserRole.ADMIN);
        MockHttpServletRequest request = new MockHttpServletRequest();
        SecurityContext.setPrincipal(request, adminPrincipal);

        String ns = NamespaceResolver.resolveNamespace(request);
        assertThat(ns).isEqualTo("");
    }

    @Test
    @DisplayName("Local dev principal (ADMIN) defaults to root empty string")
    void localDevDefaultsToRoot() {
        AuthenticatedPrincipal localDev = new AuthenticatedPrincipal("local-dev", "Local Developer", UserRole.ADMIN);
        MockHttpServletRequest request = new MockHttpServletRequest();
        SecurityContext.setPrincipal(request, localDev);

        String ns = NamespaceResolver.resolveNamespace(request);
        assertThat(ns).isEqualTo("");
    }

    @Test
    @DisplayName("toStorageKey correctly prefixes key when namespace is non-empty")
    void toStorageKeyWithNamespace() {
        assertThat(NamespaceResolver.toStorageKey("my-key", "user-alice")).isEqualTo("ns:user-alice:my-key");
        assertThat(NamespaceResolver.toStorageKey("folder/nested-key", "user-bob")).isEqualTo("ns:user-bob:folder/nested-key");
        assertThat(NamespaceResolver.toStorageKey("my-key", "")).isEqualTo("my-key");
        assertThat(NamespaceResolver.toStorageKey("my-key", null)).isEqualTo("my-key");
    }

    @Test
    @DisplayName("toClientKey correctly strips prefix when matching namespace")
    void toClientKeyWithNamespace() {
        assertThat(NamespaceResolver.toClientKey("ns:user-alice:my-key", "user-alice")).isEqualTo("my-key");
        assertThat(NamespaceResolver.toClientKey("ns:user-alice:nested/key", "user-alice")).isEqualTo("nested/key");
        assertThat(NamespaceResolver.toClientKey("my-key", "")).isEqualTo("my-key");
        assertThat(NamespaceResolver.toClientKey("my-key", null)).isEqualTo("my-key");
        // Non-matching prefix returns as is
        assertThat(NamespaceResolver.toClientKey("ns:user-bob:other", "user-alice")).isEqualTo("ns:user-bob:other");
    }

    @Test
    @DisplayName("toStoragePrefix creates correct prefix queries")
    void toStoragePrefix() {
        assertThat(NamespaceResolver.toStoragePrefix("cfg/", "user-alice")).isEqualTo("ns:user-alice:cfg/");
        assertThat(NamespaceResolver.toStoragePrefix("", "user-alice")).isEqualTo("ns:user-alice:");
        assertThat(NamespaceResolver.toStoragePrefix(null, "user-alice")).isEqualTo("ns:user-alice:");
        assertThat(NamespaceResolver.toStoragePrefix("cfg/", "")).isEqualTo("cfg/");
        assertThat(NamespaceResolver.toStoragePrefix("cfg/", null)).isEqualTo("cfg/");
    }

    @Test
    @DisplayName("toStorageLeaseId and toClientLeaseId work symmetrically")
    void leaseIdTranslation() {
        assertThat(NamespaceResolver.toStorageLeaseId("lease-123", "user-alice")).isEqualTo("ns:user-alice:lease-123");
        assertThat(NamespaceResolver.toClientLeaseId("ns:user-alice:lease-123", "user-alice")).isEqualTo("lease-123");
        assertThat(NamespaceResolver.toStorageLeaseId("lease-123", "")).isEqualTo("lease-123");
        assertThat(NamespaceResolver.toClientLeaseId("lease-123", "")).isEqualTo("lease-123");
    }
}
