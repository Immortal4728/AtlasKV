package com.atlaskv.core.rpc;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppendEntriesArgsTest {

    @Test
    @DisplayName("Valid AppendEntriesArgs creation with entry immutability")
    void testValidAppendEntriesArgs() {
        NodeId leader = NodeId.of("leader-1");
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(1L, 1L, "SET k v".getBytes()));

        AppendEntriesArgs args = new AppendEntriesArgs(1L, leader, 0L, 0L, entries, 0L);

        assertThat(args.term()).isEqualTo(1L);
        assertThat(args.leaderId()).isEqualTo(leader);
        assertThat(args.entries()).hasSize(1);

        // Mutating source list does not affect internal list
        entries.clear();
        assertThat(args.entries()).hasSize(1);
    }

    @Test
    @DisplayName("Null leaderId or entries throws NullPointerException")
    void testNullChecks() {
        assertThatThrownBy(() -> new AppendEntriesArgs(1L, null, 0L, 0L, List.of(), 0L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("LeaderId must not be null");

        assertThatThrownBy(() -> new AppendEntriesArgs(1L, NodeId.of("l1"), 0L, 0L, null, 0L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entries list must not be null");
    }

    @Test
    @DisplayName("Negative bounds throw IllegalArgumentException")
    void testNegativeBounds() {
        NodeId leader = NodeId.of("l1");
        assertThatThrownBy(() -> new AppendEntriesArgs(-1L, leader, 0L, 0L, List.of(), 0L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AppendEntriesArgs(1L, leader, -1L, 0L, List.of(), 0L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AppendEntriesArgs(1L, leader, 0L, -1L, List.of(), 0L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AppendEntriesArgs(1L, leader, 0L, 0L, List.of(), -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
