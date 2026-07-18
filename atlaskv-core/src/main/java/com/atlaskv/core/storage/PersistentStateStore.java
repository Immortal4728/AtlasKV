package com.atlaskv.core.storage;

import com.atlaskv.core.PersistentState;

/**
 * SPI for persisting and recovering volatile Raft metadata (currentTerm, votedFor).
 */
public interface PersistentStateStore extends AutoCloseable {

    /**
     * Atomically saves the persistent Raft state.
     *
     * @param state latest persistent state to write
     */
    void saveState(PersistentState state);

    /**
     * Loads the Raft persistent state from storage.
     *
     * @return recovered PersistentState, or PersistentState.initial() if non-existent
     */
    PersistentState loadState();

    @Override
    void close();
}
