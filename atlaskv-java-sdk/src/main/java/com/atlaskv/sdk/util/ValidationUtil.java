package com.atlaskv.sdk.util;

import java.time.Duration;

/**
 * Utility class for validating inputs to the AtlasKV SDK APIs.
 */
public final class ValidationUtil {

    private ValidationUtil() {
        // Prevent instantiation
    }

    /**
     * Validates that a key is not null, empty, or blank.
     *
     * @param key the key to validate
     */
    public static void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key must not be null, empty, or blank");
        }
    }

    /**
     * Validates that a value is not null.
     *
     * @param value the value to validate
     */
    public static void validateValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
    }

    /**
     * Validates that a prefix is not null, empty, or blank.
     *
     * @param prefix the prefix to validate
     */
    public static void validatePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix must not be null, empty, or blank");
        }
    }

    /**
     * Validates that a timeout duration is not null, negative, or zero.
     *
     * @param timeout the timeout duration to validate
     */
    public static void validateTimeout(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be a positive non-zero duration");
        }
    }
}
