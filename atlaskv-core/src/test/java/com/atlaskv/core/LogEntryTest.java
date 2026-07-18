package com.atlaskv.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogEntryTest {

    @Test
    @DisplayName("Valid LogEntry creation and immutable command array defensive copying")
    void testValidLogEntryAndImmutability() {
        byte[] payload = "SET key val".getBytes();
        LogEntry entry = new LogEntry(1L, 1L, payload);

        assertThat(entry.index()).isEqualTo(1L);
        assertThat(entry.term()).isEqualTo(1L);
        assertThat(entry.command()).isEqualTo(payload);

        // Mutating external array does not mutate record content
        payload[0] = 'X';
        assertThat(entry.command()).isNotEqualTo(payload);

        // Mutating return from accessor does not mutate record content
        byte[] returnedPayload = entry.command();
        returnedPayload[0] = 'Y';
        assertThat(entry.command()).isNotEqualTo(returnedPayload);
    }

    @Test
    @DisplayName("Invalid index (<= 0) throws IllegalArgumentException")
    void testInvalidIndex() {
        assertThatThrownBy(() -> new LogEntry(0L, 1L, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Log entry index must be positive");
    }

    @Test
    @DisplayName("Invalid term (<= 0) throws IllegalArgumentException")
    void testInvalidTerm() {
        assertThatThrownBy(() -> new LogEntry(1L, -1L, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Log entry term must be positive");
    }

    @Test
    @DisplayName("Null command array throws NullPointerException")
    void testNullCommand() {
        assertThatThrownBy(() -> new LogEntry(1L, 1L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Command byte array must not be null");
    }

    @Test
    @DisplayName("Deep equality check based on byte contents")
    void testDeepEquality() {
        LogEntry entry1 = new LogEntry(5L, 2L, new byte[]{1, 2, 3});
        LogEntry entry2 = new LogEntry(5L, 2L, new byte[]{1, 2, 3});
        LogEntry entry3 = new LogEntry(5L, 2L, new byte[]{1, 2, 4});

        assertThat(entry1).isEqualTo(entry2);
        assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode());
        assertThat(entry1).isNotEqualTo(entry3);
    }
}
