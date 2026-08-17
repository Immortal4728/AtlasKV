package com.atlaskv.server.statemachine;

/**
 * Represents the lifecycle status of a distributed lease.
 */
public enum LeaseStatus {
    /**
     * The lease is currently active and can have keys attached or be renewed.
     */
    ACTIVE,

    /**
     * The lease expired automatically due to TTL timeout.
     */
    EXPIRED,

    /**
     * The lease was explicitly revoked by user or administrative action.
     */
    REVOKED
}
