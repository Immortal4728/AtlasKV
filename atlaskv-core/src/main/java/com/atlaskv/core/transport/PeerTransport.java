package com.atlaskv.core.transport;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.InstallSnapshotArgs;
import com.atlaskv.core.rpc.InstallSnapshotReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import java.util.concurrent.CompletableFuture;

/**
 * Transport SPI for inter-node communication between Raft cluster peers.
 */
public interface PeerTransport extends AutoCloseable {

    /**
     * Sends a RequestVote RPC asynchronously to a target peer node.
     *
     * @param target identifier of target node
     * @param args RequestVote payload
     * @return CompletableFuture completing with RequestVoteReply response
     */
    CompletableFuture<RequestVoteReply> sendRequestVote(NodeId target, RequestVoteArgs args);

    /**
     * Sends an AppendEntries RPC asynchronously to a target peer node.
     *
     * @param target identifier of target node
     * @param args AppendEntries payload
     * @return CompletableFuture completing with AppendEntriesReply response
     */
    CompletableFuture<AppendEntriesReply> sendAppendEntries(NodeId target, AppendEntriesArgs args);

    /**
     * Sends an InstallSnapshot RPC asynchronously to a target peer node.
     *
     * @param target identifier of target node
     * @param args InstallSnapshot payload
     * @return CompletableFuture completing with InstallSnapshotReply response
     */
    CompletableFuture<InstallSnapshotReply> sendInstallSnapshot(NodeId target, InstallSnapshotArgs args);

    @Override
    void close();
}
