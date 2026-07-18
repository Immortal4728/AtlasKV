package com.atlaskv.transport.grpc;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.RaftNode;
import com.atlaskv.core.clock.SystemClock;
import com.atlaskv.core.config.RaftConfig;
import com.atlaskv.core.event.RaftEvent;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.core.storage.InMemoryLogStorage;
import com.atlaskv.test.StubStateMachine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class MultiNodeGrpcClusterIntegrationTest {

    private final Map<NodeId, RaftGrpcServer> servers = new HashMap<>();
    private final Map<NodeId, GrpcPeerTransport> transports = new HashMap<>();
    private final Map<NodeId, RaftNode> nodes = new HashMap<>();

    @BeforeEach
    void setUpCluster() throws IOException {
        NodeId n1 = NodeId.of("node-1");
        NodeId n2 = NodeId.of("node-2");
        NodeId n3 = NodeId.of("node-3");

        // 1. Create servers on ephemeral ports
        servers.put(n1, new RaftGrpcServer(0, event -> dispatchToNode(n1, event)));
        servers.put(n2, new RaftGrpcServer(0, event -> dispatchToNode(n2, event)));
        servers.put(n3, new RaftGrpcServer(0, event -> dispatchToNode(n3, event)));

        for (RaftGrpcServer server : servers.values()) {
            server.start();
        }

        // 2. Build peer address map
        Map<NodeId, String> addresses = Map.of(
                n1, "localhost:" + servers.get(n1).port(),
                n2, "localhost:" + servers.get(n2).port(),
                n3, "localhost:" + servers.get(n3).port()
        );

        // 3. Create transports and RaftNodes
        Set<NodeId> allNodes = Set.of(n1, n2, n3);
        for (NodeId id : allNodes) {
            Set<NodeId> peers = Set.copyOf(allNodes.stream().filter(p -> !p.equals(id)).toList());
            RaftConfig config = new RaftConfig(id, peers,
                    Duration.ofMillis(150), Duration.ofMillis(300), Duration.ofMillis(50));
            GrpcPeerTransport transport = new GrpcPeerTransport(addresses, Duration.ofMillis(500));
            transports.put(id, transport);

            RaftNode node = new RaftNode(config, new SystemClock(), new InMemoryLogStorage(), transport, new StubStateMachine());
            nodes.put(id, node);
        }
    }

    @AfterEach
    void tearDownCluster() {
        for (RaftNode node : nodes.values()) {
            node.close();
        }
        for (GrpcPeerTransport transport : transports.values()) {
            transport.close();
        }
        for (RaftGrpcServer server : servers.values()) {
            server.stop();
        }
    }

    @Test
    @DisplayName("3-node gRPC network allows RequestVote communication between nodes")
    void testThreeNodeGrpcRequestVote() throws Exception {
        NodeId n1 = NodeId.of("node-1");
        NodeId n2 = NodeId.of("node-2");

        GrpcPeerTransport t1 = transports.get(n1);
        RequestVoteArgs args = new RequestVoteArgs(1L, n1, 0L, 0L);

        CompletableFuture<RequestVoteReply> future = t1.sendRequestVote(n2, args);
        RequestVoteReply reply = future.get();

        assertThat(reply.term()).isEqualTo(1L);
        assertThat(reply.voteGranted()).isTrue();
    }

    @Test
    @DisplayName("3-node gRPC network allows AppendEntries heartbeats between nodes")
    void testThreeNodeGrpcAppendEntries() throws Exception {
        NodeId n1 = NodeId.of("node-1");
        NodeId n2 = NodeId.of("node-2");

        GrpcPeerTransport t1 = transports.get(n1);
        AppendEntriesArgs args = new AppendEntriesArgs(1L, n1, 0L, 0L, java.util.List.of(), 0L);

        CompletableFuture<AppendEntriesReply> future = t1.sendAppendEntries(n2, args);
        AppendEntriesReply reply = future.get();

        assertThat(reply.term()).isEqualTo(1L);
        assertThat(reply.success()).isTrue();
    }

    private void dispatchToNode(NodeId target, RaftEvent event) {
        RaftNode node = nodes.get(target);
        if (node != null) {
            node.handleEvent(event);
        }
    }
}
