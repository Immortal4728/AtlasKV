package com.atlaskv.server.api;

import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.api.dto.LeaseRequest;
import com.atlaskv.server.api.dto.LeaseResponse;
import com.atlaskv.server.api.dto.PrefixQueryResponse;
import com.atlaskv.server.api.dto.RevisionResponse;
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
class LeaseExpirationIT {

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
    @DisplayName("Key with attached lease returns ttlRemaining and leaseId in GET and Prefix queries")
    void keyWithLeaseReturnsMetadataInGetAndPrefix() {
        if (!isLeader) {
            return;
        }

        // 1. Create a 30-second lease
        LeaseRequest createLease = new LeaseRequest("cluster-lease-1", "30s");
        ResponseEntity<LeaseResponse> leaseRes = restTemplate.postForEntity(
                "/api/v1/lease", createLease, LeaseResponse.class);
        assertThat(leaseRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Put key with lease
        KeyValueRequest putReq = new KeyValueRequest("v1", null, "cluster-lease-1");
        ResponseEntity<KeyValueResponse> putRes = restTemplate.postForEntity(
                "/api/v1/kv/lease-exp/test-key-1", putReq, KeyValueResponse.class);
        assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(putRes.getBody()).isNotNull();
        assertThat(putRes.getBody().leaseId()).isEqualTo("cluster-lease-1");
        assertThat(putRes.getBody().ttlRemaining()).isNotNull().isGreaterThan(0);

        // 3. GET key
        ResponseEntity<KeyValueResponse> getRes = restTemplate.getForEntity(
                "/api/v1/kv/lease-exp/test-key-1", KeyValueResponse.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getRes.getBody()).isNotNull();
        assertThat(getRes.getBody().leaseId()).isEqualTo("cluster-lease-1");
        assertThat(getRes.getBody().ttlRemaining()).isNotNull().isGreaterThan(0);

        // 4. Prefix query
        ResponseEntity<PrefixQueryResponse> prefixRes = restTemplate.getForEntity(
                "/api/v1/kv/prefix/lease-exp/", PrefixQueryResponse.class);
        assertThat(prefixRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(prefixRes.getBody()).isNotNull();
        assertThat(prefixRes.getBody().totalCount()).isGreaterThanOrEqualTo(1);
        assertThat(prefixRes.getBody().entries()).anySatisfy(entry -> {
            if ("lease-exp/test-key-1".equals(entry.key())) {
                assertThat(entry.leaseId()).isEqualTo("cluster-lease-1");
                assertThat(entry.ttlRemaining()).isNotNull().isGreaterThan(0);
            }
        });
    }

    @Test
    @DisplayName("Keys without lease return null leaseId and null ttlRemaining")
    void persistentKeyHasNoLeaseOrTtl() {
        if (!isLeader) {
            return;
        }

        KeyValueRequest putReq = new KeyValueRequest("persistent-val");
        ResponseEntity<KeyValueResponse> putRes = restTemplate.postForEntity(
                "/api/v1/kv/persistent/key-1", putReq, KeyValueResponse.class);
        assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(putRes.getBody()).isNotNull();
        assertThat(putRes.getBody().leaseId()).isNull();
        assertThat(putRes.getBody().ttlRemaining()).isNull();

        ResponseEntity<KeyValueResponse> getRes = restTemplate.getForEntity(
                "/api/v1/kv/persistent/key-1", KeyValueResponse.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getRes.getBody()).isNotNull();
        assertThat(getRes.getBody().leaseId()).isNull();
        assertThat(getRes.getBody().ttlRemaining()).isNull();
    }

    @Test
    @DisplayName("Revoking lease immediately removes all bound keys and records EXPIRE revision")
    void revokingLeaseCleansUpAllBoundKeys() {
        if (!isLeader) {
            return;
        }

        // 1. Create lease
        LeaseRequest createReq = new LeaseRequest("multi-lease-1", "60s");
        restTemplate.postForEntity("/api/v1/lease", createReq, LeaseResponse.class);

        // 2. Attach two keys
        restTemplate.postForEntity("/api/v1/kv/multi/k1",
                new KeyValueRequest("val1", null, "multi-lease-1"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/multi/k2",
                new KeyValueRequest("val2", null, "multi-lease-1"), KeyValueResponse.class);

        // Verify keys exist
        assertThat(restTemplate.getForEntity("/api/v1/kv/multi/k1", KeyValueResponse.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.getForEntity("/api/v1/kv/multi/k2", KeyValueResponse.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // 3. Revoke lease
        ResponseEntity<Void> revokeRes = restTemplate.exchange(
                "/api/v1/lease/multi-lease-1", HttpMethod.DELETE, null, Void.class);
        assertThat(revokeRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. Verify keys are not found
        assertThat(restTemplate.getForEntity("/api/v1/kv/multi/k1", KeyValueResponse.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/api/v1/kv/multi/k2", KeyValueResponse.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 5. Verify revision history contains EXPIRE
        ResponseEntity<RevisionResponse[]> histRes = restTemplate.getForEntity(
                "/api/v1/kv/multi/k1/history", RevisionResponse[].class);
        assertThat(histRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(histRes.getBody()).isNotNull();
        assertThat(histRes.getBody()).anyMatch(r -> "EXPIRE".equals(r.operation()));
    }

    @Test
    @DisplayName("Prefix query does not return expired keys even before sweep")
    void prefixQueryFiltersExpiredKeys() throws InterruptedException {
        if (!isLeader) {
            return;
        }

        // Create 200ms lease
        LeaseRequest createReq = new LeaseRequest("quick-lease", "200ms");
        restTemplate.postForEntity("/api/v1/lease", createReq, LeaseResponse.class);

        // Put key
        restTemplate.postForEntity("/api/v1/kv/quick-prefix/item-1",
                new KeyValueRequest("quick-val", null, "quick-lease"), KeyValueResponse.class);

        // Wait 400ms for expiration
        Thread.sleep(400);

        // Prefix query should return 0 entries
        ResponseEntity<PrefixQueryResponse> prefixRes = restTemplate.getForEntity(
                "/api/v1/kv/prefix/quick-prefix/", PrefixQueryResponse.class);
        assertThat(prefixRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(prefixRes.getBody()).isNotNull();
        assertThat(prefixRes.getBody().entries()).noneMatch(e -> "quick-prefix/item-1".equals(e.key()));
    }
}
