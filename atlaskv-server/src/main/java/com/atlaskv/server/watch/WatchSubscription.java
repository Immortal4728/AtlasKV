package com.atlaskv.server.watch;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Represents a client watch subscription for a key or prefix.
 *
 * @param target   the target key or prefix being watched
 * @param isPrefix whether the target represents a prefix
 * @param emitter  the Server-Sent Events emitter associated with the watcher
 */
public record WatchSubscription(
        String target,
        boolean isPrefix,
        SseEmitter emitter
) {
    /**
     * Checks if a given key matches the target of this subscription.
     *
     * @param key the mutated key
     * @return true if matches
     */
    public boolean matches(String key) {
        if (key == null) {
            return false;
        }
        if (isPrefix) {
            return key.startsWith(target);
        }
        return key.equals(target);
    }
}
