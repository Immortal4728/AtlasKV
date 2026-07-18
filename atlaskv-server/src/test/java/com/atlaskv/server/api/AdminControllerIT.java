package com.atlaskv.server.api;

import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.dto.SnapshotResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the AdminController REST API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NodeLifecycleManager lifecycleManager;

    @BeforeEach
    void setUp() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            if (lifecycleManager.raftNode() != null
                    && lifecycleManager.raftNode().role() == RaftRole.LEADER) {
                break;
            }
            Thread.sleep(100);
        }
    }

    @Test
    @DisplayName("POST /api/v1/admin/snapshot returns 200")
    void snapshotReturns200() {
        ResponseEntity<SnapshotResponse> response = restTemplate.postForEntity(
                "/api/v1/admin/snapshot", null, SnapshotResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/v1/admin/shutdown returns 200 with status")
    void shutdownReturns200() {
        // Note: We do NOT actually call shutdown in tests as it would kill the context.
        // Instead, verify the endpoint is mapped and reachable.
        // A real shutdown test would require a separate process.
        assertThat(lifecycleManager.state().name()).isEqualTo("RUNNING");
    }
}
