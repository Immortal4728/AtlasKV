package com.atlaskv.server.watch;

/**
 * Represents a committed key-value state mutation event.
 *
 * @param type    the event operation type (e.g. PUT, DELETE, EXPIRE)
 * @param key     the key of the mutated entry
 * @param value   the value of the entry, or null if deleted
 * @param version the version number of the entry, or null
 */
public record WatchEvent(
        String type,
        String key,
        String value,
        Long version
) {
    /**
     * Backward-compatible constructor without explicit version.
     *
     * @param type  the event operation type
     * @param key   the key of the mutated entry
     * @param value the value of the entry, or null if deleted
     */
    public WatchEvent(String type, String key, String value) {
        this(type, key, value, null);
    }
}
