package com.atlaskv.sdk.models;

/**
 * Represents a key-value change event pushed over a watch stream.
 *
 * @param type  operation type (e.g. PUT, DELETE, EXPIRE)
 * @param key   mutated key
 * @param value value of the key (null if deleted or expired)
 */
public record WatchEvent(
        String type,
        String key,
        String value
) {}
