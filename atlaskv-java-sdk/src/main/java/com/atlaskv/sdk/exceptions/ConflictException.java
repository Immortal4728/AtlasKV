package com.atlaskv.sdk.exceptions;

/**
 * Thrown when a Compare-And-Swap (CAS) write fails due to a version conflict.
 */
public final class ConflictException extends AtlasKVException {

    private final long expectedVersion;
    private final long currentVersion;

    /**
     * Constructs a ConflictException.
     *
     * @param message         error details
     * @param statusCode      HTTP status code
     * @param expectedVersion version expected by the client
     * @param currentVersion  actual version in the state machine
     */
    public ConflictException(String message, int statusCode, long expectedVersion, long currentVersion) {
        super(message, statusCode, null);
        this.expectedVersion = expectedVersion;
        this.currentVersion = currentVersion;
    }

    /**
     * Returns the version expected by the CAS operation.
     *
     * @return expected version
     */
    public long getExpectedVersion() {
        return expectedVersion;
    }

    /**
     * Returns the current version of the key at the server.
     *
     * @return current version
     */
    public long getCurrentVersion() {
        return currentVersion;
    }
}
