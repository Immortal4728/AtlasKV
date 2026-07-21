package com.atlaskv.server.watch;

/**
 * Represents a committed key-value state mutation event.
 *
 * @param type  the event operation type (e.g. PUT, DELETE, EXPIRE)
 * @param key   the key of the mutated entry
 * @param value the value of the entry, or null if deleted
 */
public record WatchEvent(
        String type,
        String key,
        String value
) {}
