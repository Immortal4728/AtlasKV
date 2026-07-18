package com.atlaskv.core.rpc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppendEntriesReplyTest {

    @Test
    @DisplayName("Valid AppendEntriesReply creation succeeds")
    void testValidReply() {
        AppendEntriesReply reply = new AppendEntriesReply(2L, true, 5L);

        assertThat(reply.term()).isEqualTo(2L);
        assertThat(reply.success()).isTrue();
        assertThat(reply.matchIndex()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Negative parameters throw IllegalArgumentException")
    void testNegativeParameters() {
        assertThatThrownBy(() -> new AppendEntriesReply(-1L, true, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Term must be non-negative");

        assertThatThrownBy(() -> new AppendEntriesReply(1L, true, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MatchIndex must be non-negative");
    }
}
