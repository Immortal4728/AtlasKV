package com.atlaskv.server.security;

/**
 * Roles that can be assigned to AtlasKV users.
 */
public enum UserRole {

    /** Standard user with access to authenticated API operations. */
    USER,

    /** Administrator with full system access. */
    ADMIN
}
