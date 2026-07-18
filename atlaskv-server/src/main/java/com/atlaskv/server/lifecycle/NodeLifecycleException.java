package com.atlaskv.server.lifecycle;

/**
 * Thrown when a node lifecycle operation fails fatally.
 */
public final class NodeLifecycleException extends RuntimeException {

    /**
     * Constructs a NodeLifecycleException with the given message and cause.
     *
     * @param message description of the failure
     * @param cause underlying cause
     */
    public NodeLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a NodeLifecycleException with the given message.
     *
     * @param message description of the failure
     */
    public NodeLifecycleException(String message) {
        super(message);
    }
}
