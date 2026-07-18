package com.atlaskv.core.event;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.InstallSnapshotArgs;
import com.atlaskv.core.rpc.InstallSnapshotReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Sealed interface representing events processed sequentially on the Raft event loop (ADR-0003).
 */
public sealed interface RaftEvent permits
        RaftEvent.ElectionTimeoutEvent,
        RaftEvent.HeartbeatTimeoutEvent,
        RaftEvent.InboundRequestVoteEvent,
        RaftEvent.InboundRequestVoteReplyEvent,
        RaftEvent.InboundAppendEntriesEvent,
        RaftEvent.InboundAppendEntriesReplyEvent,
        RaftEvent.InboundInstallSnapshotEvent,
        RaftEvent.InboundInstallSnapshotReplyEvent,
        RaftEvent.ClientCommandEvent,
        RaftEvent.ClientReadIndexEvent,
        RaftEvent.ClientMembershipChangeEvent {

    /**
     * Membership change operation type.
     */
    enum MemberChangeType {
        ADD,
        REMOVE
    }

    /**
     * Fired when candidate/follower election timer expires.
     */
    record ElectionTimeoutEvent() implements RaftEvent {}

    /**
     * Fired when leader heartbeat timer expires.
     */
    record HeartbeatTimeoutEvent() implements RaftEvent {}

    /**
     * Fired when an inbound RequestVote RPC is received from a peer.
     *
     * @param args payload of RequestVote RPC
     * @param responseFuture future completed with response once processed by event loop
     */
    record InboundRequestVoteEvent(RequestVoteArgs args, CompletableFuture<RequestVoteReply> responseFuture)
            implements RaftEvent {
        public InboundRequestVoteEvent {
            Objects.requireNonNull(args, "Args must not be null");
            Objects.requireNonNull(responseFuture, "ResponseFuture must not be null");
        }
    }

    /**
     * Fired when a response to an outbound RequestVote RPC is received.
     *
     * @param fromNode peer node that responded
     * @param reply RequestVote reply payload
     */
    record InboundRequestVoteReplyEvent(NodeId fromNode, RequestVoteReply reply) implements RaftEvent {
        public InboundRequestVoteReplyEvent {
            Objects.requireNonNull(fromNode, "FromNode must not be null");
            Objects.requireNonNull(reply, "Reply must not be null");
        }
    }

    /**
     * Fired when an inbound AppendEntries RPC is received from a peer.
     *
     * @param args payload of AppendEntries RPC
     * @param responseFuture future completed with response once processed by event loop
     */
    record InboundAppendEntriesEvent(AppendEntriesArgs args, CompletableFuture<AppendEntriesReply> responseFuture)
            implements RaftEvent {
        public InboundAppendEntriesEvent {
            Objects.requireNonNull(args, "Args must not be null");
            Objects.requireNonNull(responseFuture, "ResponseFuture must not be null");
        }
    }

    /**
     * Fired when a response to an outbound AppendEntries RPC is received.
     *
     * @param fromNode peer node that responded
     * @param reply AppendEntries reply payload
     */
    record InboundAppendEntriesReplyEvent(NodeId fromNode, AppendEntriesReply reply) implements RaftEvent {
        public InboundAppendEntriesReplyEvent {
            Objects.requireNonNull(fromNode, "FromNode must not be null");
            Objects.requireNonNull(reply, "Reply must not be null");
        }
    }

    /**
     * Fired when an inbound InstallSnapshot RPC is received from a peer.
     *
     * @param args payload of InstallSnapshot RPC
     * @param responseFuture future completed with response once processed by event loop
     */
    record InboundInstallSnapshotEvent(InstallSnapshotArgs args, CompletableFuture<InstallSnapshotReply> responseFuture)
            implements RaftEvent {
        public InboundInstallSnapshotEvent {
            Objects.requireNonNull(args, "Args must not be null");
            Objects.requireNonNull(responseFuture, "ResponseFuture must not be null");
        }
    }

    /**
     * Fired when a response to an outbound InstallSnapshot RPC is received.
     *
     * @param fromNode peer node that responded
     * @param reply InstallSnapshot reply payload
     */
    record InboundInstallSnapshotReplyEvent(NodeId fromNode, InstallSnapshotReply reply) implements RaftEvent {
        public InboundInstallSnapshotReplyEvent {
            Objects.requireNonNull(fromNode, "FromNode must not be null");
            Objects.requireNonNull(reply, "Reply must not be null");
        }
    }

    /**
     * Fired when a client submits a key-value mutation command.
     *
     * @param command binary payload of command
     * @param responseFuture future completed with result once applied to state machine
     */
    record ClientCommandEvent(byte[] command, CompletableFuture<byte[]> responseFuture) implements RaftEvent {
        public ClientCommandEvent {
            Objects.requireNonNull(command, "Command byte array must not be null");
            command = command.clone();
            Objects.requireNonNull(responseFuture, "ResponseFuture must not be null");
        }

        @Override
        public byte[] command() {
            return command.clone();
        }
    }

    /**
     * Fired when a client requests a linearizable ReadIndex.
     *
     * @param responseFuture future completed with the confirmed readIndex once leadership is verified and applied
     */
    record ClientReadIndexEvent(CompletableFuture<Long> responseFuture) implements RaftEvent {
        public ClientReadIndexEvent {
            Objects.requireNonNull(responseFuture, "ResponseFuture must not be null");
        }
    }

    /**
     * Fired when a client submits a cluster membership change request.
     *
     * @param type ADD or REMOVE operation
     * @param targetNode ID of node to add or remove
     * @param responseFuture future completed once configuration transition is committed
     */
    record ClientMembershipChangeEvent(MemberChangeType type, NodeId targetNode,
                                       CompletableFuture<Void> responseFuture) implements RaftEvent {
        public ClientMembershipChangeEvent {
            Objects.requireNonNull(type, "Type must not be null");
            Objects.requireNonNull(targetNode, "TargetNode must not be null");
            Objects.requireNonNull(responseFuture, "ResponseFuture must not be null");
        }
    }
}
