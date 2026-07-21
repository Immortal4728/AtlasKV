package com.atlaskv.sdk.api;

import com.atlaskv.sdk.models.WatchEvent;

/**
 * Callback listener interface for receiving real-time mutation events from AtlasKV watch streams.
 */
public interface WatchListener {

    /**
     * Invoked when a state mutation event (PUT, DELETE) is received.
     *
     * @param event mutation details
     */
    void onEvent(WatchEvent event);

    /**
     * Invoked when an error is encountered in the watch stream.
     *
     * @param throwable error cause
     */
    void onError(Throwable throwable);

    /**
     * Invoked when the connection to the watch SSE endpoint is successfully opened.
     */
    default void onConnected() {}

    /**
     * Invoked when the watch stream disconnects.
     */
    default void onDisconnected() {}
}
