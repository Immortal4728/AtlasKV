package com.atlaskv.server.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves logical namespaces and performs bidirectional translation between
 * client-facing keys/leases and internal storage keys/leases.
 *
 * <p>Enforces strict multi-tenant isolation:</p>
 * <ul>
 *   <li><b>USER role:</b> Namespace is always derived server-side from {@link AuthenticatedPrincipal#userId()}.
 *       Client-provided headers/parameters are ignored.</li>
 *   <li><b>ADMIN role:</b> May explicitly specify a target namespace via {@code X-Namespace} header
 *       or {@code namespace} query parameter. Defaults to root/global keyspace if omitted.</li>
 *   <li><b>Local Dev / Auth Disabled:</b> Operates on root/global keyspace by default.</li>
 * </ul>
 */
public final class NamespaceResolver {

    /** Header used by ADMINs to target a specific namespace. */
    public static final String NAMESPACE_HEADER = "X-Namespace";

    /** Query parameter used by ADMINs to target a specific namespace. */
    public static final String NAMESPACE_PARAM = "namespace";

    /** Prefix applied to storage keys when namespacing is active. */
    public static final String NAMESPACE_PREFIX = "ns:";

    private NamespaceResolver() {
    }

    /**
     * Resolves the effective namespace for the current request.
     *
     * @param request the HTTP servlet request (may be null)
     * @return the effective namespace string, or empty string for root/global keyspace
     */
    public static String resolveNamespace(HttpServletRequest request) {
        AuthenticatedPrincipal principal = SecurityContext.getPrincipal(request)
                .or(SecurityContext::getPrincipal)
                .orElse(null);

        if (principal == null) {
            return extractAdminTargetNamespace(request);
        }

        // USER role: strictly derived from userId. Never trust client params.
        if (principal.isUser()) {
            return principal.userId();
        }

        // ADMIN role (including local-dev principal): allow targeting or default to root
        if (principal.isAdmin()) {
            return extractAdminTargetNamespace(request);
        }

        return "";
    }

    private static String extractAdminTargetNamespace(HttpServletRequest request) {
        if (request != null) {
            String headerNs = request.getHeader(NAMESPACE_HEADER);
            if (headerNs != null && !headerNs.isBlank()) {
                return headerNs.trim();
            }
            String paramNs = request.getParameter(NAMESPACE_PARAM);
            if (paramNs != null && !paramNs.isBlank()) {
                return paramNs.trim();
            }
        }
        return "";
    }

    /**
     * Converts a client-facing key to an internal storage key.
     *
     * @param clientKey the key provided by the client
     * @param namespace the effective namespace (empty for root)
     * @return the storage key
     */
    public static String toStorageKey(String clientKey, String namespace) {
        if (clientKey == null) {
            return null;
        }
        if (namespace == null || namespace.isBlank()) {
            return clientKey;
        }
        return NAMESPACE_PREFIX + namespace + ":" + clientKey;
    }

    /**
     * Converts an internal storage key back to the client-facing key.
     *
     * @param storageKey the key stored in the state machine
     * @param namespace  the effective namespace (empty for root)
     * @return the client-facing key
     */
    public static String toClientKey(String storageKey, String namespace) {
        if (storageKey == null) {
            return null;
        }
        if (namespace == null || namespace.isBlank()) {
            return storageKey;
        }
        String prefix = NAMESPACE_PREFIX + namespace + ":";
        if (storageKey.startsWith(prefix)) {
            return storageKey.substring(prefix.length());
        }
        return storageKey;
    }

    /**
     * Converts a client-facing prefix to an internal storage prefix.
     *
     * @param clientPrefix the prefix provided by the client
     * @param namespace    the effective namespace (empty for root)
     * @return the storage prefix
     */
    public static String toStoragePrefix(String clientPrefix, String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return clientPrefix != null ? clientPrefix : "";
        }
        String nsPrefix = NAMESPACE_PREFIX + namespace + ":";
        if (clientPrefix == null || clientPrefix.isBlank()) {
            return nsPrefix;
        }
        return nsPrefix + clientPrefix;
    }

    /**
     * Converts a client-facing lease ID to an internal storage lease ID.
     *
     * @param clientLeaseId the lease ID provided by the client
     * @param namespace     the effective namespace (empty for root)
     * @return the storage lease ID
     */
    public static String toStorageLeaseId(String clientLeaseId, String namespace) {
        if (clientLeaseId == null) {
            return null;
        }
        if (namespace == null || namespace.isBlank()) {
            return clientLeaseId;
        }
        return NAMESPACE_PREFIX + namespace + ":" + clientLeaseId;
    }

    /**
     * Converts an internal storage lease ID back to the client-facing lease ID.
     *
     * @param storageLeaseId the lease ID in the state machine
     * @param namespace      the effective namespace (empty for root)
     * @return the client-facing lease ID
     */
    public static String toClientLeaseId(String storageLeaseId, String namespace) {
        if (storageLeaseId == null) {
            return null;
        }
        if (namespace == null || namespace.isBlank()) {
            return storageLeaseId;
        }
        String prefix = NAMESPACE_PREFIX + namespace + ":";
        if (storageLeaseId.startsWith(prefix)) {
            return storageLeaseId.substring(prefix.length());
        }
        return storageLeaseId;
    }
}
