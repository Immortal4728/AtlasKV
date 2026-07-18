package com.atlaskv.core.storage;

import com.atlaskv.core.PersistentState;
import java.util.Objects;

/**
 * In-memory implementation of {@link PersistentStateStore} for testing.
 */
public final class InMemoryPersistentStateStore implements PersistentStateStore {

    private volatile PersistentState state = PersistentState.initial();

    @Override
    public void saveState(PersistentState state) {
        this.state = Objects.requireNonNull(state, "State must not be null");
    }

    @Override
    public PersistentState loadState() {
        return state;
    }

    @Override
    public void close() {
        // No-op
    }
}
