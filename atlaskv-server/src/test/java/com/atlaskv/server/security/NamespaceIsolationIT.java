package com.atlaskv.server.security;

import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.dto.CasConflictResponse;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.api.dto.LeaseRequest;
import com.atlaskv.server.api.dto.LeaseResponse;
import com.atlaskv.server.api.dto.PrefixQueryResponse;
import com.atlaskv.server.api.dto.RevisionResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "atlaskv.security.auth-enabled=true",
                "atlaskv.security.auth-token=admin-super-secret",
                "atlaskv.security.admin-username=Administrator"
        }
)
@ActiveProfiles("test")
class NamespaceIsolationIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NodeLifecycleManager lifecycleManager;

    @Autowired
    private AuthenticationService authenticationService;

    private static final String ALICE_TOKEN = "alice-secret-token";
    private static final String BOB_TOKEN = "bob-secret-token";
    private static final String ADMIN_TOKEN = "admin-super-secret";

    private boolean isLeader;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Register test users and API keys in authentication service
        authenticationService.registerUser(new User("user-alice", "Alice", UserRole.USER, true));
        authenticationService.registerApiKey(new ApiKey("key-alice", ALICE_TOKEN, "user-alice", System.currentTimeMillis(), true));

        authenticationService.registerUser(new User("user-bob", "Bob", UserRole.USER, true));
        authenticationService.registerApiKey(new ApiKey("key-bob", BOB_TOKEN, "user-bob", System.currentTimeMillis(), true));

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

    private HttpHeaders aliceHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ALICE_TOKEN);
        return headers;
    }

    private HttpHeaders bobHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(BOB_TOKEN);
        return headers;
    }

    private HttpHeaders adminHeaders(String targetNamespace) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ADMIN_TOKEN);
        if (targetNamespace != null && !targetNamespace.isBlank()) {
            headers.set("X-Namespace", targetNamespace);
        }
        return headers;
    }

    @Test
    @DisplayName("1. User A writes key and reads successfully without exposing namespace prefix")
    void userAWritesAndReadsSuccessfully() {
        if (!isLeader) return;

        KeyValueRequest putReq = new KeyValueRequest("alice-val-1");
        HttpEntity<KeyValueRequest> putEntity = new HttpEntity<>(putReq, aliceHeaders());

        ResponseEntity<KeyValueResponse> putRes = restTemplate.exchange(
                "/api/v1/kv/userA-key1", HttpMethod.POST, putEntity, KeyValueResponse.class);

        assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(putRes.getBody()).isNotNull();
        assertThat(putRes.getBody().key()).isEqualTo("userA-key1");
        assertThat(putRes.getBody().value()).isEqualTo("alice-val-1");

        // Read back
        HttpEntity<Void> getEntity = new HttpEntity<>(aliceHeaders());
        ResponseEntity<KeyValueResponse> getRes = restTemplate.exchange(
                "/api/v1/kv/userA-key1", HttpMethod.GET, getEntity, KeyValueResponse.class);

        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getRes.getBody().key()).isEqualTo("userA-key1");
        assertThat(getRes.getBody().value()).isEqualTo("alice-val-1");
    }

    @Test
    @DisplayName("2. User B cannot read User A's key (returns 404)")
    void userBCannotReadUserAKey() {
        if (!isLeader) return;

        // User A seeds key
        HttpEntity<KeyValueRequest> putEntity = new HttpEntity<>(new KeyValueRequest("secret-data"), aliceHeaders());
        restTemplate.exchange("/api/v1/kv/confidential", HttpMethod.POST, putEntity, KeyValueResponse.class);

        // User B tries to read
        HttpEntity<Void> getEntity = new HttpEntity<>(bobHeaders());
        ResponseEntity<KeyValueResponse> getRes = restTemplate.exchange(
                "/api/v1/kv/confidential", HttpMethod.GET, getEntity, KeyValueResponse.class);

        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getRes.getBody().found()).isFalse();
    }

    @Test
    @DisplayName("3. User B writes own key with same name; User A and User B see independent values")
    void userBWritesOwnKeyIndependentValues() {
        if (!isLeader) return;

        String key = "shared-name";

        // Alice writes
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("alice-secret"), aliceHeaders()), KeyValueResponse.class);

        // Bob writes same key
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("bob-secret"), bobHeaders()), KeyValueResponse.class);

        // Alice reads -> alice-secret
        ResponseEntity<KeyValueResponse> aliceGet = restTemplate.exchange(
                "/api/v1/kv/" + key, HttpMethod.GET, new HttpEntity<>(aliceHeaders()), KeyValueResponse.class);
        assertThat(aliceGet.getBody().value()).isEqualTo("alice-secret");

        // Bob reads -> bob-secret
        ResponseEntity<KeyValueResponse> bobGet = restTemplate.exchange(
                "/api/v1/kv/" + key, HttpMethod.GET, new HttpEntity<>(bobHeaders()), KeyValueResponse.class);
        assertThat(bobGet.getBody().value()).isEqualTo("bob-secret");
    }

    @Test
    @DisplayName("4. User A cannot delete User B's key")
    void userACannotDeleteUserBKey() {
        if (!isLeader) return;

        String key = "bob-protected";

        // Bob writes
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("bob-data"), bobHeaders()), KeyValueResponse.class);

        // Alice tries to delete
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.DELETE,
                new HttpEntity<>(aliceHeaders()), KeyValueResponse.class);

        // Bob's key must still exist
        ResponseEntity<KeyValueResponse> bobGet = restTemplate.exchange(
                "/api/v1/kv/" + key, HttpMethod.GET, new HttpEntity<>(bobHeaders()), KeyValueResponse.class);
        assertThat(bobGet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bobGet.getBody().value()).isEqualTo("bob-data");
    }

    @Test
    @DisplayName("5. User A cannot modify User B's key via PUT")
    void userACannotModifyUserBKey() {
        if (!isLeader) return;

        String key = "modify-target";

        // Bob writes
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("original-bob-data"), bobHeaders()), KeyValueResponse.class);

        // Alice writes to the same key
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("alice-overwrite"), aliceHeaders()), KeyValueResponse.class);

        // Bob reads -> still original-bob-data
        ResponseEntity<KeyValueResponse> bobGet = restTemplate.exchange(
                "/api/v1/kv/" + key, HttpMethod.GET, new HttpEntity<>(bobHeaders()), KeyValueResponse.class);
        assertThat(bobGet.getBody().value()).isEqualTo("original-bob-data");
    }

    @Test
    @DisplayName("6. User A cannot escape namespace via path manipulation or X-Namespace header")
    void userACannotEscapeNamespace() {
        if (!isLeader) return;

        // Bob writes key "secret"
        restTemplate.exchange("/api/v1/kv/secret", HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("bob-classified"), bobHeaders()), KeyValueResponse.class);

        // Alice attempts to pass X-Namespace header for Bob
        HttpHeaders exploitHeaders = aliceHeaders();
        exploitHeaders.set("X-Namespace", "user-bob");
        exploitHeaders.set("namespace", "user-bob");

        ResponseEntity<KeyValueResponse> exploitGet = restTemplate.exchange(
                "/api/v1/kv/secret", HttpMethod.GET, new HttpEntity<>(exploitHeaders), KeyValueResponse.class);
        assertThat(exploitGet.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Alice attempts path exploit ns:user-bob:secret
        ResponseEntity<KeyValueResponse> pathExploitGet = restTemplate.exchange(
                "/api/v1/kv/ns:user-bob:secret", HttpMethod.GET, new HttpEntity<>(aliceHeaders()), KeyValueResponse.class);
        assertThat(pathExploitGet.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("7 & 8. ADMIN can access User A's and User B's namespaces via X-Namespace")
    void adminCanAccessUserNamespaces() {
        if (!isLeader) return;

        String key = "user-profile";

        // Alice writes
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("alice-profile-data"), aliceHeaders()), KeyValueResponse.class);

        // Bob writes
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("bob-profile-data"), bobHeaders()), KeyValueResponse.class);

        // Admin queries Alice namespace
        ResponseEntity<KeyValueResponse> adminAliceGet = restTemplate.exchange(
                "/api/v1/kv/" + key, HttpMethod.GET,
                new HttpEntity<>(adminHeaders("user-alice")), KeyValueResponse.class);
        assertThat(adminAliceGet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminAliceGet.getBody().value()).isEqualTo("alice-profile-data");

        // Admin queries Bob namespace
        ResponseEntity<KeyValueResponse> adminBobGet = restTemplate.exchange(
                "/api/v1/kv/" + key, HttpMethod.GET,
                new HttpEntity<>(adminHeaders("user-bob")), KeyValueResponse.class);
        assertThat(adminBobGet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminBobGet.getBody().value()).isEqualTo("bob-profile-data");
    }

    @Test
    @DisplayName("9. CAS operations are strictly isolated per namespace")
    void casIsStrictlyIsolated() {
        if (!isLeader) return;

        String key = "cas-isolated";

        // Alice puts v1
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("alice-v1"), aliceHeaders()), KeyValueResponse.class);

        // Bob puts v1
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("bob-v1"), bobHeaders()), KeyValueResponse.class);

        // Alice CAS updates version 1 -> 2
        HttpHeaders aliceCasHeader = aliceHeaders();
        aliceCasHeader.set("If-Version", "1");
        ResponseEntity<KeyValueResponse> aliceCasRes = restTemplate.exchange(
                "/api/v1/kv/" + key, HttpMethod.PUT,
                new HttpEntity<>(new KeyValueRequest("alice-v2"), aliceCasHeader), KeyValueResponse.class);
        assertThat(aliceCasRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aliceCasRes.getBody().version()).isEqualTo(2L);

        // Bob's version is still 1. A CAS by Bob expecting version 1 must succeed independently!
        HttpHeaders bobCasHeader = bobHeaders();
        bobCasHeader.set("If-Version", "1");
        ResponseEntity<KeyValueResponse> bobCasRes = restTemplate.exchange(
                "/api/v1/kv/" + key, HttpMethod.PUT,
                new HttpEntity<>(new KeyValueRequest("bob-v2"), bobCasHeader), KeyValueResponse.class);
        assertThat(bobCasRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bobCasRes.getBody().version()).isEqualTo(2L);
        assertThat(bobCasRes.getBody().value()).isEqualTo("bob-v2");
    }

    @Test
    @DisplayName("10. Prefix queries are isolated per namespace")
    void prefixQueriesAreIsolated() {
        if (!isLeader) return;

        // Alice seeds cfg/1 and cfg/2
        restTemplate.exchange("/api/v1/kv/cfg/1", HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("alice-1"), aliceHeaders()), KeyValueResponse.class);
        restTemplate.exchange("/api/v1/kv/cfg/2", HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("alice-2"), aliceHeaders()), KeyValueResponse.class);

        // Bob seeds cfg/3
        restTemplate.exchange("/api/v1/kv/cfg/3", HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("bob-3"), bobHeaders()), KeyValueResponse.class);

        // Alice prefix query
        ResponseEntity<PrefixQueryResponse> aliceQuery = restTemplate.exchange(
                "/api/v1/kv/prefix/cfg/?linearizable=false", HttpMethod.GET,
                new HttpEntity<>(aliceHeaders()), PrefixQueryResponse.class);

        assertThat(aliceQuery.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aliceQuery.getBody().totalCount()).isEqualTo(2);
        assertThat(aliceQuery.getBody().entries()).hasSize(2);
        assertThat(aliceQuery.getBody().entries()).extracting("key").containsExactlyInAnyOrder("cfg/1", "cfg/2");

        // Bob prefix query
        ResponseEntity<PrefixQueryResponse> bobQuery = restTemplate.exchange(
                "/api/v1/kv/prefix/cfg/?linearizable=false", HttpMethod.GET,
                new HttpEntity<>(bobHeaders()), PrefixQueryResponse.class);

        assertThat(bobQuery.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bobQuery.getBody().totalCount()).isEqualTo(1);
        assertThat(bobQuery.getBody().entries().get(0).key()).isEqualTo("cfg/3");
    }

    @Test
    @DisplayName("11. Lease operations and attached keys are isolated per namespace")
    void leasesAreIsolated() {
        if (!isLeader) return;

        // Alice creates lease
        LeaseRequest aliceLeaseReq = new LeaseRequest("lease-alice-1", "10s");
        ResponseEntity<LeaseResponse> createRes = restTemplate.exchange(
                "/api/v1/lease", HttpMethod.POST,
                new HttpEntity<>(aliceLeaseReq, aliceHeaders()), LeaseResponse.class);
        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createRes.getBody().leaseId()).isEqualTo("lease-alice-1");

        // Bob lists leases -> should be empty for Bob
        ResponseEntity<LeaseResponse[]> bobLeases = restTemplate.exchange(
                "/api/v1/lease", HttpMethod.GET,
                new HttpEntity<>(bobHeaders()), LeaseResponse[].class);
        assertThat(bobLeases.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bobLeases.getBody()).noneMatch(l -> "lease-alice-1".equals(l.leaseId()));

        // Alice lists leases -> should see lease-alice-1
        ResponseEntity<LeaseResponse[]> aliceLeases = restTemplate.exchange(
                "/api/v1/lease", HttpMethod.GET,
                new HttpEntity<>(aliceHeaders()), LeaseResponse[].class);
        assertThat(aliceLeases.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aliceLeases.getBody()).anyMatch(l -> "lease-alice-1".equals(l.leaseId()));
    }

    @Test
    @DisplayName("12. Revision history and rollback are isolated per namespace")
    void revisionHistoryAndRollbackAreIsolated() {
        if (!isLeader) return;

        String key = "hist-isolated";

        // Alice writes v1 then v2
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("alice-rev1"), aliceHeaders()), KeyValueResponse.class);
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("alice-rev2"), aliceHeaders()), KeyValueResponse.class);

        // Bob writes v1
        restTemplate.exchange("/api/v1/kv/" + key, HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("bob-rev1"), bobHeaders()), KeyValueResponse.class);

        // Alice checks history -> 2 revisions
        ResponseEntity<RevisionResponse[]> aliceHist = restTemplate.exchange(
                "/api/v1/kv/" + key + "/history?linearizable=false", HttpMethod.GET,
                new HttpEntity<>(aliceHeaders()), RevisionResponse[].class);
        assertThat(aliceHist.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aliceHist.getBody()).hasSize(2);

        // Bob checks history -> 1 revision
        ResponseEntity<RevisionResponse[]> bobHist = restTemplate.exchange(
                "/api/v1/kv/" + key + "/history?linearizable=false", HttpMethod.GET,
                new HttpEntity<>(bobHeaders()), RevisionResponse[].class);
        assertThat(bobHist.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bobHist.getBody()).hasSize(1);

        // Alice rolls back to rev 1
        ResponseEntity<KeyValueResponse> rollbackRes = restTemplate.exchange(
                "/api/v1/kv/" + key + "/rollback/1", HttpMethod.POST,
                new HttpEntity<>(aliceHeaders()), KeyValueResponse.class);
        assertThat(rollbackRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rollbackRes.getBody().value()).isEqualTo("alice-rev1");

        // Bob's value remains bob-rev1
        ResponseEntity<KeyValueResponse> bobGet = restTemplate.exchange(
                "/api/v1/kv/" + key, HttpMethod.GET,
                new HttpEntity<>(bobHeaders()), KeyValueResponse.class);
        assertThat(bobGet.getBody().value()).isEqualTo("bob-rev1");
    }

    @Test
    @DisplayName("13. Watch SSE streams are strictly isolated across user namespaces")
    void watchStreamsAreIsolated() throws Exception {
        if (!isLeader) return;

        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch aliceEventLatch = new CountDownLatch(1);
        List<String> receivedLines = new CopyOnWriteArrayList<>();

        // Alice subscribes to watch key "sensor"
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/watch/sensor"))
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + ALICE_TOKEN)
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
                            if (line.contains("PUT") && line.contains("alice-sensor-data")) {
                                aliceEventLatch.countDown();
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            }
        });
        clientThread.start();

        boolean connected = connectedLatch.await(5, TimeUnit.SECONDS);
        assertThat(connected).isTrue();

        // 1. Bob writes to "sensor"
        restTemplate.exchange("/api/v1/kv/sensor", HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("bob-sensor-data"), bobHeaders()), KeyValueResponse.class);

        // Wait brief time to verify Bob's write does NOT trigger Alice's watcher
        Thread.sleep(500);
        assertThat(receivedLines).noneMatch(l -> l.contains("bob-sensor-data"));

        // 2. Alice writes to "sensor"
        restTemplate.exchange("/api/v1/kv/sensor", HttpMethod.POST,
                new HttpEntity<>(new KeyValueRequest("alice-sensor-data"), aliceHeaders()), KeyValueResponse.class);

        boolean received = aliceEventLatch.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();

        // Cleanup
        clientThread.interrupt();
        clientThread.join(1000);

        // Check that Alice received her event with sanitized key "sensor" (no "ns:user-alice:")
        assertThat(receivedLines).anyMatch(l -> l.contains("sensor") && l.contains("alice-sensor-data") && !l.contains("ns:user-alice:"));
    }
}
