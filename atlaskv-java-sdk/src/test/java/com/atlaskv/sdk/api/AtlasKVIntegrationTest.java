package com.atlaskv.sdk.api;

import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.ClusterStatus;
import com.atlaskv.sdk.models.KeyValue;
import com.atlaskv.sdk.models.Lease;
import com.atlaskv.sdk.models.Metrics;
import com.atlaskv.sdk.models.PrefixResult;
import com.atlaskv.sdk.models.Revision;
import com.atlaskv.sdk.models.WatchEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.atlaskv.server.AtlasKvApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AtlasKVIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private com.atlaskv.server.lifecycle.NodeLifecycleManager lifecycleManager;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
    private org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping handlerMapping;

    private AtlasKVClient client;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Print all mappings
        System.out.println("=== REGISTERED MAPPINGS ===");
        handlerMapping.getHandlerMethods().forEach((key, value) -> {
            System.out.println("MAPPING: " + key + " -> " + value);
        });
        System.out.println("==========================");
        // Wait for single-node leader election
        long deadline = System.currentTimeMillis() + 10000;
        boolean leaderElected = false;
        while (System.currentTimeMillis() < deadline) {
            if (lifecycleManager.raftNode() != null
                    && lifecycleManager.raftNode().role() == com.atlaskv.core.RaftRole.LEADER) {
                leaderElected = true;
                break;
            }
            Thread.sleep(100);
        }
        assertThat(leaderElected).isTrue();

        client = AtlasKVClient.builder()
                .host("localhost")
                .port(port)
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testCrudOperations() {
        // 1. Put
        KeyValue kv = client.keyValue().put("user/1", "Alice");
        assertThat(kv.key()).isEqualTo("user/1");
        assertThat(kv.value()).isEqualTo("Alice");
        assertThat(kv.exists()).isTrue();

        // 2. Get
        KeyValue getKv = client.keyValue().get("user/1");
        assertThat(getKv.exists()).isTrue();
        assertThat(getKv.value()).isEqualTo("Alice");

        // 3. Exists
        assertThat(client.keyValue().exists("user/1")).isTrue();

        // 4. Delete
        boolean deleted = client.keyValue().delete("user/1");
        assertThat(deleted).isTrue();

        // 5. Verify deletion
        KeyValue deletedKv = client.keyValue().get("user/1");
        assertThat(deletedKv.exists()).isFalse();
        assertThat(deletedKv.value()).isNull();
    }

    @Test
    void testCasOperations() {
        // First put
        KeyValue kv = client.keyValue().put("cas_key", "initial");
        long version = kv.version();

        // Success CAS
        KeyValue updated = client.keyValue().casPut("cas_key", "updated", version);
        assertThat(updated.value()).isEqualTo("updated");
        assertThat(updated.version()).isGreaterThan(version);

        // Failed CAS (outdated version)
        try {
            client.keyValue().casPut("cas_key", "stale", version);
        } catch (com.atlaskv.sdk.exceptions.ConflictException e) {
            assertThat(e.getExpectedVersion()).isEqualTo(version);
            assertThat(e.getCurrentVersion()).isEqualTo(updated.version());
        }
    }

    @Test
    void testPrefixScans() {
        client.keyValue().put("config/db/host", "localhost");
        client.keyValue().put("config/db/port", "5432");
        client.keyValue().put("config/redis/host", "127.0.0.1");

        PrefixResult result = client.keyValue().prefix("config/db/");
        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries().stream().map(PrefixResult.PrefixEntry::key))
                .containsExactlyInAnyOrder("config/db/host", "config/db/port");
    }

    @Test
    void testLeaseLifecycle() throws InterruptedException {
        Lease lease = client.lease().createLease("10s");
        assertThat(lease.leaseId()).isNotNull();
        assertThat(lease.durationMs()).isEqualTo(10000L);

        // Put with lease
        client.keyValue().putWithLease("leased_key", "leased_val", lease.leaseId());

        // Verify key associated with lease
        List<Lease> activeLeases = client.lease().listLeases();
        assertThat(activeLeases.stream().anyMatch(l -> l.leaseId().equals(lease.leaseId()))).isTrue();

        // Renew lease
        client.lease().renewLease(lease.leaseId());

        // Revoke lease
        client.lease().revokeLease(lease.leaseId());

        // Verify key expired
        KeyValue getKv = client.keyValue().get("leased_key");
        assertThat(getKv.exists()).isFalse();
    }

    @Test
    void testHistoryAndRollback() {
        client.keyValue().put("history_key", "v1");
        client.keyValue().put("history_key", "v2");
        KeyValue v3 = client.keyValue().put("history_key", "v3");

        List<Revision> history = client.history().history("history_key");
        assertThat(history).hasSize(3);

        // Get single revision
        Revision rev1 = client.history().revision("history_key", history.get(0).revisionNumber()).orElseThrow();
        assertThat(rev1.value()).isEqualTo("v1");

        // Rollback to v1
        KeyValue rolledBack = client.history().rollback("history_key", history.get(0).revisionNumber());
        assertThat(rolledBack.value()).isEqualTo("v1");
    }

    @Test
    void testClusterInfo() {
        ClusterStatus status = client.cluster().status();
        assertThat(status.nodeId()).isEqualTo("test-node");
        assertThat(status.healthy()).isTrue();

        String leader = client.cluster().leader();
        assertThat(leader).isEqualTo("test-node");

        List<String> members = client.cluster().members();
        assertThat(members).contains("test-node");

        Metrics metrics = client.cluster().metrics();
        assertThat(metrics.nodeId()).isEqualTo("test-node");
    }

    @Test
    void testWatchEvents() throws InterruptedException {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        List<WatchEvent> events = new ArrayList<>();

        WatchListener listener = new WatchListener() {
            @Override
            public void onEvent(WatchEvent event) {
                events.add(event);
                if ("PUT".equals(event.type()) && "watch_key".equals(event.key())) {
                    eventLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onConnected() {
                connectedLatch.countDown();
            }
        };

        try (WatchApi.WatchSession session = client.watch().watch("watch_key", listener)) {
            // Wait for handshake
            boolean connected = connectedLatch.await(5, TimeUnit.SECONDS);
            assertThat(connected).isTrue();

            // Mutate key
            client.keyValue().put("watch_key", "triggered");

            // Wait for SSE notification
            boolean received = eventLatch.await(5, TimeUnit.SECONDS);
            assertThat(received).isTrue();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).value()).isEqualTo("triggered");
        }
    }
}
