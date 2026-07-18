package com.atlaskv.core.storage;

import com.atlaskv.core.LogEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLogStorageTest {

    private InMemoryLogStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryLogStorage();
    }

    @Test
    @DisplayName("Empty log returns zero for lastLogIndex and lastLogTerm")
    void emptyLogDefaults() {
        assertThat(storage.getLastLogIndex()).isEqualTo(0L);
        assertThat(storage.getLastLogTerm()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Append and retrieve single entry")
    void appendAndGet() {
        LogEntry entry = new LogEntry(1, 1, "cmd".getBytes(StandardCharsets.UTF_8));
        storage.append(entry);

        assertThat(storage.getLastLogIndex()).isEqualTo(1L);
        assertThat(storage.getLastLogTerm()).isEqualTo(1L);

        Optional<LogEntry> retrieved = storage.getEntry(1);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().index()).isEqualTo(1L);
    }

    @Test
    @DisplayName("appendAll adds multiple entries")
    void appendAll() {
        LogEntry e1 = new LogEntry(1, 1, "a".getBytes(StandardCharsets.UTF_8));
        LogEntry e2 = new LogEntry(2, 1, "b".getBytes(StandardCharsets.UTF_8));
        storage.appendAll(List.of(e1, e2));

        assertThat(storage.getLastLogIndex()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getEntry returns empty for out-of-range index")
    void getEntryOutOfRange() {
        assertThat(storage.getEntry(1)).isEmpty();
        assertThat(storage.getEntry(0)).isEmpty();
        assertThat(storage.getEntry(-1)).isEmpty();
    }

    @Test
    @DisplayName("getEntriesFrom returns entries starting at specified index")
    void getEntriesFrom() {
        storage.append(new LogEntry(1, 1, "a".getBytes(StandardCharsets.UTF_8)));
        storage.append(new LogEntry(2, 1, "b".getBytes(StandardCharsets.UTF_8)));
        storage.append(new LogEntry(3, 2, "c".getBytes(StandardCharsets.UTF_8)));

        List<LogEntry> from2 = storage.getEntriesFrom(2);
        assertThat(from2).hasSize(2);
        assertThat(from2.get(0).index()).isEqualTo(2L);
        assertThat(from2.get(1).index()).isEqualTo(3L);

        assertThat(storage.getEntriesFrom(10)).isEmpty();
    }

    @Test
    @DisplayName("truncateFrom removes entries from specified index onward")
    void truncateFrom() {
        storage.append(new LogEntry(1, 1, "a".getBytes(StandardCharsets.UTF_8)));
        storage.append(new LogEntry(2, 1, "b".getBytes(StandardCharsets.UTF_8)));
        storage.append(new LogEntry(3, 2, "c".getBytes(StandardCharsets.UTF_8)));

        storage.truncateFrom(2);
        assertThat(storage.getLastLogIndex()).isEqualTo(1L);
        assertThat(storage.getEntry(2)).isEmpty();
    }

    @Test
    @DisplayName("getTermAt returns correct term or 0 for out-of-range")
    void getTermAt() {
        storage.append(new LogEntry(1, 3, "x".getBytes(StandardCharsets.UTF_8)));
        storage.append(new LogEntry(2, 5, "y".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.getTermAt(0)).isEqualTo(0L);
        assertThat(storage.getTermAt(1)).isEqualTo(3L);
        assertThat(storage.getTermAt(2)).isEqualTo(5L);
        assertThat(storage.getTermAt(3)).isEqualTo(0L);
    }
}
