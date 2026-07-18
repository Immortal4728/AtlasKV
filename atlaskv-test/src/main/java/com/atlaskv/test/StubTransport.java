package com.atlaskv.test;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.InstallSnapshotArgs;
import com.atlaskv.core.rpc.InstallSnapshotReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.core.transport.PeerTransport;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controllable PeerTransport for deterministic testing.
 * All futures are never-completing by default — use the reply methods to resolve them.
 */
public final class StubTransport implements PeerTransport {

    private final CopyOnWriteArrayList<SentRequestVote> sentRequestVotes = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SentAppendEntries> sentAppendEntries = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SentInstallSnapshot> sentInstallSnapshots = new CopyOnWriteArrayList<>();
    private final Map<NodeId, CompletableFuture<RequestVoteReply>> pendingVotes = new ConcurrentHashMap<>();
    private final Map<NodeId, CompletableFuture<AppendEntriesReply>> pendingAppends = new ConcurrentHashMap<>();
    private final Map<NodeId, CompletableFuture<InstallSnapshotReply>> pendingSnapshots = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<RequestVoteReply> sendRequestVote(NodeId target, RequestVoteArgs args) {
        CompletableFuture<RequestVoteReply> future = new CompletableFuture<>();
        sentRequestVotes.add(new SentRequestVote(target, args));
        pendingVotes.put(target, future);
        return future;
    }

    @Override
    public CompletableFuture<AppendEntriesReply> sendAppendEntries(NodeId target, AppendEntriesArgs args) {
        CompletableFuture<AppendEntriesReply> future = new CompletableFuture<>();
        sentAppendEntries.add(new SentAppendEntries(target, args));
        pendingAppends.put(target, future);
        return future;
    }

    @Override
    public CompletableFuture<InstallSnapshotReply> sendInstallSnapshot(NodeId target, InstallSnapshotArgs args) {
        CompletableFuture<InstallSnapshotReply> future = new CompletableFuture<>();
        sentInstallSnapshots.add(new SentInstallSnapshot(target, args));
        pendingSnapshots.put(target, future);
        return future;
    }

    public void replyRequestVote(NodeId peer, RequestVoteReply reply) {
        CompletableFuture<RequestVoteReply> future = pendingVotes.remove(peer);
        if (future != null) {
            future.complete(reply);
        }
    }

    public void replyAppendEntries(NodeId peer, AppendEntriesReply reply) {
        CompletableFuture<AppendEntriesReply> future = pendingAppends.remove(peer);
        if (future != null) {
            future.complete(reply);
        }
    }

    public void replyInstallSnapshot(NodeId peer, InstallSnapshotReply reply) {
        CompletableFuture<InstallSnapshotReply> future = pendingSnapshots.remove(peer);
        if (future != null) {
            future.complete(reply);
        }
    }

    public CopyOnWriteArrayList<SentRequestVote> sentRequestVotes() {
        return sentRequestVotes;
    }

    public CopyOnWriteArrayList<SentAppendEntries> sentAppendEntries() {
        return sentAppendEntries;
    }

    public CopyOnWriteArrayList<SentInstallSnapshot> sentInstallSnapshots() {
        return sentInstallSnapshots;
    }

    public SentAppendEntries lastAppendEntriesTo(NodeId peer) {
        for (int i = sentAppendEntries.size() - 1; i >= 0; i--) {
            SentAppendEntries sent = sentAppendEntries.get(i);
            if (sent.target().equals(peer)) {
                return sent;
            }
        }
        return null;
    }

    public SentInstallSnapshot lastInstallSnapshotTo(NodeId peer) {
        for (int i = sentInstallSnapshots.size() - 1; i >= 0; i--) {
            SentInstallSnapshot sent = sentInstallSnapshots.get(i);
            if (sent.target().equals(peer)) {
                return sent;
            }
        }
        return null;
    }

    public void clearHistory() {
        sentRequestVotes.clear();
        sentAppendEntries.clear();
        sentInstallSnapshots.clear();
    }

    @Override
    public void close() {
        // No resources to release
    }

    public record SentRequestVote(NodeId target, RequestVoteArgs args) { }

    public record SentAppendEntries(NodeId target, AppendEntriesArgs args) { }

    public record SentInstallSnapshot(NodeId target, InstallSnapshotArgs args) { }
}
