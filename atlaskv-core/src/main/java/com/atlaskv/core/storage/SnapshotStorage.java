package com.atlaskv.core.storage;

import java.util.Optional;

/**
 * Storage SPI for managing Raft snapshot persistence, retrieval, and metadata queries.
 */
public interface SnapshotStorage extends AutoCloseable {

    /**
     * Persists a new snapshot atomically.
     *
     * @param snapshot snapshot payload to save
     */
    void saveSnapshot(Snapshot snapshot);

    /**
     * Loads the latest valid snapshot if present.
     *
     * @return Optional containing latest snapshot, or empty if none exists
     */
    Optional<Snapshot> loadLatestSnapshot();

    /**
     * Queries metadata for the latest snapshot without loading full binary payload.
     *
     * @return Optional containing latest snapshot metadata, or empty if none exists
     */
    Optional<SnapshotMetadata> getLatestSnapshotMetadata();

    @Override
    void close();
}
