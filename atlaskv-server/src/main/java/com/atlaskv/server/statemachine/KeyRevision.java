package com.atlaskv.server.statemachine;

import java.io.Serializable;

/**
 * Represents an immutable historical revision of a key-value pair.
 */
public final class KeyRevision implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long revisionNumber;
    private final String value;
    private final long timestamp;
    private final String operation;
    private final String nodeId;
    private final String leaseId;
    private final String ttl;

    /**
     * Constructs a new KeyRevision.
     */
    public KeyRevision(long revisionNumber, String value, long timestamp, String operation,
                       String nodeId, String leaseId, String ttl) {
        this.revisionNumber = revisionNumber;
        this.value = value;
        this.timestamp = timestamp;
        this.operation = operation;
        this.nodeId = nodeId;
        this.leaseId = leaseId;
        this.ttl = ttl;
    }

    public long revisionNumber() {
        return revisionNumber;
    }

    public String value() {
        return value;
    }

    public long timestamp() {
        return timestamp;
    }

    public String operation() {
        return operation;
    }

    public String nodeId() {
        return nodeId;
    }

    public String leaseId() {
        return leaseId;
    }

    public String ttl() {
        return ttl;
    }
}
