package com.atlaskv.core;

import com.atlaskv.core.rpc.InstallSnapshotArgs;
import com.atlaskv.core.rpc.InstallSnapshotReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.core.storage.LogStorage;

/**
 * Package-private helper utility for validating and processing Raft RPC requests.
 */
final class RaftRpcHelper {

    private RaftRpcHelper() {
        // Utility class
    }

    /**
     * Evaluates a RequestVote RPC request and constructs a reply.
     *
     * @param args RequestVote arguments
     * @param currentTerm receiver's current term
     * @param votedFor node ID receiver voted for in current term
     * @param lastLogTerm receiver's last log term
     * @param lastLogIndex receiver's last log index
     * @return RequestVoteReply result
     */
    static RequestVoteReply processRequestVote(RequestVoteArgs args, long currentTerm, NodeId votedFor,
                                              long lastLogTerm, long lastLogIndex) {
        boolean voteGranted = false;
        if (args.term() == currentTerm) {
            boolean canVote = (votedFor == null || votedFor.equals(args.candidateId()));
            boolean logIsUpToDate = (args.lastLogTerm() > lastLogTerm)
                    || (args.lastLogTerm() == lastLogTerm && args.lastLogIndex() >= lastLogIndex);

            if (canVote && logIsUpToDate) {
                voteGranted = true;
            }
        }
        return new RequestVoteReply(currentTerm, voteGranted);
    }

    /**
     * Evaluates an InstallSnapshot RPC request header.
     *
     * @param args InstallSnapshot arguments
     * @param currentTerm receiver's current term
     * @return InstallSnapshotReply result if rejected, or empty optional if valid
     */
    static InstallSnapshotReply evaluateInstallSnapshotHeader(InstallSnapshotArgs args, long currentTerm) {
        if (args.term() < currentTerm) {
            return new InstallSnapshotReply(currentTerm, false);
        }
        return new InstallSnapshotReply(currentTerm, true);
    }

    /**
     * Re-scans log storage to update current membership following a log truncation.
     *
     * @param logStorage log storage instance
     * @return latest cluster membership found in log
     */
    static com.atlaskv.core.config.ClusterMembership rescanLogMembership(LogStorage logStorage) {
        com.atlaskv.core.config.ClusterMembership latest = null;
        for (long i = logStorage.getFirstLogIndex(); i <= logStorage.getLastLogIndex(); i++) {
            var entryOpt = logStorage.getEntry(i);
            if (entryOpt.isPresent()) {
                byte[] command = entryOpt.get().command();
                if (com.atlaskv.core.config.ClusterMembershipCodec.isMembershipCommand(command)) {
                    var decoded = com.atlaskv.core.config.ClusterMembershipCodec.decode(command);
                    if (decoded.isPresent()) {
                        latest = decoded.get();
                    }
                }
            }
        }
        return latest;
    }
}
