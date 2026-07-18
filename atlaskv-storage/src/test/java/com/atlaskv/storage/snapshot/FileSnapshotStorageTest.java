package com.atlaskv.storage.snapshot;

import com.atlaskv.core.storage.CorruptedStorageException;
import com.atlaskv.core.storage.Snapshot;
import com.atlaskv.core.storage.SnapshotMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSnapshotStorageTest {

    @TempDir
    Path tempDir;

    private FileSnapshotStorage snapshotStorage;

    @BeforeEach
    void setUp() {
        snapshotStorage = new FileSnapshotStorage(tempDir);
    }

    @Test
    void testSaveAndLoadSnapshot() {
        byte[] data = "state-machine-data-v1".getBytes();
        Snapshot snapshot = new Snapshot(new SnapshotMetadata(10L, 2L), data);

        snapshotStorage.saveSnapshot(snapshot);

        Optional<Snapshot> loadedOpt = snapshotStorage.loadLatestSnapshot();
        assertTrue(loadedOpt.isPresent());

        Snapshot loaded = loadedOpt.get();
        assertEquals(10L, loaded.metadata().lastIncludedIndex());
        assertEquals(2L, loaded.metadata().lastIncludedTerm());
        assertArrayEquals(data, loaded.data());
    }

    @Test
    void testLoadLatestSnapshotFindsHighestIndex() {
        snapshotStorage.saveSnapshot(new Snapshot(new SnapshotMetadata(5L, 1L), "v1".getBytes()));
        snapshotStorage.saveSnapshot(new Snapshot(new SnapshotMetadata(20L, 3L), "v3".getBytes()));
        snapshotStorage.saveSnapshot(new Snapshot(new SnapshotMetadata(10L, 2L), "v2".getBytes()));

        Optional<Snapshot> loadedOpt = snapshotStorage.loadLatestSnapshot();
        assertTrue(loadedOpt.isPresent());
        assertEquals(20L, loadedOpt.get().metadata().lastIncludedIndex());
        assertEquals(3L, loadedOpt.get().metadata().lastIncludedTerm());
        assertArrayEquals("v3".getBytes(), loadedOpt.get().data());
    }

    @Test
    void testGarbageCollectionDeletesOlderSnapshots() throws IOException {
        snapshotStorage.saveSnapshot(new Snapshot(new SnapshotMetadata(5L, 1L), "v1".getBytes()));
        snapshotStorage.saveSnapshot(new Snapshot(new SnapshotMetadata(10L, 2L), "v2".getBytes()));

        try (var stream = Files.list(tempDir)) {
            long snapCount = stream.filter(p -> p.getFileName().toString().endsWith(".snap")).count();
            assertEquals(1, snapCount);
        }

        Optional<Snapshot> loaded = snapshotStorage.loadLatestSnapshot();
        assertTrue(loaded.isPresent());
        assertEquals(10L, loaded.get().metadata().lastIncludedIndex());
    }

    @Test
    void testCorruptedSnapshotTriggersFallback() throws IOException {
        snapshotStorage.saveSnapshot(new Snapshot(new SnapshotMetadata(5L, 1L), "v1".getBytes()));

        // Save a newer snapshot directly corrupted
        Path corruptFile = tempDir.resolve("snapshot-15-2.snap");
        Files.write(corruptFile, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20});

        Optional<Snapshot> loadedOpt = snapshotStorage.loadLatestSnapshot();
        assertTrue(loadedOpt.isPresent());
        assertEquals(5L, loadedOpt.get().metadata().lastIncludedIndex());
        assertArrayEquals("v1".getBytes(), loadedOpt.get().data());
    }
}
