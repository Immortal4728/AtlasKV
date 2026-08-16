package com.atlaskv.server.security;

/**
 * Represents a registered user in the AtlasKV identity system.
 *
 * @param id       unique user identifier
 * @param username display name
 * @param role     assigned role
 * @param active   whether the account is active
 */
public record User(String id, String username, UserRole role, boolean active) {
}
