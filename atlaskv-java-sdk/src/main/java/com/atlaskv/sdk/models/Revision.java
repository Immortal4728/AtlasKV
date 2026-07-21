package com.atlaskv.sdk.models;

/**
 * Immutable representation of a single historical key revision.
 *
 * @param revisionNumber version revision number
 * @param value          value of the key at this revision (null if deleted/expired)
 * @param timestamp      epoch timestamp of this revision
 * @param operation      operation type (PUT, DELETE, EXPIRE, ROLLBACK)
 * @param nodeId         ID of the node that committed the revision
 * @param leaseId        associated lease ID (null if none)
 * @param ttl            associated TTL string (null if none)
 */
public record Revision(
        long revisionNumber,
        String value,
        long timestamp,
        String operation,
        String nodeId,
        String leaseId,
        String ttl
) {}
