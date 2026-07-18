package com.atlaskv.core.rpc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestVoteReplyTest {

    @Test
    @DisplayName("Valid RequestVoteReply creation succeeds")
    void testValidReply() {
        RequestVoteReply replyGranted = new RequestVoteReply(3L, true);
        RequestVoteReply replyDenied = new RequestVoteReply(3L, false);

        assertThat(replyGranted.term()).isEqualTo(3L);
        assertThat(replyGranted.voteGranted()).isTrue();

        assertThat(replyDenied.term()).isEqualTo(3L);
        assertThat(replyDenied.voteGranted()).isFalse();
    }

    @Test
    @DisplayName("Negative term throws IllegalArgumentException")
    void testNegativeTerm() {
        assertThatThrownBy(() -> new RequestVoteReply(-1L, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Term must be non-negative");
    }
}
