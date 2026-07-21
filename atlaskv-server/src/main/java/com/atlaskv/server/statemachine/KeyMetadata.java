package com.atlaskv.server.statemachine;

import java.io.Serializable;

/**
 * Metadata associated with each key.
 */
public final class KeyMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long version;
    private final long createdAt;
    private final long updatedAt;

    /**
     * Constructs a KeyMetadata instance.
     *
     * @param version   version number
     * @param createdAt created timestamp
     * @param updatedAt updated timestamp
     */
    public KeyMetadata(long version, long createdAt, long updatedAt) {
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long version() {
        return version;
    }

    public long createdAt() {
        return createdAt;
    }

    public long updatedAt() {
        return updatedAt;
    }
}
