package com.atlaskv.sdk.exceptions;

/**
 * Base exception for all errors encountered while using the AtlasKV SDK.
 */
public class AtlasKVException extends RuntimeException {
    
    private final int statusCode;

    /**
     * Constructs a new AtlasKVException with a message.
     *
     * @param message error message
     */
    public AtlasKVException(String message) {
        this(message, -1, null);
    }

    /**
     * Constructs a new AtlasKVException with a message and cause.
     *
     * @param message error message
     * @param cause   cause of the error
     */
    public AtlasKVException(String message, Throwable cause) {
        this(message, -1, cause);
    }

    /**
     * Constructs a new AtlasKVException with status code, message, and cause.
     *
     * @param message    error message
     * @param statusCode HTTP status code (or -1 if not applicable)
     * @param cause      cause of the error
     */
    public AtlasKVException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code associated with this error, or -1 if none.
     *
     * @return HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
}
