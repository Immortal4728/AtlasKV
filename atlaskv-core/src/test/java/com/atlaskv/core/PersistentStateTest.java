package com.atlaskv.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistentStateTest {

    @Test
    @DisplayName("Initial persistent state starts at term 0 with null votedFor")
    void testInitialState() {
        PersistentState initial = PersistentState.initial();
        assertThat(initial.currentTerm()).isEqualTo(0L);
        assertThat(initial.votedFor()).isNull();
    }

    @Test
    @DisplayName("Negative current term throws IllegalArgumentException")
    void testNegativeTerm() {
        assertThatThrownBy(() -> new PersistentState(-1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current term must be non-negative");
    }

    @Test
    @DisplayName("withTerm resets votedFor to null and updates currentTerm")
    void testWithTerm() {
        PersistentState state = new PersistentState(1L, NodeId.of("node-1"));
        PersistentState newState = state.withTerm(2L);

        assertThat(newState.currentTerm()).isEqualTo(2L);
        assertThat(newState.votedFor()).isNull();
    }

    @Test
    @DisplayName("withVote updates candidateId while preserving currentTerm")
    void testWithVote() {
        PersistentState state = PersistentState.initial().withTerm(3L);
        NodeId candidate = NodeId.of("node-2");
        PersistentState votedState = state.withVote(candidate);

        assertThat(votedState.currentTerm()).isEqualTo(3L);
        assertThat(votedState.votedFor()).isEqualTo(candidate);
    }
}
