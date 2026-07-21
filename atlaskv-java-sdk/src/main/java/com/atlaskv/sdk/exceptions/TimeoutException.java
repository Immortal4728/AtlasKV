package com.atlaskv.sdk.exceptions;

/**
 * Thrown when an AtlasKV operation or connection times out.
 */
public final class TimeoutException extends AtlasKVException {

    /**
     * Constructs a TimeoutException.
     *
     * @param message error details
     */
    public TimeoutException(String message) {
        super(message);
    }

    /**
     * Constructs a TimeoutException with message and cause.
     *
     * @param message error details
     * @param cause   underlying cause
     */
    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
