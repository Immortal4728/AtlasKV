package com.atlaskv.sdk;

import com.atlaskv.sdk.api.WatchListener;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.exceptions.ConflictException;
import com.atlaskv.sdk.models.KeyValue;
import com.atlaskv.sdk.models.Lease;
import com.atlaskv.sdk.models.PrefixResult;
import com.atlaskv.sdk.models.WatchEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@org.junit.jupiter.api.Disabled("Requires running 3-node cluster")
public class JavaSdkManualValidationTest {

    @Test
    void runFullManualValidation() throws Exception {
        System.out.println("=== PART 1: JAVA SDK MANUAL VALIDATION ===");

        // 1. Client Creation & Connection (Connect to FOLLOWER node 8081 to test leader redirection!)
        System.out.println("\n[1] Creating client connected to node1 (port 8081 - Follower)...");
        try (AtlasKVClient client = AtlasKVClient.builder()
                .host("localhost")
                .port(8081)
                .timeout(Duration.ofSeconds(5))
                .build()) {

            assertThat(client).isNotNull();
            System.out.println("-> Client created successfully. Initial URI: " + client.activeBaseUri());

            // 2. Leader Redirection & CRUD
            System.out.println("\n[2] Testing Leader Redirection & PUT...");
            KeyValue putRes = client.keyValue().put("java_test_key", "java_test_value");
            System.out.println("-> PUT result: key=" + putRes.key() + ", value=" + putRes.value() + ", version=" + putRes.version() + ", activeUri=" + client.activeBaseUri());
            assertThat(putRes.key()).isEqualTo("java_test_key");
            assertThat(putRes.value()).isEqualTo("java_test_value");
            assertThat(putRes.version()).isGreaterThan(0);

            System.out.println("\n[3] Testing GET...");
            KeyValue getRes = client.keyValue().get("java_test_key");
            System.out.println("-> GET result: key=" + getRes.key() + ", value=" + getRes.value() + ", exists=" + getRes.exists() + ", version=" + getRes.version());
            assertThat(getRes.exists()).isTrue();
            assertThat(getRes.value()).isEqualTo("java_test_value");

            System.out.println("\n[4] Testing EXISTS...");
            boolean exists = client.keyValue().exists("java_test_key");
            System.out.println("-> EXISTS result: " + exists);
            assertThat(exists).isTrue();

            // 3. CAS Operations
            long currentVersion = getRes.version();
            System.out.println("\n[5] Testing CAS Success (expectedVersion=" + currentVersion + ")...");
            KeyValue casSuccess = client.keyValue().casPut("java_test_key", "cas_updated_val", currentVersion);
            System.out.println("-> CAS Success result: new version=" + casSuccess.version() + ", value=" + casSuccess.value());
            assertThat(casSuccess.version()).isEqualTo(currentVersion + 1);
            assertThat(casSuccess.value()).isEqualTo("cas_updated_val");

            System.out.println("\n[6] Testing CAS Failure (expectedVersion=" + currentVersion + " - stale version)...");
            assertThatThrownBy(() -> client.keyValue().casPut("java_test_key", "should_fail", currentVersion))
                    .isInstanceOf(ConflictException.class)
                    .satisfies(ex -> {
                        ConflictException ce = (ConflictException) ex;
                        System.out.println("-> ConflictException caught: " + ce.getMessage() + " [Expected=" + ce.getExpectedVersion() + ", Current=" + ce.getCurrentVersion() + "]");
                    });

            // 4. Prefix Queries
            System.out.println("\n[7] Testing Prefix Queries...");
            client.keyValue().put("pref/k1", "v1");
            client.keyValue().put("pref/k2", "v2");
            PrefixResult prefixRes = client.keyValue().prefix("pref/");
            System.out.println("-> Prefix result count: " + prefixRes.entries().size() + ", total: " + prefixRes.totalCount());
            assertThat(prefixRes.entries()).hasSize(2);

            // 5. Lease Operations
            System.out.println("\n[8] Testing Lease Creation, Renewal, Listing & Revocation...");
            Lease lease = client.lease().createLease("10s");
            System.out.println("-> Lease created: id=" + lease.leaseId() + ", durationMs=" + lease.durationMs() + "ms");
            assertThat(lease.leaseId()).isNotNull();

            client.lease().renewLease(lease.leaseId());
            System.out.println("-> Lease renewed successfully: " + lease.leaseId());

            List<Lease> leases = client.lease().listLeases();
            System.out.println("-> Active leases count: " + leases.size());
            assertThat(leases).anyMatch(l -> l.leaseId().equals(lease.leaseId()));

            client.lease().revokeLease(lease.leaseId());
            System.out.println("-> Lease revoked: " + lease.leaseId());

            // 6. Watch API
            System.out.println("\n[9] Testing Watch API...");
            CountDownLatch watchLatch = new CountDownLatch(1);
            AtomicReference<WatchEvent> capturedEvent = new AtomicReference<>();

            try (var session = client.watch().watch("watch_key", new WatchListener() {
                @Override
                public void onConnected() {
                    System.out.println("-> Watch connected!");
                }

                @Override
                public void onEvent(WatchEvent event) {
                    System.out.println("-> Watch event received: type=" + event.type() + ", key=" + event.key() + ", value=" + event.value());
                    capturedEvent.set(event);
                    watchLatch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("-> Watch error: " + throwable.getMessage());
                }

                @Override
                public void onDisconnected() {
                    System.out.println("-> Watch disconnected");
                }
            })) {
                Thread.sleep(500); // Give watch stream time to connect
                client.keyValue().put("watch_key", "watched_value");
                boolean received = watchLatch.await(5, TimeUnit.SECONDS);
                System.out.println("-> Event received within timeout: " + received);
                assertThat(received).isTrue();
                assertThat(capturedEvent.get().key()).isEqualTo("watch_key");
                assertThat(capturedEvent.get().value()).isEqualTo("watched_value");
            }

            // 7. Cleanup
            System.out.println("\n[10] Testing DELETE & Cleanup...");
            boolean delRes = client.keyValue().delete("java_test_key");
            System.out.println("-> DELETE result: " + delRes);
            client.keyValue().delete("pref/k1");
            client.keyValue().delete("pref/k2");
            client.keyValue().delete("watch_key");

            System.out.println("\n=== JAVA SDK MANUAL VALIDATION COMPLETE ===");
        }
    }
}
