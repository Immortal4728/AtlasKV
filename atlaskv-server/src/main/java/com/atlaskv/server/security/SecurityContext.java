package com.atlaskv.server.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Provides access to the authenticated principal for the current request.
 * Stores the principal both as a servlet request attribute and in a thread-local
 * so downstream services can retrieve it without direct servlet dependency.
 */
public final class SecurityContext {

    /** Request attribute key used to store the authenticated principal. */
    public static final String PRINCIPAL_ATTRIBUTE = "atlaskv.security.principal";

    private static final ThreadLocal<AuthenticatedPrincipal> THREAD_LOCAL_PRINCIPAL = new ThreadLocal<>();

    private SecurityContext() {
    }

    /**
     * Sets the authenticated principal for the current request and thread.
     *
     * @param request   the current HTTP request
     * @param principal the authenticated principal
     */
    public static void setPrincipal(HttpServletRequest request, AuthenticatedPrincipal principal) {
        if (request != null) {
            request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
        }
        THREAD_LOCAL_PRINCIPAL.set(principal);
    }

    /**
     * Returns the authenticated principal from the servlet request attribute.
     *
     * @param request the current HTTP request
     * @return optional containing the principal if present
     */
    public static Optional<AuthenticatedPrincipal> getPrincipal(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Object attr = request.getAttribute(PRINCIPAL_ATTRIBUTE);
        if (attr instanceof AuthenticatedPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * Returns the authenticated principal from the thread-local context.
     * Useful for services that do not have direct access to the servlet request.
     *
     * @return optional containing the principal if present
     */
    public static Optional<AuthenticatedPrincipal> getPrincipal() {
        return Optional.ofNullable(THREAD_LOCAL_PRINCIPAL.get());
    }

    /**
     * Clears the thread-local principal. Should be called after request processing completes.
     */
    public static void clear() {
        THREAD_LOCAL_PRINCIPAL.remove();
    }
}
