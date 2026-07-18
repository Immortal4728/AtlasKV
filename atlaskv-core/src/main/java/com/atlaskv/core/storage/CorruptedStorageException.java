package com.atlaskv.core.storage;

/**
 * Thrown when persistent storage corruption or invalid checksum is detected.
 */
public class CorruptedStorageException extends StorageException {

    public CorruptedStorageException(String message) {
        super(message);
    }

    public CorruptedStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
