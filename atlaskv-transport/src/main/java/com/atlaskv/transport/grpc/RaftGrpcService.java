package com.atlaskv.transport.grpc;

import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.transport.proto.AppendEntriesProtoArgs;
import com.atlaskv.transport.proto.AppendEntriesProtoReply;
import com.atlaskv.transport.proto.RaftServiceGrpc;
import com.atlaskv.transport.proto.RequestVoteProtoArgs;
import com.atlaskv.transport.proto.RequestVoteProtoReply;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * gRPC service handler for incoming Raft consensus RPCs.
 * Decouples network communication from core consensus logic by dispatching {@link RaftEvent}s.
 */
public final class RaftGrpcService extends RaftServiceGrpc.RaftServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(RaftGrpcService.class);

    private final Consumer<RaftEvent> eventDispatcher;

    /**
     * Constructs a RaftGrpcService backed by an event dispatcher consumer.
     *
     * @param eventDispatcher consumer that receives inbound Raft events
     */
    public RaftGrpcService(Consumer<RaftEvent> eventDispatcher) {
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "EventDispatcher must not be null");
    }

    @Override
    public void requestVote(RequestVoteProtoArgs request, StreamObserver<RequestVoteProtoReply> responseObserver) {
        try {
            RequestVoteArgs domainArgs = GrpcProtoCodec.fromProto(request);
            CompletableFuture<RequestVoteReply> responseFuture = new CompletableFuture<>();

            responseFuture.whenComplete((reply, ex) -> {
                if (ex != null) {
                    LOG.error("Failed to process RequestVote RPC from candidate: {}", domainArgs.candidateId(), ex);
                    responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());
                } else {
                    responseObserver.onNext(GrpcProtoCodec.toProto(reply));
                    responseObserver.onCompleted();
                }
            });

            eventDispatcher.accept(new RaftEvent.InboundRequestVoteEvent(domainArgs, responseFuture));
        } catch (Exception e) {
            LOG.error("Failed to parse RequestVote request", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withCause(e).asRuntimeException());
        }
    }

    @Override
    public void appendEntries(AppendEntriesProtoArgs request, StreamObserver<AppendEntriesProtoReply> responseObserver) {
        try {
            AppendEntriesArgs domainArgs = GrpcProtoCodec.fromProto(request);
            CompletableFuture<AppendEntriesReply> responseFuture = new CompletableFuture<>();

            responseFuture.whenComplete((reply, ex) -> {
                if (ex != null) {
                    LOG.error("Failed to process AppendEntries RPC from leader: {}", domainArgs.leaderId(), ex);
                    responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());
                } else {
                    responseObserver.onNext(GrpcProtoCodec.toProto(reply));
                    responseObserver.onCompleted();
                }
            });

            eventDispatcher.accept(new RaftEvent.InboundAppendEntriesEvent(domainArgs, responseFuture));
        } catch (Exception e) {
            LOG.error("Failed to parse AppendEntries request", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withCause(e).asRuntimeException());
        }
    }

    @Override
    public void installSnapshot(
            com.atlaskv.transport.proto.InstallSnapshotProtoArgs request,
            StreamObserver<com.atlaskv.transport.proto.InstallSnapshotProtoReply> responseObserver) {
        try {
            com.atlaskv.core.rpc.InstallSnapshotArgs domainArgs = GrpcProtoCodec.fromProto(request);
            CompletableFuture<com.atlaskv.core.rpc.InstallSnapshotReply> responseFuture = new CompletableFuture<>();

            responseFuture.whenComplete((reply, ex) -> {
                if (ex != null) {
                    LOG.error("Failed to process InstallSnapshot RPC from leader: {}", domainArgs.leaderId(), ex);
                    responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());
                } else {
                    responseObserver.onNext(GrpcProtoCodec.toProto(reply));
                    responseObserver.onCompleted();
                }
            });

            eventDispatcher.accept(new RaftEvent.InboundInstallSnapshotEvent(domainArgs, responseFuture));
        } catch (Exception e) {
            LOG.error("Failed to parse InstallSnapshot request", e);
            responseObserver.onError(Status.INVALID_ARGUMENT.withCause(e).asRuntimeException());
        }
    }
}
