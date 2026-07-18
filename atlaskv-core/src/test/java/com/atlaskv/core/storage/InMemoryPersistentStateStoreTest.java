package com.atlaskv.core.storage;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.PersistentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryPersistentStateStoreTest {

    private InMemoryPersistentStateStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryPersistentStateStore();
    }

    @Test
    @DisplayName("Initial load returns default PersistentState with term 0 and null vote")
    void initialLoad() {
        PersistentState state = store.loadState();
        assertThat(state.currentTerm()).isEqualTo(0L);
        assertThat(state.votedFor()).isNull();
    }

    @Test
    @DisplayName("saveState updates and loadState retrieves modified state")
    void saveAndLoad() {
        PersistentState newState = new PersistentState(5L, NodeId.of("node-2"));
        store.saveState(newState);

        PersistentState loaded = store.loadState();
        assertThat(loaded.currentTerm()).isEqualTo(5L);
        assertThat(loaded.votedFor()).isEqualTo(NodeId.of("node-2"));
    }

    @Test
    @DisplayName("saveState throws NullPointerException when given null state")
    void saveNullThrows() {
        assertThatThrownBy(() -> store.saveState(null))
                .isInstanceOf(NullPointerException.class);
    }
}
