package com.atlaskv.storage;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.NodeId;
import com.atlaskv.core.PersistentState;
import com.atlaskv.storage.metadata.FilePersistentStateStore;
import com.atlaskv.storage.wal.WalLogStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;

class CrashRecoveryIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Trailing partial write in WAL caused by power loss/crash is truncated cleanly on restart")
    void partialWriteCrashRecovery() throws IOException {
        Path walFile = tempDir.resolve("wal.log");

        try (WalLogStorage storage = new WalLogStorage(walFile)) {
            storage.append(new LogEntry(1, 1, "cmd1".getBytes(StandardCharsets.UTF_8)));
            storage.append(new LogEntry(2, 1, "cmd2".getBytes(StandardCharsets.UTF_8)));
        }

        // Simulate crash during partial write: append incomplete entry header bytes to WAL file
        byte[] partialBytes = new byte[]{0x57, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00};
        Files.write(walFile, partialBytes, StandardOpenOption.APPEND);

        // Reopen WAL storage — crash recovery should truncate partial bytes and restore valid state
        try (WalLogStorage recovered = new WalLogStorage(walFile)) {
            assertThat(recovered.getLastLogIndex()).isEqualTo(2L);
            assertThat(recovered.getLastLogTerm()).isEqualTo(1L);
            assertThat(recovered.getEntry(1)).isPresent();
            assertThat(recovered.getEntry(2)).isPresent();
            assertThat(recovered.getEntry(3)).isEmpty();

            // Should be able to append new entries cleanly after recovery
            recovered.append(new LogEntry(3, 2, "cmd3".getBytes(StandardCharsets.UTF_8)));
            assertThat(recovered.getLastLogIndex()).isEqualTo(3L);
        }
    }

    @Test
    @DisplayName("Combined persistent state store and WAL log storage survive crash and restart")
    void persistentStateAndWalCrashRecovery() {
        Path metaFile = tempDir.resolve("metadata.dat");
        Path walFile = tempDir.resolve("wal.log");

        // 1. Initial run before crash
        try (FilePersistentStateStore stateStore = new FilePersistentStateStore(metaFile);
             WalLogStorage logStorage = new WalLogStorage(walFile)) {
            stateStore.saveState(new PersistentState(10L, NodeId.of("node-3")));
            logStorage.append(new LogEntry(1, 10, "put foo bar".getBytes(StandardCharsets.UTF_8)));
        }

        // 2. Simulated crash & restart: create fresh store instances pointing to same files
        try (FilePersistentStateStore stateStore = new FilePersistentStateStore(metaFile);
             WalLogStorage logStorage = new WalLogStorage(walFile)) {
            PersistentState state = stateStore.loadState();
            assertThat(state.currentTerm()).isEqualTo(10L);
            assertThat(state.votedFor()).isEqualTo(NodeId.of("node-3"));

            assertThat(logStorage.getLastLogIndex()).isEqualTo(1L);
            assertThat(logStorage.getLastLogTerm()).isEqualTo(10L);
            assertThat(logStorage.getEntry(1).orElseThrow().command())
                    .isEqualTo("put foo bar".getBytes(StandardCharsets.UTF_8));
        }
    }
}
