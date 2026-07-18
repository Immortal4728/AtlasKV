package com.atlaskv.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Tracks in-flight client command futures indexed by the log index they were appended at.
 * When the leader commits and applies a log entry, it completes the corresponding future.
 */
public final class PendingCommands {

    private final Map<Long, CompletableFuture<byte[]>> pending = new HashMap<>();

    public void register(long logIndex, CompletableFuture<byte[]> future) {
        pending.put(logIndex, future);
    }

    public CompletableFuture<byte[]> remove(long logIndex) {
        return pending.remove(logIndex);
    }

    /**
     * Fails all pending commands. Called when leader steps down.
     *
     * @param reason exception to complete futures with
     */
    public void failAll(Exception reason) {
        for (CompletableFuture<byte[]> future : pending.values()) {
            future.completeExceptionally(reason);
        }
        pending.clear();
    }
}
