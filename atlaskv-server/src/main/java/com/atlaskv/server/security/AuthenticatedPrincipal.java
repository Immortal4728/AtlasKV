package com.atlaskv.server.security;

/**
 * Represents the verified identity of an authenticated request.
 * Available to downstream controllers and services after successful authentication.
 *
 * @param userId   unique user identifier
 * @param username display name
 * @param role     assigned role
 */
public record AuthenticatedPrincipal(String userId, String username, UserRole role) {

    /**
     * Returns true if the principal has the ADMIN role.
     *
     * @return true if admin
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Returns true if the principal has the USER role.
     *
     * @return true if user
     */
    public boolean isUser() {
        return role == UserRole.USER;
    }
}
