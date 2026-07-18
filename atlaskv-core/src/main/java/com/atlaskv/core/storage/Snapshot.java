package com.atlaskv.core.storage;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable snapshot container representing the state machine snapshot at a specific point in the log.
 *
 * @param metadata metadata describing snapshot index and term
 * @param data serialized state machine binary payload
 */
public record Snapshot(SnapshotMetadata metadata, byte[] data) {

    public Snapshot {
        Objects.requireNonNull(metadata, "Metadata must not be null");
        Objects.requireNonNull(data, "Data must not be null");
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Snapshot snapshot = (Snapshot) o;
        return Objects.equals(metadata, snapshot.metadata) && Arrays.equals(data, snapshot.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(metadata);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
