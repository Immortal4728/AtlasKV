package com.atlaskv.core.rpc;

import com.atlaskv.core.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestVoteArgsTest {

    @Test
    @DisplayName("Valid RequestVoteArgs creation succeeds")
    void testValidRequestVoteArgs() {
        NodeId candidate = NodeId.of("node-1");
        RequestVoteArgs args = new RequestVoteArgs(2L, candidate, 10L, 2L);

        assertThat(args.term()).isEqualTo(2L);
        assertThat(args.candidateId()).isEqualTo(candidate);
        assertThat(args.lastLogIndex()).isEqualTo(10L);
        assertThat(args.lastLogTerm()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Negative term throws IllegalArgumentException")
    void testNegativeTerm() {
        assertThatThrownBy(() -> new RequestVoteArgs(-1L, NodeId.of("node-1"), 0L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Term must be non-negative");
    }

    @Test
    @DisplayName("Null candidateId throws NullPointerException")
    void testNullCandidateId() {
        assertThatThrownBy(() -> new RequestVoteArgs(1L, null, 0L, 0L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CandidateId must not be null");
    }

    @Test
    @DisplayName("Negative lastLogIndex throws IllegalArgumentException")
    void testNegativeLastLogIndex() {
        assertThatThrownBy(() -> new RequestVoteArgs(1L, NodeId.of("node-1"), -1L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LastLogIndex must be non-negative");
    }

    @Test
    @DisplayName("Negative lastLogTerm throws IllegalArgumentException")
    void testNegativeLastLogTerm() {
        assertThatThrownBy(() -> new RequestVoteArgs(1L, NodeId.of("node-1"), 0L, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LastLogTerm must be non-negative");
    }
}
