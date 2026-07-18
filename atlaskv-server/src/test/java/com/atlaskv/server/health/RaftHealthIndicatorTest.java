package com.atlaskv.server.health;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.RaftRole;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class RaftHealthIndicatorTest {

    @Test
    @DisplayName("Reports Status.UP when node is healthy")
    void healthyNodeReportsUp() {
        NodeLifecycleManager manager = Mockito.mock(NodeLifecycleManager.class);
        NodeHealthStatus status = new NodeHealthStatus(
                NodeId.of("node-1"),
                RaftRole.LEADER,
                5L, 100L, 100L,
                NodeId.of("node-1"),
                true,
                System.currentTimeMillis() - 1000);
        given(manager.healthStatus()).willReturn(status);

        RaftHealthIndicator indicator = new RaftHealthIndicator(manager);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("nodeId", "node-1");
        assertThat(health.getDetails()).containsEntry("role", "LEADER");
        assertThat(health.getDetails()).containsEntry("term", 5L);
        assertThat(health.getDetails()).containsEntry("commitIndex", 100L);
    }

    @Test
    @DisplayName("Reports Status.DOWN when node is unhealthy")
    void unhealthyNodeReportsDown() {
        NodeLifecycleManager manager = Mockito.mock(NodeLifecycleManager.class);
        NodeHealthStatus status = new NodeHealthStatus(
                NodeId.of("node-1"),
                RaftRole.FOLLOWER,
                0L, 0L, 0L, null,
                false, 0L);
        given(manager.healthStatus()).willReturn(status);

        RaftHealthIndicator indicator = new RaftHealthIndicator(manager);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "Raft node is not running");
    }
}
