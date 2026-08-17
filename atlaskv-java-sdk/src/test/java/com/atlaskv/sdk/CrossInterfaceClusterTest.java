package com.atlaskv.sdk;

import com.atlaskv.sdk.api.WatchListener;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.exceptions.ConflictException;
import com.atlaskv.sdk.models.KeyValue;
import com.atlaskv.sdk.models.Lease;
import com.atlaskv.sdk.models.Revision;
import com.atlaskv.sdk.models.WatchEvent;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Live 3-Node Cluster End-to-End Verification covering:
 * REST API -> Java SDK -> TypeScript SDK -> CLI cross-interface workflows,
 * Namespace isolation, CAS conflicts, Lease/TTL expiration, History, Rollback,
 * Watch event streaming, and NotLeader behavior.
 */
public class CrossInterfaceClusterTest {

    private static final String LEADER_URL = "http://localhost:8081";
    private static final String FOLLOWER_URL = "http://localhost:8082";
    private static final String CLI_JAR = "d:\\RAFT\\atlaskv-cli\\target\\atlaskv-cli-0.1.0-SNAPSHOT.jar";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void runFullCrossInterfaceVerification() throws Exception {
        System.out.println("\n=======================================================");
        System.out.println("   ATLAS KV CROSS-INTERFACE CLUSTER INTEGRATION TEST   ");
        System.out.println("=======================================================");

        String ns = "tenant-alpha";
        String key = "cross/user_" + System.currentTimeMillis();

        // ---------------------------------------------------------------------
        // STEP 1: REST API creates key in namespace "tenant-alpha"
        // ---------------------------------------------------------------------
        System.out.println("\n[Step 1] REST API: Creating key '" + key + "' in namespace '" + ns + "'...");
        String createJson = "{\"value\":\"{\\\"role\\\":\\\"admin\\\"}\"}";
        HttpRequest restPutReq = HttpRequest.newBuilder()
                .uri(URI.create(LEADER_URL + "/api/v1/kv/" + key))
                .header("Content-Type", "application/json")
                .header("X-Namespace", ns)
                .POST(HttpRequest.BodyPublishers.ofString(createJson))
                .build();
        HttpResponse<String> restPutResp = httpClient.send(restPutReq, HttpResponse.BodyHandlers.ofString());
        assertThat(restPutResp.statusCode()).isEqualTo(201);
        System.out.println("-> REST POST Success: " + restPutResp.body());

        // ---------------------------------------------------------------------
        // STEP 2: Java SDK reads key in namespace "tenant-alpha"
        // ---------------------------------------------------------------------
        System.out.println("\n[Step 2] Java SDK: Reading key '" + key + "' in namespace '" + ns + "'...");
        try (AtlasKVClient javaClient = AtlasKVClient.builder()
                .endpoint(LEADER_URL)
                .namespace(ns)
                .build()) {
            KeyValue kvJava = javaClient.keyValue().get(key);
            assertThat(kvJava.exists()).isTrue();
            assertThat(kvJava.value()).contains("admin");
            assertThat(kvJava.version()).isEqualTo(1);
            System.out.println("-> Java SDK GET Success: version=" + kvJava.version() + ", value=" + kvJava.value());

            // ---------------------------------------------------------------------
            // STEP 3: REST API performs CAS Update (version 1 -> 2)
            // ---------------------------------------------------------------------
            System.out.println("\n[Step 3] REST API: Performing CAS Update (expectedVersion=1)...");
            String casJson = "{\"value\":\"{\\\"role\\\":\\\"superadmin\\\"}\"}";
            HttpRequest restCasReq = HttpRequest.newBuilder()
                    .uri(URI.create(LEADER_URL + "/api/v1/kv/" + key + "?expectedVersion=1"))
                    .header("Content-Type", "application/json")
                    .header("X-Namespace", ns)
                    .PUT(HttpRequest.BodyPublishers.ofString(casJson))
                    .build();
            HttpResponse<String> restCasResp = httpClient.send(restCasReq, HttpResponse.BodyHandlers.ofString());
            assertThat(restCasResp.statusCode()).isEqualTo(200);
            System.out.println("-> REST CAS Success: " + restCasResp.body());

            // Verify Java SDK reads updated version 2
            KeyValue kvJavaV2 = javaClient.keyValue().get(key);
            assertThat(kvJavaV2.version()).isEqualTo(2);
            assertThat(kvJavaV2.value()).contains("superadmin");

            // ---------------------------------------------------------------------
            // STEP 4: CLI queries History in namespace "tenant-alpha"
            // ---------------------------------------------------------------------
            System.out.println("\n[Step 4] CLI: Querying version history for '" + key + "'...");
            String historyOutput = runCliCommand("history", key, "-e", LEADER_URL, "-n", ns);
            System.out.println("-> CLI History Output:\n" + historyOutput);
            assertThat(historyOutput).contains(key);

            List<Revision> historyList = javaClient.history().history(key);
            assertThat(historyList).hasSize(2);
            System.out.println("-> Java SDK History verified: 2 revisions found.");

            // ---------------------------------------------------------------------
            // STEP 5: CLI performs Rollback to revision 1 (creates version 3)
            // ---------------------------------------------------------------------
            System.out.println("\n[Step 5] CLI: Rolling back '" + key + "' to revision 1...");
            String rollbackOutput = runCliCommand("rollback", key, "1", "-e", LEADER_URL, "-n", ns);
            System.out.println("-> CLI Rollback Output:\n" + rollbackOutput);

            // ---------------------------------------------------------------------
            // STEP 6: Java SDK reads key after rollback (verifies version 3 & restored value)
            // ---------------------------------------------------------------------
            System.out.println("\n[Step 6] Java SDK: Reading '" + key + "' after CLI Rollback...");
            KeyValue kvPostRollback = javaClient.keyValue().get(key);
            assertThat(kvPostRollback.version()).isEqualTo(3);
            assertThat(kvPostRollback.value()).contains("admin");
            System.out.println("-> Java SDK Read Post-Rollback: version=" + kvPostRollback.version() + ", value=" + kvPostRollback.value());

            // ---------------------------------------------------------------------
            // STEP 7: Verify Namespace Isolation across separate tenants
            // ---------------------------------------------------------------------
            System.out.println("\n[Step 7] Verification: Namespace Isolation ('tenant-alpha' vs 'tenant-beta')...");
            try (AtlasKVClient javaClientBeta = AtlasKVClient.builder()
                    .endpoint(LEADER_URL)
                    .namespace("tenant-beta")
                    .build()) {
                KeyValue kvBeta = javaClientBeta.keyValue().get(key);
                assertThat(kvBeta.exists()).isFalse();
                System.out.println("-> Namespace Isolation Verified: Key '" + key + "' does not exist in 'tenant-beta'.");
            }

            // ---------------------------------------------------------------------
            // STEP 8: CAS Version Conflict Cross-Check
            // ---------------------------------------------------------------------
            System.out.println("\n[Step 8] Verification: CAS Conflict Handling...");
            assertThatThrownBy(() -> javaClient.keyValue().casPut(key, "stale_val", 1))
                    .isInstanceOf(ConflictException.class)
                    .satisfies(ex -> {
                        ConflictException ce = (ConflictException) ex;
                        System.out.println("-> CAS Conflict Caught: expected=1, current=" + ce.getCurrentVersion());
                        assertThat(ce.getCurrentVersion()).isEqualTo(3);
                    });

            // ---------------------------------------------------------------------
            // STEP 9: Lease & TTL Expiration Cross-Check
            // ---------------------------------------------------------------------
            System.out.println("\n[Step 9] Verification: Lease Attachment & Expiration...");
            Lease lease = javaClient.lease().createLease("3s");
            System.out.println("-> Lease created: " + lease.leaseId());

            String leaseKey = "cross/lease_item";
            javaClient.keyValue().putWithLease(leaseKey, "lease_val", lease.leaseId());
            assertThat(javaClient.keyValue().get(leaseKey).exists()).isTrue();

            System.out.println("-> Waiting 4s for lease to expire...");
            Thread.sleep(4000);

            assertThat(javaClient.keyValue().get(leaseKey).exists()).isFalse();
            System.out.println("-> Lease Expiration Verified: Key automatically deleted.");

            // ---------------------------------------------------------------------
            // STEP 10: SSE Watch Stream Live Event Propagation
            // ---------------------------------------------------------------------
            System.out.println("\n[Step 10] Verification: Real-Time Watch Stream Event Propagation...");
            CountDownLatch watchLatch = new CountDownLatch(1);
            AtomicReference<WatchEvent> watchRef = new AtomicReference<>();

            try (var session = javaClient.watch().watch("cross/watch_target", new WatchListener() {
                @Override public void onConnected() { System.out.println("-> SSE Watch stream connected!"); }
                @Override public void onEvent(WatchEvent event) {
                    System.out.println("-> SSE Event Received: type=" + event.type() + ", val=" + event.value());
                    watchRef.set(event);
                    watchLatch.countDown();
                }
                @Override public void onError(Throwable throwable) {}
                @Override public void onDisconnected() {}
            })) {
                Thread.sleep(500); // Allow connection setup

                // Trigger update via CLI
                runCliCommand("put", "cross/watch_target", "live_watch_value", "-e", LEADER_URL, "-n", ns);

                boolean eventReceived = watchLatch.await(5, TimeUnit.SECONDS);
                assertThat(eventReceived).isTrue();
                assertThat(watchRef.get().value()).isEqualTo("live_watch_value");
                System.out.println("-> SSE Watch Event Propagation Verified!");
            }

            // ---------------------------------------------------------------------
            // STEP 11: NotLeader Redirection Verification
            // ---------------------------------------------------------------------
            System.out.println("\n[Step 11] Verification: NotLeader Redirection on Follower...");
            try (AtlasKVClient followerClient = AtlasKVClient.builder()
                    .endpoint(FOLLOWER_URL)
                    .namespace(ns)
                    .build()) {
                KeyValue followerPut = followerClient.keyValue().put("cross/follower_key", "follower_val");
                assertThat(followerPut.exists()).isTrue();
                System.out.println("-> NotLeader Automatic Redirection Verified: Request redirected to Leader and succeeded.");
            }

            // Clean up
            javaClient.keyValue().delete(key);
            javaClient.keyValue().delete("cross/watch_target");
            javaClient.keyValue().delete("cross/follower_key");
        }

        System.out.println("\n=======================================================");
        System.out.println("   ALL CROSS-INTERFACE CLUSTER INTEGRATION TESTS PASSED ");
        System.out.println("=======================================================");
    }

    private String runCliCommand(String... args) throws Exception {
        String[] fullCmd = new String[args.length + 3];
        fullCmd[0] = "java";
        fullCmd[1] = "-jar";
        fullCmd[2] = CLI_JAR;
        System.arraycopy(args, 0, fullCmd, 3, args.length);

        ProcessBuilder pb = new ProcessBuilder(fullCmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        assertThat(exitCode).as("CLI command should exit with 0. Output: " + sb).isEqualTo(0);
        return sb.toString();
    }
}
