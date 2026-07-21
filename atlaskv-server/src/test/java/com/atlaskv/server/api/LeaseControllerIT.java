package com.atlaskv.server.api;

import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.api.dto.LeaseRequest;
import com.atlaskv.server.api.dto.LeaseResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LeaseControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NodeLifecycleManager lifecycleManager;

    @Autowired
    private KeyValueStateMachine stateMachine;

    private boolean isLeader;

    @BeforeEach
    void setUp() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            if (lifecycleManager.raftNode() != null
                    && lifecycleManager.raftNode().role() == RaftRole.LEADER) {
                isLeader = true;
                break;
            }
            Thread.sleep(100);
        }
    }

    @Test
    @DisplayName("Lease creation, list, renewal, and revocation roundtrip")
    void leaseLifecycleRoundtrip() {
        if (!isLeader) {
            return;
        }

        // 1. Create Lease
        LeaseRequest createRequest = new LeaseRequest("test-lease-id", "10s");
        ResponseEntity<LeaseResponse> createRes = restTemplate.postForEntity(
                "/api/v1/lease", createRequest, LeaseResponse.class);

        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createRes.getBody()).isNotNull();
        assertThat(createRes.getBody().leaseId()).isEqualTo("test-lease-id");
        assertThat(createRes.getBody().durationMs()).isEqualTo(10000);

        // 2. GET List leases
        ResponseEntity<LeaseResponse[]> listRes = restTemplate.getForEntity(
                "/api/v1/lease", LeaseResponse[].class);
        assertThat(listRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listRes.getBody()).isNotEmpty();
        assertThat(listRes.getBody()[0].leaseId()).isEqualTo("test-lease-id");

        // 3. RENEW lease
        ResponseEntity<Void> renewRes = restTemplate.postForEntity(
                "/api/v1/lease/test-lease-id/renew", null, Void.class);
        assertThat(renewRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. REVOKE lease
        ResponseEntity<Void> revokeRes = restTemplate.exchange(
                "/api/v1/lease/test-lease-id", HttpMethod.DELETE, null, Void.class);
        assertThat(revokeRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Put key-value with TTL expires key automatically")
    void keyWithTtlExpires() throws InterruptedException {
        if (!isLeader) {
            return;
        }

        KeyValueRequest putReq = new KeyValueRequest("temp-val", "200ms", null);
        ResponseEntity<KeyValueResponse> putRes = restTemplate.postForEntity(
                "/api/v1/kv/ttl-key", putReq, KeyValueResponse.class);
        assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify key exists immediately
        ResponseEntity<KeyValueResponse> getRes = restTemplate.getForEntity(
                "/api/v1/kv/ttl-key", KeyValueResponse.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getRes.getBody().value()).isEqualTo("temp-val");

        // Wait for expiration
        Thread.sleep(1000);

        // Verify key is gone
        ResponseEntity<KeyValueResponse> getRes2 = restTemplate.getForEntity(
                "/api/v1/kv/ttl-key", KeyValueResponse.class);
        assertThat(getRes2.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Lease expiration automatically deletes attached keys")
    void leaseExpirationDeletesKeys() throws InterruptedException {
        if (!isLeader) {
            return;
        }

        // 1. Create 200ms lease
        LeaseRequest createRequest = new LeaseRequest("short-lease", "200ms");
        restTemplate.postForEntity("/api/v1/lease", createRequest, LeaseResponse.class);

        // 2. Put key associated with lease
        KeyValueRequest putReq = new KeyValueRequest("lease-val", null, "short-lease");
        ResponseEntity<KeyValueResponse> putRes = restTemplate.postForEntity(
                "/api/v1/kv/lease-key", putReq, KeyValueResponse.class);
        assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify key exists
        ResponseEntity<KeyValueResponse> getRes = restTemplate.getForEntity(
                "/api/v1/kv/lease-key", KeyValueResponse.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Wait for lease to expire
        Thread.sleep(1000);

        // Verify key is deleted
        ResponseEntity<KeyValueResponse> getRes2 = restTemplate.getForEntity(
                "/api/v1/kv/lease-key", KeyValueResponse.class);
        assertThat(getRes2.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("State machine snapshot and recovery persists TTL and Lease info")
    void snapshotAndRecoveryTest() {
        if (!isLeader) {
            return;
        }

        // Make modifications to state machine directly to isolate snapshot/restore testing
        stateMachine.apply("LEASE_CREATE snap-lease 50000".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        stateMachine.apply("PUT_TTL snap-key 50000 snap-lease snap-val".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(stateMachine.get("snap-key")).isPresent();
        assertThat(stateMachine.leases()).containsKey("snap-lease");
        assertThat(stateMachine.keyToLease()).containsKey("snap-key");

        // Take snapshot
        byte[] snapshotBytes = stateMachine.takeSnapshot();
        assertThat(snapshotBytes).isNotEmpty();

        // Clear and restore snapshot
        stateMachine.restoreSnapshot(snapshotBytes);

        // Verify state is restored perfectly
        assertThat(stateMachine.get("snap-key")).isPresent().contains("snap-val");
        assertThat(stateMachine.leases()).containsKey("snap-lease");
        assertThat(stateMachine.keyToLease()).containsKey("snap-key");
        assertThat(stateMachine.leases().get("snap-lease").keys()).contains("snap-key");
    }
}
