package com.atlaskv.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class PendingReadIndexManagerTest {

    @Test
    @DisplayName("Single-node read completes immediately when lastApplied >= readIndex")
    void singleNodeReadCompletes() {
        PendingReadIndexManager manager = new PendingReadIndexManager();
        CompletableFuture<Long> future = new CompletableFuture<>();

        manager.register(5L, future, 1L, true);
        manager.tryProcessPendingReads(1L, 5L);

        assertThat(future.isDone()).isTrue();
        assertThat(future.join()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Multi-node read waits for heartbeat quorum and application barrier")
    void multiNodeReadWaitsForQuorumAndApplied() {
        PendingReadIndexManager manager = new PendingReadIndexManager();
        CompletableFuture<Long> future = new CompletableFuture<>();
        NodeId selfId = NodeId.of("node-1");
        NodeId peer1 = NodeId.of("node-2");

        manager.register(10L, future, 1L, false);
        manager.resetHeartbeatAcks(selfId);

        // Before quorum, processing does not complete
        manager.tryProcessPendingReads(1L, 10L);
        assertThat(future.isDone()).isFalse();

        // Receive peer heartbeat ACK (majority = 2 in a 3-node cluster)
        manager.recordHeartbeatAck(peer1, 1L, 2);

        // Process pending reads
        manager.tryProcessPendingReads(1L, 10L);

        assertThat(future.isDone()).isTrue();
        assertThat(future.join()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Fail all completes pending futures exceptionally")
    void failAllCompletesExceptionally() {
        PendingReadIndexManager manager = new PendingReadIndexManager();
        CompletableFuture<Long> future = new CompletableFuture<>();

        manager.register(5L, future, 1L, false);
        manager.failAll(new IllegalStateException("Term changed"));

        assertThat(future.isCompletedExceptionally()).isTrue();
    }
}
