package com.atlaskv.server.api.dto;

/**
 * Response DTO describing the authenticated identity and effective namespace of the current request.
 *
 * @param authenticated true if authenticated with an API key, false if running in local dev mode
 * @param userId        unique user identifier
 * @param username      display name of the user
 * @param role          assigned role (ADMIN or USER)
 * @param namespace     enforced namespace for USERs, or empty string for ADMIN root keyspace
 */
public record AuthInfoResponse(
        boolean authenticated,
        String userId,
        String username,
        String role,
        String namespace
) {
}
