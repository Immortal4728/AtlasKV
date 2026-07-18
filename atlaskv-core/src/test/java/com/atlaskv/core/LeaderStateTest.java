package com.atlaskv.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LeaderStateTest {

    private LeaderState leaderState;
    private NodeId peer1;
    private NodeId peer2;

    @BeforeEach
    void setUp() {
        peer1 = NodeId.of("peer-1");
        peer2 = NodeId.of("peer-2");
        leaderState = new LeaderState(Set.of(peer1, peer2), 5L);
    }

    @Test
    @DisplayName("nextIndex initialized to lastLogIndex + 1")
    void nextIndexInitialized() {
        assertThat(leaderState.getNextIndex(peer1)).isEqualTo(6L);
        assertThat(leaderState.getNextIndex(peer2)).isEqualTo(6L);
    }

    @Test
    @DisplayName("matchIndex initialized to 0")
    void matchIndexInitialized() {
        assertThat(leaderState.getMatchIndex(peer1)).isEqualTo(0L);
        assertThat(leaderState.getMatchIndex(peer2)).isEqualTo(0L);
    }

    @Test
    @DisplayName("setNextIndex and getNextIndex round-trip")
    void setAndGetNextIndex() {
        leaderState.setNextIndex(peer1, 3L);
        assertThat(leaderState.getNextIndex(peer1)).isEqualTo(3L);
    }

    @Test
    @DisplayName("decrementNextIndex does not go below 1")
    void decrementNextIndex() {
        leaderState.setNextIndex(peer1, 2L);
        leaderState.decrementNextIndex(peer1);
        assertThat(leaderState.getNextIndex(peer1)).isEqualTo(1L);

        leaderState.decrementNextIndex(peer1);
        assertThat(leaderState.getNextIndex(peer1)).isEqualTo(1L);
    }

    @Test
    @DisplayName("setMatchIndex and getMatchIndex round-trip")
    void setAndGetMatchIndex() {
        leaderState.setMatchIndex(peer1, 10L);
        assertThat(leaderState.getMatchIndex(peer1)).isEqualTo(10L);
    }
}
