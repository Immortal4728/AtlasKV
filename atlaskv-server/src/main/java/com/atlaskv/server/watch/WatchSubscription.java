package com.atlaskv.server.watch;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Represents a client watch subscription for a key or prefix.
 *
 * @param target    the target storage key or prefix being watched
 * @param isPrefix  whether the target represents a prefix
 * @param namespace the caller's namespace (empty string for root)
 * @param emitter   the Server-Sent Events emitter associated with the watcher
 */
public record WatchSubscription(
        String target,
        boolean isPrefix,
        String namespace,
        SseEmitter emitter
) {
    /**
     * Backward-compatible constructor without explicit namespace.
     *
     * @param target   the target storage key or prefix
     * @param isPrefix whether the target represents a prefix
     * @param emitter  the SSE emitter
     */
    public WatchSubscription(String target, boolean isPrefix, SseEmitter emitter) {
        this(target, isPrefix, "", emitter);
    }

    /**
     * Checks if a given storage key matches the target of this subscription.
     *
     * @param storageKey the mutated storage key
     * @return true if matches
     */
    public boolean matches(String storageKey) {
        if (storageKey == null) {
            return false;
        }
        if (isPrefix) {
            return storageKey.startsWith(target);
        }
        return storageKey.equals(target);
    }
}
