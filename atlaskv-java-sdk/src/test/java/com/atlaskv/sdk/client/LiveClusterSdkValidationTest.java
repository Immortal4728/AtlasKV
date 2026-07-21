package com.atlaskv.sdk.client;

import com.atlaskv.sdk.api.WatchListener;
import com.atlaskv.sdk.connection.RetryPolicy;
import com.atlaskv.sdk.exceptions.ConflictException;
import com.atlaskv.sdk.models.KeyValue;
import com.atlaskv.sdk.models.Lease;
import com.atlaskv.sdk.models.PrefixResult;
import com.atlaskv.sdk.models.WatchEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Live E2E Integration test for AtlasKV Java SDK against running 3-node cluster.
 */
public class LiveClusterSdkValidationTest {

    private static AtlasKVClient client;
    private final String runId = UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    static void setUp() {
        // Connect to node1 (port 8081). Node 2 (8082) is current leader.
        // SDK automatic leader redirection will redirect request to leader!
        client = AtlasKVClient.builder()
                .host("localhost")
                .port(8081)
                .timeout(Duration.ofSeconds(5))
                .retryPolicy(RetryPolicy.builder().maxRetries(3).build())
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    @DisplayName("Phase 2 & 8 — Leader Redirection and Connection Setup")
    void testConnectionAndLeaderRedirection() {
        String key = "conn_test_key_" + runId;
        KeyValue kv = client.keyValue().put(key, "conn_val");
        assertThat(kv).isNotNull();
        assertThat(kv.value()).isEqualTo("conn_val");
        assertThat(client.activeBaseUri().toString()).contains("8082");
    }

    @Test
    @DisplayName("Phase 3 — Key-Value CRUD Operations")
    void testCrudOperations() {
        String key = "sdk_crud_key_" + runId;
        String val1 = "initial_val";
        String val2 = "updated_val";

        // Create
        KeyValue created = client.keyValue().put(key, val1);
        assertThat(created.key()).isEqualTo(key);
        assertThat(created.value()).isEqualTo(val1);
        assertThat(created.version()).isEqualTo(1L);
        assertThat(created.exists()).isTrue();

        // Read
        KeyValue read1 = client.keyValue().get(key);
        assertThat(read1.value()).isEqualTo(val1);
        assertThat(read1.version()).isEqualTo(1L);

        // Update
        KeyValue updated = client.keyValue().put(key, val2);
        assertThat(updated.value()).isEqualTo(val2);
        assertThat(updated.version()).isEqualTo(2L);

        // Delete
        boolean deleted = client.keyValue().delete(key);
        assertThat(deleted).isTrue();

        // Read deleted
        KeyValue readDeleted = client.keyValue().get(key);
        assertThat(readDeleted.exists()).isFalse();
        assertThat(readDeleted.value()).isNull();
    }

    @Test
    @DisplayName("Phase 4 — Compare-And-Swap (CAS)")
    void testCasOperations() {
        String key = "sdk_cas_key_" + runId;

        // Create if absent (expectedVersion = 0)
        KeyValue cas0 = client.keyValue().casPut(key, "cas_val_0", 0);
        assertThat(cas0.version()).isEqualTo(1L);
        assertThat(cas0.value()).isEqualTo("cas_val_0");

        // Successful CAS (expectedVersion = 1)
        KeyValue cas1 = client.keyValue().casPut(key, "cas_val_1", 1);
        assertThat(cas1.version()).isEqualTo(2L);

        // Stale CAS conflict (expectedVersion = 1, but current is 2)
        assertThatThrownBy(() -> client.keyValue().casPut(key, "stale_val", 1))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> {
                    ConflictException ce = (ConflictException) e;
                    assertThat(ce.getExpectedVersion()).isEqualTo(1L);
                    assertThat(ce.getCurrentVersion()).isEqualTo(2L);
                    assertThat(ce.getStatusCode()).isEqualTo(409);
                });

        // Valid CAS (expectedVersion = 2)
        KeyValue cas2 = client.keyValue().casPut(key, "cas_val_2", 2);
        assertThat(cas2.version()).isEqualTo(3L);
        assertThat(cas2.value()).isEqualTo("cas_val_2");

        // Clean up
        client.keyValue().delete(key);
    }

    @Test
    @DisplayName("Phase 5 — Prefix Queries")
    void testPrefixQueries() {
        String prefix = "sdk_pref_" + runId + "_";
        client.keyValue().put(prefix + "a", "val_a");
        client.keyValue().put(prefix + "b", "val_b");
        client.keyValue().put(prefix + "c", "val_c");
        client.keyValue().put(prefix + "d", "val_d");

        // Query prefix
        PrefixResult res = client.keyValue().prefix(prefix, 0, 2);
        assertThat(res.totalCount()).isEqualTo(4);
        assertThat(res.entries()).hasSize(2);
        assertThat(res.entries().get(0).key()).isEqualTo(prefix + "a");
        assertThat(res.entries().get(1).key()).isEqualTo(prefix + "b");

        // Query offset 2
        PrefixResult page2 = client.keyValue().prefix(prefix, 2, 2);
        assertThat(page2.entries().get(0).key()).isEqualTo(prefix + "c");
        assertThat(page2.entries().get(1).key()).isEqualTo(prefix + "d");

        // Clean up
        client.keyValue().delete(prefix + "a");
        client.keyValue().delete(prefix + "b");
        client.keyValue().delete(prefix + "c");
        client.keyValue().delete(prefix + "d");
    }

    @Test
    @DisplayName("Phase 6 — Lease Creation and Expiration")
    void testLeaseLifecycle() throws Exception {
        // Create 3-second lease
        Lease lease = client.lease().createLease("3s");
        assertThat(lease.leaseId()).isNotNull();

        // Attach key to lease
        String key = "sdk_lease_key_" + runId;
        client.keyValue().putWithLease(key, "lease_val", lease.leaseId());

        // Read key (present)
        assertThat(client.keyValue().get(key).exists()).isTrue();

        // Wait 4 seconds for expiration
        Thread.sleep(4000);

        // Read key (expired)
        assertThat(client.keyValue().get(key).exists()).isFalse();
    }

    @Test
    @DisplayName("Phase 7 — Watch API Stream")
    void testWatchApi() throws Exception {
        String watchKey = "sdk_watch_key_" + runId;
        List<WatchEvent> events = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        com.atlaskv.sdk.api.WatchApi.WatchSession session = client.watch().watch(watchKey, new WatchListener() {
            @Override
            public void onEvent(WatchEvent event) {
                events.add(event);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {}
        });

        // Wait 1 second for SSE stream to establish
        Thread.sleep(1000);

        // Perform mutations
        client.keyValue().put(watchKey, "w1");
        client.keyValue().put(watchKey, "w2");
        client.keyValue().delete(watchKey);

        boolean await = latch.await(5, TimeUnit.SECONDS);
        session.close();

        assertThat(await).isTrue();
        assertThat(events).hasSize(3);
        assertThat(events.get(0).type()).isEqualTo("PUT");
        assertThat(events.get(0).value()).isEqualTo("w1");
        assertThat(events.get(1).type()).isEqualTo("PUT");
        assertThat(events.get(1).value()).isEqualTo("w2");
        assertThat(events.get(2).type()).isEqualTo("DELETE");
    }

    @Test
    @DisplayName("Phase 9 — Multi-Threaded Concurrency")
    void testThreadSafety() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCounter = new AtomicInteger(0);

        String key = "sdk_concurrent_key_" + runId;
        client.keyValue().put(key, "val_0");

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    client.keyValue().get(key);
                    client.keyValue().put("thread_key_" + Thread.currentThread().getId() + "_" + runId, "val");
                    successCounter.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean done = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(done).isTrue();
        assertThat(successCounter.get()).isEqualTo(threads);
    }
}
