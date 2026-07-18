package com.atlaskv.core.event;

/**
 * Functional handler interface invoked sequentially by the {@link EventLoop} thread for each event.
 */
@FunctionalInterface
public interface RaftEventHandler {

    /**
     * Dispatches and processes a single Raft event.
     *
     * @param event event to handle
     */
    void handleEvent(RaftEvent event);
}
