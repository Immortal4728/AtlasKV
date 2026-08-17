package com.atlaskv.server.api;

import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.api.dto.LeaseRequest;
import com.atlaskv.server.api.dto.LeaseResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.metrics.WatchMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.util.function.Predicate;
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

    private SseTestClient createClient(String path, String namespace) {
        String uriStr = "http://localhost:" + port + path;
        return new SseTestClient(URI.create(uriStr), namespace);
    }

    @Test
    @DisplayName("Watch key receives PUT mutation events with version over SSE stream")
    void watchKeyReceivesPutEvents() throws Exception {
        if (!isLeader) return;

        try (SseTestClient client = createClient("/api/v1/watch/alert_key", null)) {
            assertThat(client.awaitConnected(5)).isTrue();
            assertThat(watchMetrics.activeWatchers()).isGreaterThanOrEqualTo(1);

            // PUT key
            KeyValueRequest kvRequest = new KeyValueRequest("high-priority");
            ResponseEntity<KeyValueResponse> putResponse = restTemplate.postForEntity(
                    "/api/v1/kv/alert_key", kvRequest, KeyValueResponse.class);
            assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            boolean received = client.awaitLineMatching(
                    line -> line.contains("PUT") && line.contains("alert_key") && line.contains("high-priority") && line.contains("version"),
                    5);
            assertThat(received).isTrue();
        }
    }

    @Test
    @DisplayName("Watch key receives DELETE mutation events over SSE stream")
    void watchKeyReceivesDeleteEvents() throws Exception {
        if (!isLeader) return;

        // 1. Write initial key
        restTemplate.postForEntity("/api/v1/kv/del_watch_key", new KeyValueRequest("temp-val"), KeyValueResponse.class);

        try (SseTestClient client = createClient("/api/v1/watch/del_watch_key", null)) {
            assertThat(client.awaitConnected(5)).isTrue();

            // 2. Delete the key
            restTemplate.delete("/api/v1/kv/del_watch_key");

            boolean received = client.awaitLineMatching(
                    line -> line.contains("DELETE") && line.contains("del_watch_key"),
                    5);
            assertThat(received).isTrue();
        }
    }

    @Test
    @DisplayName("Watch key receives EXPIRE mutation events when lease expires")
    void watchKeyReceivesExpireEvents() throws Exception {
        if (!isLeader) return;

        // 1. Create a short 1-second lease
        ResponseEntity<LeaseResponse> leaseRes = restTemplate.postForEntity(
                "/api/v1/leases", new LeaseRequest("1s"), LeaseResponse.class);
        assertThat(leaseRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String leaseId = leaseRes.getBody().leaseId();

        try (SseTestClient client = createClient("/api/v1/watch/exp_watch_key", null)) {
            assertThat(client.awaitConnected(5)).isTrue();

            // 2. Write key with attached lease
            KeyValueRequest kvRequest = new KeyValueRequest("expiring-val", null, leaseId);
            ResponseEntity<KeyValueResponse> putRes = restTemplate.postForEntity(
                    "/api/v1/kv/exp_watch_key", kvRequest, KeyValueResponse.class);
            assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // 3. Wait for lease to expire naturally
            boolean received = client.awaitLineMatching(
                    line -> line.contains("EXPIRE") && line.contains("exp_watch_key"),
                    6);
            assertThat(received).isTrue();
        }
    }

    @Test
    @DisplayName("Watch prefix receives multi-key mutations matching prefix and ignores others")
    void watchPrefixReceivesMultiKeyEvents() throws Exception {
        if (!isLeader) return;

        try (SseTestClient client = createClient("/api/v1/watch/prefix/orders/", null)) {
            assertThat(client.awaitConnected(5)).isTrue();

            // Write 2 keys under orders/ prefix
            restTemplate.postForEntity("/api/v1/kv/orders/item1", new KeyValueRequest("v1"), KeyValueResponse.class);
            restTemplate.postForEntity("/api/v1/kv/orders/item2", new KeyValueRequest("v2"), KeyValueResponse.class);

            // Write 1 key outside prefix
            restTemplate.postForEntity("/api/v1/kv/customers/c1", new KeyValueRequest("cx"), KeyValueResponse.class);

            boolean receivedItem1 = client.awaitLineMatching(l -> l.contains("orders/item1"), 5);
            boolean receivedItem2 = client.awaitLineMatching(l -> l.contains("orders/item2"), 5);
            assertThat(receivedItem1).isTrue();
            assertThat(receivedItem2).isTrue();

            // Confirm customers/c1 was never received
            assertThat(client.getReceivedLines()).noneMatch(l -> l.contains("customers/c1"));
        }
    }

    @Test
    @DisplayName("Watch streams strictly isolate events by tenant namespace")
    void watchNamespaceIsolation() throws Exception {
        if (!isLeader) return;

        try (SseTestClient alphaClient = createClient("/api/v1/watch/prefix/metrics/", "team-alpha")) {
            assertThat(alphaClient.awaitConnected(5)).isTrue();

            // Write in team-beta namespace
            HttpHeaders betaHeaders = new HttpHeaders();
            betaHeaders.set("X-Namespace", "team-beta");
            HttpEntity<KeyValueRequest> betaEntity = new HttpEntity<>(new KeyValueRequest("beta-val"), betaHeaders);
            restTemplate.exchange("/api/v1/kv/metrics/cpu", HttpMethod.POST, betaEntity, KeyValueResponse.class);

            Thread.sleep(300);
            assertThat(alphaClient.getReceivedLines()).noneMatch(l -> l.contains("beta-val"));

            // Write in team-alpha namespace
            HttpHeaders alphaHeaders = new HttpHeaders();
            alphaHeaders.set("X-Namespace", "team-alpha");
            HttpEntity<KeyValueRequest> alphaEntity = new HttpEntity<>(new KeyValueRequest("alpha-val"), alphaHeaders);
            restTemplate.exchange("/api/v1/kv/metrics/mem", HttpMethod.POST, alphaEntity, KeyValueResponse.class);

            boolean receivedAlpha = alphaClient.awaitLineMatching(
                    l -> l.contains("metrics/mem") && l.contains("alpha-val"), 5);
            assertThat(receivedAlpha).isTrue();
        }
    }

    static class SseTestClient implements AutoCloseable {
        private final HttpClient client;
        private final CountDownLatch connectedLatch = new CountDownLatch(1);
        private final List<String> receivedLines = new CopyOnWriteArrayList<>();
        private final Thread thread;

        public SseTestClient(URI uri, String namespace) {
            this.client = HttpClient.newHttpClient();
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", "text/event-stream")
                    .GET();
            if (namespace != null) {
                reqBuilder.header("X-Namespace", namespace);
            }
            HttpRequest request = reqBuilder.build();

            this.thread = new Thread(() -> {
                try {
                    HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
                    if (response.statusCode() == 200) {
                        try (Stream<String> lines = response.body()) {
                            lines.forEach(line -> {
                                receivedLines.add(line);
                                if (line.contains("connected")) {
                                    connectedLatch.countDown();
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            });
            this.thread.start();
        }

        public boolean awaitConnected(long timeoutSec) throws InterruptedException {
            return connectedLatch.await(timeoutSec, TimeUnit.SECONDS);
        }

        public boolean awaitLineMatching(Predicate<String> predicate, long timeoutSec) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutSec * 1000;
            while (System.currentTimeMillis() < deadline) {
                for (String line : receivedLines) {
                    if (predicate.test(line)) {
                        return true;
                    }
                }
                Thread.sleep(50);
            }
            return false;
        }

        public List<String> getReceivedLines() {
            return receivedLines;
        }

        @Override
        public void close() {
            thread.interrupt();
            try {
                thread.join(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
