package com.atlaskv.transport.grpc;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.NodeId;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrpcTransportIntegrationTest {

    private RaftGrpcServer serverNode2;
    private GrpcPeerTransport transportNode1;

    @BeforeEach
    void setUp() throws IOException {
        serverNode2 = new RaftGrpcServer(0, event -> {
            if (event instanceof RaftEvent.InboundRequestVoteEvent voteEvent) {
                RequestVoteArgs args = voteEvent.args();
                boolean granted = args.term() >= 2;
                voteEvent.responseFuture().complete(new RequestVoteReply(Math.max(args.term(), 2L), granted));
            } else if (event instanceof RaftEvent.InboundAppendEntriesEvent appendEvent) {
                AppendEntriesArgs args = appendEvent.args();
                boolean success = args.term() >= 2;
                long matchIdx = success ? args.prevLogIndex() + args.entries().size() : 0L;
                appendEvent.responseFuture().complete(new AppendEntriesReply(Math.max(args.term(), 2L), success, matchIdx));
            }
        });
        serverNode2.start();

        Map<NodeId, String> peers = Map.of(NodeId.of("node-2"), "localhost:" + serverNode2.port());
        transportNode1 = new GrpcPeerTransport(peers, Duration.ofSeconds(3));
    }

    @AfterEach
    void tearDown() {
        if (transportNode1 != null) {
            transportNode1.close();
        }
        if (serverNode2 != null) {
            serverNode2.stop();
        }
    }

    @Test
    @DisplayName("RequestVote RPC over gRPC transport succeeds")
    void sendRequestVoteOverGrpc() throws Exception {
        RequestVoteArgs args = new RequestVoteArgs(2L, NodeId.of("node-1"), 0L, 0L);
        CompletableFuture<RequestVoteReply> future = transportNode1.sendRequestVote(NodeId.of("node-2"), args);

        RequestVoteReply reply = future.get();
        assertThat(reply.term()).isEqualTo(2L);
        assertThat(reply.voteGranted()).isTrue();
    }

    @Test
    @DisplayName("AppendEntries RPC over gRPC transport succeeds")
    void sendAppendEntriesOverGrpc() throws Exception {
        LogEntry entry = new LogEntry(1L, 2L, "put k v".getBytes(StandardCharsets.UTF_8));
        AppendEntriesArgs args = new AppendEntriesArgs(2L, NodeId.of("node-1"), 0L, 0L, List.of(entry), 0L);

        CompletableFuture<AppendEntriesReply> future = transportNode1.sendAppendEntries(NodeId.of("node-2"), args);

        AppendEntriesReply reply = future.get();
        assertThat(reply.term()).isEqualTo(2L);
        assertThat(reply.success()).isTrue();
        assertThat(reply.matchIndex()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Request timeout completes future exceptionally")
    void requestTimeoutHandling() {
        // Transport pointing to non-listening port with short timeout
        Map<NodeId, String> peers = Map.of(NodeId.of("node-dead"), "localhost:59999");
        try (GrpcPeerTransport shortTimeoutTransport = new GrpcPeerTransport(peers, Duration.ofMillis(100))) {
            RequestVoteArgs args = new RequestVoteArgs(1L, NodeId.of("node-1"), 0L, 0L);
            CompletableFuture<RequestVoteReply> future = shortTimeoutTransport.sendRequestVote(NodeId.of("node-dead"), args);

            assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class);
        }
    }
}
