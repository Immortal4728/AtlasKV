package com.atlaskv.server.api;

import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.metrics.WatchMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WatchControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NodeLifecycleManager lifecycleManager;

    @Autowired
    private WatchMetrics watchMetrics;

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
    @DisplayName("Watch key receives PUT mutation events over SSE stream")
    void watchKeyReceivesEvents() throws Exception {
        if (!isLeader) {
            return;
        }

        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        List<String> receivedLines = new CopyOnWriteArrayList<>();

        // 1. Start watcher client in a separate thread
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/watch/alert_key"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

        Thread clientThread = new Thread(() -> {
            try {
                HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
                if (response.statusCode() == 200) {
                    try (Stream<String> lines = response.body()) {
                        lines.forEach(line -> {
                            receivedLines.add(line);
                            if (line.contains("connected")) {
                                connectedLatch.countDown();
                            }
                            if (line.contains("PUT") && line.contains("alert_key")) {
                                eventLatch.countDown();
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            }
        });
        clientThread.start();

        // Wait for connection to establish and handshake
        boolean connected = connectedLatch.await(5, TimeUnit.SECONDS);
        assertThat(connected).isTrue();
        assertThat(watchMetrics.activeWatchers()).isGreaterThanOrEqualTo(1);

        // 2. Perform a KV write (PUT)
        KeyValueRequest kvRequest = new KeyValueRequest("high-priority");
        ResponseEntity<KeyValueResponse> putResponse = restTemplate.postForEntity(
                "/api/v1/kv/alert_key", kvRequest, KeyValueResponse.class);
        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Wait for the mutation event to be received by the client stream
        boolean received = eventLatch.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();

        // 3. Clean up client thread
        clientThread.interrupt();
        clientThread.join(1000);

        assertThat(receivedLines).contains("data:connected");
        assertThat(receivedLines).anyMatch(line -> line.contains("PUT") && line.contains("high-priority"));
    }
}
