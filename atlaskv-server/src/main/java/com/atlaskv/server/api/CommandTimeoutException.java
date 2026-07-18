package com.atlaskv.server.api;

/**
 * Thrown when a Raft command times out waiting for consensus.
 */
public final class CommandTimeoutException extends RuntimeException {

    /**
     * Constructs a CommandTimeoutException with the given message.
     *
     * @param message description of the timeout
     */
    public CommandTimeoutException(String message) {
        super(message);
    }
}
