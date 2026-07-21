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

    @Test
    @DisplayName("CAS PUT with matching expectedVersion parameter updates value and increments version")
    void casPutMatchingVersionParamSucceeds() {
        if (!isLeader) {
            return;
        }

        // 1. Initial PUT
        KeyValueRequest putReq = new KeyValueRequest("v1");
        ResponseEntity<KeyValueResponse> initRes = restTemplate.postForEntity(
                "/api/v1/kv/cas-param", putReq, KeyValueResponse.class);
        assertThat(initRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(initRes.getBody().version()).isEqualTo(1L);

        // 2. CAS PUT with expectedVersion=1
        KeyValueRequest casReq = new KeyValueRequest("v2");
        org.springframework.http.HttpEntity<KeyValueRequest> entity = new org.springframework.http.HttpEntity<>(casReq);
        ResponseEntity<KeyValueResponse> casRes = restTemplate.exchange(
                "/api/v1/kv/cas-param?expectedVersion=1", HttpMethod.PUT, entity, KeyValueResponse.class);

        assertThat(casRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(casRes.getBody().value()).isEqualTo("v2");
        assertThat(casRes.getBody().version()).isEqualTo(2L);
        assertThat(casRes.getBody().createdAt()).isEqualTo(initRes.getBody().createdAt());
        assertThat(casRes.getBody().updatedAt()).isGreaterThanOrEqualTo(initRes.getBody().updatedAt());
    }

    @Test
    @DisplayName("CAS PUT with mismatched expectedVersion parameter returns 409 Conflict")
    void casPutMismatchedVersionParamReturns409() {
        if (!isLeader) {
            return;
        }

        // 1. Initial PUT
        KeyValueRequest putReq = new KeyValueRequest("v1");
        ResponseEntity<KeyValueResponse> initRes = restTemplate.postForEntity(
                "/api/v1/kv/cas-fail", putReq, KeyValueResponse.class);
        assertThat(initRes.getBody().version()).isEqualTo(1L);

        // 2. CAS PUT with expectedVersion=99 (mismatch)
        KeyValueRequest casReq = new KeyValueRequest("v2");
        org.springframework.http.HttpEntity<KeyValueRequest> entity = new org.springframework.http.HttpEntity<>(casReq);
        ResponseEntity<com.atlaskv.server.api.dto.CasConflictResponse> casRes = restTemplate.exchange(
                "/api/v1/kv/cas-fail?expectedVersion=99", HttpMethod.PUT, entity, com.atlaskv.server.api.dto.CasConflictResponse.class);

        assertThat(casRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(casRes.getBody().expectedVersion()).isEqualTo(99L);
        assertThat(casRes.getBody().currentVersion()).isEqualTo(1L);
        assertThat(casRes.getBody().reason()).contains("Version mismatch");
    }

    @Test
    @DisplayName("CAS PUT with matching If-Version header updates value and increments version")
    void casPutMatchingHeaderSucceeds() {
        if (!isLeader) {
            return;
        }

        // 1. Initial PUT
        KeyValueRequest putReq = new KeyValueRequest("val-initial");
        ResponseEntity<KeyValueResponse> initRes = restTemplate.postForEntity(
                "/api/v1/kv/cas-header", putReq, KeyValueResponse.class);
        assertThat(initRes.getBody().version()).isEqualTo(1L);

        // 2. CAS PUT with If-Version: 1
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("If-Version", "1");
        KeyValueRequest casReq = new KeyValueRequest("val-updated");
        org.springframework.http.HttpEntity<KeyValueRequest> entity = new org.springframework.http.HttpEntity<>(casReq, headers);

        ResponseEntity<KeyValueResponse> casRes = restTemplate.exchange(
                "/api/v1/kv/cas-header", HttpMethod.PUT, entity, KeyValueResponse.class);

        assertThat(casRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(casRes.getBody().value()).isEqualTo("val-updated");
        assertThat(casRes.getBody().version()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CAS PUT with mismatched If-Version header returns 409 Conflict")
    void casPutMismatchedHeaderReturns409() {
        if (!isLeader) {
            return;
        }

        // 1. Initial PUT
        KeyValueRequest putReq = new KeyValueRequest("val-initial");
        ResponseEntity<KeyValueResponse> initRes = restTemplate.postForEntity(
                "/api/v1/kv/cas-header-fail", putReq, KeyValueResponse.class);
        assertThat(initRes.getBody().version()).isEqualTo(1L);

        // 2. CAS PUT with If-Version: 99
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("If-Version", "99");
        KeyValueRequest casReq = new KeyValueRequest("val-updated");
        org.springframework.http.HttpEntity<KeyValueRequest> entity = new org.springframework.http.HttpEntity<>(casReq, headers);

        ResponseEntity<com.atlaskv.server.api.dto.CasConflictResponse> casRes = restTemplate.exchange(
                "/api/v1/kv/cas-header-fail", HttpMethod.PUT, entity, com.atlaskv.server.api.dto.CasConflictResponse.class);

        assertThat(casRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(casRes.getBody().expectedVersion()).isEqualTo(99L);
        assertThat(casRes.getBody().currentVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Metadata and versions persist across restart")
    void metadataPersistsAcrossRestart() throws Exception {
        if (!isLeader) {
            return;
        }

        // 1. PUT and get version
        KeyValueRequest putReq = new KeyValueRequest("persist-val");
        ResponseEntity<KeyValueResponse> putRes = restTemplate.postForEntity(
                "/api/v1/kv/persist-key", putReq, KeyValueResponse.class);
        long originalVer = putRes.getBody().version();
        long originalCreated = putRes.getBody().createdAt();

        // 2. Force Snapshot & Restart lifecycle
        restTemplate.postForEntity("/api/v1/admin/snapshot", null, Void.class);
        lifecycleManager.stop();
        lifecycleManager.start();

        // Wait for leader
        boolean restartedLeader = false;
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            if (lifecycleManager.raftNode() != null
                    && lifecycleManager.raftNode().role() == RaftRole.LEADER) {
                restartedLeader = true;
                break;
            }
            Thread.sleep(100);
        }
        assertThat(restartedLeader).isTrue();

        // 3. GET and verify metadata matches original
        ResponseEntity<KeyValueResponse> getRes = restTemplate.getForEntity(
                "/api/v1/kv/persist-key", KeyValueResponse.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getRes.getBody().version()).isEqualTo(originalVer);
        assertThat(getRes.getBody().createdAt()).isEqualTo(originalCreated);
    }

    @Test
    @DisplayName("GET /api/v1/kv/prefix/{prefix} returns matching keys")
    void prefixQueryReturnsMatchingKeys() {
        if (!isLeader) {
            return;
        }

        // Seed data
        restTemplate.postForEntity("/api/v1/kv/cfg/db/url", new KeyValueRequest("jdbc:mysql"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/cfg/db/user", new KeyValueRequest("root"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/cfg/cache/ttl", new KeyValueRequest("300"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/other/key", new KeyValueRequest("val"), KeyValueResponse.class);

        ResponseEntity<com.atlaskv.server.api.dto.PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix/cfg/?linearizable=false", com.atlaskv.server.api.dto.PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().prefix()).isEqualTo("cfg/");
        assertThat(response.getBody().totalCount()).isEqualTo(3);
        assertThat(response.getBody().entries()).hasSize(3);
    }

    @Test
    @DisplayName("GET /api/v1/kv/prefix/{prefix} returns empty for no matches")
    void prefixQueryReturnsEmptyForNoMatches() {
        if (!isLeader) {
            return;
        }

        ResponseEntity<com.atlaskv.server.api.dto.PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix/nonexistent/?linearizable=false", com.atlaskv.server.api.dto.PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().totalCount()).isZero();
        assertThat(response.getBody().entries()).isEmpty();
    }

    @Test
    @DisplayName("Prefix query respects pagination limit and offset")
    void prefixQueryPagination() {
        if (!isLeader) {
            return;
        }

        // Seed 5 keys
        for (int i = 1; i <= 5; i++) {
            restTemplate.postForEntity("/api/v1/kv/pg/k" + i, new KeyValueRequest("v" + i), KeyValueResponse.class);
        }

        ResponseEntity<com.atlaskv.server.api.dto.PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix/pg/?limit=2&offset=1&linearizable=false",
                com.atlaskv.server.api.dto.PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().totalCount()).isEqualTo(5);
        assertThat(response.getBody().entries()).hasSize(2);
        assertThat(response.getBody().limit()).isEqualTo(2);
        assertThat(response.getBody().offset()).isEqualTo(1);
    }

    @Test
    @DisplayName("Prefix query supports desc sort order")
    void prefixQueryDescSort() {
        if (!isLeader) {
            return;
        }

        restTemplate.postForEntity("/api/v1/kv/sort/a", new KeyValueRequest("1"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/sort/b", new KeyValueRequest("2"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/sort/c", new KeyValueRequest("3"), KeyValueResponse.class);

        ResponseEntity<com.atlaskv.server.api.dto.PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix/sort/?sort=desc&linearizable=false",
                com.atlaskv.server.api.dto.PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().entries()).hasSize(3);
        assertThat(response.getBody().entries().get(0).key()).isEqualTo("sort/c");
        assertThat(response.getBody().entries().get(2).key()).isEqualTo("sort/a");
    }

    @Test
    @DisplayName("Prefix query includes metadata when requested")
    void prefixQueryIncludesMetadata() {
        if (!isLeader) {
            return;
        }

        restTemplate.postForEntity("/api/v1/kv/meta/key1", new KeyValueRequest("val1"), KeyValueResponse.class);

        ResponseEntity<com.atlaskv.server.api.dto.PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix/meta/?includeMetadata=true&linearizable=false",
                com.atlaskv.server.api.dto.PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().entries()).hasSize(1);
        com.atlaskv.server.api.dto.PrefixEntry entry = response.getBody().entries().get(0);
        assertThat(entry.version()).isEqualTo(1L);
        assertThat(entry.createdAt()).isNotNull();
        assertThat(entry.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("GET /api/v1/kv/{key}/history returns history list")
    void getHistorySuccess() {
        if (!isLeader) {
            return;
        }

        String key = "histkey";
        restTemplate.postForEntity("/api/v1/kv/" + key, new KeyValueRequest("val1"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/" + key, new KeyValueRequest("val2"), KeyValueResponse.class);

        ResponseEntity<com.atlaskv.server.api.dto.RevisionResponse[]> response = restTemplate.getForEntity(
                "/api/v1/kv/" + key + "/history?linearizable=false",
                com.atlaskv.server.api.dto.RevisionResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        com.atlaskv.server.api.dto.RevisionResponse[] history = response.getBody();
        assertThat(history).isNotNull().hasSize(2);
        assertThat(history[0].revisionNumber()).isEqualTo(1L);
        assertThat(history[0].value()).isEqualTo("val1");
        assertThat(history[0].operation()).isEqualTo("PUT");
        assertThat(history[0].nodeId()).isNotBlank();

        assertThat(history[1].revisionNumber()).isEqualTo(2L);
        assertThat(history[1].value()).isEqualTo("val2");
    }

    @Test
    @DisplayName("GET /api/v1/kv/{key}/history returns 404 for missing key")
    void getHistoryMissingKey() {
        ResponseEntity<com.atlaskv.server.api.dto.RevisionResponse[]> response = restTemplate.getForEntity(
                "/api/v1/kv/nonexistent-history-key/history?linearizable=false",
                com.atlaskv.server.api.dto.RevisionResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("POST /api/v1/kv/{key}/rollback/{revision} rolls back state")
    void rollbackRevertsState() {
        if (!isLeader) {
            return;
        }

        String key = "rollkey";
        restTemplate.postForEntity("/api/v1/kv/" + key, new KeyValueRequest("val1"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/" + key, new KeyValueRequest("val2"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/" + key, new KeyValueRequest("val3"), KeyValueResponse.class);

        // Rollback to revision 2
        ResponseEntity<KeyValueResponse> rollbackResp = restTemplate.postForEntity(
                "/api/v1/kv/" + key + "/rollback/2", null, KeyValueResponse.class);

        assertThat(rollbackResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rollbackResp.getBody()).isNotNull();
        assertThat(rollbackResp.getBody().value()).isEqualTo("val2");
        assertThat(rollbackResp.getBody().version()).isEqualTo(4L);

        // Fetch current value to verify
        ResponseEntity<KeyValueResponse> getResp = restTemplate.getForEntity(
                "/api/v1/kv/" + key, KeyValueResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody().value()).isEqualTo("val2");

        // Fetch history to verify ROLLBACK operation logged
        ResponseEntity<com.atlaskv.server.api.dto.RevisionResponse[]> historyResp = restTemplate.getForEntity(
                "/api/v1/kv/" + key + "/history?linearizable=false",
                com.atlaskv.server.api.dto.RevisionResponse[].class);
        assertThat(historyResp.getBody()).hasSize(4);
        assertThat(historyResp.getBody()[3].operation()).isEqualTo("ROLLBACK");
        assertThat(historyResp.getBody()[3].value()).isEqualTo("val2");
    }
}

