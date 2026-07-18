package com.atlaskv.core;

import com.atlaskv.core.rpc.InstallSnapshotArgs;
import com.atlaskv.core.statemachine.StateMachine;
import com.atlaskv.core.storage.LogStorage;
import com.atlaskv.core.storage.Snapshot;
import com.atlaskv.core.storage.SnapshotMetadata;
import com.atlaskv.core.storage.SnapshotStorage;

import java.util.Objects;

/**
 * Internal helper for managing Raft snapshotting and log compaction operations.
 */
final class RaftSnapshotManager {

    private RaftSnapshotManager() {
    }

    static boolean shouldTakeSnapshot(long lastApplied, LogStorage logStorage, long threshold) {
        long uncompactedEntries = lastApplied - (logStorage.getFirstLogIndex() - 1);
        return uncompactedEntries >= threshold;
    }

    static SnapshotMetadata takeSnapshot(
            long lastApplied,
            LogStorage logStorage,
            StateMachine stateMachine,
            SnapshotStorage snapshotStorage) {
        Objects.requireNonNull(logStorage, "LogStorage must not be null");
        Objects.requireNonNull(stateMachine, "StateMachine must not be null");
        Objects.requireNonNull(snapshotStorage, "SnapshotStorage must not be null");

        if (lastApplied < logStorage.getFirstLogIndex()) {
            return snapshotStorage.getLatestSnapshotMetadata()
                    .orElse(new SnapshotMetadata(0L, 0L));
        }

        long snapshotTerm = logStorage.getTermAt(lastApplied);
        byte[] data = stateMachine.takeSnapshot();
        SnapshotMetadata meta = new SnapshotMetadata(lastApplied, snapshotTerm);
        Snapshot snapshot = new Snapshot(meta, data);

        snapshotStorage.saveSnapshot(snapshot);
        logStorage.compactUpTo(lastApplied, snapshotTerm);
        return meta;
    }

    static InstallSnapshotArgs createInstallSnapshotArgs(
            long term,
            NodeId selfId,
            Snapshot snapshot) {
        SnapshotMetadata meta = snapshot.metadata();
        return new InstallSnapshotArgs(
                term,
                selfId,
                meta.lastIncludedIndex(),
                meta.lastIncludedTerm(),
                0,
                snapshot.data(),
                true
        );
    }
}
