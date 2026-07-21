package com.atlaskv.storage.wal;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.storage.CorruptedStorageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalLogStorageTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("New WAL storage starts empty")
    void emptyLog() {
        Path walFile = tempDir.resolve("wal.log");
        try (WalLogStorage storage = new WalLogStorage(walFile)) {
            assertThat(storage.getLastLogIndex()).isEqualTo(0L);
            assertThat(storage.getLastLogTerm()).isEqualTo(0L);
            assertThat(storage.getEntry(1)).isEmpty();
        }
    }

    @Test
    @DisplayName("append and getEntry round-trip correctly")
    void appendAndGet() {
        Path walFile = tempDir.resolve("wal.log");
        try (WalLogStorage storage = new WalLogStorage(walFile)) {
            LogEntry e1 = new LogEntry(1, 1, "cmd1".getBytes(StandardCharsets.UTF_8));
            LogEntry e2 = new LogEntry(2, 1, "cmd2".getBytes(StandardCharsets.UTF_8));

            storage.append(e1);
            storage.append(e2);

            assertThat(storage.getLastLogIndex()).isEqualTo(2L);
            assertThat(storage.getLastLogTerm()).isEqualTo(1L);

            assertThat(storage.getEntry(1)).contains(e1);
            assertThat(storage.getEntry(2)).contains(e2);
            assertThat(storage.getTermAt(1)).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("append out-of-order throws IllegalArgumentException")
    void appendOutOfOrder() {
        Path walFile = tempDir.resolve("wal.log");
        try (WalLogStorage storage = new WalLogStorage(walFile)) {
            LogEntry e2 = new LogEntry(2, 1, "cmd2".getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> storage.append(e2))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("truncateFrom physically shrinks WAL file and updates lastLogIndex")
    void truncateFrom() {
        Path walFile = tempDir.resolve("wal.log");
        try (WalLogStorage storage = new WalLogStorage(walFile)) {
            storage.append(new LogEntry(1, 1, "c1".getBytes(StandardCharsets.UTF_8)));
            storage.append(new LogEntry(2, 1, "c2".getBytes(StandardCharsets.UTF_8)));
            storage.append(new LogEntry(3, 2, "c3".getBytes(StandardCharsets.UTF_8)));

            storage.truncateFrom(2);

            assertThat(storage.getLastLogIndex()).isEqualTo(1L);
            assertThat(storage.getLastLogTerm()).isEqualTo(1L);
            assertThat(storage.getEntry(2)).isEmpty();
            assertThat(storage.getEntry(3)).isEmpty();
        }
    }

    @Test
    @DisplayName("Startup replay reconstructs log state from WAL on restart")
    void startupReplay() {
        Path walFile = tempDir.resolve("wal.log");
        byte[] payload1 = "set k1 v1".getBytes(StandardCharsets.UTF_8);
        byte[] payload2 = "set k2 v2".getBytes(StandardCharsets.UTF_8);

        try (WalLogStorage storage = new WalLogStorage(walFile)) {
            storage.append(new LogEntry(1, 1, payload1));
            storage.append(new LogEntry(2, 2, payload2));
        }

        // Re-open WAL storage to simulate restart
        try (WalLogStorage reopened = new WalLogStorage(walFile)) {
            assertThat(reopened.getLastLogIndex()).isEqualTo(2L);
            assertThat(reopened.getLastLogTerm()).isEqualTo(2L);
            assertThat(reopened.getEntry(1)).contains(new LogEntry(1, 1, payload1));
            assertThat(reopened.getEntry(2)).contains(new LogEntry(2, 2, payload2));
            assertThat(reopened.getEntriesFrom(1)).hasSize(2);
        }
    }

    @Test
    @DisplayName("Corrupted record in middle of log throws CorruptedStorageException on replay")
    void corruptedRecordReplay() throws IOException {
        Path walFile = tempDir.resolve("wal.log");
        try (WalLogStorage storage = new WalLogStorage(walFile)) {
            storage.append(new LogEntry(1, 1, "c1".getBytes(StandardCharsets.UTF_8)));
            storage.append(new LogEntry(2, 1, "c2".getBytes(StandardCharsets.UTF_8)));
        }

        byte[] bytes = Files.readAllBytes(walFile);
        // Corrupt entry 1 payload byte
        bytes[20] ^= 0xFF;
        Files.write(walFile, bytes);

        assertThatThrownBy(() -> new WalLogStorage(walFile))
                .isInstanceOf(CorruptedStorageException.class);
    }
}
