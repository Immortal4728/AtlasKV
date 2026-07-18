package com.atlaskv.core;

import com.atlaskv.core.config.ClusterMembership;
import com.atlaskv.core.config.ClusterMembershipCodec;
import com.atlaskv.core.config.JointConsensusHelper;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.InstallSnapshotArgs;
import com.atlaskv.core.rpc.InstallSnapshotReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.core.storage.Snapshot;
import com.atlaskv.core.storage.SnapshotMetadata;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Package-private engine managing state transitions and RPC handling logic for {@link RaftNode}.
 */
final class RaftNodeEngine {

    private RaftNodeEngine() {
        // Utility class
    }

    static void onElectionTimeout(RaftNode node) {
        if (node.role() == RaftRole.LEADER) {
            return;
        }
        node.setRole(RaftRole.CANDIDATE);
        node.updatePersistentState(node.persistentState().withTerm(node.currentTerm() + 1).withVote(node.config().selfId()));
        node.setCurrentLeader(null);
        node.votesReceived().clear();
        node.votesReceived().add(node.config().selfId());

        if (node.currentMembership().isQuorum(node.votesReceived())) {
            becomeLeader(node);
            return;
        }
        node.timerManager().resetElectionTimer();
        for (NodeId peer : node.currentMembership().activePeers(node.config().selfId())) {
            RequestVoteArgs args = new RequestVoteArgs(node.currentTerm(), node.config().selfId(),
                    node.logStorage().getLastLogIndex(), node.logStorage().getLastLogTerm());
            node.transport().sendRequestVote(peer, args).thenAccept(reply ->
                    node.eventLoop().submit(new RaftEvent.InboundRequestVoteReplyEvent(peer, reply)));
        }
    }

    static void onRequestVoteReply(RaftNode node, NodeId fromNode, RequestVoteReply reply) {
        if (reply.term() > node.currentTerm()) {
            becomeFollower(node, reply.term());
            return;
        }
        if (node.role() == RaftRole.CANDIDATE && reply.term() == node.currentTerm() && reply.voteGranted()) {
            node.votesReceived().add(fromNode);
            if (node.currentMembership().isQuorum(node.votesReceived())) {
                becomeLeader(node);
            }
        }
    }

    static void onRequestVote(RaftNode node, RequestVoteArgs args, CompletableFuture<RequestVoteReply> responseFuture) {
        long currentTerm = node.currentTerm();
        if (args.term() > currentTerm) {
            becomeFollower(node, args.term());
            currentTerm = args.term();
        }
        RequestVoteReply reply = RaftRpcHelper.processRequestVote(args, currentTerm, node.votedFor(),
                node.logStorage().getLastLogTerm(), node.logStorage().getLastLogIndex());
        if (reply.voteGranted()) {
            node.updatePersistentState(node.persistentState().withVote(args.candidateId()));
            node.timerManager().resetElectionTimer();
        }
        responseFuture.complete(reply);
    }

    static void onAppendEntries(RaftNode node, AppendEntriesArgs args, CompletableFuture<AppendEntriesReply> responseFuture) {
        long currentTerm = node.currentTerm();
        if (args.term() > currentTerm) {
            becomeFollower(node, args.term());
            currentTerm = args.term();
        }
        if (args.term() < currentTerm) {
            responseFuture.complete(new AppendEntriesReply(currentTerm, false, node.logStorage().getLastLogIndex()));
            return;
        }
        node.setCurrentLeader(args.leaderId());
        if (node.role() != RaftRole.FOLLOWER) {
            node.setRole(RaftRole.FOLLOWER);
            node.timerManager().cancelHeartbeatTimer();
        }
        node.timerManager().resetElectionTimer();

        if (args.prevLogIndex() > 0) {
            long termAtPrev = node.logStorage().getTermAt(args.prevLogIndex());
            if (termAtPrev == 0 || termAtPrev != args.prevLogTerm()) {
                responseFuture.complete(new AppendEntriesReply(currentTerm, false, node.logStorage().getLastLogIndex()));
                return;
            }
        }
        if (!args.entries().isEmpty()) {
            appendEntries(node, args.prevLogIndex(), args.entries());
        }
        if (args.leaderCommit() > node.commitIndex()) {
            node.setCommitIndex(Math.min(args.leaderCommit(), node.logStorage().getLastLogIndex()));
            applyCommitted(node);
        }
        responseFuture.complete(new AppendEntriesReply(currentTerm, true, node.logStorage().getLastLogIndex()));
    }

    private static void appendEntries(RaftNode node, long prevLogIndex, List<LogEntry> entries) {
        long insertIndex = prevLogIndex + 1;
        for (int i = 0; i < entries.size(); i++) {
            long logIndex = insertIndex + i;
            LogEntry newEntry = entries.get(i);
            if (logIndex <= node.logStorage().getLastLogIndex()) {
                if (node.logStorage().getTermAt(logIndex) != newEntry.term()) {
                    node.logStorage().truncateFrom(logIndex);
                    node.scanLogForMembership();
                    node.logStorage().append(newEntry);
                }
            } else {
                node.logStorage().append(newEntry);
            }
            if (ClusterMembershipCodec.isMembershipCommand(newEntry.command())) {
                ClusterMembershipCodec.decode(newEntry.command()).ifPresent(node::setCurrentMembership);
            }
        }
    }

    static void onAppendEntriesReply(RaftNode node, NodeId fromNode, AppendEntriesReply reply) {
        if (reply.term() > node.currentTerm()) {
            becomeFollower(node, reply.term());
            return;
        }
        if (node.role() != RaftRole.LEADER || node.leaderState() == null) {
            return;
        }
        if (reply.term() == node.currentTerm()) {
            node.pendingReadIndexManager().recordHeartbeatAck(fromNode, node.currentTerm(), node.currentMembership());
            node.pendingReadIndexManager().tryProcessPendingReads(node.currentTerm(), node.lastApplied());
        }
        if (reply.success()) {
            node.leaderState().setMatchIndex(fromNode, reply.matchIndex());
            node.leaderState().setNextIndex(fromNode, reply.matchIndex() + 1);
            advanceCommitIndex(node);
        } else {
            node.leaderState().decrementNextIndex(fromNode);
            replicateToFollower(node, fromNode);
        }
    }

    static void onInstallSnapshot(RaftNode node, InstallSnapshotArgs args, CompletableFuture<InstallSnapshotReply> responseFuture) {
        long currentTerm = node.currentTerm();
        if (args.term() > currentTerm) {
            becomeFollower(node, args.term());
            currentTerm = args.term();
        }
        InstallSnapshotReply reply = RaftRpcHelper.evaluateInstallSnapshotHeader(args, currentTerm);
        if (!reply.success()) {
            responseFuture.complete(reply);
            return;
        }
        node.setCurrentLeader(args.leaderId());
        if (node.role() != RaftRole.FOLLOWER) {
            node.setRole(RaftRole.FOLLOWER);
            node.timerManager().cancelHeartbeatTimer();
        }
        node.timerManager().resetElectionTimer();

        if (args.done()) {
            Snapshot snapshot = new Snapshot(new SnapshotMetadata(args.lastIncludedIndex(),
                    args.lastIncludedTerm(), node.currentMembership()), args.data());
            node.snapshotStorage().saveSnapshot(snapshot);
            node.stateMachine().restoreSnapshot(args.data());
            node.logStorage().compactUpTo(args.lastIncludedIndex(), args.lastIncludedTerm());
            node.setCommitIndex(Math.max(node.commitIndex(), args.lastIncludedIndex()));
            node.setLastApplied(Math.max(node.lastApplied(), args.lastIncludedIndex()));
        }
        responseFuture.complete(reply);
    }

    static void onInstallSnapshotReply(RaftNode node, NodeId fromNode, InstallSnapshotReply reply) {
        if (reply.term() > node.currentTerm()) {
            becomeFollower(node, reply.term());
            return;
        }
        if (node.role() != RaftRole.LEADER || node.leaderState() == null) {
            return;
        }
        if (reply.success()) {
            node.snapshotStorage().getLatestSnapshotMetadata().ifPresent(meta -> {
                node.leaderState().setMatchIndex(fromNode, meta.lastIncludedIndex());
                node.leaderState().setNextIndex(fromNode, meta.lastIncludedIndex() + 1);
                advanceCommitIndex(node);
                replicateToFollower(node, fromNode);
            });
        } else {
            replicateToFollower(node, fromNode);
        }
    }

    static void onHeartbeatTimeout(RaftNode node) {
        if (node.role() == RaftRole.LEADER) {
            replicateToAllFollowers(node);
            node.timerManager().resetHeartbeatTimer();
        }
    }

    static void replicateToAllFollowers(RaftNode node) {
        for (NodeId peer : node.currentMembership().activePeers(node.config().selfId())) {
            replicateToFollower(node, peer);
        }
    }

    static void replicateToFollower(RaftNode node, NodeId peer) {
        if (node.leaderState() == null) {
            return;
        }
        long nextIdx = node.leaderState().getNextIndex(peer);
        if (nextIdx < node.logStorage().getFirstLogIndex()) {
            node.snapshotStorage().loadLatestSnapshot().ifPresent(snapshot -> {
                InstallSnapshotArgs args = RaftSnapshotManager.createInstallSnapshotArgs(
                        node.currentTerm(), node.config().selfId(), snapshot);
                node.transport().sendInstallSnapshot(peer, args).thenAccept(reply ->
                        node.eventLoop().submit(new RaftEvent.InboundInstallSnapshotReplyEvent(peer, reply)));
            });
            return;
        }
        long prevLogIndex = nextIdx - 1;
        AppendEntriesArgs args = new AppendEntriesArgs(node.currentTerm(), node.config().selfId(),
                prevLogIndex, node.logStorage().getTermAt(prevLogIndex), node.logStorage().getEntriesFrom(nextIdx), node.commitIndex());
        node.transport().sendAppendEntries(peer, args).thenAccept(reply ->
                node.eventLoop().submit(new RaftEvent.InboundAppendEntriesReplyEvent(peer, reply)));
    }

    static void onClientCommand(RaftNode node, byte[] command, CompletableFuture<byte[]> responseFuture) {
        if (node.role() != RaftRole.LEADER) {
            responseFuture.completeExceptionally(new IllegalStateException("Not leader. Current leader: " + node.currentLeader()));
            return;
        }
        long newIndex = node.logStorage().getLastLogIndex() + 1;
        node.logStorage().append(new LogEntry(newIndex, node.currentTerm(), command));
        node.pendingCommands().register(newIndex, responseFuture);
        replicateToAllFollowers(node);
        if (node.currentMembership().activePeers(node.config().selfId()).isEmpty()) {
            advanceCommitIndex(node);
        }
    }

    static void onClientMembershipChange(RaftNode node, RaftEvent.MemberChangeType type, NodeId targetNode,
                                         CompletableFuture<Void> responseFuture) {
        if (node.role() != RaftRole.LEADER) {
            responseFuture.completeExceptionally(new IllegalStateException("Not leader. Current leader: " + node.currentLeader()));
            return;
        }
        try {
            boolean isAdd = (type == RaftEvent.MemberChangeType.ADD);
            ClusterMembership jointMembership = JointConsensusHelper.createJointMembership(node.currentMembership(), isAdd, targetNode);
            byte[] command = ClusterMembershipCodec.encode(jointMembership);
            long newIndex = node.logStorage().getLastLogIndex() + 1;
            node.logStorage().append(new LogEntry(newIndex, node.currentTerm(), command));
            node.setCurrentMembership(jointMembership);
            node.setPendingMembershipFuture(responseFuture);

            if (node.leaderState() != null) {
                node.leaderState().updatePeers(node.currentMembership().activePeers(node.config().selfId()), node.logStorage().getLastLogIndex());
            }
            replicateToAllFollowers(node);
            if (node.currentMembership().activePeers(node.config().selfId()).isEmpty()) {
                advanceCommitIndex(node);
            }
        } catch (Exception e) {
            responseFuture.completeExceptionally(e);
        }
    }

    static void onClientReadIndex(RaftNode node, CompletableFuture<Long> responseFuture) {
        if (node.role() != RaftRole.LEADER) {
            responseFuture.completeExceptionally(new IllegalStateException("Not leader. Current leader: " + node.currentLeader()));
            return;
        }
        long readIndex = node.commitIndex();
        boolean singleNode = node.currentMembership().activePeers(node.config().selfId()).isEmpty();
        node.pendingReadIndexManager().register(readIndex, responseFuture, node.currentTerm(), singleNode);

        if (!singleNode) {
            node.pendingReadIndexManager().resetHeartbeatAcks(node.config().selfId());
            replicateToAllFollowers(node);
        }
        node.pendingReadIndexManager().tryProcessPendingReads(node.currentTerm(), node.lastApplied());
    }

    static void advanceCommitIndex(RaftNode node) {
        for (long n = node.logStorage().getLastLogIndex(); n > node.commitIndex(); n--) {
            if (node.logStorage().getTermAt(n) != node.currentTerm()) {
                continue;
            }
            Set<NodeId> ackedNodes = new HashSet<>();
            ackedNodes.add(node.config().selfId());
            for (NodeId peer : node.currentMembership().activePeers(node.config().selfId())) {
                if (node.leaderState() != null && node.leaderState().getMatchIndex(peer) >= n) {
                    ackedNodes.add(peer);
                }
            }
            if (node.currentMembership().isQuorum(ackedNodes)) {
                node.setCommitIndex(n);
                applyCommitted(node);
                break;
            }
        }
    }

    static void applyCommitted(RaftNode node) {
        while (node.lastApplied() < node.commitIndex()) {
            node.setLastApplied(node.lastApplied() + 1);
            long index = node.lastApplied();
            node.logStorage().getEntry(index).ifPresent(entry -> {
                byte[] result = node.stateMachine().apply(entry.command());
                CompletableFuture<byte[]> pending = node.pendingCommands().remove(entry.index());
                if (pending != null) {
                    pending.complete(result);
                }
                processMembershipEntryIfPresent(node, entry);
            });
        }

        if (RaftSnapshotManager.shouldTakeSnapshot(node.lastApplied(), node.logStorage(), node.config().snapshotThresholdEntries())) {
            node.takeSnapshot();
        }
        node.pendingReadIndexManager().tryProcessPendingReads(node.currentTerm(), node.lastApplied());
    }

    private static void processMembershipEntryIfPresent(RaftNode node, LogEntry entry) {
        if (ClusterMembershipCodec.isMembershipCommand(entry.command())) {
            ClusterMembershipCodec.decode(entry.command()).ifPresent(mem -> {
                node.setCurrentMembership(mem);
                if (node.role() == RaftRole.LEADER && mem.isJoint()) {
                    ClusterMembership finalMembership = mem.toSingle();
                    byte[] finalCmd = ClusterMembershipCodec.encode(finalMembership);
                    long newIndex = node.logStorage().getLastLogIndex() + 1;
                    node.logStorage().append(new LogEntry(newIndex, node.currentTerm(), finalCmd));
                    node.setCurrentMembership(finalMembership);
                    node.setPendingMembershipTargetIndex(newIndex);
                    if (node.leaderState() != null) {
                        node.leaderState().updatePeers(node.currentMembership().activePeers(node.config().selfId()), node.logStorage().getLastLogIndex());
                    }
                    replicateToAllFollowers(node);
                    if (node.currentMembership().activePeers(node.config().selfId()).isEmpty()) {
                        advanceCommitIndex(node);
                    }
                } else if (node.role() == RaftRole.LEADER && !mem.isJoint()) {
                    if (node.pendingMembershipFuture() != null && (node.pendingMembershipTargetIndex() == -1
                            || entry.index() >= node.pendingMembershipTargetIndex())) {
                        node.pendingMembershipFuture().complete(null);
                        node.clearPendingMembershipFuture();
                    }
                    if (!mem.oldMembers().contains(node.config().selfId())) {
                        becomeFollower(node, node.currentTerm());
                    }
                }
            });
        }
    }

    static void becomeLeader(RaftNode node) {
        node.setRole(RaftRole.LEADER);
        node.setCurrentLeader(node.config().selfId());
        node.setLeaderState(new LeaderState(node.currentMembership().activePeers(node.config().selfId()), node.logStorage().getLastLogIndex()));
        node.pendingReadIndexManager().resetHeartbeatAcks(node.config().selfId());
        node.timerManager().cancelElectionTimer();

        long newIndex = node.logStorage().getLastLogIndex() + 1;
        node.logStorage().append(new LogEntry(newIndex, node.currentTerm(),
                "NOOP".getBytes(StandardCharsets.UTF_8)));
        replicateToAllFollowers(node);
        if (node.currentMembership().activePeers(node.config().selfId()).isEmpty()) {
            advanceCommitIndex(node);
        }
        node.timerManager().resetHeartbeatTimer();
    }

    static void becomeFollower(RaftNode node, long newTerm) {
        node.setRole(RaftRole.FOLLOWER);
        node.updatePersistentState(node.persistentState().withTerm(newTerm));
        node.setCurrentLeader(null);
        if (node.leaderState() != null) {
            node.pendingCommands().failAll(new IllegalStateException("Leadership lost at term " + newTerm));
            node.setLeaderState(null);
        }
        node.pendingReadIndexManager().failAll(new IllegalStateException("Leadership lost at term " + newTerm));
        node.failPendingMembershipFuture(new IllegalStateException("Leadership lost at term " + newTerm));
        node.timerManager().cancelHeartbeatTimer();
        node.timerManager().resetElectionTimer();
    }
}
