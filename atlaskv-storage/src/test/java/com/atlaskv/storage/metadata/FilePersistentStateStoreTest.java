package com.atlaskv.storage.metadata;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.PersistentState;
import com.atlaskv.core.storage.CorruptedStorageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilePersistentStateStoreTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("loadState returns initial state when file does not exist")
    void loadNonExistent() {
        Path filePath = tempDir.resolve("metadata.dat");
        FilePersistentStateStore store = new FilePersistentStateStore(filePath);

        PersistentState state = store.loadState();
        assertThat(state.currentTerm()).isEqualTo(0L);
        assertThat(state.votedFor()).isNull();
    }

    @Test
    @DisplayName("saveState persists term and votedFor atomically and loadState recovers it")
    void saveAndLoad() {
        Path filePath = tempDir.resolve("metadata.dat");
        FilePersistentStateStore store = new FilePersistentStateStore(filePath);

        PersistentState stateToSave = new PersistentState(3L, NodeId.of("node-1"));
        store.saveState(stateToSave);

        PersistentState loaded = store.loadState();
        assertThat(loaded.currentTerm()).isEqualTo(3L);
        assertThat(loaded.votedFor()).isEqualTo(NodeId.of("node-1"));
    }

    @Test
    @DisplayName("saveState can overwrite state with null votedFor")
    void saveAndLoadNullVote() {
        Path filePath = tempDir.resolve("metadata.dat");
        FilePersistentStateStore store = new FilePersistentStateStore(filePath);

        store.saveState(new PersistentState(4L, NodeId.of("node-2")));
        store.saveState(new PersistentState(5L, null));

        PersistentState loaded = store.loadState();
        assertThat(loaded.currentTerm()).isEqualTo(5L);
        assertThat(loaded.votedFor()).isNull();
    }

    @Test
    @DisplayName("loadState throws CorruptedStorageException when CRC32 checksum is corrupted")
    void corruptedCrcThrows() throws IOException {
        Path filePath = tempDir.resolve("metadata.dat");
        FilePersistentStateStore store = new FilePersistentStateStore(filePath);

        store.saveState(new PersistentState(2L, NodeId.of("node-1")));

        byte[] bytes = Files.readAllBytes(filePath);
        // Corrupt term byte
        bytes[10] ^= 0xFF;
        Files.write(filePath, bytes);

        assertThatThrownBy(store::loadState)
                .isInstanceOf(CorruptedStorageException.class);
    }
}
