package com.atlaskv.core.rpc;

import com.atlaskv.core.NodeId;
import java.util.Arrays;
import java.util.Objects;

/**
 * Arguments for the InstallSnapshot RPC (Raft §7).
 *
 * @param term leader's term
 * @param leaderId leader's node identifier
 * @param lastIncludedIndex the snapshot replaces all entries up through this index
 * @param lastIncludedTerm term of lastIncludedIndex
 * @param offset byte offset where chunk is positioned in the snapshot file
 * @param data raw bytes of the snapshot chunk
 * @param done true if this is the last chunk
 */
public record InstallSnapshotArgs(
        long term,
        NodeId leaderId,
        long lastIncludedIndex,
        long lastIncludedTerm,
        int offset,
        byte[] data,
        boolean done
) {

    public InstallSnapshotArgs {
        if (term < 0) {
            throw new IllegalArgumentException("Term must be >= 0, got: " + term);
        }
        Objects.requireNonNull(leaderId, "LeaderId must not be null");
        if (lastIncludedIndex < 0) {
            throw new IllegalArgumentException("lastIncludedIndex must be >= 0, got: " + lastIncludedIndex);
        }
        if (lastIncludedTerm < 0) {
            throw new IllegalArgumentException("lastIncludedTerm must be >= 0, got: " + lastIncludedTerm);
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be >= 0, got: " + offset);
        }
        Objects.requireNonNull(data, "Data byte array must not be null");
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
        InstallSnapshotArgs args = (InstallSnapshotArgs) o;
        return term == args.term
                && lastIncludedIndex == args.lastIncludedIndex
                && lastIncludedTerm == args.lastIncludedTerm
                && offset == args.offset
                && done == args.done
                && Objects.equals(leaderId, args.leaderId)
                && Arrays.equals(data, args.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(term, leaderId, lastIncludedIndex, lastIncludedTerm, offset, done);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
