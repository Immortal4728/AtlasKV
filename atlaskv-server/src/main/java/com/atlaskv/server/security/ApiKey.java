package com.atlaskv.server.security;

/**
 * Represents an API key that authenticates requests against an associated user.
 *
 * @param id        key identifier (e.g. "admin-api-key")
 * @param secret    secret token used in HTTP headers (never logged or returned)
 * @param userId    associated user ID
 * @param createdAt creation epoch timestamp in milliseconds
 * @param active    whether the key is active (false = revoked)
 */
public record ApiKey(String id, String secret, String userId, long createdAt, boolean active) {
}
