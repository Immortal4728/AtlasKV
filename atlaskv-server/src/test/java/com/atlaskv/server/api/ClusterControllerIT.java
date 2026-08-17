package com.atlaskv.server.api;

import com.atlaskv.server.api.dto.ClusterStatusResponse;
import com.atlaskv.server.api.dto.LeaderResponse;
import com.atlaskv.server.api.dto.MetricsResponse;
import com.atlaskv.server.api.dto.NodeDetailResponse;
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
 * Integration tests for the ClusterController REST API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ClusterControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /api/v1/cluster/status returns 200 with node info")
    void getStatusReturns200() {
        ResponseEntity<ClusterStatusResponse> response = restTemplate.getForEntity(
                "/api/v1/cluster/status", ClusterStatusResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().nodeId()).isEqualTo("test-node");
        assertThat(response.getBody().healthy()).isTrue();
        assertThat(response.getBody().nodeState()).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("GET /api/v1/cluster/leader returns 200 with leader info")
    void getLeaderReturns200() {
        ResponseEntity<LeaderResponse> response = restTemplate.getForEntity(
                "/api/v1/cluster/leader", LeaderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().currentTerm()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("GET /api/v1/cluster/metrics returns 200 with all metrics")
    void getMetricsReturns200() {
        ResponseEntity<MetricsResponse> response = restTemplate.getForEntity(
                "/api/v1/cluster/metrics", MetricsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().nodeId()).isEqualTo("test-node");
        assertThat(response.getBody().kvStoreSize()).isGreaterThanOrEqualTo(0);
        assertThat(response.getBody().activeWatchers()).isGreaterThanOrEqualTo(0L);
        assertThat(response.getBody().totalEventsDelivered()).isGreaterThanOrEqualTo(0L);
        assertThat(response.getBody().activeLeases()).isGreaterThanOrEqualTo(0L);
        assertThat(response.getBody().expiredLeases()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("GET /api/v1/cluster/nodes returns 200 with node details list")
    void getNodesReturns200() {
        ResponseEntity<NodeDetailResponse[]> response = restTemplate.getForEntity(
                "/api/v1/cluster/nodes", NodeDetailResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThanOrEqualTo(1);

        NodeDetailResponse localNode = response.getBody()[0];
        assertThat(localNode.id()).isEqualTo("test-node");
        assertThat(localNode.isLocal()).isTrue();
        assertThat(localNode.healthy()).isTrue();
    }
}
