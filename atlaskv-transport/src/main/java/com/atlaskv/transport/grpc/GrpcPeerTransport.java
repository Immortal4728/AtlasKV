package com.atlaskv.transport.grpc;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.core.transport.PeerTransport;
import com.atlaskv.transport.proto.AppendEntriesProtoReply;
import com.atlaskv.transport.proto.RaftServiceGrpc;
import com.atlaskv.transport.proto.RequestVoteProtoReply;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Production-quality gRPC implementation of {@link PeerTransport}.
 * Manages channel pooling, request deadlines, and async RPC futures to peer Raft nodes.
 */
public final class GrpcPeerTransport implements PeerTransport {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcPeerTransport.class);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMillis(1000);

    private final ConcurrentHashMap<NodeId, String> peerAddresses = new ConcurrentHashMap<>();
    private final Duration requestTimeout;
    private final ConcurrentHashMap<NodeId, ManagedChannel> channels = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    /**
     * Constructs a GrpcPeerTransport with default 1000ms request timeout.
     *
     * @param peerAddresses mapping of peer NodeId to address ("host:port")
     */
    public GrpcPeerTransport(Map<NodeId, String> peerAddresses) {
        this(peerAddresses, DEFAULT_REQUEST_TIMEOUT);
    }

    /**
     * Constructs a GrpcPeerTransport with configurable request timeout.
     *
     * @param peerAddresses mapping of peer NodeId to address ("host:port")
     * @param requestTimeout RPC deadline timeout
     */
    public GrpcPeerTransport(Map<NodeId, String> peerAddresses, Duration requestTimeout) {
        Objects.requireNonNull(peerAddresses, "PeerAddresses must not be null");
        this.peerAddresses.putAll(peerAddresses);
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "RequestTimeout must not be null");
    }

    /**
     * Dynamically registers or updates a peer's network address.
     *
     * @param target peer node ID
     * @param address target "host:port" address
     */
    public void registerPeer(NodeId target, String address) {
        Objects.requireNonNull(target, "Target must not be null");
        Objects.requireNonNull(address, "Address must not be null");
        peerAddresses.put(target, address);
    }

    /**
     * Dynamically unregisters a peer node and closes its channel.
     *
     * @param target peer node ID
     */
    public void unregisterPeer(NodeId target) {
        peerAddresses.remove(target);
        ManagedChannel channel = channels.remove(target);
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    @Override
    public CompletableFuture<RequestVoteReply> sendRequestVote(NodeId target, RequestVoteArgs args) {
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("GrpcPeerTransport is closed"));
        }

        try {
            ManagedChannel channel = getOrCreateChannel(target);
            RaftServiceGrpc.RaftServiceFutureStub stub = RaftServiceGrpc.newFutureStub(channel)
                    .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);

            ListenableFuture<RequestVoteProtoReply> future = stub.requestVote(GrpcProtoCodec.toProto(args));
            return adaptFuture(future, GrpcProtoCodec::fromProto);
        } catch (Exception e) {
            LOG.error("Failed to initiate RequestVote RPC to target: {}", target, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<AppendEntriesReply> sendAppendEntries(NodeId target, AppendEntriesArgs args) {
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("GrpcPeerTransport is closed"));
        }

        try {
            ManagedChannel channel = getOrCreateChannel(target);
            RaftServiceGrpc.RaftServiceFutureStub stub = RaftServiceGrpc.newFutureStub(channel)
                    .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);

            ListenableFuture<AppendEntriesProtoReply> future = stub.appendEntries(GrpcProtoCodec.toProto(args));
            return adaptFuture(future, GrpcProtoCodec::fromProto);
        } catch (Exception e) {
            LOG.error("Failed to initiate AppendEntries RPC to target: {}", target, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<com.atlaskv.core.rpc.InstallSnapshotReply> sendInstallSnapshot(
            NodeId target, com.atlaskv.core.rpc.InstallSnapshotArgs args) {
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("GrpcPeerTransport is closed"));
        }

        try {
            ManagedChannel channel = getOrCreateChannel(target);
            RaftServiceGrpc.RaftServiceFutureStub stub = RaftServiceGrpc.newFutureStub(channel)
                    .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);

            ListenableFuture<com.atlaskv.transport.proto.InstallSnapshotProtoReply> future =
                    stub.installSnapshot(GrpcProtoCodec.toProto(args));
            return adaptFuture(future, GrpcProtoCodec::fromProto);
        } catch (Exception e) {
            LOG.error("Failed to initiate InstallSnapshot RPC to target: {}", target, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            for (Map.Entry<NodeId, ManagedChannel> entry : channels.entrySet()) {
                ManagedChannel channel = entry.getValue();
                if (!channel.isShutdown()) {
                    channel.shutdown();
                    try {
                        if (!channel.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                            channel.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        channel.shutdownNow();
                    }
                }
            }
            channels.clear();
        }
    }

    private ManagedChannel getOrCreateChannel(NodeId target) {
        return channels.computeIfAbsent(target, id -> {
            String address = peerAddresses.get(id);
            if (address == null) {
                throw new IllegalArgumentException("Unknown peer node: " + id);
            }
            return ManagedChannelBuilder.forTarget(address)
                    .usePlaintext()
                    .build();
        });
    }

    private static <T, R> CompletableFuture<R> adaptFuture(
            ListenableFuture<T> listenableFuture,
            Function<T, R> mapper) {
        CompletableFuture<R> completableFuture = new CompletableFuture<>();
        Futures.addCallback(listenableFuture, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                try {
                    completableFuture.complete(mapper.apply(result));
                } catch (Exception e) {
                    completableFuture.completeExceptionally(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        }, MoreExecutors.directExecutor());
        return completableFuture;
    }
}
