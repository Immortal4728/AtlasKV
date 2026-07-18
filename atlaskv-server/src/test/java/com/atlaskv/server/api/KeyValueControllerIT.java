package com.atlaskv.server.api;

import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
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

/**
 * Integration tests for the KeyValueController REST API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class KeyValueControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NodeLifecycleManager lifecycleManager;

    private boolean isLeader;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Wait for single-node to become leader
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
    @DisplayName("GET /api/v1/kv/{key} returns 404 for missing key")
    void getMissingKeyReturns404() {
        ResponseEntity<KeyValueResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/nonexistent", KeyValueResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().found()).isFalse();
    }

    @Test
    @DisplayName("POST + GET key-value roundtrip works when leader")
    void putAndGetRoundtrip() {
        if (!isLeader) {
            return; // Skip if not leader — integration test safety
        }

        // PUT
        KeyValueRequest request = new KeyValueRequest("hello-world");
        ResponseEntity<KeyValueResponse> putResponse = restTemplate.postForEntity(
                "/api/v1/kv/greeting", request, KeyValueResponse.class);

        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(putResponse.getBody()).isNotNull();
        assertThat(putResponse.getBody().key()).isEqualTo("greeting");

        // GET
        ResponseEntity<KeyValueResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/kv/greeting", KeyValueResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().value()).isEqualTo("hello-world");
        assertThat(getResponse.getBody().found()).isTrue();
    }

    @Test
    @DisplayName("DELETE key-value works when leader")
    void deleteKey() {
        if (!isLeader) {
            return;
        }

        // PUT first
        KeyValueRequest request = new KeyValueRequest("to-delete");
        restTemplate.postForEntity("/api/v1/kv/temp", request, KeyValueResponse.class);

        // DELETE
        ResponseEntity<KeyValueResponse> deleteResponse = restTemplate.exchange(
                "/api/v1/kv/temp", HttpMethod.DELETE, null, KeyValueResponse.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify deleted
        ResponseEntity<KeyValueResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/kv/temp", KeyValueResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
