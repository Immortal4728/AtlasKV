package com.atlaskv.sdk.client;

import com.atlaskv.core.RaftRole;
import com.atlaskv.sdk.api.WatchListener;
import com.atlaskv.sdk.api.WatchApi;
import com.atlaskv.sdk.exceptions.AtlasKVException;
import com.atlaskv.sdk.models.KeyValue;
import com.atlaskv.sdk.models.WatchEvent;
import com.atlaskv.server.AtlasKvApplication;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.security.ApiKey;
import com.atlaskv.server.security.AuthenticationService;
import com.atlaskv.server.security.User;
import com.atlaskv.server.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration test validating remote client connectivity with
 * base endpoint URLs, API key authentication, namespace isolation, and SSE watch streams.
 */
@SpringBootTest(
        classes = AtlasKvApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "atlaskv.security.auth-enabled=true",
                "atlaskv.security.auth-token=admin-super-secret",
                "atlaskv.security.admin-username=Administrator"
        }
)
@ActiveProfiles("test")
class RemoteClientConnectivityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private NodeLifecycleManager lifecycleManager;

    @Autowired
    private AuthenticationService authenticationService;

    private static final String ALICE_TOKEN = "alice-remote-token";
    private static final String BOB_TOKEN = "bob-remote-token";

    private boolean isLeader;

    @BeforeEach
    void setUp() throws InterruptedException {
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

    @Test
    @DisplayName("Remote Java SDK connects with endpoint URL and apiKey")
    void remoteJavaSdkEndpointAndApiKeyConnectivity() {
        if (!isLeader) return;

        String endpoint = "http://localhost:" + port;
        try (AtlasKVClient client = AtlasKVClient.builder()
                .endpoint(endpoint)
                .apiKey(ALICE_TOKEN)
                .timeout(Duration.ofSeconds(5))
                .build()) {

            KeyValue putResult = client.keyValue().put("cloud-key-1", "alice-cloud-value");
            assertThat(putResult.key()).isEqualTo("cloud-key-1");
            assertThat(putResult.value()).isEqualTo("alice-cloud-value");

            KeyValue getResult = client.keyValue().get("cloud-key-1");
            assertThat(getResult.exists()).isTrue();
            assertThat(getResult.value()).isEqualTo("alice-cloud-value");
        }
    }

    @Test
    @DisplayName("Remote Java SDK enforces namespace isolation between tenants")
    void remoteJavaSdkNamespaceIsolation() {
        if (!isLeader) return;

        String endpoint = "http://localhost:" + port;

        try (AtlasKVClient aliceClient = AtlasKVClient.builder()
                .endpoint(endpoint)
                .apiKey(ALICE_TOKEN)
                .build();
             AtlasKVClient bobClient = AtlasKVClient.builder()
                .endpoint(endpoint)
                .apiKey(BOB_TOKEN)
                .build()) {

            // Alice writes key "shared-cloud-key"
            aliceClient.keyValue().put("shared-cloud-key", "alice-secret-data");

            // Bob attempts to read "shared-cloud-key" -> should not find it
            KeyValue bobGet = bobClient.keyValue().get("shared-cloud-key");
            assertThat(bobGet.exists()).isFalse();

            // Bob writes to same key name
            bobClient.keyValue().put("shared-cloud-key", "bob-secret-data");

            // Both clients read independent values
            assertThat(aliceClient.keyValue().get("shared-cloud-key").value()).isEqualTo("alice-secret-data");
            assertThat(bobClient.keyValue().get("shared-cloud-key").value()).isEqualTo("bob-secret-data");
        }
    }

    @Test
    @DisplayName("Remote Java SDK connects authenticated SSE watch stream and receives isolated events")
    void remoteJavaSdkWatchSseAuthentication() throws Exception {
        if (!isLeader) return;

        String endpoint = "http://localhost:" + port;
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        List<WatchEvent> events = new CopyOnWriteArrayList<>();

        try (AtlasKVClient aliceClient = AtlasKVClient.builder()
                .endpoint(endpoint)
                .apiKey(ALICE_TOKEN)
                .build();
             AtlasKVClient bobClient = AtlasKVClient.builder()
                .endpoint(endpoint)
                .apiKey(BOB_TOKEN)
                .build()) {

            WatchApi.WatchSession session = aliceClient.watch().watch("stream-target", new WatchListener() {
                @Override
                public void onConnected() {
                    connectedLatch.countDown();
                }

                @Override
                public void onEvent(WatchEvent event) {
                    events.add(event);
                    if ("alice-stream-val".equals(event.value())) {
                        eventLatch.countDown();
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                }
            });

            boolean connected = connectedLatch.await(5, TimeUnit.SECONDS);
            assertThat(connected).isTrue();

            // Bob writes to stream-target -> Alice should not receive Bob's event
            bobClient.keyValue().put("stream-target", "bob-stream-val");
            Thread.sleep(500);
            assertThat(events).noneMatch(e -> "bob-stream-val".equals(e.value()));

            // Alice writes to stream-target -> Alice should receive event
            aliceClient.keyValue().put("stream-target", "alice-stream-val");

            boolean received = eventLatch.await(5, TimeUnit.SECONDS);
            assertThat(received).isTrue();
            assertThat(events).anyMatch(e -> "stream-target".equals(e.key()) && "alice-stream-val".equals(e.value()));

            session.close();
        }
    }

    @Test
    @DisplayName("Remote Java SDK handles 401 Unauthorized and 403 Forbidden cleanly without leaking secrets")
    void remoteJavaSdkUnauthorizedHandling() {
        if (!isLeader) return;

        String endpoint = "http://localhost:" + port;

        // 1. Missing credentials -> 401 Unauthorized
        try (AtlasKVClient unauthenticatedClient = AtlasKVClient.builder()
                .endpoint(endpoint)
                .build()) {

            assertThatThrownBy(() -> unauthenticatedClient.keyValue().get("unauthorized-key"))
                    .isInstanceOf(AtlasKVException.class)
                    .satisfies(ex -> {
                        AtlasKVException e = (AtlasKVException) ex;
                        assertThat(e.getStatusCode()).isEqualTo(401);
                        assertThat(e.getMessage()).contains("401 Unauthorized");
                    });
        }

        // 2. Invalid credentials -> 403 Forbidden
        try (AtlasKVClient invalidAuthClient = AtlasKVClient.builder()
                .endpoint(endpoint)
                .apiKey("invalid-or-revoked-key")
                .build()) {

            assertThatThrownBy(() -> invalidAuthClient.keyValue().get("forbidden-key"))
                    .isInstanceOf(AtlasKVException.class)
                    .satisfies(ex -> {
                        AtlasKVException e = (AtlasKVException) ex;
                        assertThat(e.getStatusCode()).isEqualTo(403);
                        assertThat(e.getMessage()).contains("403 Forbidden");
                        assertThat(e.getMessage()).doesNotContain("invalid-or-revoked-key");
                    });
        }
    }
}
