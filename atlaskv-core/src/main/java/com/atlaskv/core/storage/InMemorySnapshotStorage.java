package com.atlaskv.core.storage;

import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of {@link SnapshotStorage} for testing and volatile operations.
 */
public final class InMemorySnapshotStorage implements SnapshotStorage {

    private volatile Snapshot latestSnapshot;

    @Override
    public void saveSnapshot(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot must not be null");
        this.latestSnapshot = snapshot;
    }

    @Override
    public Optional<Snapshot> loadLatestSnapshot() {
        return Optional.ofNullable(latestSnapshot);
    }

    @Override
    public Optional<SnapshotMetadata> getLatestSnapshotMetadata() {
        return Optional.ofNullable(latestSnapshot).map(Snapshot::metadata);
    }

    @Override
    public void close() {
        // No resources to release
    }
}
