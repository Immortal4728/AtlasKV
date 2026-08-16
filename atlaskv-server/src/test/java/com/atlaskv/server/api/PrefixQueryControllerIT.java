package com.atlaskv.server.api;

import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.api.dto.PrefixQueryResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PrefixQueryController REST API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PrefixQueryControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NodeLifecycleManager lifecycleManager;

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
    @DisplayName("GET /api/v1/kv/prefix with empty prefix returns 200 and all keys")
    void getPrefixEmptyWithoutTrailingSlash() {
        if (!isLeader) {
            return;
        }

        restTemplate.postForEntity("/api/v1/kv/test-empty-1", new KeyValueRequest("v1"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/test-empty-2", new KeyValueRequest("v2"), KeyValueResponse.class);

        ResponseEntity<PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix", PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalCount()).isGreaterThanOrEqualTo(2);
        assertThat(response.getBody().entries())
                .extracting(entry -> entry.key())
                .contains("test-empty-1", "test-empty-2");
    }

    @Test
    @DisplayName("GET /api/v1/kv/prefix/ with empty prefix returns 200 and all keys")
    void getPrefixEmptyWithTrailingSlash() {
        if (!isLeader) {
            return;
        }

        restTemplate.postForEntity("/api/v1/kv/test-slash-1", new KeyValueRequest("v1"), KeyValueResponse.class);

        ResponseEntity<PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix/", PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().entries())
                .extracting(entry -> entry.key())
                .contains("test-slash-1");
    }

    @Test
    @DisplayName("GET /api/v1/kv/prefix/app returns only app/* keys")
    void getPrefixAppMatchesOnlyAppKeys() {
        if (!isLeader) {
            return;
        }

        restTemplate.postForEntity("/api/v1/kv/app/config/theme", new KeyValueRequest("dark"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/app/test", new KeyValueRequest("hello"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/other/item", new KeyValueRequest("val"), KeyValueResponse.class);

        ResponseEntity<PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix/app", PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().prefix()).isEqualTo("app");
        assertThat(response.getBody().entries())
                .extracting(entry -> entry.key())
                .contains("app/config/theme", "app/test")
                .doesNotContain("other/item");
    }

    @Test
    @DisplayName("GET /api/v1/kv/prefix/manual returns only manual/* keys")
    void getPrefixManualMatchesOnlyManualKeys() {
        if (!isLeader) {
            return;
        }

        restTemplate.postForEntity("/api/v1/kv/manual-test-key", new KeyValueRequest("val-m"), KeyValueResponse.class);
        restTemplate.postForEntity("/api/v1/kv/manual-test/app", new KeyValueRequest("val-app"), KeyValueResponse.class);

        ResponseEntity<PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix/manual", PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().entries())
                .extracting(entry -> entry.key())
                .contains("manual-test-key", "manual-test/app");
    }

    @Test
    @DisplayName("Empty prefix scan respects pagination (limit and offset)")
    void getPrefixPagination() {
        if (!isLeader) {
            return;
        }

        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity("/api/v1/kv/page-test-" + i, new KeyValueRequest("v" + i), KeyValueResponse.class);
        }

        ResponseEntity<PrefixQueryResponse> response = restTemplate.getForEntity(
                "/api/v1/kv/prefix?limit=2&offset=0", PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().limit()).isEqualTo(2);
        assertThat(response.getBody().offset()).isEqualTo(0);
        assertThat(response.getBody().entries()).hasSize(2);
    }

    @Test
    @DisplayName("Empty prefix scan respects namespace isolation via X-Namespace")
    void getPrefixNamespaceIsolation() {
        if (!isLeader) {
            return;
        }

        HttpHeaders headersTenant = new HttpHeaders();
        headersTenant.set("X-Namespace", "tenant-alpha");

        HttpEntity<KeyValueRequest> putEntity = new HttpEntity<>(new KeyValueRequest("tenant-val"), headersTenant);
        restTemplate.exchange("/api/v1/kv/tenant-key-1", HttpMethod.POST, putEntity, KeyValueResponse.class);

        HttpEntity<Void> getEntity = new HttpEntity<>(headersTenant);
        ResponseEntity<PrefixQueryResponse> response = restTemplate.exchange(
                "/api/v1/kv/prefix", HttpMethod.GET, getEntity, PrefixQueryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().entries())
                .extracting(entry -> entry.key())
                .contains("tenant-key-1")
                .doesNotContain("test-empty-1");
    }
}
