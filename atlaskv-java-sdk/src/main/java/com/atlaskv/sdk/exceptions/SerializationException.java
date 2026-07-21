package com.atlaskv.sdk.exceptions;

/**
 * Thrown when JSON serialization or deserialization fails.
 */
public final class SerializationException extends AtlasKVException {

    /**
     * Constructs a SerializationException.
     *
     * @param message error details
     * @param cause   underlying cause
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
