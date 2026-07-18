package com.atlaskv.server.health;

import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Spring Boot HealthIndicator reporting Raft cluster node health.
 */
@Component
public class RaftHealthIndicator implements HealthIndicator {

    private final NodeLifecycleManager lifecycleManager;

    /**
     * Constructs the RaftHealthIndicator.
     *
     * @param lifecycleManager node lifecycle manager
     */
    public RaftHealthIndicator(NodeLifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    public Health health() {
        NodeHealthStatus status = lifecycleManager.healthStatus();
        if (!status.healthy()) {
            return Health.down()
                    .withDetail("nodeId", status.nodeId().value())
                    .withDetail("reason", "Raft node is not running")
                    .build();
        }

        return Health.up()
                .withDetail("nodeId", status.nodeId().value())
                .withDetail("role", status.role().name())
                .withDetail("term", status.currentTerm())
                .withDetail("commitIndex", status.commitIndex())
                .withDetail("lastApplied", status.lastApplied())
                .withDetail("currentLeader", status.currentLeader() != null ? status.currentLeader().value() : "none")
                .withDetail("uptimeMs", status.uptimeMillis())
                .build();
    }
}
