package com.atlaskv.server.config;

/**
 * Thrown when cluster configuration fails validation.
 */
public final class ConfigValidationException extends RuntimeException {

    /**
     * Constructs a ConfigValidationException with the given message.
     *
     * @param message validation failure description
     */
    public ConfigValidationException(String message) {
        super(message);
    }
}
